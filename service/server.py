from flask import Flask
from flask import request
from iperf_wrapper import *

from balancer_routine import balancer_routine
from balancer_routine import Watchdog
from balancer_routine import env_data

import signal
import sys

app = Flask(__name__)

iperf: Iperf_wrapper = Iperf_wrapper(verbose=True)


@app.route("/start-iperf", methods=['GET'])
def start_iperf_binary():
    global iperf
    watchdog.reset()

    iperf_parameters = request.args.get("args")
    if iperf_parameters is not None:
        iperf.iperf_parameters = iperf_parameters

    status = iperf.start(port_iperf=balancer_routine.env_data['IPERF_PORT'])
    if status:
        return f"iPerf started with parameters {iperf.iperf_parameters}"

    iperf.stop()
    status = iperf.start(port_iperf=balancer_routine.env_data['IPERF_PORT'])
    if status:
        return f"iPerf restarted with parameters {iperf.iperf_parameters}"

    return "Error"


@app.route("/stop-iperf", methods=['GET'])
def stop_iperf():
    global iperf

    if iperf.is_started:
        status = iperf.stop()
        balancer_routine.post_to_server(port=int(balancer_routine.env_data['SERVICE_PORT']), port_iperf=int(balancer_routine.env_data['IPERF_PORT']))
        watchdog.reset()
        return str(f"iPerf stopped with status {status}")

    return "iPerf already stopped"

def TimeoutHandler():
    if iperf.is_started:
        status = iperf.stop()
    balancer_routine.post_to_server(port=int(balancer_routine.env_data['SERVICE_PORT']), port_iperf=int(balancer_routine.env_data['IPERF_PORT']))
    watchdog.reset()

def signal_handler(sig, frame):
    watchdog.stop()
    balancer_routine.delete_from_server(port=int(balancer_routine.env_data['SERVICE_PORT']), port_iperf=int(balancer_routine.env_data['IPERF_PORT']))
    sys.exit(0)

signal.signal(signal.SIGINT, signal_handler)


balancer_routine.env_data = read_env_data() # read env variables
for key, value in balancer_routine.env_data.items(): # print env variables
    print(f'{key}: {value}')

watchdog = Watchdog(int(balancer_routine.env_data['CONNECTING_TIMEOUT']), TimeoutHandler) # start watchdog
balancer_routine.post_to_server(port=int(balancer_routine.env_data['SERVICE_PORT']), port_iperf=int(balancer_routine.env_data['IPERF_PORT']))


app.run(host="0.0.0.0", port=balancer_routine.env_data['SERVICE_PORT'])


