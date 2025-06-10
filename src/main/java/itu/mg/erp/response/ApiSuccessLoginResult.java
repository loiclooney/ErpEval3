package itu.mg.erp.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApiSuccessLoginResult<T> {
    private String message;
    private String home_page;
    private String full_name;

}
