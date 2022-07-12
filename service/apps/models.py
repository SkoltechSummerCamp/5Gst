from django.core import validators
from django.db import models


class ServerAddress(models.Model):  # For future purposes
    ip = models.GenericIPAddressField()
    port = models.IntegerField(
        validators=[
            validators.MinValueValidator(0),
            validators.MaxValueValidator(65535),
        ]
    )


class BalancerAddress(ServerAddress):
    pass
