from rest_framework.status import HTTP_200_OK
from rest_framework.decorators import api_view
from rest_framework.request import Request
from rest_framework.response import Response
from iperf_wrapper import iperf
from balancer_communicator import balancer_communicator
from apps.apps import watchdog


@api_view(['GET'])
def start_iperf(request: Request):
    watchdog.reset()
    iperf_parameters = request.query_params.get("args")
    if iperf_parameters is not None:
        iperf.iperf_parameters = iperf_parameters

    status = iperf.start(port_iperf=balancer_communicator.env_data['IPERF_PORT'])
    if status:
        print(f"iPerf started with parameters {iperf.iperf_parameters}")
        return Response(data=f"iPerf started with parameters {iperf.iperf_parameters}", status=HTTP_200_OK,
                        content_type="text/html")

    iperf.stop()
    status = iperf.start(port_iperf=balancer_communicator.env_data['IPERF_PORT'])
    if status:
        return Response(f"iPerf restarted with parameters {iperf.iperf_parameters}")

    return Response("Failed to start iperf")


@api_view(['GET'])
def stop_iperf(request: Request):

    if iperf.is_started:
        status = iperf.stop()
        balancer_communicator.post_to_server()
        watchdog.reset()
        return Response(f"iPerf stopped with status {status}")

    return Response("iPerf already stopped")
