package vn.vnpay.receiver.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiRequest {
    private  String token;
    private  String data;
}
