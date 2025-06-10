package itu.mg.erp.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import com.itextpdf.text.DocumentException;

import itu.mg.erp.response.ERPNextResourceResponse;
import itu.mg.erp.response.ERPNextResourceSingleResponse;
import itu.mg.erp.service.PDFService;
import jakarta.servlet.http.HttpServletResponse;
import itu.mg.erp.service.DatabaseService;
import itu.mg.erp.service.ERPNextService;
import itu.mg.erp.service.EmployeeService;

@Controller
@RequestMapping("/database")
public class DatabaseController {

    private final DatabaseService _databaseService;
    private ERPNextService _erpNextService = null;

    public DatabaseController(DatabaseService _databaseService, ERPNextService _erpNextService) {
        this._databaseService = _databaseService;
        this._erpNextService = _erpNextService;
    }

    @GetMapping
    public String getDatabaseManagementPage(Model model) {
        Object success = model.asMap().get("success");
        Object error = model.asMap().get("error");

        if (success != null) {
            System.out.println("Success: " + success.toString());
        }

        if (error != null) {
            System.out.println("Error: " + error.toString());
        }

        return "app/database-management";
    }

    @GetMapping("/reset")
    public String resetData(Model model) {
        _databaseService.resetData();
        return "redirect:/database";
    }

    @PostMapping("/csv")
    public ResponseEntity<?> uploadCsvFiles(
            @RequestParam("file1") MultipartFile file1,
            @RequestParam("file2") MultipartFile file2,
            @RequestParam("file3") MultipartFile file3,
            @RequestHeader(value = "Accept", defaultValue = "") String acceptHeader,
            RedirectAttributes redirectAttributes) {

        File tempFile1 = null;
        File tempFile2 = null;
        File tempFile3 = null;

        boolean isAjaxRequest = acceptHeader.contains("application/json") ||
                acceptHeader.contains("*/*");

        try {
            if (file1.isEmpty() || file2.isEmpty() || file3.isEmpty()) {
                String errorMessage = "Tous les fichiers CSV sont requis";

                if (isAjaxRequest) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("status", "error");
                    response.put("message", errorMessage);
                    response.put("details", List.of());
                    return ResponseEntity.badRequest().body(response);
                } else {
                    redirectAttributes.addFlashAttribute("error", errorMessage);
                    return ResponseEntity.status(HttpStatus.FOUND)
                            .location(URI.create("/database"))
                            .build();
                }
            }

            tempFile1 = _databaseService.convertToFile(file1);
            tempFile2 = _databaseService.convertToFile(file2);
            tempFile3 = _databaseService.convertToFile(file3);

            String url1 = _databaseService.uploadCsv(tempFile1);
            String url2 = _databaseService.uploadCsv(tempFile2);
            String url3 = _databaseService.uploadCsv(tempFile3);

            Map<String, Object> importResult = _databaseService.importAllFiles(url1, url2, url3);

            String status = (String) importResult.get("status");
            String message = (String) importResult.get("message");

            if (isAjaxRequest) {
                return ResponseEntity.ok(importResult);
            } else {
                if ("error".equals(status)) {
                    StringBuilder errorMessage = new StringBuilder(message);

                    @SuppressWarnings("unchecked")
                    List<String> details = (List<String>) importResult.get("details");
                    if (details != null && !details.isEmpty()) {
                        errorMessage.append(" - Détails: ");
                        errorMessage.append(String.join(", ", details));
                    }

                    redirectAttributes.addFlashAttribute("error", errorMessage.toString());
                } else {
                    redirectAttributes.addFlashAttribute("success", message);
                }

                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create("/database"))
                        .build();
            }

        } catch (HttpClientErrorException e) {
            String errorMessage = String.format("Erreur lors de l'import : %d %s - %s",
                    e.getStatusCode().value(),
                    e.getStatusText(),
                    e.getResponseBodyAsString());

            if (isAjaxRequest) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "Erreur HTTP lors de l'import");
                response.put("details", List.of(errorMessage));
                return ResponseEntity.status(e.getStatusCode()).body(response);
            } else {
                redirectAttributes.addFlashAttribute("error", errorMessage);
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create("/database"))
                        .build();
            }

        } catch (Exception e) {
            Throwable cause = e.getCause();
            while (cause != null) {
                System.err.println("Caused by: " + cause.getClass().getName() + " - " + cause.getMessage());
                cause = cause.getCause();
            }

            String errorMessage = "Import failed";
            if (e.getMessage() != null && !e.getMessage().isEmpty()) {
                errorMessage += ": " + e.getMessage();
            }

            if (isAjaxRequest) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "Erreur inattendue lors de l'import");
                response.put("details", List.of(errorMessage));
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            } else {
                redirectAttributes.addFlashAttribute("error", errorMessage);
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create("/database"))
                        .build();
            }

        } finally {
            _databaseService.cleanupTempFile(tempFile1);
            _databaseService.cleanupTempFile(tempFile2);
            _databaseService.cleanupTempFile(tempFile3);
        }
    }

}