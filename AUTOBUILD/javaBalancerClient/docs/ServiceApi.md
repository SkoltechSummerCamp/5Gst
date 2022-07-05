# ServiceApi

All URIs are relative to *http://127.0.0.1:8000/Skoltech_OpenRAN_5G/iperf_load_balancer/0.1.0*

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
//import io.swagger.client.ApiClient;
//import io.swagger.client.ApiException;
//import io.swagger.client.Configuration;
//import io.swagger.client.auth.*;
//import io.swagger.client.api.ServiceApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure HTTP basic authorization: Basic
HttpBasicAuth Basic = (HttpBasicAuth) defaultClient.getAuthentication("Basic");
Basic.setUsername("YOUR USERNAME");
Basic.setPassword("YOUR PASSWORD");

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

[Basic](../README.md#Basic)

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
//import io.swagger.client.ApiClient;
//import io.swagger.client.ApiException;
//import io.swagger.client.Configuration;
//import io.swagger.client.auth.*;
//import io.swagger.client.api.ServiceApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure HTTP basic authorization: Basic
HttpBasicAuth Basic = (HttpBasicAuth) defaultClient.getAuthentication("Basic");
Basic.setUsername("YOUR USERNAME");
Basic.setPassword("YOUR PASSWORD");

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

[Basic](../README.md#Basic)

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
//import io.swagger.client.ApiClient;
//import io.swagger.client.ApiException;
//import io.swagger.client.Configuration;
//import io.swagger.client.auth.*;
//import io.swagger.client.api.ServiceApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure HTTP basic authorization: Basic
HttpBasicAuth Basic = (HttpBasicAuth) defaultClient.getAuthentication("Basic");
Basic.setUsername("YOUR USERNAME");
Basic.setPassword("YOUR PASSWORD");

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

[Basic](../README.md#Basic)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

