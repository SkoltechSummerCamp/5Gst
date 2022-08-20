import datetime
import logging
import secrets

from django.conf import settings
from django.db import transaction, IntegrityError
from django.utils.timezone import make_aware
from drf_yasg import openapi
from drf_yasg.utils import swagger_auto_schema
from rest_framework import mixins, status
from rest_framework.generics import GenericAPIView, get_object_or_404
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

import service_api.rest
from services import serializers, models
from services.authentication import FiveGstAuthentication
from services.models import FiveGstToken

logger = logging.getLogger(__name__)


class FiveGstLoginView(APIView):
    permission_classes = []
    authentication_classes = [FiveGstAuthentication]

    @swagger_auto_schema(
        operation_description='Log in to 5Gst service',
        operation_id='login',
        responses={
            201: openapi.Response('Authentication succeeded', serializers.FiveGstTokenSerializer),
            500: openapi.Response('Could not generate token, please try again'),
        },
        security=[],
    )
    def post(self, request):
        try:
            token = FiveGstToken.objects.create(
                token=secrets.token_hex(32),
                expires_at=make_aware(datetime.datetime.now()
                                      + datetime.timedelta(seconds=settings.FIVE_GST_TOKEN_LIFETIME_SECONDS)),
            )
        except IntegrityError:
            return Response('Could not generate token, please try again', status=status.HTTP_500_INTERNAL_SERVER_ERROR)

        response = serializers.FiveGstTokenSerializer(instance=token).data
        return Response(response, status=status.HTTP_201_CREATED)


class FiveGstLogoutView(APIView):
    permission_classes = [IsAuthenticated]
    authentication_classes = [FiveGstAuthentication]

    @swagger_auto_schema(
        operation_description='Log out from 5Gst service',
        operation_id='logout',
        responses={
            200: openapi.Response('Logged out successfully'),
            502: openapi.Response('Could not stop session on service'),
        },
    )
    def post(self, request):
        acquired_service = request.user.token.acquired_service
        if acquired_service is not None:
            try:
                acquired_service.to_api_instance().stop_session()
            except service_api.rest.ApiException as e:
                logger.error('Could not stop session on service', exc_info=e)
                return Response('Could not stop session on service', status=status.HTTP_502_BAD_GATEWAY)

        request.user.token.delete()
        return Response("Logged out successfully", status=status.HTTP_200_OK)


class ServiceRegistrationView(mixins.DestroyModelMixin,
                              mixins.CreateModelMixin,
                              GenericAPIView):
    # TODO add service authentication
    # permission_classes = [IsAuthenticated]
    permission_classes = []
    authentication_classes = []
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
        security=[],
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
        security=[],
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
            409: openapi.Response('Could not acquire service due to a conflict, try again',
                                  serializers.ServerAddressResponseSerializer),
            503: openapi.Response('No available services found'),
        },
    )
    @transaction.atomic
    def post(self, request, *args, **kwargs):
        if request.user.token.acquired_service is not None:
            acquired_address = request.user.token.acquired_service
        else:
            acquired_address = models.ServerAddress.objects.select_for_update(skip_locked=True).first()
            if not acquired_address:
                return Response(status=status.HTTP_503_SERVICE_UNAVAILABLE)

            was_acquired = models.FiveGstToken.objects\
                .filter(token=request.user.token.token, acquired_service__isnull=True)\
                .update(acquired_service=acquired_address)
            if not was_acquired:
                return Response('Could not acquire service due to a conflict, try again',
                                status=status.HTTP_409_CONFLICT)

        acquired_address.to_api_instance().start_session()

        serializer = serializers.ServerAddressResponseSerializer(instance=acquired_address)
        return Response(serializer.data, status=status.HTTP_200_OK)


class PingView(APIView):
    permission_classes = []
    authentication_classes = [FiveGstAuthentication]

    @swagger_auto_schema(
        operation_description='Check that server is up',
        operation_id='ping',
        security=[],
    )
    def get(self, request):
        return Response(status=status.HTTP_200_OK)
