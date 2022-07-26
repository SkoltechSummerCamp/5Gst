import atexit
import logging
import signal
import sys
from threading import Thread, Event, RLock
from typing import Optional

from django.conf import settings
from urllib3.exceptions import MaxRetryError

from apps.logic.balancer_communicator import balancer_communicator
from apps.logic.iperf_wrapper import iperf

logger = logging.getLogger(__name__)


class Watchdog(Thread):
    def __init__(self, interval, function, args=None, kwargs=None, *thread_args, **thread_kwargs):
        super(Watchdog, self).__init__(*thread_args, **thread_kwargs)
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
        atexit.register(self._stop_at_exit)

    def start(self, stop_timeout_seconds: float = settings.WATCHDOG_STOP_TIMEOUT_SECONDS):
        with self._lock:
            self.stop(stop_timeout_seconds)
            logger.info("Starting watchdog...")
            self._watchdog = Watchdog(self._interval_seconds, self._on_watchdog_timeout, daemon=True)
            self._watchdog.start()
            logger.info("Successfully started watchdog ")

    def stop(self, timeout_seconds: float = settings.WATCHDOG_STOP_TIMEOUT_SECONDS):
        with self._lock:
            if self._watchdog is not None:
                logger.info("Stopping watchdog...")
                self._watchdog.stop()

                self._watchdog.join(timeout=timeout_seconds)
                if self._watchdog.is_alive():
                    logger.error(f"Watchdog was not stopped after {timeout_seconds} seconds")
                else:
                    logger.info("Successfully stopped watchdog")

                self._watchdog = None

    def reset_timer(self):
        with self._lock:
            if self._watchdog is not None:
                self._watchdog.reset_timer()

    def _on_watchdog_timeout(self):
        with self._lock:
            logger.debug("Handling watchdog timeout...")
            if iperf.is_started:
                iperf.stop()
            balancer_communicator.register_service()
            logger.debug("Watchdog timeout was handled")

    def _stop_at_exit(self):
        with self._lock:
            logger.info(f"Unregistering service due to application tear down...")
            self.stop()
            try:
                balancer_communicator.unregister_service()
            except MaxRetryError as e:
                logger.error("Could not unregister service due to error", exc_info=e)
            else:
                logger.info(f"Successfully unregistered service")
            sys.exit(0)


watchdog_service = BalancerCommunicationWatchdogService(settings.CONNECTING_TIMEOUT)
