package itu.mg.erp.service;

import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import itu.mg.erp.config.ApiClient;
import itu.mg.erp.features.request.ERPNextRequest;
import itu.mg.erp.response.ERPNextResourceResponse;
import itu.mg.erp.response.ERPNextResourceSingleResponse;

@Service
public class PurchaseInvoiceService {

    private final ERPNextService _erpNextService;
    private final ApiClient _apiClient;


    public PurchaseInvoiceService(ERPNextService erpNextService, ApiClient apiClient) {
        this._apiClient = apiClient;
        this._erpNextService = erpNextService;
    }

    public ERPNextResourceResponse getListPurchaseInvoice() {
        String endpoint = "/resource/Purchase Invoice";
        ERPNextRequest request = new ERPNextRequest();
        return _erpNextService.getItems(endpoint, request);
    }

    public ERPNextResourceSingleResponse  getPurchaseInvoice(String invoice) {
        String endpoint = "/resource/Purchase Invoice/"+invoice;
        ERPNextRequest request = new ERPNextRequest();
        return _erpNextService.getSingleItems(endpoint, request);
    } 

    public ERPNextResourceSingleResponse payEntryForPurchaseInvoice(Map<String, Object> paymentEntry) {
        String endpoint = "/resource/Payment Entry";
        ParameterizedTypeReference<ERPNextResourceSingleResponse> responseType = 
            new ParameterizedTypeReference<ERPNextResourceSingleResponse>() {};
        ERPNextResourceSingleResponse result = _apiClient.callApi(endpoint, HttpMethod.POST, paymentEntry, responseType);
        return result;
    }
    

   
}
