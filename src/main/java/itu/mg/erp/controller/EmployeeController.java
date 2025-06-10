package itu.mg.erp.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.io.IOException;

import com.itextpdf.text.DocumentException;

import itu.mg.erp.response.ERPNextResourceResponse;
import itu.mg.erp.response.ERPNextResourceSingleResponse;
import itu.mg.erp.service.PDFService;
import itu.mg.erp.util.ReflectionUtil;
import jakarta.servlet.http.HttpServletResponse;
import itu.mg.erp.service.EmployeeService;

@Controller
@RequestMapping("/employee")
public class EmployeeController {

    private final EmployeeService _employeeService;
    private final PDFService _pdfService;

    public EmployeeController(EmployeeService employeeService, PDFService pdfService) {
        this._pdfService = pdfService;
        this._employeeService = employeeService;
    }

    @GetMapping
    public String getListEmployee(
            Model model) {
        ERPNextResourceResponse data = _employeeService.getListEmployee();
        
        model.addAttribute("employee", data.getData());
        return "app/employee-list";
    }

    @GetMapping("/{name}")
    public String ficheEmployee(@PathVariable String name, Model model) {
        ERPNextResourceSingleResponse response = _employeeService.getEmployee(name);
        model.addAttribute("employee", response.getData());
        return "app/employee-detail";
    }

    @GetMapping("/salaryslip/{name}")
    public String getSalarySlip(@PathVariable String name, Model model) {
        ERPNextResourceResponse response = _employeeService.getSalarySplits(name);
        model.addAttribute("salaryslip", response.getData());
        model.addAttribute("idEmployer", name);
        return "app/salaryslip";
    }

    @GetMapping("/salaryslips")
    public String getAllSalarySlips(Model model) {
        ERPNextResourceResponse response = _employeeService.getAllSalarySlips();
        model.addAttribute("salaryslip", response.getData());
        return "app/list-salary-slip";
    }

    @GetMapping(value = "/export")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam String salaryName,
            Model model) throws IllegalArgumentException, IllegalAccessException {
        try {
            ERPNextResourceSingleResponse fullData = _employeeService.getSalarySplit(salaryName);
            Map<String, Object> data = fullData.getData();

            // Extraction des données essentielles
            Map<String, Object> essentialData = new HashMap<>();
            essentialData.put("name", data.get("name"));
            essentialData.put("employee", data.get("employee"));
            essentialData.put("employee_name", data.get("employee_name"));
            essentialData.put("company", data.get("company"));
            essentialData.put("posting_date", data.get("posting_date"));
            essentialData.put("status", data.get("status"));
            essentialData.put("currency", data.get("currency"));
            essentialData.put("payroll_frequency", data.get("payroll_frequency"));
            essentialData.put("start_date", data.get("start_date"));
            essentialData.put("end_date", data.get("end_date"));
            essentialData.put("gross_pay", data.get("gross_pay"));
            essentialData.put("total_deduction", data.get("total_deduction"));
            essentialData.put("net_pay", data.get("net_pay"));
            essentialData.put("total_in_words", data.get("total_in_words"));

            // Récupérer earnings et deductions en gardant uniquement les champs essentiels
            List<Map<String, Object>> earnings = new ArrayList<>();
            List<Map<String, Object>> deductions = new ArrayList<>();

            if (data.get("earnings") instanceof List<?>) {
                List<?> rawEarnings = (List<?>) data.get("earnings");
                for (Object obj : rawEarnings) {
                    if (obj instanceof Map<?, ?>) {
                        Map<?, ?> earning = (Map<?, ?>) obj;
                        Map<String, Object> e = new HashMap<>();
                        e.put("salary_component", earning.get("salary_component"));
                        e.put("abbr", earning.get("abbr"));
                        e.put("amount", earning.get("amount"));
                        e.put("year_to_date", earning.get("year_to_date"));
                        earnings.add(e);
                    }
                }
            }

            if (data.get("deductions") instanceof List<?>) {
                List<?> rawDeductions = (List<?>) data.get("deductions");
                for (Object obj : rawDeductions) {
                    if (obj instanceof Map<?, ?>) {
                        Map<?, ?> deduction = (Map<?, ?>) obj;
                        Map<String, Object> d = new HashMap<>();
                        d.put("salary_component", deduction.get("salary_component"));
                        d.put("abbr", deduction.get("abbr"));
                        d.put("amount", deduction.get("amount"));
                        d.put("year_to_date", deduction.get("year_to_date"));
                        deductions.add(d);
                    }
                }
            }

            essentialData.put("earnings", earnings);
            essentialData.put("deductions", deductions);

            // Cols adaptés aux données RH simplifiées
            Map<String, List<String>> cols = Map.of(
                    "earnings", List.of("salary_component", "abbr", "amount", "year_to_date"),
                    "deductions", List.of("salary_component", "abbr", "amount", "year_to_date"));

            byte[] pdf = _pdfService.generate(essentialData, cols);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentDispositionFormData("attachment", PDFService.generateUniqueFileName(essentialData));
            return ResponseEntity.ok().headers(headers).body(pdf);

        } catch (DocumentException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}