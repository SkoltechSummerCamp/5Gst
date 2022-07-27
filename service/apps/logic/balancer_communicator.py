import logging

from django.conf import settings

import balancer_api
from balancer_api.configuration import Configuration
from balancer_api.rest import ApiException

logger = logging.getLogger(__name__)


class BalancerCommunicator:
    def __init__(self):
        configuration = Configuration()
        configuration.host = settings.BALANCER_ADDRESS
        api_client = balancer_api.ApiClient(configuration=configuration)
        self.api_instance = balancer_api.BalancerApi(api_client=api_client)

    def register_service(self):
        body = balancer_api.ServerAddressRequest(ip=settings.SERVICE_IP_ADDRESS,
                                                 port=settings.SERVICE_PORT,
                                                 port_iperf=settings.IPERF_PORT)
        try:
            self.api_instance.register_service(data=body)
        except ApiException as e:
            logger.error("Exception when calling BalancerApi->register_service", exc_info=e)

    def unregister_service(self):
        body = balancer_api.ServerAddressRequest(ip=settings.SERVICE_IP_ADDRESS,
                                                 port=settings.SERVICE_PORT,
                                                 port_iperf=settings.IPERF_PORT)
        try:
            self.api_instance.unregister_service(data=body)
        except ApiException as e:
            logger.error("Exception when calling BalancerApi->unregister_service", exc_info=e)


balancer_communicator = BalancerCommunicator()
