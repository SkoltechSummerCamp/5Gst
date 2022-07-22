import signal
import sys
from threading import Thread, Event, RLock
from typing import Optional

from django.conf import settings

from apps.logic.balancer_communicator import balancer_communicator
from apps.logic.iperf_wrapper import iperf


class Watchdog(Thread):
    def __init__(self, interval, function, args=None, kwargs=None):
        super(Watchdog, self).__init__()
        self._interval = interval
        self._function = function
        self._args = args if args is not None else []
        self._kwargs = kwargs if kwargs is not None else {}
        self._timer_is_reset = Event()
        self._finished = Event()

    def stop(self):
        self._finished.set()
        self._timer_is_reset.set()

    def reset_timer(self):
        self._timer_is_reset.set()

    def run(self):
        while not self._finished.is_set():
            if self._timer_is_reset.is_set():
                self._timer_is_reset.clear()
            else:
                self._function(*self._args, **self._kwargs)

            self._timer_is_reset.wait(self._interval)


class BalancerCommunicationWatchdogService:
    def __init__(self, interval_seconds: float):
        self._watchdog: Optional[Watchdog] = None
        self._lock: RLock = RLock()
        self._interval_seconds = interval_seconds
        signal.signal(signal.SIGINT, self._on_interruption)

    def start(self, stop_timeout_seconds: float = settings.WATCHDOG_STOP_TIMEOUT_SECONDS):
        with self._lock:
            self.stop(stop_timeout_seconds)
            print("Starting watchdog...")
            self._watchdog = Watchdog(self._interval_seconds, self._on_watchdog_timeout)
            self._watchdog.start()
            print("Successfully started watchdog ")

    def stop(self, timeout_seconds: float = settings.WATCHDOG_STOP_TIMEOUT_SECONDS):
        with self._lock:
            if self._watchdog is not None:
                print("Stopping watchdog...")
                self._watchdog.stop()

                self._watchdog.join(timeout=timeout_seconds)
                if self._watchdog.is_alive():
                    print(f"WARN: watchdog was not stopped after {timeout_seconds} seconds")
                else:
                    print("Successfully stopped watchdog")

                self._watchdog = None

    def reset_timer(self):
        with self._lock:
            if self._watchdog is not None:
                self._watchdog.reset_timer()

    def _on_watchdog_timeout(self):
        with self._lock:
            print("Handling watchdog timeout...")
            if iperf.is_started:
                iperf.stop()
            balancer_communicator.register_service()
            print("Watchdog timeout was handled")

    def _on_interruption(self, sig, frame):
        with self._lock:
            print(f"Stopping watchdog due to signal #{sig}...")
            self.stop()
            balancer_communicator.unregister_service()
            print(f"Successfully handled signal #{sig}")
            sys.exit(0)


watchdog_service = BalancerCommunicationWatchdogService(settings.CONNECTING_TIMEOUT)
