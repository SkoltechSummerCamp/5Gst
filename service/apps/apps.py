import logging
import os
import sys

from django.apps import AppConfig
from django.core.management.commands import diffsettings
from django.utils.autoreload import DJANGO_AUTORELOAD_ENV

from apps.logic.watchdog_service import balancer_communication_watchdog_service

logger = logging.getLogger(__name__)


class MyAppConfig(AppConfig):
    name = 'apps'

    def ready(self):
        # Workaround for development server with auto reload. (we need to execute initialization once)
        if 'runserver' in sys.argv \
                and '--noreload' not in sys.argv \
                and os.getenv(DJANGO_AUTORELOAD_ENV) != 'true':
            return

        self.print_env()
        balancer_communication_watchdog_service.start()

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
