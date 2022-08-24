import datetime

from django.core import validators
from django.db import transaction
from rest_framework import serializers
from django.utils.timezone import make_aware

import services.models as models


class ServerAddressResponseSerializer(serializers.ModelSerializer):
    class Meta:
        model = models.ServerAddress
        fields = ('ip', 'port', 'port_iperf', 'time')


class ServerAddressRequestSerializer(serializers.Serializer):
    ip = serializers.IPAddressField()
    port = serializers.IntegerField(
        validators=[
            validators.MinValueValidator(0),
            validators.MaxValueValidator(65535),
        ]
    )

    port_iperf = serializers.IntegerField(
        validators=[
            validators.MinValueValidator(0),
            validators.MaxValueValidator(65535),
        ]
    )

    def validate(self, attrs):
        return {'time': make_aware(datetime.datetime.now()), **attrs}

    @transaction.atomic
    def create(self, validated_data):
        service = models.ServerAddress.objects.update_or_create(
            ip=validated_data['ip'],
            port=validated_data['port'],
            port_iperf=validated_data['port_iperf'],
            defaults={'acquired_by': None, **validated_data}
        )[0]

        models.FiveGstToken.objects.filter(acquired_service=service).update(acquired_service=None)

        return service

    def update(self, instance, validated_data):
        raise NotImplementedError('`update()` must not be used.')


class FiveGstTokenSerializer(serializers.ModelSerializer):
    class Meta:
        model = models.FiveGstToken
        fields = ('token', 'expires_at')
