from django.urls import path

from apps import views

urlpatterns = [
    path('start-iperf', views.start_iperf, name='start-iperf'),
    path('stop-iperf', views.stop_iperf, name='stop-iperf'),
]
