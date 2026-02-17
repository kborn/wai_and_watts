package nz.waiwatts.explanations.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response from Intent Parser containing either a parsed ExplanationRequest or a refusal.
 * 
 * Follows Phase 12 NL intent contract with deterministic success/failure handling.
 */
public class IntentParseResponse {
    
    @JsonProperty("ok")
    private boolean ok;
    
    private ExplanationRequest request;
    
    private RefusalResponse refusal;

    @JsonProperty("parserUsed")
    private String parserUsed;

    public IntentParseResponse() {}

    public static IntentParseResponse success(ExplanationRequest request) {
        IntentParseResponse response = new IntentParseResponse();
        response.ok = true;
        response.request = request;
        return response;
    }

    public static IntentParseResponse refusal(String category, String message) {
        IntentParseResponse response = new IntentParseResponse();
        response.ok = false;
        response.refusal = new RefusalResponse(category, message);
        return response;
    }

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public ExplanationRequest getRequest() {
        return request;
    }

    public void setRequest(ExplanationRequest request) {
        this.request = request;
    }

    public RefusalResponse getRefusal() {
        return refusal;
    }

    public void setRefusal(RefusalResponse refusal) {
        this.refusal = refusal;
    }

    public String getParserUsed() {
        return parserUsed;
    }

    public void setParserUsed(String parserUsed) {
        this.parserUsed = parserUsed;
    }

    /**
     * Inner class for refusal details following Phase 12 taxonomy.
     */
    public static class RefusalResponse {
        @JsonProperty("category")
        private String category;
        
        @JsonProperty("message")
        private String message;

        public RefusalResponse() {}

        public RefusalResponse(String category, String message) {
            this.category = category;
            this.message = message;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
