package itu.mg.erp.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import itu.mg.erp.config.ApiClient;
import itu.mg.erp.features.request.ERPNextRequest;
import itu.mg.erp.features.request.QuotationSatusUpdateRequest;
import itu.mg.erp.features.request.QuotationUpdateRequest;
import itu.mg.erp.response.ERPNextResourceResponse;
import itu.mg.erp.response.ERPNextResourceSingleResponse;

@Service
public class SupplierQuotationService {

    private final ERPNextService _erpNextService;
    private final ApiClient _apiClient;


    public SupplierQuotationService(ERPNextService erpNextService, ApiClient apiClient) {
        this._apiClient = apiClient;
        this._erpNextService = erpNextService;
    }

    public ERPNextResourceResponse getListSupplierQuotation(String quotation) {
        String endpoint = "/resource/Supplier Quotation";
        ERPNextRequest request = new ERPNextRequest();
        List<List<Object>> filters = Arrays.asList(
            Arrays.asList("supplier", "=", quotation)
        );
        request.setFilters(filters);
        return _erpNextService.getItems(endpoint, request);
    }

    public ERPNextResourceSingleResponse getListSupplierQuotationItem(String quotation) {
        String endpoint = "/resource/Supplier Quotation/"+quotation;
        ERPNextRequest request = new ERPNextRequest();
        return _erpNextService.getSingleItems(endpoint, request);
    }

    public void updateRate(String itemCode, QuotationUpdateRequest updateRequest) {
        String endpoint = "/resource/Supplier Quotation Item/"+itemCode;
        ParameterizedTypeReference<ERPNextResourceSingleResponse> responseType =
            new ParameterizedTypeReference<ERPNextResourceSingleResponse>() {};
        _apiClient.callApi(endpoint, HttpMethod.PUT, updateRequest, responseType);
    }

    public void updateStatus(String name, QuotationSatusUpdateRequest updateRequest) {
        String endpoint = "/resource/Supplier Quotation/"+name;
        ParameterizedTypeReference<ERPNextResourceSingleResponse> responseType =
            new ParameterizedTypeReference<ERPNextResourceSingleResponse>() {};
        _apiClient.callApi(endpoint, HttpMethod.PUT, updateRequest, responseType);
    }

    public void insertQuotation(Map<String, Object> value) {
        String endpoint = "/resource/Supplier Quotation/";
        ParameterizedTypeReference<ERPNextResourceSingleResponse> responseType =
            new ParameterizedTypeReference<ERPNextResourceSingleResponse>() {};
        _apiClient.callApi(endpoint, HttpMethod.POST, value, responseType);
    }

    public ERPNextResourceResponse getListCurrency() {
        String endpoint = "/resource/Currency/";
        ERPNextRequest request = new ERPNextRequest();
        return _erpNextService.getItems(endpoint, request);
    }

    public ERPNextResourceResponse getListItems() {
        String endpoint = "/resource/Item/";
        ERPNextRequest request = new ERPNextRequest();
        return _erpNextService.getItems(endpoint, request);
    }

    public ERPNextResourceResponse getListUOMs() {
        String endpoint = "/resource/UOM/";
        ERPNextRequest request = new ERPNextRequest();
        return _erpNextService.getItems(endpoint, request);
    }

    public ERPNextResourceResponse getListWarehouses() {
        String endpoint = "/resource/Warehouse/";
        ERPNextRequest request = new ERPNextRequest();
        return _erpNextService.getItems(endpoint, request);
    }

}
