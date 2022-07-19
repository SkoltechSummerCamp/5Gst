# BalancerApi

All URIs are relative to *https://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**acquireService**](BalancerApi.md#acquireService) | **POST** /Skoltech_OpenRAN_5G/iperf_load_balancer/0.1.0/service/acquire/ | 
[**ping**](BalancerApi.md#ping) | **GET** /Skoltech_OpenRAN_5G/iperf_load_balancer/0.1.0/ping/ | 
[**registerService**](BalancerApi.md#registerService) | **POST** /Skoltech_OpenRAN_5G/iperf_load_balancer/0.1.0/service/ | 
[**unregisterService**](BalancerApi.md#unregisterService) | **DELETE** /Skoltech_OpenRAN_5G/iperf_load_balancer/0.1.0/service/ | 


<a name="acquireService"></a>
# **acquireService**
> ServerAddressResponse acquireService()



Acquires service for further iperf tests

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.BalancerApi;


BalancerApi apiInstance = new BalancerApi();
try {
    ServerAddressResponse result = apiInstance.acquireService();
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling BalancerApi#acquireService");
    e.printStackTrace();
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**ServerAddressResponse**](ServerAddressResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="ping"></a>
# **ping**
> ping()



Check that server is up

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.BalancerApi;


BalancerApi apiInstance = new BalancerApi();
try {
    apiInstance.ping();
} catch (ApiException e) {
    System.err.println("Exception when calling BalancerApi#ping");
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

<a name="registerService"></a>
# **registerService**
> ServerAddressRequest registerService(data)



Register caller as service

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.BalancerApi;


BalancerApi apiInstance = new BalancerApi();
ServerAddressRequest data = new ServerAddressRequest(); // ServerAddressRequest | 
try {
    ServerAddressRequest result = apiInstance.registerService(data);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling BalancerApi#registerService");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **data** | [**ServerAddressRequest**](ServerAddressRequest.md)|  |

### Return type

[**ServerAddressRequest**](ServerAddressRequest.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="unregisterService"></a>
# **unregisterService**
> ServerAddressRequest unregisterService(data)



Unregister caller as service

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.BalancerApi;


BalancerApi apiInstance = new BalancerApi();
ServerAddressRequest data = new ServerAddressRequest(); // ServerAddressRequest | 
try {
    ServerAddressRequest result = apiInstance.unregisterService(data);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling BalancerApi#unregisterService");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **data** | [**ServerAddressRequest**](ServerAddressRequest.md)|  |

### Return type

[**ServerAddressRequest**](ServerAddressRequest.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

