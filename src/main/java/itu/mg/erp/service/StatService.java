package itu.mg.erp.service;

import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

import itu.mg.erp.features.request.ERPNextRequest;
import itu.mg.erp.response.ERPNextResourceResponse;
import itu.mg.erp.response.ERPNextResourceSingleResponse;

@Service
public class StatService {
    private final ERPNextService _erpNextService;

    public StatService(ERPNextService _erpNextService) {
        this._erpNextService = _erpNextService;
    }

    public ERPNextResourceResponse getAllSalarySlips() {
        String endpoint = "/resource/Salary Slip?limit_page_length=none";
        ERPNextRequest request = new ERPNextRequest();
        return _erpNextService.getItems(endpoint, request);
    }

    public ERPNextResourceResponse getSalaryYM(String year, String month) {
        String endpoint = "/resource/Salary Slip";

        // Formater le début et la fin du mois (ex : "2025-05-01" à "2025-05-31")
        String debut = String.format("%s-%s-01", year, month);
        // On utilise YearMonth pour calculer automatiquement le dernier jour du mois
        YearMonth ym = YearMonth.of(Integer.parseInt(year), Integer.parseInt(month));
        String fin = String.format("%s-%02d-%02d", year, Integer.parseInt(month), ym.lengthOfMonth());

        ERPNextRequest request = new ERPNextRequest();
        List<List<Object>> filters = Arrays.asList(
                Arrays.asList("posting_date", "between", Arrays.asList(debut, fin)));
        request.setFilters(filters);

        return _erpNextService.getItems(endpoint, request);
    }

    public ERPNextResourceSingleResponse getSalarySplit(String name) {
        String endpoint = "/resource/Salary Slip/" + name;
        ERPNextRequest request = new ERPNextRequest();
        return _erpNextService.getSingleItems(endpoint, request);
    }

}
