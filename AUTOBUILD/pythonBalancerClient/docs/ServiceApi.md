# swagger_client.ServiceApi

All URIs are relative to *http://127.0.0.1:8000/Skoltech_OpenRAN_5G/iperf_load_balancer/0.1.0*

Method | HTTP request | Description
------------- | ------------- | -------------
[**service_acquire_create**](ServiceApi.md#service_acquire_create) | **POST** /service/acquire/ | 
[**service_create**](ServiceApi.md#service_create) | **POST** /service/ | 
[**service_delete**](ServiceApi.md#service_delete) | **DELETE** /service/ | 


# **service_acquire_create**
> ServerAddressResponse service_acquire_create()



Acquires service for further iperf tests

### Example
```python
from __future__ import print_function
import time
import swagger_client
from swagger_client.rest import ApiException
from pprint import pprint

# Configure HTTP basic authorization: Basic
configuration = swagger_client.Configuration()
configuration.username = 'YOUR_USERNAME'
configuration.password = 'YOUR_PASSWORD'

# create an instance of the API class
api_instance = swagger_client.ServiceApi(swagger_client.ApiClient(configuration))

try:
    api_response = api_instance.service_acquire_create()
    pprint(api_response)
except ApiException as e:
    print("Exception when calling ServiceApi->service_acquire_create: %s\n" % e)
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**ServerAddressResponse**](ServerAddressResponse.md)

### Authorization

[Basic](../README.md#Basic)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **service_create**
> ServerAddressRequest service_create(data)



Register caller as service

### Example
```python
from __future__ import print_function
import time
import swagger_client
from swagger_client.rest import ApiException
from pprint import pprint

# Configure HTTP basic authorization: Basic
configuration = swagger_client.Configuration()
configuration.username = 'YOUR_USERNAME'
configuration.password = 'YOUR_PASSWORD'

# create an instance of the API class
api_instance = swagger_client.ServiceApi(swagger_client.ApiClient(configuration))
data = swagger_client.ServerAddressRequest() # ServerAddressRequest | 

try:
    api_response = api_instance.service_create(data)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling ServiceApi->service_create: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **data** | [**ServerAddressRequest**](ServerAddressRequest.md)|  | 

### Return type

[**ServerAddressRequest**](ServerAddressRequest.md)

### Authorization

[Basic](../README.md#Basic)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **service_delete**
> ServerAddressRequest service_delete(data)



Unregister caller as service

### Example
```python
from __future__ import print_function
import time
import swagger_client
from swagger_client.rest import ApiException
from pprint import pprint

# Configure HTTP basic authorization: Basic
configuration = swagger_client.Configuration()
configuration.username = 'YOUR_USERNAME'
configuration.password = 'YOUR_PASSWORD'

# create an instance of the API class
api_instance = swagger_client.ServiceApi(swagger_client.ApiClient(configuration))
data = swagger_client.ServerAddressRequest() # ServerAddressRequest | 

try:
    api_response = api_instance.service_delete(data)
    pprint(api_response)
except ApiException as e:
    print("Exception when calling ServiceApi->service_delete: %s\n" % e)
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **data** | [**ServerAddressRequest**](ServerAddressRequest.md)|  | 

### Return type

[**ServerAddressRequest**](ServerAddressRequest.md)

### Authorization

[Basic](../README.md#Basic)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

