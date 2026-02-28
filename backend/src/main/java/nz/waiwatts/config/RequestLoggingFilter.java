package nz.waiwatts.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestLoggingFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        long startTime = System.currentTimeMillis();
        
        try {
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
            String path = getRequestPath(httpRequest);
            String method = httpRequest.getMethod();
            int status = httpResponse.getStatus();
            
            logger.info("Request completed: {} {} {} {}ms", 
                method, path, status, duration);
        }
    }
    
    private String getRequestPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String queryString = request.getQueryString();
        return queryString != null ? path + "?" + queryString : path;
    }
}