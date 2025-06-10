package itu.mg.erp.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.itextpdf.text.DocumentException;

import itu.mg.erp.response.ERPNextResourceResponse;
import itu.mg.erp.response.ERPNextResourceSingleResponse;
import itu.mg.erp.service.PDFService;
import itu.mg.erp.service.SupplierService;

@Controller
@RequestMapping("/suppliers")
public class SupplierController {

    private final SupplierService _supplierService;
    private final PDFService _pdfService;

    public SupplierController(SupplierService supplierService, PDFService pdfService) {
        this._pdfService = pdfService;
        this._supplierService = supplierService;
    }

    @GetMapping
    public String getListSuppliers(
        Model model
    ) {
        ERPNextResourceResponse data = _supplierService.getListSupplier();
        model.addAttribute("suppliers", data.getData());
        return "app/supplier-list"; 
    }

    
    @GetMapping(value = "/export/{name}")
    public ResponseEntity<byte[]> exportPdf(
             @PathVariable("name") String name,
             Model model
            ) throws IllegalArgumentException, IllegalAccessException {
        try {
            ERPNextResourceSingleResponse data = _supplierService.getSupplier(name);
            Map<String, List<String>> cols = Map.of(
                "items", List.of("item_code","item_name","qty","rate","amount"),
                "payment_schedule", List.of("due_date","payment_amount","outstanding")
            );
            byte[] pdf = _pdfService.generate(data.getData(), cols);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentDispositionFormData("attachment", PDFService.generateUniqueFileName(data.getData()));
            return ResponseEntity.ok().headers(headers).body(pdf);
        } catch (DocumentException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } 
    }
}

