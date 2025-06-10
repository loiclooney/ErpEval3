package itu.mg.erp.service;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import itu.mg.erp.config.ApiClient;
import itu.mg.erp.features.request.LoginRequest;
import itu.mg.erp.response.ApiSuccessLoginResult;

@Service
public class SecurityService {

    private final ApiClient _apiClient;

    public SecurityService(ApiClient apiClient) {
        this._apiClient = apiClient;
    }

    public ApiSuccessLoginResult<Object> login(LoginRequest request) {
        String endpoint = "/method/login";
        ParameterizedTypeReference<ApiSuccessLoginResult<Object>> responseType = 
            new ParameterizedTypeReference<ApiSuccessLoginResult<Object>>() {};
        ApiSuccessLoginResult<Object> result = _apiClient.callApi(endpoint, HttpMethod.POST, request, responseType);
        return result;
    }

    public ApiSuccessLoginResult<Object> logout() {
        String endpoint = "/method/logout";
        ParameterizedTypeReference<ApiSuccessLoginResult<Object>> responseType = 
            new ParameterizedTypeReference<ApiSuccessLoginResult<Object>>() {};
        ApiSuccessLoginResult<Object> result = _apiClient.callApi(endpoint, HttpMethod.POST, null, responseType);
        return result;
    }
}