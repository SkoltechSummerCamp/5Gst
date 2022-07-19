# BalancerApi

All URIs are relative to *https://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**skoltechOpenRAN5GIperfLoadBalancer010PingList**](BalancerApi.md#skoltechOpenRAN5GIperfLoadBalancer010PingList) | **GET** /Skoltech_OpenRAN_5G/iperf_load_balancer/0.1.0/ping/ | 
[**skoltechOpenRAN5GIperfLoadBalancer010ServiceAcquireCreate**](BalancerApi.md#skoltechOpenRAN5GIperfLoadBalancer010ServiceAcquireCreate) | **POST** /Skoltech_OpenRAN_5G/iperf_load_balancer/0.1.0/service/acquire/ | 
[**skoltechOpenRAN5GIperfLoadBalancer010ServiceCreate**](BalancerApi.md#skoltechOpenRAN5GIperfLoadBalancer010ServiceCreate) | **POST** /Skoltech_OpenRAN_5G/iperf_load_balancer/0.1.0/service/ | 
[**skoltechOpenRAN5GIperfLoadBalancer010ServiceDelete**](BalancerApi.md#skoltechOpenRAN5GIperfLoadBalancer010ServiceDelete) | **DELETE** /Skoltech_OpenRAN_5G/iperf_load_balancer/0.1.0/service/ | 


<a name="skoltechOpenRAN5GIperfLoadBalancer010PingList"></a>
# **skoltechOpenRAN5GIperfLoadBalancer010PingList**
> skoltechOpenRAN5GIperfLoadBalancer010PingList()





### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.BalancerApi;


BalancerApi apiInstance = new BalancerApi();
try {
    apiInstance.skoltechOpenRAN5GIperfLoadBalancer010PingList();
} catch (ApiException e) {
    System.err.println("Exception when calling BalancerApi#skoltechOpenRAN5GIperfLoadBalancer010PingList");
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

<a name="skoltechOpenRAN5GIperfLoadBalancer010ServiceAcquireCreate"></a>
# **skoltechOpenRAN5GIperfLoadBalancer010ServiceAcquireCreate**
> ServerAddressResponse skoltechOpenRAN5GIperfLoadBalancer010ServiceAcquireCreate()



Acquires service for further iperf tests

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.BalancerApi;


BalancerApi apiInstance = new BalancerApi();
try {
    ServerAddressResponse result = apiInstance.skoltechOpenRAN5GIperfLoadBalancer010ServiceAcquireCreate();
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling BalancerApi#skoltechOpenRAN5GIperfLoadBalancer010ServiceAcquireCreate");
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

<a name="skoltechOpenRAN5GIperfLoadBalancer010ServiceCreate"></a>
# **skoltechOpenRAN5GIperfLoadBalancer010ServiceCreate**
> ServerAddressRequest skoltechOpenRAN5GIperfLoadBalancer010ServiceCreate(data)



Register caller as service

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.BalancerApi;


BalancerApi apiInstance = new BalancerApi();
ServerAddressRequest data = new ServerAddressRequest(); // ServerAddressRequest | 
try {
    ServerAddressRequest result = apiInstance.skoltechOpenRAN5GIperfLoadBalancer010ServiceCreate(data);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling BalancerApi#skoltechOpenRAN5GIperfLoadBalancer010ServiceCreate");
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

<a name="skoltechOpenRAN5GIperfLoadBalancer010ServiceDelete"></a>
# **skoltechOpenRAN5GIperfLoadBalancer010ServiceDelete**
> ServerAddressRequest skoltechOpenRAN5GIperfLoadBalancer010ServiceDelete(data)



Unregister caller as service

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.BalancerApi;


BalancerApi apiInstance = new BalancerApi();
ServerAddressRequest data = new ServerAddressRequest(); // ServerAddressRequest | 
try {
    ServerAddressRequest result = apiInstance.skoltechOpenRAN5GIperfLoadBalancer010ServiceDelete(data);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling BalancerApi#skoltechOpenRAN5GIperfLoadBalancer010ServiceDelete");
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

