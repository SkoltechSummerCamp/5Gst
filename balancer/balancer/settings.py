"""
 Django settings for balancer project.

Generated by 'django-admin startproject' using Django 4.0.5.

For more information on this file, see
https://docs.djangoproject.com/en/4.0/topics/settings/

For the full list of settings and their values, see
https://docs.djangoproject.com/en/4.0/ref/settings/
"""

import atexit
import logging
import os
from pathlib import Path

from django.urls import get_script_prefix
from dotenv import load_dotenv
from drf_yasg import openapi
from drf_yasg.generators import OpenAPISchemaGenerator
from drf_yasg.inspectors import SwaggerAutoSchema

logger = logging.getLogger(__name__)

load_dotenv()

# Build paths inside the project like this: os.path.join(BASE_DIR, 'subdir').
BASE_DIR = Path(__file__).resolve().parent.parent


# Quick-start development settings - unsuitable for production
# See https://docs.djangoproject.com/en/4.0/howto/deployment/checklist/

# SECURITY WARNING: keep the secret key used in production secret!
SECRET_KEY = os.environ['SECRET_KEY']

# SECURITY WARNING: don't run with debug turned on in production!
DEBUG = (os.getenv('DEBUG', 'False') == 'True')

ALLOWED_HOSTS = os.getenv('ALLOWED_HOSTS', '127.0.0.1,localhost').split(',')

# Application definition

INSTALLED_APPS = [
    'django.contrib.auth',
    'django.contrib.contenttypes',
    'django.contrib.sessions',
    'django.contrib.messages',
    'rest_framework',
    'services',
    'drf_yasg',
]

MIDDLEWARE = [
    'django.middleware.security.SecurityMiddleware',
    'django.contrib.sessions.middleware.SessionMiddleware',
    'django.middleware.common.CommonMiddleware',
    'django.middleware.csrf.CsrfViewMiddleware',
    'django.contrib.auth.middleware.AuthenticationMiddleware',
    'django.contrib.messages.middleware.MessageMiddleware',
    'django.middleware.clickjacking.XFrameOptionsMiddleware',
]


ROOT_URLCONF = 'balancer.urls'

WSGI_APPLICATION = 'balancer.wsgi.application'

TEST_MODE = os.getenv('TEST_MODE', 'False') == 'True'

# Database
# https://docs.djangoproject.com/en/4.0/ref/settings/#databases

if TEST_MODE:
    import testing.postgresql

    logger.warning("Test mode was enabled. Creating embedded postgresql database...")
    postgresql = None
    for i in range(10):
        port = 5432 + i
        logger.warning(f"Trying to start postgresql at port={port}")
        try:
            postgresql = testing.postgresql.Postgresql(port=port)
            break
        except RuntimeError as e:
            logger.warning(msg=str(e))

    if postgresql is None:
        raise RuntimeError("Could not start embedded postgresql")
    EMBEDDED_DB_URL = postgresql.url()


    def stop_postgresql():
        logger.info(f"Stopping postgresql at {EMBEDDED_DB_URL}...")
        postgresql.stop()
        logger.info(f"Successfully stopped postgresql at {EMBEDDED_DB_URL}!")


    atexit.register(stop_postgresql)
    dsn = postgresql.dsn()

    DATABASES = {
        'default': {
            'ENGINE': 'django.db.backends.postgresql',
            'NAME': dsn['database'],
            'USER': dsn['user'],
            'HOST': dsn['host'],
            'PORT': dsn['port'],
        }
    }
else:
    DATABASES = {
        'default': {
            'ENGINE': 'django.db.backends.postgresql',
            'NAME': os.environ['DB_NAME'],
            'USER': os.environ['DB_USER'],
            'PASSWORD': os.environ['DB_PASSWORD'],
            'HOST': os.environ['DB_HOST'],
            'PORT': os.environ['DB_PORT'],
        }
    }


# Password validation
# https://docs.djangoproject.com/en/4.0/ref/settings/#auth-password-validators

AUTH_PASSWORD_VALIDATORS = [
    {
        'NAME': 'django.contrib.auth.password_validation.UserAttributeSimilarityValidator',
    },
    {
        'NAME': 'django.contrib.auth.password_validation.MinimumLengthValidator',
    },
    {
        'NAME': 'django.contrib.auth.password_validation.CommonPasswordValidator',
    },
    {
        'NAME': 'django.contrib.auth.password_validation.NumericPasswordValidator',
    },
]

LOGGING = {
    'version': 1,
    'disable_existing_loggers': False,
    'handlers': {
        'console': {
            'class': 'logging.StreamHandler',
            'formatter': 'verbose',
        },
    },
    'formatters': {
        'verbose': {
            'format': '{name} [{levelname}] [{asctime}] [process {process:d}] [thread {thread:d}] {message}',
            'style': '{',
        },
    },
    'root': {
        'handlers': ['console'],
        'level': os.getenv('LOG_LEVEL', 'DEBUG'),
    },
}

# TODO not importing because of drf_yasg :(
REST_FRAMEWORK = {
    'DEFAULT_AUTHENTICATION_CLASSES': [
        'services.authentication.FiveGstAnonymousAuthentication',
    ],
    'DEFAULT_PERMISSION_CLASSES': [
        'rest_framework.permissions.IsAuthenticated',
    ],
}

DEFAULT_SWAGGER_TAG = os.getenv('DEFAULT_SWAGGER_TAG', 'balancer')


class SpeedtestAPISchemeGenerator(OpenAPISchemaGenerator):
    def determine_path_prefix(self, paths):
        return ''


class SpeedtestSwaggerAutoSchema(SwaggerAutoSchema):
    def get_tags(self, operation_keys=None):
        tags = self.overrides.get('tags')

        if not tags:
            tags = [DEFAULT_SWAGGER_TAG]

        return tags


SWAGGER_SETTINGS = {
    'DEFAULT_INFO': openapi.Info(
        title="Balancer API",
        default_version='0.1.0',
        description="Speedtest load balancer",
        contact=openapi.Contact(email=os.getenv("SUPPORT_EMAIL", "dev@5gst.ru")),
        license=openapi.License(name="BSD 3-Clause",
                                url='https://raw.githubusercontent.com/SkoltechSummerCamp/5Gst/main/LICENSE'),
    ),
    'DEFAULT_GENERATOR_CLASS': SpeedtestAPISchemeGenerator,
    'DEFAULT_AUTO_SCHEMA_CLASS': SpeedtestSwaggerAutoSchema,
    'SECURITY_DEFINITIONS': {},
}

# Internationalization
# https://docs.djangoproject.com/en/4.0/topics/i18n/

LANGUAGE_CODE = 'en-us'

TIME_ZONE = 'UTC'

USE_I18N = True

USE_TZ = True

# Default primary key field type
# https://docs.djangoproject.com/en/4.0/ref/settings/#default-auto-field

DEFAULT_AUTO_FIELD = 'django.db.models.BigAutoField'

SERVICE_URL_SCHEME = os.getenv('SERVICE_URL_SCHEME', 'https')

FIVE_GST_TOKEN_LIFETIME_SECONDS = int(os.getenv('FIVE_GST_TOKEN_LIFETIME_SECONDS', 60 * 5))
