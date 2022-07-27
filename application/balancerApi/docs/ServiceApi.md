# ServiceApi

All URIs are relative to *https://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**startIperfList**](ServiceApi.md#startIperfList) | **GET** /start-iperf | 
[**stopIperfList**](ServiceApi.md#stopIperfList) | **GET** /stop-iperf | 


<a name="startIperfList"></a>
# **startIperfList**
> startIperfList(args)





### Example
```java
// Import classes:
//import ru.scoltech.openran.speedtest.client.balancer.ApiException;
//import ru.scoltech.openran.speedtest.client.balancer.api.ServiceApi;


ServiceApi apiInstance = new ServiceApi();
String args = "args_example"; // String | Iperf args used to run service iperf
try {
    apiInstance.startIperfList(args);
} catch (ApiException e) {
    System.err.println("Exception when calling ServiceApi#startIperfList");
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

<a name="stopIperfList"></a>
# **stopIperfList**
> stopIperfList()





### Example
```java
// Import classes:
//import ru.scoltech.openran.speedtest.client.balancer.ApiException;
//import ru.scoltech.openran.speedtest.client.balancer.api.ServiceApi;


ServiceApi apiInstance = new ServiceApi();
try {
    apiInstance.stopIperfList();
} catch (ApiException e) {
    System.err.println("Exception when calling ServiceApi#stopIperfList");
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

