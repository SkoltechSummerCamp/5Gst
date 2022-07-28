import abc
import atexit
import logging
import sys
from threading import Thread, Event, RLock
from typing import Optional

from django.conf import settings


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


class WatchdogService(abc.ABC):
    def __init__(self, interval_seconds: float, timer_mode: bool = False):
        self._watchdog: Optional[Watchdog] = None
        self._lock: RLock = RLock()
        self._interval_seconds = interval_seconds
        self._timer_mode = timer_mode

        self._logger = logging.getLogger(f"{__name__}__{self.__class__.__name__}")
        self._atexit_registered = False

    def start(self, stop_timeout_seconds: float = settings.WATCHDOG_STOP_TIMEOUT_SECONDS):
        with self._lock:
            if not self._atexit_registered:
                self._atexit_registered = True
                atexit.register(self.__stop_at_exit)

            self.stop(stop_timeout_seconds)
            self._logger.info(f"Starting watchdog...")
            self._watchdog = Watchdog(self._interval_seconds, self.__private_on_watchdog_timeout, daemon=True)

            if self._timer_mode:
                self._watchdog.reset_timer()
            self._watchdog.start()
            self._logger.info("Successfully started watchdog ")

    def stop(self, timeout_seconds: float = settings.WATCHDOG_STOP_TIMEOUT_SECONDS):
        with self._lock:
            if self._watchdog is not None:
                self._logger.info("Stopping watchdog...")
                self._watchdog.stop()

                self._watchdog.join(timeout=timeout_seconds)
                if self._watchdog.is_alive():
                    self._logger.error(f"Watchdog was not stopped after {timeout_seconds} seconds")
                else:
                    self._logger.info("Successfully stopped watchdog")

                self._watchdog = None

    def reset_timer(self):
        with self._lock:
            if self._watchdog is not None:
                self._watchdog.reset_timer()

    def __private_on_watchdog_timeout(self):
        with self._lock:
            self._logger.debug("Handling watchdog timeout...")
            self._on_watchdog_timeout()
            if self._timer_mode:
                self._watchdog.stop()
            self._logger.debug("Watchdog timeout was handled")

    def __stop_at_exit(self):
        with self._lock:
            self._logger.info(f"Stopping watchdog due to application tear down...")
            self.stop()
            self._close_resources()
            self._logger.info(f"Successfully stopped watchdog")
            sys.exit(0)

    @abc.abstractmethod
    def _on_watchdog_timeout(self):
        pass

    def _close_resources(self):
        # no resources acquired by default
        pass
