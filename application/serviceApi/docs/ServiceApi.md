# ServiceApi

All URIs are relative to *https://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**startIperf**](ServiceApi.md#startIperf) | **POST** /api/v1/iperf/start | 
[**startSession**](ServiceApi.md#startSession) | **POST** /api/v1/session/start | 
[**stopIperf**](ServiceApi.md#stopIperf) | **POST** /api/v1/iperf/stop | 
[**stopSession**](ServiceApi.md#stopSession) | **POST** /api/v1/session/stop | 


<a name="startIperf"></a>
# **startIperf**
> startIperf(data)





### Example
```java
// Import classes:
//import ru.scoltech.openran.speedtest.client.service.ApiException;
//import ru.scoltech.openran.speedtest.client.service.api.ServiceApi;


ServiceApi apiInstance = new ServiceApi();
IperfArgs data = new IperfArgs(); // IperfArgs | 
try {
    apiInstance.startIperf(data);
} catch (ApiException e) {
    System.err.println("Exception when calling ServiceApi#startIperf");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **data** | [**IperfArgs**](IperfArgs.md)|  |

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="startSession"></a>
# **startSession**
> startSession()





### Example
```java
// Import classes:
//import ru.scoltech.openran.speedtest.client.service.ApiException;
//import ru.scoltech.openran.speedtest.client.service.api.ServiceApi;


ServiceApi apiInstance = new ServiceApi();
try {
    apiInstance.startSession();
} catch (ApiException e) {
    System.err.println("Exception when calling ServiceApi#startSession");
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

<a name="stopIperf"></a>
# **stopIperf**
> stopIperf()





### Example
```java
// Import classes:
//import ru.scoltech.openran.speedtest.client.service.ApiException;
//import ru.scoltech.openran.speedtest.client.service.api.ServiceApi;


ServiceApi apiInstance = new ServiceApi();
try {
    apiInstance.stopIperf();
} catch (ApiException e) {
    System.err.println("Exception when calling ServiceApi#stopIperf");
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

<a name="stopSession"></a>
# **stopSession**
> stopSession()





### Example
```java
// Import classes:
//import ru.scoltech.openran.speedtest.client.service.ApiException;
//import ru.scoltech.openran.speedtest.client.service.api.ServiceApi;


ServiceApi apiInstance = new ServiceApi();
try {
    apiInstance.stopSession();
} catch (ApiException e) {
    System.err.println("Exception when calling ServiceApi#stopSession");
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

