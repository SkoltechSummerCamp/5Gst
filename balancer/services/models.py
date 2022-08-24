import logging

import urllib3
from django.conf import settings
from django.core import validators
from django.db import models

import service_api

logger = logging.getLogger(__name__)


class ServerAddress(models.Model):
    ip = models.GenericIPAddressField()
    port = models.IntegerField(
        validators=[
            validators.MinValueValidator(0),
            validators.MaxValueValidator(65535),
        ]
    )
    port_iperf = models.IntegerField(
        validators=[
            validators.MinValueValidator(0),
            validators.MaxValueValidator(65535),
        ]
    )
    time = models.DateTimeField()

    def to_api_instance(self):
        base_url = urllib3.util.Url(scheme=settings.SERVICE_URL_SCHEME,
                                    host=self.ip,
                                    port=self.port).url
        logger.info(f"Built base url {base_url} for service {self.id}")
        configuration = service_api.Configuration()
        configuration.host = base_url
        return service_api.ServiceApi(service_api.ApiClient(configuration))

    class Meta:
        unique_together = ('ip', 'port', 'port_iperf')


class FiveGstToken(models.Model):
    token = models.CharField(max_length=64, unique=True)
    expires_at = models.DateTimeField()
    acquired_service = models.OneToOneField(ServerAddress,
                                            on_delete=models.SET_NULL,
                                            null=True,
                                            blank=True,
                                            related_name='acquired_by')

    class Meta:
        indexes = [
            models.Index(fields=('expires_at',))
        ]
