package itu.mg.erp.response;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ERPNextResourceResponse {
    private List<Map<String, Object>> data; 

    public List<Map<String, Object>> getData() {
        return data;
    }

    public void setData(List<Map<String, Object>> data) {
        this.data = data;
    }

    public byte[] getPdfBytes() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPdfBytes'");
    }
}
