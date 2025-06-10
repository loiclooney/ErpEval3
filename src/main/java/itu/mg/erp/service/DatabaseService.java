package itu.mg.erp.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import itu.mg.erp.config.ApiClient;
import itu.mg.erp.features.request.ERPNextRequest;
import itu.mg.erp.response.ERPNextResourceResponse;
import itu.mg.erp.response.ERPNextResourceSingleResponse;

@Service
public class DatabaseService {

    private final ERPNextService _erpNextService;
    private final ApiClient _apiClient;

    public DatabaseService(ERPNextService erpNextService, ApiClient apiClient) {
        this._apiClient = apiClient;
        this._erpNextService = erpNextService;
    }

    public void resetData() {
        String endpoint = "/method/erpnext.data_management.page.data_management.reinit_base";
        ParameterizedTypeReference<ERPNextResourceSingleResponse> responseType = new ParameterizedTypeReference<ERPNextResourceSingleResponse>() {
        };
        _apiClient.callApi(endpoint, HttpMethod.POST, null, responseType);
    }

    public String uploadCsv(File file) {
        String url = "http://eval.local:8000/api/method/upload_file";

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(file));
        body.add("is_private", "0");
        body.add("folder", "Home");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("Authorization", "token 44c1663c1a73d70:5c3867d0adc2f1c");

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            // Exemple de structure JSON donnée par toi :
            // { "message": { ..., "file_url": "/files/import-p16 - fichier1.csv", ... } }

            if (root.has("message") && root.get("message").has("file_url")) {
                return root.get("message").get("file_url").asText();
            } else {
                throw new RuntimeException("Réponse invalide: pas de file_url");
            }
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du parsing de la réponse", e);
        }
    }

    public File convertToFile(MultipartFile multipartFile) throws IOException {
        File convFile = File.createTempFile("upload-", multipartFile.getOriginalFilename());
        try (FileOutputStream fos = new FileOutputStream(convFile)) {
            fos.write(multipartFile.getBytes());
        }
        return convFile;
    }

    public void cleanupTempFile(File tempFile) {
        if (tempFile != null && tempFile.exists()) {
            try {
                tempFile.delete();
            } catch (Exception e) {
                System.err.println("Failed to delete temp file: " + tempFile.getAbsolutePath());
            }
        }
    }

    public Map<String, Object> importAllFiles(String file1Url, String file2Url, String file3Url) {
        String url = "http://eval.local:8000/api/method/erpnext.data_management.page.datarh.import_all";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "token 44c1663c1a73d70:5c3867d0adc2f1c");

        Map<String, String> body = new HashMap<>();
        body.put("file1_url", file1Url);
        body.put("file2_url", file2Url);
        body.put("file3_url", file3Url);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
        RestTemplate restTemplate = new RestTemplate();

        Map<String, Object> result = new HashMap<>();

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            String responseBody = response.getBody();

            if (!response.getStatusCode().is2xxSuccessful()) {
                result.put("status", "error");
                result.put("message", "Erreur HTTP : " + response.getStatusCode());
                result.put("details", List.of(responseBody != null ? responseBody : "Pas de détails disponibles"));
                return result;
            }

            // Vérification des erreurs ERPNext dans la réponse
            if (responseBody != null && (responseBody.toLowerCase().contains("traceback") ||
                    responseBody.toLowerCase().contains("error") ||
                    responseBody.toLowerCase().contains("exception"))) {
                result.put("status", "error");
                result.put("message", "Erreur ERPNext détectée dans la réponse");
                result.put("details", List.of(responseBody));
                return result;
            }

            // Succès
            result.put("status", "success");
            result.put("message", "Importation réussie");
            result.put("details", responseBody != null ? List.of(responseBody) : List.of());
            return result;

        } catch (HttpClientErrorException e) {
            result.put("status", "error");
            result.put("message", "Erreur HTTP client : " + e.getStatusCode());
            result.put("details", List.of(e.getResponseBodyAsString()));
            return result;
        } catch (HttpServerErrorException e) {
            result.put("status", "error");
            result.put("message", "Erreur serveur HTTP : " + e.getStatusCode());
            result.put("details", List.of(e.getResponseBodyAsString()));
            return result;
        } catch (ResourceAccessException e) {
            result.put("status", "error");
            result.put("message", "Erreur de connexion au serveur");
            result.put("details", List.of(e.getMessage()));
            return result;
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "Erreur inattendue lors de l'importation");
            result.put("details", List.of(e.getMessage() != null ? e.getMessage() : "Erreur inconnue"));
            return result;
        }
    }

}
