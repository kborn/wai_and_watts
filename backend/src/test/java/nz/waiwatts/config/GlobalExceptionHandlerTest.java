package nz.waiwatts.config;

import nz.waiwatts.explanations.dto.AskResult;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void returnsAskRefusalEnvelopeForAskEndpointUnexpectedException() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRequestURI("/api/v1/explanations/ask");
        WebRequest webRequest = new ServletWebRequest(servletRequest);

        ResponseEntity<Object> response = handler.handleAllExceptions(
            new RuntimeException("boom"),
            webRequest
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        AskResult body = assertInstanceOf(AskResult.class, response.getBody());
        assertTrue(body.isRefusal());
        assertEquals("INTERNAL_ERROR", body.getRefusal().getCode());
        assertEquals("EXCEPTION", body.getDebug().getRefusalTrigger());
    }

    @Test
    void returnsGeneric500EnvelopeForNonAskEndpointUnexpectedException() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRequestURI("/api/v1/mbie/generation/annual");
        WebRequest webRequest = new ServletWebRequest(servletRequest);

        ResponseEntity<Object> response = handler.handleAllExceptions(
            new RuntimeException("boom"),
            webRequest
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertNotNull(body);
        assertEquals("Internal server error", body.get("error"));
    }
}
