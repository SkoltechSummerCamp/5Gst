# PingApi

All URIs are relative to *https://localhost/Skoltech_OpenRAN_5G/iperf_load_balancer/0.1.0*

Method | HTTP request | Description
------------- | ------------- | -------------
[**pingList**](PingApi.md#pingList) | **GET** /ping/ | 


<a name="pingList"></a>
# **pingList**
> pingList()





### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.PingApi;


PingApi apiInstance = new PingApi();
try {
    apiInstance.pingList();
} catch (ApiException e) {
    System.err.println("Exception when calling PingApi#pingList");
    e.printStackTrace();
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

