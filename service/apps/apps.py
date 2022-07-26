import logging
import os

from django.apps import AppConfig
from django.core.management.commands import diffsettings
from django.utils.autoreload import DJANGO_AUTORELOAD_ENV

from apps.logic.watchdog import watchdog_service

logger = logging.getLogger(__name__)


class MyAppConfig(AppConfig):
    name = 'apps'

    def ready(self):
        if os.getenv(DJANGO_AUTORELOAD_ENV) != 'true':
            self.print_env()
            watchdog_service.start()

    def print_env(self):
        django_settings = diffsettings.Command().handle(output='unified', all=True, default=None)
        environment_variables = [f"{key}: {value}" for key, value in sorted(os.environ.items())]
        environment_variables = os.linesep.join(environment_variables)

        message = f'''
Django settings:
{django_settings}

Environment variables:
{environment_variables}
'''
        logger.debug(message)
