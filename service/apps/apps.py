import signal
import sys

from balancer_communicator import balancer_communicator
from django.apps import AppConfig
from iperf_wrapper import iperf
from watchdog import Watchdog

watchdog = Watchdog(5)


class MyAppConfig(AppConfig):
    name = 'apps'

    def ready(self):
        if 'runserver' not in sys.argv:
            return True

        def TimeoutHandler():
            if iperf.is_started:
                iperf.stop()
            balancer_communicator.post_to_server()
            watchdog.reset()

        def signal_handler(sig, frame):
            watchdog.stop()
            balancer_communicator.delete_from_server()
            sys.exit(0)

        signal.signal(signal.SIGINT, signal_handler)

        for key, value in balancer_communicator.env_data.items():
            print(f'{key}: {value}')

        global watchdog
        watchdog = Watchdog(int(balancer_communicator.env_data['CONNECTING_TIMEOUT']), TimeoutHandler)
        balancer_communicator.post_to_server()
