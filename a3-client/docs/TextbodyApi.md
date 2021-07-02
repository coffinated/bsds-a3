# TextbodyApi

All URIs are relative to *https://virtserver.swaggerhub.com/gortonator/TextProcessor/1.0.2*

Method | HTTP request | Description
------------- | ------------- | -------------
[**analyzeNewLine**](TextbodyApi.md#analyzeNewLine) | **POST** /textbody/{function} | posts a new line of text for analysis

<a name="analyzeNewLine"></a>
# **analyzeNewLine**
> ResultVal analyzeNewLine(body, function)

posts a new line of text for analysis

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.TextbodyApi;


TextbodyApi apiInstance = new TextbodyApi();
TextLine body = new TextLine(); // TextLine | text string to analyze
String function = "function_example"; // String | the operation to perform on the text
try {
    ResultVal result = apiInstance.analyzeNewLine(body, function);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling TextbodyApi#analyzeNewLine");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**TextLine**](TextLine.md)| text string to analyze |
 **function** | **String**| the operation to perform on the text |

### Return type

[**ResultVal**](ResultVal.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

