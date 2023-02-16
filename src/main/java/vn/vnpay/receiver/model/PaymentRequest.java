package vn.vnpay.receiver.model;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class PaymentRequest {
    private String server;
    private String method;
    private String uri;
    private int http_status;
    private String code;
    private String mid ;
    private String requestid;
    private int processtime;
    private String client_ip;
    private String device;
    private String user;
}
