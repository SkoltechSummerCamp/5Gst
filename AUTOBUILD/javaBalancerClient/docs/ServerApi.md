# ServerApi

All URIs are relative to *http://localhost:8080/Skoltech_OpenRAN_5G/iperf_load_balancer/0.1.0*

Method | HTTP request | Description
------------- | ------------- | -------------
[**serverDeleteIp**](ServerApi.md#serverDeleteIp) | **POST** /addr_del | delete server IP
[**serverPostIp**](ServerApi.md#serverPostIp) | **POST** /addr | post self ip to balancer

<a name="serverDeleteIp"></a>
# **serverDeleteIp**
> List&lt;InlineResponse200&gt; serverDeleteIp(body)

delete server IP

Send by server during shutdown.

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.ServerApi;


ServerApi apiInstance = new ServerApi();
ServerAddr body = new ServerAddr(); // ServerAddr | port of iperf server. Ip and time could be emply
try {
    List<InlineResponse200> result = apiInstance.serverDeleteIp(body);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ServerApi#serverDeleteIp");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**ServerAddr**](ServerAddr.md)| port of iperf server. Ip and time could be emply | [optional]

### Return type

[**List&lt;InlineResponse200&gt;**](InlineResponse200.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/Json

<a name="serverPostIp"></a>
# **serverPostIp**
> serverPostIp(body)

post self ip to balancer

When server makes free, post ip to balancer

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.ServerApi;


ServerApi apiInstance = new ServerApi();
ServerAddr body = new ServerAddr(); // ServerAddr | port of iperf server. Ip and time could be emply
try {
    apiInstance.serverPostIp(body);
} catch (ApiException e) {
    System.err.println("Exception when calling ServerApi#serverPostIp");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**ServerAddr**](ServerAddr.md)| port of iperf server. Ip and time could be emply | [optional]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: Not defined

