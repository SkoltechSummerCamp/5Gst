import os
import shlex
import argparse
import datetime
import subprocess
import sys
from typing import IO
from io import TextIOWrapper
from threading import Thread


class IperfWrapper:

    def __init__(self, parameters: str = "-s -u", verbose: bool = False) -> None:
        self.threads: list = []
        self.iperf_waiting_thread: Thread = None
        self.iperf_process: subprocess.Popen = None

        self.verbose: bool = verbose
        self.is_started: bool = False
        self.iperf_parameters: str = parameters
        cmd = ["./iperf.elf", '--version']  # TODO write version to logs
        iperf_version_process = subprocess.Popen(
            cmd, stdout=sys.stdout, stderr=sys.stderr, universal_newlines=True)
        iperf_version_process.wait()

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
        print(f"iPerf stopped with status {return_code}")

    def start(self, port_iperf):
        if not self.is_started:
            output_file, error_file = self.__create_logs_stream()

            cmd = shlex.split("./iperf.elf " + '-p ' + str(port_iperf) + ' ' + self.iperf_parameters)
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


def create_arg_parser():
    parser = argparse.ArgumentParser()
    parser.add_argument('-V', '--verbose', action='store_true')
    parser.add_argument('-p', '--parameters', help="parameters for iPerf", type=str,
                        action="store", default='-s -u')
    parser.add_argument('-P', '--port', help=" iPerf port", type=str,
                        action="store", default='-p 5005')
    return parser


iperf: IperfWrapper = IperfWrapper(verbose=True)

if __name__ == "__main__":
    arg_parser = create_arg_parser()
    namespace = arg_parser.parse_args()

    print('Params ' + namespace.parameters)
    iperf_wrapper = IperfWrapper(namespace.parameters, True)
    iperf_wrapper.start(namespace.port)
    try:
        while True:
            pass
    except KeyboardInterrupt:
        iperf_wrapper.stop()
