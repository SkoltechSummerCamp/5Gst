# ServiceApi

All URIs are relative to *https://localhost/Skoltech_OpenRAN_5G/iperf_load_balancer/0.1.0*

Method | HTTP request | Description
------------- | ------------- | -------------
[**serviceAcquireCreate**](ServiceApi.md#serviceAcquireCreate) | **POST** /service/acquire/ | 
[**serviceCreate**](ServiceApi.md#serviceCreate) | **POST** /service/ | 
[**serviceDelete**](ServiceApi.md#serviceDelete) | **DELETE** /service/ | 


<a name="serviceAcquireCreate"></a>
# **serviceAcquireCreate**
> ServerAddressResponse serviceAcquireCreate()



Acquires service for further iperf tests

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.ServiceApi;


ServiceApi apiInstance = new ServiceApi();
try {
    ServerAddressResponse result = apiInstance.serviceAcquireCreate();
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ServiceApi#serviceAcquireCreate");
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

<a name="serviceCreate"></a>
# **serviceCreate**
> ServerAddressRequest serviceCreate(data)



Register caller as service

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.ServiceApi;


ServiceApi apiInstance = new ServiceApi();
ServerAddressRequest data = new ServerAddressRequest(); // ServerAddressRequest | 
try {
    ServerAddressRequest result = apiInstance.serviceCreate(data);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ServiceApi#serviceCreate");
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

<a name="serviceDelete"></a>
# **serviceDelete**
> ServerAddressRequest serviceDelete(data)



Unregister caller as service

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.ServiceApi;


ServiceApi apiInstance = new ServiceApi();
ServerAddressRequest data = new ServerAddressRequest(); // ServerAddressRequest | 
try {
    ServerAddressRequest result = apiInstance.serviceDelete(data);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ServiceApi#serviceDelete");
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

