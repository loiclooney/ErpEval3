package itu.mg.erp.service;

import org.springframework.stereotype.Service;

import itu.mg.erp.features.request.ERPNextRequest;
import itu.mg.erp.response.ERPNextResourceResponse;
import itu.mg.erp.response.ERPNextResourceSingleResponse;

@Service
public class SupplierService {

    private final ERPNextService _erpNextService;


    public SupplierService(ERPNextService erpNextService) {
        this._erpNextService = erpNextService;
    }

    public ERPNextResourceResponse getListSupplier() {
        String endpoint = "/resource/Supplier";
        ERPNextRequest request = new ERPNextRequest(); 
        return _erpNextService.getItems(endpoint, request);
    }

     public ERPNextResourceSingleResponse getSupplier(String name) {
        String endpoint = "/resource/Supplier/"+name;
        ERPNextRequest request = new ERPNextRequest();
        return _erpNextService.getSingleItems(endpoint, request);
    }
}
