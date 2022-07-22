from django.conf import settings
from rest_framework.decorators import api_view
from rest_framework.request import Request
from rest_framework.response import Response
from rest_framework.status import HTTP_200_OK

from apps.logic.balancer_communicator import balancer_communicator
from apps.logic.iperf_wrapper import iperf
from apps.logic.watchdog import watchdog_service


@api_view(['GET'])
def start_iperf(request: Request):
    watchdog_service.reset_timer()
    iperf_parameters = request.query_params.get("args")
    if iperf_parameters is not None:
        iperf.iperf_parameters = iperf_parameters

    status = iperf.start(port_iperf=settings.IPERF_PORT)
    if status:
        print(f"iPerf started with parameters {iperf.iperf_parameters}")
        return Response(data=f"iPerf started with parameters {iperf.iperf_parameters}",
                        status=HTTP_200_OK,
                        content_type="text/html")

    iperf.stop()
    status = iperf.start(port_iperf=settings.IPERF_PORT)
    if status:
        return Response(f"iPerf restarted with parameters {iperf.iperf_parameters}")

    return Response("Failed to start iperf")


@api_view(['GET'])
def stop_iperf(request: Request):

    if iperf.is_started:
        status = iperf.stop()
        balancer_communicator.register_service()
        watchdog_service.reset_timer()
        return Response(f"iPerf stopped with status {status}")

    return Response("iPerf already stopped")
