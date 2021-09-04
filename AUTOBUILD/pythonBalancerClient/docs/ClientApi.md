# swagger_client.ClientApi

All URIs are relative to *http://localhost:8080/Skoltech_OpenRAN_5G/iperf_load_balancer/0.1.0*

Method | HTTP request | Description
------------- | ------------- | -------------
[**client_obtain_ip**](ClientApi.md#client_obtain_ip) | **GET** /addr | obtain iperf server ip list to connect to

# **client_obtain_ip**
> list[ServerAddr] client_obtain_ip()

obtain iperf server ip list to connect to

Return servers ip list

### Example
```python
from __future__ import print_function
import time
import swagger_client
from swagger_client.rest import ApiException
from pprint import pprint

# create an instance of the API class
api_instance = swagger_client.ClientApi()

try:
    # obtain iperf server ip list to connect to
    api_response = api_instance.client_obtain_ip()
    pprint(api_response)
except ApiException as e:
    print("Exception when calling ClientApi->client_obtain_ip: %s\n" % e)
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**list[ServerAddr]**](ServerAddr.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

