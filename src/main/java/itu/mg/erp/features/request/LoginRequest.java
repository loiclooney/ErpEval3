package itu.mg.erp.features.request;

import lombok.Data;

@Data
public class LoginRequest {
    private String usr;
    private String pwd;
}
