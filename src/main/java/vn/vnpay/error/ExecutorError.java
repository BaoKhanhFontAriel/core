package vn.vnpay.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
@AllArgsConstructor
public class ExecutorError {
    private String resCode;
    private String errorMessage;
}
