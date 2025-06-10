package itu.mg.erp.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

import itu.mg.erp.config.ApiClient;
import itu.mg.erp.features.request.ERPNextRequest;
import itu.mg.erp.response.ERPNextResourceResponse;

@Service
public class PurchaseOrderService {

    private final ERPNextService _erpNextService;
    private final ApiClient _apiClient;


    public PurchaseOrderService(ERPNextService erpNextService, ApiClient apiClient) {
        this._apiClient = apiClient;
        this._erpNextService = erpNextService;
    }

    public ERPNextResourceResponse getListPurchaseOrderHave(String quotation) {
        String endpoint = "/resource/Purchase Order";
        List<List<Object>> filtres = Arrays.asList(
            Arrays.asList("supplier", "=", quotation)
        );

        ERPNextRequest request = new ERPNextRequest();
        List<Object> singleFilter = new ArrayList<>();
        List<List<Object>> filters = new ArrayList<>();
        singleFilter.add("status");
        singleFilter.add("in");

        List<String> values = new ArrayList<>();
        values.add("To bild");
        values.add("Completed");
        values.add("Closed");
        values.add("Delivered");

        singleFilter.add(values);

        filters.add(singleFilter);
        request.setFilters(filters);
        request.getFilters().addAll(filtres);
        return _erpNextService.getItems(endpoint, request);
    }

    public ERPNextResourceResponse getListPurchaseOrderPayed(String quotation) {
        String endpoint = "/resource/Purchase Order";
        List<List<Object>> filtres = Arrays.asList(
            Arrays.asList("supplier", "=", quotation)
        );

        ERPNextRequest request = new ERPNextRequest();
        List<Object> singleFilter = new ArrayList<>();
        List<List<Object>> filters = new ArrayList<>();
        singleFilter.add("status");
        singleFilter.add("in");

        List<String> values = new ArrayList<>();
        values.add("To receive");
        values.add("Completed");
        values.add("Closed");

        singleFilter.add(values);

        filters.add(singleFilter);
        request.setFilters(filters);
        request.getFilters().addAll(filtres);
        return _erpNextService.getItems(endpoint, request);
    }

   
}
