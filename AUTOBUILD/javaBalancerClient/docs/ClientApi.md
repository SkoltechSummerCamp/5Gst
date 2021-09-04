# ClientApi

All URIs are relative to *http://localhost:8080/Skoltech_OpenRAN_5G/iperf_load_balancer/0.1.0*

Method | HTTP request | Description
------------- | ------------- | -------------
[**clientObtainIp**](ClientApi.md#clientObtainIp) | **GET** /addr | obtain iperf server ip list to connect to

<a name="clientObtainIp"></a>
# **clientObtainIp**
> List&lt;ServerAddr&gt; clientObtainIp()

obtain iperf server ip list to connect to

Return servers ip list

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.ClientApi;


ClientApi apiInstance = new ClientApi();
try {
    List<ServerAddr> result = apiInstance.clientObtainIp();
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ClientApi#clientObtainIp");
    e.printStackTrace();
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**List&lt;ServerAddr&gt;**](ServerAddr.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

