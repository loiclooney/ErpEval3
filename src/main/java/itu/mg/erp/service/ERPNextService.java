package itu.mg.erp.service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import itu.mg.erp.config.ApiClient;
import itu.mg.erp.features.request.ERPNextRequest;
import itu.mg.erp.response.ERPNextResourceResponse;
import itu.mg.erp.response.ERPNextResourceSingleResponse;

@Service
public class ERPNextService {

    private final ApiClient _apiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ERPNextService(ApiClient apiClient) {
        this._apiClient = apiClient;
    }

    public ERPNextResourceSingleResponse getSingleItems(String link, ERPNextRequest request) {

        ParameterizedTypeReference<ERPNextResourceSingleResponse> responseType = new ParameterizedTypeReference<ERPNextResourceSingleResponse>() {
        };

        String fullUrl = buildERPNextEndpoint(link, request);
        ERPNextResourceSingleResponse result = _apiClient.callApi(fullUrl, HttpMethod.GET, null, responseType);

        return result;
    }

    public ERPNextResourceResponse getItems(String link, ERPNextRequest request) {
        // Sélectionner des champs
        // request.setFields(Arrays.asList("name", "item_name", "description"));

        // List<List<Object>> filters = Arrays.asList(
        // Arrays.asList("item_group", "=", "Products"),
        // Arrays.asList("disabled", "=", 0)
        // );
        // request.setFilters(filters);

        // request.setLimitPageLength(10);
        // request.setLimitStart(0);
        // request.setOrderBy("modified desc");

        // request.setWithCommentCount(true);
        // request.setIncludeMetadata(true);

        ParameterizedTypeReference<ERPNextResourceResponse> responseType = new ParameterizedTypeReference<ERPNextResourceResponse>() {
        };

        String fullUrl = buildERPNextEndpoint(link, request);
        ERPNextResourceResponse result = _apiClient.callApi(fullUrl, HttpMethod.GET, null, responseType);

        return result;
    }

    private String buildERPNextEndpoint(String baseEndpoint, ERPNextRequest request) {
        StringBuilder url = new StringBuilder(baseEndpoint);
        if (!baseEndpoint.contains("?")) {
            url.append("?");
        } else if (!baseEndpoint.endsWith("&")) {
            url.append("&");
        }

        try {
            if (request.getFields() != null && !request.getFields().isEmpty()) {
                String fieldsJson = objectMapper.writeValueAsString(request.getFields());
                url.append("fields=").append(fieldsJson).append("&");
            }
            if (request.getFilters() != null && !request.getFilters().isEmpty()) {
                String filtersJson = objectMapper.writeValueAsString(request.getFilters());
                url.append("filters=").append(filtersJson).append("&");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to serialize fields or filters", e);
        }
        if (request.getLimitPageLength() != null) {
            url.append("limit_page_length=").append(request.getLimitPageLength()).append("&");
        }
        if (request.getLimitStart() != null) {
            url.append("limit_start=").append(request.getLimitStart()).append("&");
        }
        if (request.getOrderBy() != null) {
            url.append("order_by=").append(encode(request.getOrderBy())).append("&");
        }
        if (request.getWithCommentCount() != null && request.getWithCommentCount()) {
            url.append("with_comment_count=1&");
        }
        if (request.getWithChildnames() != null && request.getWithChildnames()) {
            url.append("with_childnames=1&");
        }
        if (request.getIncludeMetadata() != null && request.getIncludeMetadata()) {
            url.append("include_metadata=1&");
        }
        if (request.getDistinct() != null && request.getDistinct()) {
            url.append("distinct=1&");
        }
        if (request.getParent() != null) {
            url.append("parent=").append(encode(request.getParent())).append("&");
        }
        if (request.getParenttype() != null) {
            url.append("parenttype=").append(encode(request.getParenttype())).append("&");
        }
        if (request.getParentfield() != null) {
            url.append("parentfield=").append(encode(request.getParentfield())).append("&");
        }
        if (request.getAdditionalParams() != null && !request.getAdditionalParams().isEmpty()) {
            request.getAdditionalParams().forEach((key, value) -> {
                url.append(key).append("=").append(encode(value.toString())).append("&");
            });
        }

        if (url.charAt(url.length() - 1) == '&') {
            url.setLength(url.length() - 1);
        }

        return url.toString();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public byte[] getPdf(String url) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<byte[]> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                byte[].class);

        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody();
        }
        return null;
    }

}
