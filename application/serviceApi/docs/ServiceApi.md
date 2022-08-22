# ServiceApi

All URIs are relative to *https://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**startIperf**](ServiceApi.md#startIperf) | **POST** /api/v1/iperf/start | 
[**startIperfOld**](ServiceApi.md#startIperfOld) | **GET** /start-iperf | 
[**startSession**](ServiceApi.md#startSession) | **POST** /api/v1/session/start | 
[**stopIperf**](ServiceApi.md#stopIperf) | **POST** /api/v1/iperf/stop | 
[**stopIperfOld**](ServiceApi.md#stopIperfOld) | **GET** /stop-iperf | 
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

<a name="startIperfOld"></a>
# **startIperfOld**
> startIperfOld(args)





### Example
```java
// Import classes:
//import ru.scoltech.openran.speedtest.client.service.ApiException;
//import ru.scoltech.openran.speedtest.client.service.api.ServiceApi;


ServiceApi apiInstance = new ServiceApi();
String args = "args_example"; // String | Iperf args used to run service iperf
try {
    apiInstance.startIperfOld(args);
} catch (ApiException e) {
    System.err.println("Exception when calling ServiceApi#startIperfOld");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **args** | **String**| Iperf args used to run service iperf | [optional]

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

<a name="stopIperfOld"></a>
# **stopIperfOld**
> stopIperfOld()





### Example
```java
// Import classes:
//import ru.scoltech.openran.speedtest.client.service.ApiException;
//import ru.scoltech.openran.speedtest.client.service.api.ServiceApi;


ServiceApi apiInstance = new ServiceApi();
try {
    apiInstance.stopIperfOld();
} catch (ApiException e) {
    System.err.println("Exception when calling ServiceApi#stopIperfOld");
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

