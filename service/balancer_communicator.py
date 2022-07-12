from __future__ import print_function
import swagger_client
from swagger_client.rest import ApiException
from pprint import pprint


class BalancerCommunicator:
    def __init__(self):
        # create an instance of the API class
        self.api_instance = swagger_client.ServiceApi()
        self.env_data = {}

    def post_to_server(self, port=5000, port_iperf=5001):
        body = swagger_client.ServerAddressRequest(ip=self.env_data['SERVICE_IP_ADDRESS'], port=port, port_iperf=port_iperf) # ServerAddr | port of iperf server. Ip and time could be emply (optional)
        try:
            # post self ip to balancer
            self.api_instance.service_create(data=body)
        except ApiException as e:
            print("Exception when calling ServerApi->server_post_ip: %s\n" % e)

    def delete_from_server(self, port=5000, port_iperf=5001):
        body = swagger_client.ServerAddressRequest(ip=self.env_data['SERVICE_IP_ADDRESS'], port=port, port_iperf=port_iperf) # ServerAddr | port of iperf server. Ip and time could be emply (optional)
        try:
            # delete server IP
            api_response = self.api_instance.service_delete(data=body)
            pprint(api_response)
        except ApiException as e:
            print("Exception when calling ServerApi->server_delete_ip: %s\n" % e)


env_data = {}
balancer_communicator = BalancerCommunicator()
