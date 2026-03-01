package nz.waiwatts.explanations.service;

import org.springframework.http.HttpStatus;

public enum AskRefusalCode {
    VALIDATION_FAILED("VALIDATION_FAILED", HttpStatus.BAD_REQUEST, "REQUEST", "Question is required"),
    UNSUPPORTED_CAPABILITY("UNSUPPORTED_CAPABILITY", HttpStatus.OK, "PARSE", "The requested capability is not supported."),
    MISSING_REQUIRED_FILTERS("MISSING_REQUIRED_FILTERS", HttpStatus.OK, "VALIDATION", "Required filters are missing."),
    DATASET_MISMATCH("DATASET_MISMATCH", HttpStatus.OK, "SELECTION", "Question type and dataset source are not a supported combination."),
    LLM_REQUIRED("LLM_REQUIRED", HttpStatus.OK, "PARSE", "Configure LLM API key to enable natural language questions."),
    UNABLE_TO_PARSE("UNABLE_TO_PARSE", HttpStatus.OK, "PARSE", "I can't confidently map this question to a supported explanation type."),
    NO_DATA("NO_DATA", HttpStatus.OK, "EXPLANATION", "Valid request, but no rows matched."),
    INTERNAL_ERROR("INTERNAL_ERROR", HttpStatus.OK, "EXCEPTION", "An internal error occurred while processing your request. Please try again.");

    private final String code;
    private final HttpStatus httpStatus;
    private final String debugTrigger;
    private final String defaultMessage;

    AskRefusalCode(String code, HttpStatus httpStatus, String debugTrigger, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.debugTrigger = debugTrigger;
        this.defaultMessage = defaultMessage;
    }

    public String code() {
        return code;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }

    public String debugTrigger() {
        return debugTrigger;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
