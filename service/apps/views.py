import logging

from django.conf import settings
from drf_yasg import openapi
from drf_yasg.utils import swagger_auto_schema
from rest_framework import status
from rest_framework.request import Request
from rest_framework.response import Response
from rest_framework.views import APIView

from apps import serializers
from apps.logic.balancer_communicator import balancer_communicator
from apps.logic.iperf_wrapper import iperf
from apps.logic.session_web_service import session_web_service, SessionWebService
from apps.logic.watchdog_service import balancer_communication_watchdog_service

logger = logging.getLogger(__name__)


class StartIperfViewV1(APIView):
    @swagger_auto_schema(
        operation_id='start_iperf_old',
        manual_parameters=[
            openapi.Parameter(
                "args",
                openapi.IN_QUERY,
                type=openapi.TYPE_STRING,
                description="Iperf args used to run service iperf",
            ),
        ],
        deprecated=True,
    )
    def get(self, request: Request):
        balancer_communication_watchdog_service.reset_timer()
        iperf_parameters = request.query_params.get("args")
        if iperf_parameters is not None:
            iperf.iperf_parameters = iperf_parameters

        iperf_status = iperf.start(port_iperf=settings.IPERF_PORT)
        if iperf_status:
            logger.info(f"iPerf started with parameters {iperf.iperf_parameters}")
            return Response(data=f"iPerf started with parameters {iperf.iperf_parameters}",
                            status=status.HTTP_200_OK,
                            content_type="text/html")

        iperf.stop()
        iperf_status = iperf.start(port_iperf=settings.IPERF_PORT)
        if iperf_status:
            return Response(f"iPerf restarted with parameters {iperf.iperf_parameters}")

        return Response("Failed to start iperf")


class StopIperfViewV1(APIView):
    @swagger_auto_schema(
        operation_id='stop_iperf_old',
        deprecated=True,
    )
    def get(self, request: Request):
        if iperf.is_started:
            status = iperf.stop()
            balancer_communicator.register_service()
            balancer_communication_watchdog_service.reset_timer()
            return Response(f"iPerf stopped with status {status}")

        return Response("iPerf already stopped")


class StartSessionView(APIView):
    @SessionWebService.start_session_swagger_auto_schema
    def post(self, request: Request):
        return session_web_service.start_session()


class StopSessionView(APIView):
    @SessionWebService.stop_session_swagger_auto_schema
    def post(self, request: Request):
        return session_web_service.stop_session()


class StartIperfView(APIView):
    @SessionWebService.start_iperf_swagger_auto_schema
    def post(self, request: Request):
        serializer = serializers.IperfArgsSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        iperf_args = serializer.validated_data['iperf_args']
        iperf_args = iperf_args if iperf_args is not None else ''
        return session_web_service.start_iperf(iperf_args)


class StopIperfView(APIView):
    @SessionWebService.stop_iperf_swagger_auto_schema
    def post(self, request: Request):
        return session_web_service.stop_iperf()
