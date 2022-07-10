import os
import shlex
import argparse
import datetime
import subprocess
from typing import IO
from io import TextIOWrapper
from threading import Thread

from balancer_routine import balancer_routine
from balancer_routine import env_data


class Iperf_wrapper():

    def __init__(self, parameters: str = "-s -u", verbose: bool = False) -> None:
        self.threads: list = []
        self.iperf_waiting_thread: Thread = None
        self.iperf_process: subprocess.Popen = None

        self.verbose: bool = verbose
        self.is_started: bool = False
        self.iperf_parameters: str = parameters

    def __logger_thread(self, stream: IO, file: TextIOWrapper):
        def logger(stream: IO, file: TextIOWrapper):
            for stdout_line in iter(stream.readline, ""):
                file.writelines(stdout_line)
                file.flush()
                if self.verbose:
                    print(stdout_line.replace('\n', ""))
            stream.close()
            file.close()

        t = Thread(target=logger, args=(stream, file))
        t.daemon = True
        t.start()
        return t

    def __create_logs_stream(self):
        logs_dir = "iperf_logs"
        if not os.path.exists(logs_dir):
            try:
                os.mkdir(logs_dir)
            except OSError:
                print(f"Creation of the directory {logs_dir} failed")

        curr_datetime = datetime.datetime.now().strftime("%Y-%m-%d_%I-%M-%S")

        output_file = open(f"{logs_dir}/iperf_log-{curr_datetime}.txt", 'w')
        error_file = open(f"{logs_dir}/iperf_errors-{curr_datetime}.txt", 'w')
        return output_file, error_file

    def __waiting_thread(self):
        self.iperf_process.wait()
        return_code = self.iperf_process.poll()

        for t in self.threads:
            t.join()

        self.is_started = False
        balancer_routine.post_to_server(port=int(balancer_routine.env_data['IPERF_PORT']))
        print(f"iPerf stopped with status {return_code}")

    def start(self, port_iperf):
        if not self.is_started:
            output_file, error_file = self.__create_logs_stream()

            cmd = shlex.split("./iperf.elf " + '-p ' + port_iperf + ' ' + self.iperf_parameters)
            self.iperf_process = subprocess.Popen(
                cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, universal_newlines=True)
            print("iPerf is started")
            self.is_started = True

            self.iperf_waiting_thread = Thread(target=self.__waiting_thread)
            self.iperf_waiting_thread.start()

            self.threads = []
            if self.iperf_process.stdout is not None:
                self.threads.append(self.__logger_thread(
                    self.iperf_process.stdout, output_file))

            if self.iperf_process.stderr is not None:
                self.threads.append(self.__logger_thread(
                    self.iperf_process.stderr, error_file))

            return True
        else:
            return False

    def stop(self):
        self.iperf_process.terminate()
        self.iperf_waiting_thread.join()
        return_code = self.iperf_process.poll()

        return return_code


def read_env_data():
    env_data = {}
    env_data['SPEED_TEST_SERVICE_NAME'] = os.environ.get(
        'SPEED_TEST_SERVICE_NAME')
    env_data['SERVICE_IP_ADDRESS'] = os.environ.get('SERVICE_IP_ADDRESS')
    env_data['SERVICE_LOCATION'] = os.environ.get('SERVICE_LOCATION')
    env_data['BALANCER_ADDRESS'] = os.environ.get('BALANCER_ADDRESS')
    env_data['IPERF_PORT'] = os.getenv('IPERF_PORT', '5001')
    env_data['SERVICE_PORT'] = os.getenv('SERVICE_PORT', '5000')
    env_data['CONNECTING_TIMEOUT'] = os.getenv('CONNECTING_TIMEOUT', '30')
    return env_data


def create_arg_parser():
    parser = argparse.ArgumentParser()
    parser.add_argument('-V', '--verbose', action='store_true')
    parser.add_argument('-p', '--parameters', help="parameters for iPerf", type=str,
                        action="store", default='-s -u')

    return parser


if __name__ == "__main__":
    arg_parser = create_arg_parser()
    namespace = arg_parser.parse_args()

    env_data = read_env_data()
    for key, value in env_data.items():
        print(f'{key}: {value}')

    iperf_wrapper = Iperf_wrapper(namespace.parameters, True)
    iperf_wrapper.start(env_data['IPERF_PORT'])
    try:
        while True:
            pass
    except KeyboardInterrupt:
        iperf_wrapper.stop()
