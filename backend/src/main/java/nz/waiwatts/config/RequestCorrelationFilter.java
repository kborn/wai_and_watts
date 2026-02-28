package nz.waiwatts.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCorrelationFilter implements Filter {

    private static final String REQUEST_ID_HEADER = "Request-Id";
    private static final String LEGACY_REQUEST_ID_HEADER = "X-Request-Id";
    private static final String MDC_REQUEST_ID_KEY = "requestId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String requestId = httpRequest.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.trim().isEmpty()) {
            requestId = httpRequest.getHeader(LEGACY_REQUEST_ID_HEADER);
        }
        if (requestId == null || requestId.trim().isEmpty()) {
            requestId = UUID.randomUUID().toString().substring(0, 8);
        }
        
        MDC.put(MDC_REQUEST_ID_KEY, requestId);
        httpResponse.setHeader(REQUEST_ID_HEADER, requestId);
        // Backward compatibility for existing clients still expecting X-Request-Id.
        httpResponse.setHeader(LEGACY_REQUEST_ID_HEADER, requestId);
        
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_REQUEST_ID_KEY);
        }
    }
}
