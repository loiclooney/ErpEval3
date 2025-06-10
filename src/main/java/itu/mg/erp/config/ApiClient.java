package itu.mg.erp.config;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import itu.mg.erp.exception.ApiException;
import itu.mg.erp.response.ApiErrorResult;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;

@Component
public class ApiClient {
    private final RestTemplate _restTemplate;

    private final HttpSession _session;

    @Value("${erp.api.base-url}")
    private String _apiBaseUrl;

    public ApiClient(RestTemplate restTemplate, HttpSession session) {
        this._restTemplate = restTemplate;
        this._session = session;
    }

    public <T, R> T callApi(String endpoint, HttpMethod method, R request, ParameterizedTypeReference<T> responseType) {
        String sidCookie = (String) _session.getAttribute("sid_cookie");
        boolean isLoginRequest = endpoint.equals("/method/login");
        if (sidCookie == null && !endpoint.equals("/method/login")) {
            throw new ApiException(401, "No ERPNext session found. Please log in first.", null);
        }

        String url = _apiBaseUrl + endpoint;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (sidCookie != null && !isLoginRequest) {
            headers.add("Cookie", "sid=" + sidCookie);
        }

        HttpEntity<R> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<T> response = _restTemplate.exchange(
                    url, method, entity, responseType);
            if (isLoginRequest && response.getHeaders().containsKey(HttpHeaders.SET_COOKIE)) {
                setSessionCookieFromResponse(response);
            }

            if (response.getStatusCode() == HttpStatus.OK) {
                T responseBody = response.getBody();
                if (responseBody == null) {
                    return null;
                }
                return responseBody;
            } else {
                if (response.getBody() != null) {
                    ApiErrorResult errorResult = parseResponse(response.getBody().toString(), ApiErrorResult.class);
                    handleError(errorResult);
                }
                return null;
            }
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            String responseBody = ex.getResponseBodyAsString();
            if (responseBody != null && !responseBody.isEmpty()) {
                ex.printStackTrace();
                ApiErrorResult errorResult = parseResponse(responseBody, ApiErrorResult.class);
                handleError(errorResult);
            } else {
                throw new ApiException(ex.getStatusCode().value(), ex.getStatusText(), null);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new ApiException(500, "API call failed: " + ex.getMessage(), null);
        }
        return null;
    }

    public void setSessionCookieFromResponse(ResponseEntity<?> response) {
        List<String> cookies = response.getHeaders().get("Set-Cookie");
        if (cookies != null) {
            for (String cookie : cookies) {
                if (cookie.startsWith("sid=")) {
                    String sidValue = cookie.split(";")[0].split("=")[1];
                    _session.setAttribute("sid_cookie", sidValue);
                    break;
                }
            }
        }
    }

    public void clearSession() {
        _session.removeAttribute("sid_cookie");
    }

    private <T> T parseResponse(String responseBody, Class<T> responseType) {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            throw new IllegalArgumentException("Response body is empty or null");
        }
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(responseBody, responseType);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to parse API response: " + responseBody, e);
        }
    }

    private void handleError(ApiErrorResult errorResult) {
        int code = errorResult.getCode();
        String errorMessage = errorResult.getMessage();
        String errorType = (errorResult.getExc_type() != null) ? errorResult.getExc_type() : null;
        throw new ApiException(code, errorMessage, errorType);

    }


}
