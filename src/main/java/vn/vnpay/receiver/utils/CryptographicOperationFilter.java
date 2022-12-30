package vn.vnpay.receiver.utils;

//import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
//import lombok.extern.slf4j.Slf4j;
//import org.slf4j.MDC;
//import org.springframework.core.annotation.Order;
//import org.springframework.stereotype.Component;
//
//import javax.servlet.*;
//import javax.servlet.http.HttpServletRequest;
//import java.io.IOException;
//
//@Component
//@Order(1)
//@Slf4j
//public class CryptographicOperationFilter implements Filter {
//
//    @Override
//    public void doFilter(
//            ServletRequest request,
//            ServletResponse response,
//            FilterChain chain) throws IOException, ServletException {
//
//        HttpServletRequest req = (HttpServletRequest) request;
//        MDC.put("token", NanoIdUtils.randomNanoId());
//        log.info(
//                "Starting a transaction for req : {}",
//                req.getRequestURI());
//
//        chain.doFilter(request, response);
//        log.info(
//                "Committing a transaction for req : {}",
//                req.getRequestURI());
//    }
//    // other methods
//}
