from django.conf import settings
from urllib3.exceptions import MaxRetryError

from apps.logic.balancer_communicator import balancer_communicator
from apps.logic.iperf_wrapper import iperf
from apps.logic.watchdog import WatchdogService


class BalancerCommunicationWatchdogService(WatchdogService):
    def __init__(self, interval_seconds: float = settings.BALANCER_REGISTRATION_INTERVAL_SECONDS):
        super(BalancerCommunicationWatchdogService, self).__init__(interval_seconds=interval_seconds)

    def _on_watchdog_timeout(self):
        balancer_communicator.register_service()

    def _close_resources(self):
        try:
            balancer_communicator.unregister_service()
        except MaxRetryError as e:
            self._logger.error("Could not unregister service due to error", exc_info=e)
        else:
            self._logger.info("Successfully unregistered service")


class IperfStopWatchdogService(WatchdogService):
    def __init__(self, interval_seconds: float = settings.IPERF_MEASUREMENT_MAX_TIME_SECONDS):
        super(IperfStopWatchdogService, self).__init__(interval_seconds=interval_seconds, timer_mode=True)

    def _on_watchdog_timeout(self):
        iperf.stop()


balancer_communication_watchdog_service = BalancerCommunicationWatchdogService()

iperf_stop_watchdog_service = IperfStopWatchdogService()
