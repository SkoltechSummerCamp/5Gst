import os

from django.apps import AppConfig
from django.utils.autoreload import DJANGO_AUTORELOAD_ENV

from apps.logic.watchdog import watchdog_service


class MyAppConfig(AppConfig):
    name = 'apps'

    def ready(self):
        if os.getenv(DJANGO_AUTORELOAD_ENV) != 'true':
            watchdog_service.start()
            self.print_env()

    def print_env(self):
        pass  # TODO
