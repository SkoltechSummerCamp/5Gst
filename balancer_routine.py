from __future__ import print_function
import time
import swagger_client
from swagger_client.rest import ApiException
from pprint import pprint
from threading import Timer
import sys

class BalancerRoutine:
    def __init__(self):
        # create an instance of the API class
        self.api_instance = swagger_client.ServiceApi()

    def post_to_server(self, port=5000, port_iperf=5001):
        body = swagger_client.ServerAddressRequest(ip=self.env_data['SERVICE_IP_ADDRESS'], port=port, port_iperf=port_iperf) # ServerAddr | port of iperf server. Ip and time could be emply (optional)
        try:
            # post self ip to balancer
            self.api_instance.service_create(body=body)
        except ApiException as e:
            print("Exception when calling ServerApi->server_post_ip: %s\n" % e)

    def delete_from_server(self, port=5000, port_iperf=5001):
        body = swagger_client.ServerAddressRequest(ip=self.env_data['SERVICE_IP_ADDRESS'], port=port, port_iperf=port_iperf) # ServerAddr | port of iperf server. Ip and time could be emply (optional)
        try:
            # delete server IP
            api_response = self.api_instance.service_delete(body=body)
            pprint(api_response)
        except ApiException as e:
            print("Exception when calling ServerApi->server_delete_ip: %s\n" % e)

class Watchdog(Exception):
    def __init__(self, timeout, userHandler=None):  # timeout in seconds
        self.timeout = timeout
        self.handler = userHandler if userHandler is not None else self.defaultHandler
        self.timer = Timer(self.timeout, self.handler)
        self.timer.start()
        

    def reset(self):
        self.timer.cancel()
        self.timer = Timer(self.timeout, self.handler)
        self.timer.start()

    def stop(self):
        self.timer.cancel()

    def __del__(self):
        self.timer.cancel()

    def defaultHandler(self):
        return 

env_data = {}
balancer_routine = BalancerRoutine()