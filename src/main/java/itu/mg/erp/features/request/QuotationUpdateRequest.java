package itu.mg.erp.features.request;

import lombok.Data;

@Data
public class QuotationUpdateRequest {
    private Double rate;
    private int docstatus;
}
