import datetime

from django.core import validators
from rest_framework import serializers

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
        return {'time': datetime.datetime.utcnow(), **attrs}

    def create(self, validated_data):
        return models.ServerAddress.objects.create(**validated_data)

    def update(self, instance, validated_data):
        pass

    def save(self, **kwargs):
        models.ServerAddress.objects.update_or_create(**self.data, defaults=self.validate(self.data))
