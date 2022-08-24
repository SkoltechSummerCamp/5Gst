import datetime

from django.urls import reverse
from django.utils.timezone import make_aware
from rest_framework import status
from rest_framework.test import APITestCase

from services import models


class AcquireServiceTestCase(APITestCase):
    @classmethod
    def setUpTestData(cls):
        models.FiveGstToken.objects.create(
            token='test',
            expires_at=make_aware(datetime.datetime.now()) + datetime.timedelta(weeks=123),
        )

    def test_cant_acquire_service(self):
        response = self.client.post(
            path=reverse('service-acquirement'),
            data={},
            content_type='application/json',
            HTTP_AUTHORIZATION='5Gst test',
        )
        self.assertEqual(response.status_code, status.HTTP_503_SERVICE_UNAVAILABLE)
