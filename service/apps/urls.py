from django.urls import path, include

from apps import views

urlpatterns = [
    path('api/v1/', include([
        path('session/start', views.StartSessionView.as_view()),
        path('session/stop', views.StopSessionView.as_view()),
        path('iperf/start', views.StartIperfView.as_view()),
        path('iperf/stop', views.StopIperfView.as_view()),
    ]))
]
