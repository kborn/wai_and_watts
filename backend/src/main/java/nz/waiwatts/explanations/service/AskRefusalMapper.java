package nz.waiwatts.explanations.service;

import org.springframework.stereotype.Component;

@Component
public class AskRefusalMapper {

    public AskRefusal refusalForMissingQuestion() {
        return new AskRefusal(AskRefusalCode.VALIDATION_FAILED, AskRefusalCode.VALIDATION_FAILED.defaultMessage());
    }

    public AskRefusal fromParserRefusal(String category, String message) {
        return new AskRefusal(switch (normalize(category)) {
            case "UNSUPPORTED_INTENT", "UNSUPPORTED_QUESTION_TYPE" -> AskRefusalCode.UNSUPPORTED_CAPABILITY;
            case "LLM_REQUIRED" -> AskRefusalCode.LLM_REQUIRED;
            case "UNABLE_TO_PARSE" -> AskRefusalCode.UNABLE_TO_PARSE;
            case "UNSUPPORTED_CAPABILITY" -> AskRefusalCode.UNSUPPORTED_CAPABILITY;
            default -> AskRefusalCode.UNABLE_TO_PARSE;
        }, firstNonBlank(message, AskRefusalCode.UNABLE_TO_PARSE.defaultMessage()));
    }

    public AskRefusal fromSelectionRefusal(String category, String message) {
        return new AskRefusal(switch (normalize(category)) {
            case "DATASET_MISMATCH" -> AskRefusalCode.DATASET_MISMATCH;
            case "LLM_REQUIRED" -> AskRefusalCode.LLM_REQUIRED;
            default -> AskRefusalCode.UNSUPPORTED_CAPABILITY;
        }, firstNonBlank(message, AskRefusalCode.UNSUPPORTED_CAPABILITY.defaultMessage()));
    }

    public AskRefusal fromValidationRefusal(String category, String message) {
        return new AskRefusal(switch (normalize(category)) {
            case "UNSUPPORTED_QUESTION_TYPE", "UNSUPPORTED_CAPABILITY" -> AskRefusalCode.UNSUPPORTED_CAPABILITY;
            case "MISSING_REQUIRED_FILTERS" -> AskRefusalCode.MISSING_REQUIRED_FILTERS;
            case "DATASET_MISMATCH" -> AskRefusalCode.DATASET_MISMATCH;
            default -> AskRefusalCode.VALIDATION_FAILED;
        }, firstNonBlank(message, AskRefusalCode.VALIDATION_FAILED.defaultMessage()));
    }

    public AskRefusal fromExplanationRefusal(String reason) {
        AskRefusalCode code;
        String message = reason;
        if (reason == null || reason.isBlank()) {
            code = AskRefusalCode.INTERNAL_ERROR;
            message = code.defaultMessage();
        } else if (reason.startsWith("Invalid request:")) {
            code = AskRefusalCode.VALIDATION_FAILED;
        } else if (reason.startsWith("No facts available")) {
            code = AskRefusalCode.NO_DATA;
            message = code.defaultMessage();
        } else if (reason.startsWith("No data source available")) {
            code = AskRefusalCode.UNSUPPORTED_CAPABILITY;
        } else if (reason.startsWith("Unable to build FactPack")) {
            code = AskRefusalCode.UNSUPPORTED_CAPABILITY;
        } else if (reason.startsWith("FactPack missing")) {
            code = AskRefusalCode.INTERNAL_ERROR;
        } else if (reason.startsWith("Generated explanation missing required citations")) {
            code = AskRefusalCode.INTERNAL_ERROR;
        } else if (reason.startsWith("Explanation provider failed")) {
            code = AskRefusalCode.INTERNAL_ERROR;
        } else {
            code = AskRefusalCode.UNSUPPORTED_CAPABILITY;
        }
        return new AskRefusal(code, firstNonBlank(message, code.defaultMessage()));
    }

    public AskRefusal internalError() {
        return new AskRefusal(AskRefusalCode.INTERNAL_ERROR, AskRefusalCode.INTERNAL_ERROR.defaultMessage());
    }

    public record AskRefusal(AskRefusalCode code, String message) {}

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String firstNonBlank(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }
}
