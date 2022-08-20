from django.core import validators
from django.db import models


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

    class Meta:
        unique_together = ('ip', 'port', 'port_iperf')


class FiveGstToken(models.Model):
    token = models.CharField(max_length=64, unique=True)
    expires_at = models.DateTimeField()
    reserved_service = models.ForeignKey(ServerAddress,
                                         null=True,
                                         blank=True,
                                         on_delete=models.SET_NULL,
                                         related_name='anonymously_reserved_by')

    class Meta:
        indexes = [
            models.Index(fields=('expires_at',))
        ]
