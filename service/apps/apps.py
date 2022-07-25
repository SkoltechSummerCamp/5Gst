import os

from django.apps import AppConfig
from django.core.management.commands import diffsettings
from django.utils.autoreload import DJANGO_AUTORELOAD_ENV

from apps.logic.watchdog import watchdog_service


class MyAppConfig(AppConfig):
    name = 'apps'

    def ready(self):
        if os.getenv(DJANGO_AUTORELOAD_ENV) != 'true':
            self.print_env()
            watchdog_service.start()

    def print_env(self):
        print("Django settings:")
        print(diffsettings.Command().handle(output='unified', all=True, default=None))
        print()
        print("Environment variables:")
        for key, value in sorted(os.environ.items()):
            print(f"{key}: {value}")
