package vn.vnpay.receiver.model;


import lombok.AllArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@ToString
public class ApiResponse {
    private String resCode;
    private String message;
    private String data;


}
