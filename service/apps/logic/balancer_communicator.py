import logging

from django.conf import settings

import swagger_client
from swagger_client.configuration import Configuration
from swagger_client.rest import ApiException

logger = logging.getLogger(__name__)


class BalancerCommunicator:
    def __init__(self):
        configuration = Configuration()
        configuration.host = settings.BALANCER_ADDRESS
        api_client = swagger_client.ApiClient(configuration=configuration)
        self.api_instance = swagger_client.BalancerApi(api_client=api_client)

    def register_service(self):
        body = swagger_client.ServerAddressRequest(ip=settings.SERVICE_IP_ADDRESS,
                                                   port=settings.SERVICE_PORT,
                                                   port_iperf=settings.IPERF_PORT)
        try:
            self.api_instance.register_service(data=body)
        except ApiException as e:
            logger.error("Exception when calling BalancerApi->register_service", exc_info=e)

    def unregister_service(self):
        body = swagger_client.ServerAddressRequest(ip=settings.SERVICE_IP_ADDRESS,
                                                   port=settings.SERVICE_PORT,
                                                   port_iperf=settings.IPERF_PORT)
        try:
            self.api_instance.unregister_service(data=body)
        except ApiException as e:
            logger.error("Exception when calling BalancerApi->unregister_service", exc_info=e)


balancer_communicator = BalancerCommunicator()
