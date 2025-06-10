package itu.mg.erp.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.itextpdf.text.DocumentException;

import itu.mg.erp.response.ERPNextResourceResponse;
import itu.mg.erp.response.ERPNextResourceSingleResponse;
import itu.mg.erp.service.PDFService;
import itu.mg.erp.service.PurchaseInvoiceService;

@Controller
@RequestMapping("/invoices")
public class PurchaseInvoiceController {

    private final PurchaseInvoiceService _purchaseInvoiceService;
    private final PDFService _pdfService;

    public PurchaseInvoiceController(PurchaseInvoiceService invoiceService, PDFService pdfservice) {
        this._purchaseInvoiceService = invoiceService;
        this._pdfService = pdfservice;
    }

    @GetMapping
    public String getListPurchaseInvoice(
        Model model
    ) {
        ERPNextResourceResponse data = _purchaseInvoiceService.getListPurchaseInvoice();
        model.addAttribute("invoices", data.getData());
        return "app/purchase-invoice-list"; 
    }

    @GetMapping("pay/{name}")
    public String getListPurchaseOrder(
        @PathVariable("name") String name,
        Model model
    ) {
        ERPNextResourceSingleResponse  data = _purchaseInvoiceService.getPurchaseInvoice(name);
        model.addAttribute("invoice", data.getData());
        return "app/payement-entry-form"; 
    }

    @PostMapping("/pay")
    public String payPaymentEntry(  
            @RequestParam String party,
            @RequestParam double paid_amount,
            @RequestParam String reference_name,
            Model model
    ) {
        Map<String, Object> paymentEntry = new HashMap<>();

        paymentEntry.put("docstatus", 1);
        paymentEntry.put("doctype", "Payment Entry");
        paymentEntry.put("payment_type", "Pay");
        paymentEntry.put("party_type", "Supplier");
        paymentEntry.put("party", party );
        paymentEntry.put("paid_from", "Cash - MC");
        paymentEntry.put("paid_to", "Creditors - MC");
        paymentEntry.put("paid_amount", paid_amount);
        paymentEntry.put("received_amount", paid_amount);

        Map<String, Object> reference = new HashMap<>();
        reference.put("reference_doctype", "Purchase Invoice");
        reference.put("reference_name", reference_name);
        reference.put("allocated_amount", paid_amount);

        paymentEntry.put("references", List.of(reference));

        _purchaseInvoiceService.payEntryForPurchaseInvoice(paymentEntry);
        return "redirect:/invoices";
    }

    @GetMapping(value = "/export/{name}")
    public ResponseEntity<byte[]> exportPdf(
             @PathVariable("name") String name,
             Model model
            ) throws IllegalArgumentException, IllegalAccessException {
        try {
            ERPNextResourceSingleResponse data = _purchaseInvoiceService.getPurchaseInvoice(name);
            System.out.println("Data: "+data.getData());
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

