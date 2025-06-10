package itu.mg.erp.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import itu.mg.erp.features.request.ERPNextRequest;
import itu.mg.erp.response.ERPNextResourceResponse;
import itu.mg.erp.response.ERPNextResourceSingleResponse;

@Service
public class EmployeeService {

    private final ERPNextService _erpNextService;

    public EmployeeService(ERPNextService erpNextService) {
        this._erpNextService = erpNextService;
    }

    public ERPNextResourceResponse getListEmployee() {
        String endpoint = "/resource/Employee";
        ERPNextRequest request = new ERPNextRequest();
        return _erpNextService.getItems(endpoint, request);
    }

    public ERPNextResourceSingleResponse getEmployee(String name) {
        String endpoint = "/resource/Employee/" + name;
        ERPNextRequest request = new ERPNextRequest();
        return _erpNextService.getSingleItems(endpoint, request);
    }

    public ERPNextResourceResponse getSalarySplits(String idEmploye) {
        String endpoint = String.format(
                "/resource/Salary Slip");
        ERPNextRequest request = new ERPNextRequest();
        List<List<Object>> filters = Arrays.asList(
                Arrays.asList("employee", "=", idEmploye));
        request.setFilters(filters);
        return _erpNextService.getItems(endpoint, request);
    }

    public ERPNextResourceResponse getAllSalarySlips() {
        String endpoint = "/resource/Salary Slip?limit_page_length=none";
        ERPNextRequest request = new ERPNextRequest();
        return _erpNextService.getItems(endpoint, request);
    }

    public ERPNextResourceSingleResponse getSalarySplit(String name) {
        String endpoint = "/resource/Salary Slip/" + name;
        ERPNextRequest request = new ERPNextRequest();
        return _erpNextService.getSingleItems(endpoint, request);
    }



}
