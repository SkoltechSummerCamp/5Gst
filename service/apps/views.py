import logging

from rest_framework.request import Request
from rest_framework.views import APIView

from apps import serializers
from apps.logic.session_web_service import session_web_service, SessionWebService

logger = logging.getLogger(__name__)


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
