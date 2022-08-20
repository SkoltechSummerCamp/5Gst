import logging

import urllib3.util
from django.conf import settings
from django.db import transaction
from drf_yasg import openapi
from drf_yasg.utils import swagger_auto_schema
from rest_framework import mixins, status
from rest_framework.generics import GenericAPIView, get_object_or_404
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

import service_api
from services import serializers, models
from services.authentication import FiveGstAuthentication

logger = logging.getLogger(__name__)


class ServiceRegistrationView(mixins.DestroyModelMixin,
                              mixins.CreateModelMixin,
                              GenericAPIView):
    permission_classes = [IsAuthenticated]
    authentication_classes = [FiveGstAuthentication]
    serializer_class = serializers.ServerAddressRequestSerializer

    def get_queryset(self):
        serializer = self.get_serializer(data=self.request.data)
        serializer.is_valid(raise_exception=True)
        data = serializer.validated_data
        return models.ServerAddress.objects.filter(ip=data['ip'],
                                                   port=data['port'],
                                                   port_iperf=data['port_iperf'])

    def get_object(self):
        return get_object_or_404(self.get_queryset())

    @swagger_auto_schema(
        operation_description='Register caller as service',
        operation_id='register_service',
        responses={
            201: openapi.Response('Service registered', serializers.ServerAddressRequestSerializer),
            400: openapi.Response('Invalid request body'),
        },
    )
    def post(self, request, *args, **kwargs):
        return self.create(request, *args, **kwargs)

    @swagger_auto_schema(
        operation_description='Unregister caller as service',
        request_body=serializers.ServerAddressRequestSerializer,
        operation_id='unregister_service',
        responses={
            204: openapi.Response('Service unregistered', serializers.ServerAddressRequestSerializer),
            400: openapi.Response('Invalid request body'),
            404: openapi.Response('Service was not found'),
        },
    )
    def delete(self, request, *args, **kwargs):
        return self.destroy(request, *args, **kwargs)


class ServiceAcquirementView(APIView):
    permission_classes = [IsAuthenticated]
    authentication_classes = [FiveGstAuthentication]

    @swagger_auto_schema(
        operation_description='Acquires service for further iperf tests',
        operation_id='acquire_service',
        responses={
            200: openapi.Response('Service acquired', serializers.ServerAddressResponseSerializer),
            503: openapi.Response('No available services found'),
        },
    )
    @transaction.atomic
    def post(self, request, *args, **kwargs):
        acquired_address = models.ServerAddress.objects.select_for_update(skip_locked=True).first()
        if not acquired_address:
            return Response(status=status.HTTP_503_SERVICE_UNAVAILABLE)

        base_url = urllib3.util.Url(scheme=settings.SERVICE_URL_SCHEME,
                                    host=acquired_address.ip,
                                    port=acquired_address.port).url
        logger.info(f"Built base url {base_url} for acquired service")
        configuration = service_api.Configuration()
        configuration.host = base_url
        api_instance = service_api.ServiceApi(service_api.ApiClient(configuration))
        api_instance.start_session()

        acquired_address.delete()
        serializer = serializers.ServerAddressResponseSerializer(instance=acquired_address)
        return Response(serializer.data, status=status.HTTP_200_OK)


class PingView(APIView):
    permission_classes = []
    authentication_classes = [FiveGstAuthentication]

    @swagger_auto_schema(
        operation_description='Check that server is up',
        operation_id='ping',
    )
    def get(self, request):
        return Response(status=status.HTTP_200_OK)
