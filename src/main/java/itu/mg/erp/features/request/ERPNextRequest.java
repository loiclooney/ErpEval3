package itu.mg.erp.features.request;

import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Data
public class ERPNextRequest {
    private List<String> fields; 
    private List<List<Object>> filters; 
    private Integer limitPageLength;                         
    private Integer limitStart;                              
    private String orderBy;                     
    private Boolean withCommentCount;                    
    private Boolean withChildnames;                      
    private Boolean includeMetadata;                     
    private Boolean distinct;                            
    private String parent;                                
    private String parenttype;                            
    private String parentfield;                           
    private Map<String, Object> additionalParams; 

    public ERPNextRequest() {
        this.fields = Collections.singletonList("*");
        this.filters = Collections.emptyList();
        this.limitPageLength = 100;
        this.limitStart = 0;
        this.orderBy = "modified desc";
        this.withCommentCount = false;
        this.withChildnames = true;
        this.includeMetadata = false;
        this.distinct = false;
        this.parent = null;
        this.parenttype = null;
        this.parentfield = null;
        this.additionalParams = Collections.emptyMap();
    }
}
