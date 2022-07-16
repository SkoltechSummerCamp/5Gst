import logging

from django.apps import AppConfig
from django.conf import settings
from django.db import DEFAULT_DB_ALIAS, connections
from django.db.migrations.executor import MigrationExecutor

logger = logging.getLogger(__name__)


class ServicesConfig(AppConfig):
    name = 'services'
    default_auto_field = 'django.db.models.BigAutoField'

    def ready(self):
        if settings.TEST_MODE:
            self.migrate()
            logger.info(f"Data source URL is {settings.EMBEDDED_DB_URL}")

    def migrate(self):
        connection = connections[DEFAULT_DB_ALIAS]
        connection.prepare_database()
        executor = MigrationExecutor(connection, progress_callback=self.migration_progress_callback)
        targets = executor.loader.graph.leaf_nodes()
        executor.migrate(targets=targets)

    def migration_progress_callback(self, action, migration=None, fake=False):
        logger.info(f"{action.ljust(20)} for migration {migration}")
