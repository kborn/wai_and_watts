package nz.waiwatts.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsLoggingFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(CorsLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String origin = httpRequest.getHeader("Origin");
        String method = httpRequest.getMethod();
        String requestURI = httpRequest.getRequestURI();
        
        // Log CORS-related requests
        if (method.equals("OPTIONS") || origin != null) {
            logger.info("CORS Request: {} {} from Origin: {}", method, requestURI, origin);
        }
        
        // Add CORS headers manually for OPTIONS requests
        if (method.equals("OPTIONS")) {
            httpResponse.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            httpResponse.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS");
            httpResponse.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type, Request-Id, Authorization");
            httpResponse.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Request-Id");
            httpResponse.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            httpResponse.setHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        
        try {
            chain.doFilter(request, response);
        } finally {
            // Add CORS headers to all responses
            if (origin != null) {
                httpResponse.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
                httpResponse.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Request-Id");
            }
        }
    }
}
