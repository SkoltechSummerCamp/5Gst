from django.urls import path

from apps import views

urlpatterns = [
    path('start-iperf', views.StartIperfView.as_view(), name='start-iperf'),
    path('stop-iperf', views.StopIperfView.as_view(), name='stop-iperf'),
]
