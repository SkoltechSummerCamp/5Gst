from django.urls import path, include

from services import views

urlpatterns = [
    path('service/', views.ServiceRegistrationView.as_view(), name='service-registration'),
    path('service/acquire/', views.ServiceAcquirementView.as_view(), name='service-acquirement'),
    path('ping/',views.PingView.is_running),
]
