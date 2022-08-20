from django.contrib.auth.models import AnonymousUser
from rest_framework import exceptions
from rest_framework.authentication import TokenAuthentication

from services import models


# AnonymousUser intentionally leaves some methods unimplemented so do we
# noinspection PyAbstractClass
class TokenAnonymousUser(AnonymousUser):
    is_active = True

    def __init__(self, token: models.FiveGstToken):
        super(TokenAnonymousUser, self).__init__()
        self.token = token

    def __str__(self):
        return f"TokenAnonymousUser(token={self.token})"

    def __eq__(self, other):
        return super(TokenAnonymousUser, self).__eq__(other) and self.token == other.token

    def __hash__(self):
        return hash(self.token)

    @property
    def is_anonymous(self):
        return True

    @property
    def is_authenticated(self):
        return True


class FiveGstAuthentication(TokenAuthentication):
    keyword = '5Gst'

    def authenticate_credentials(self, key):
        tokens = list(models.FiveGstToken.objects.filter(token=key))
        if len(tokens) == 1:
            return TokenAnonymousUser(tokens[0]), tokens[0]
        else:
            raise exceptions.AuthenticationFailed('Invalid token.')
