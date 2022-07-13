import swagger_client
from swagger_client.rest import ApiException
from django.conf import settings
from swagger_client.configuration import Configuration


class BalancerCommunicator:
    def __init__(self):
        self.api_instance = swagger_client.ServiceApi()
        self.env_data = {
            'SERVICE_IP_ADDRESS': settings.SERVICE_IP_ADDRESS,
            'BALANCER_ADDRESS': settings.BALANCER_ADDRESS,
            'BALANCER_BASE_URL': settings.BALANCER_BASE_URL,
            'IPERF_PORT': settings.IPERF_PORT,
            'SERVICE_PORT': settings.SERVICE_PORT,
            'CONNECTING_TIMEOUT': settings.CONNECTING_TIMEOUT
        }
        configuration = Configuration()
        configuration.host = 'http://' + self.env_data['BALANCER_ADDRESS'] + self.env_data['BALANCER_BASE_URL']
        self.api_instance.api_client.configuration = configuration

    def post_to_server(self):
        body = swagger_client.ServerAddressRequest(ip=self.env_data['SERVICE_IP_ADDRESS'],
                                                   port=self.env_data['SERVICE_PORT'],
                                                   port_iperf=self.env_data['IPERF_PORT'])
        try:
            self.api_instance.service_create(data=body)
        except ApiException as e:
            print("Exception when calling ServerApi->server_post_ip: %s\n" % e)

    def delete_from_server(self):
        body = swagger_client.ServerAddressRequest(ip=self.env_data['SERVICE_IP_ADDRESS'],
                                                   port=self.env_data['SERVICE_PORT'],
                                                   port_iperf=self.env_data['IPERF_PORT'])
        try:
            self.api_instance.service_delete(data=body)
        except ApiException as e:
            print("Exception when calling ServerApi->server_delete_ip: %s\n" % e)


balancer_communicator = BalancerCommunicator()
