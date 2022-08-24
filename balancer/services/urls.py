from django.urls import path

from services import views

urlpatterns = [
    path('login/', views.FiveGstLoginView.as_view(), name='five-gst-login'),
    path('logout/', views.FiveGstLogoutView.as_view(), name='five-gst-logout'),
    path('service/', views.ServiceRegistrationView.as_view(), name='service-registration'),
    path('service/acquire/', views.ServiceAcquirementView.as_view(), name='service-acquirement'),
    path('ping/', views.PingView.as_view()),
]
