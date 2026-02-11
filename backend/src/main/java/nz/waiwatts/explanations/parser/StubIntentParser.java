package nz.waiwatts.explanations.parser;

import nz.waiwatts.explanations.dto.ExplanationRequest;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.List;
import java.util.ArrayList;

/**
 * Stub rule-based intent parser for Phase 12 minimal implementation.
 * 
 * This implementation uses deterministic rules to map natural language to structured requests.
 * In a full LLM implementation, this would be replaced by an LLM provider.
 */
@Component("stubIntentParser")
public class StubIntentParser implements IntentParser {

    @Override
    public ExplanationRequest parseQuestion(String question) {
        // For now, implement rule-based parsing as a minimal implementation
        // In a full implementation, this would call an LLM with structured prompts
        ExplanationRequest request = parseWithRules(question);

        // Let service handle the null with refusal

        return request;
    }

    /**
     * Minimal rule-based parser for Phase 12 implementation.
     * 
     * A full implementation would use LLM function calling with structured prompts.
     * This rule-based approach ensures deterministic behavior for basic patterns.
     */
    private ExplanationRequest parseWithRules(String question) {
        String lowerQuestion = question.toLowerCase().trim();
        
        // Parse dataset source
        String datasetSource = parseDatasetSource(lowerQuestion);
        if (datasetSource == null) {
            return null;
        }
        
        // Parse question type based on patterns
        String questionType = parseQuestionType(lowerQuestion, datasetSource);
        if (questionType == null) {
            return null;
        }
        
        // Parse filters
        Map<String, Object> filters = parseFilters(lowerQuestion);
        
        return new ExplanationRequest(questionType, datasetSource, filters);
    }
    
    private String parseDatasetSource(String question) {
        if (question.contains("mbie") || question.contains("electricity") || question.contains("generation")) {
            if (question.contains("quarter") || question.contains("quarterly")) {
                return "mbie.generation.quarterly";
            }
            return "mbie.generation.annual";
        }
        
        if (question.contains("lawa") || question.contains("water") || question.contains("quality")) {
            if (question.contains("trend") || question.contains("trends")) {
                return "lawa.water_quality.trend.multi_year";
            }
            return "lawa.water_quality.state.multi_year";
        }
        
        return null;
    }
    
    private String parseQuestionType(String question, String datasetSource) {
        if (datasetSource.startsWith("mbie")) {
            if (question.contains("renewable") && (question.contains("trend") || question.contains("change"))) {
                return "renewable_generation_trend";
            }
            if (question.contains("hydro") && (question.contains("trend") || question.contains("change"))) {
                return "hydro_generation_trend";
            }
            if ((question.contains("compare") || question.contains("versus") || question.contains("vs")) 
                && (question.contains("wind") && question.contains("hydro"))) {
                return "fuel_type_comparison";
            }
        }
        
        if (datasetSource.startsWith("lawa")) {
            if (question.contains("overview") || question.contains("summary")) {
                return "water_quality_overview";
            }
            if (question.contains("excellent") || question.contains("good")) {
                return "excellent_sites_trend";
            }
            if (question.contains("region") || question.contains("regional")) {
                if (datasetSource.contains("trend")) {
                    return "regional_trend_comparison";
                }
                return "regional_water_quality";
            }
            if (question.contains("trend") || question.contains("trends")) {
                return "water_quality_trends";
            }
            if (question.contains("improving") || question.contains("improve")) {
                return "improving_sites_trend";
            }
        }
        
        return null;
    }
    
    private Map<String, Object> parseFilters(String question) {
        Map<String, Object> filters = new java.util.HashMap<>();
        
        // Extract year patterns
        Pattern yearPattern = Pattern.compile("(\\d{4})");
        Matcher matcher = yearPattern.matcher(question);
        List<Integer> years = new ArrayList<>();
        while (matcher.find()) {
            int year = Integer.parseInt(matcher.group(1));
            if (year >= 1974 && year <= 2030) { // Reasonable range
                years.add(year);
            }
        }
        
        if (years.size() >= 2) {
            years.sort(Integer::compareTo);
            filters.put("startYear", years.getFirst());
            filters.put("endYear", years.getLast());
        } else if (years.size() == 1) {
            filters.put("startYear", years.getFirst());
            filters.put("endYear", years.getFirst());
        }
        
        // Extract fuel type for MBIE
        if (question.contains("wind")) {
            filters.put("fuelType", "WIND");
        } else if (question.contains("hydro")) {
            filters.put("fuelType", "HYDRO");
        } else if (question.contains("solar")) {
            filters.put("fuelType", "SOLAR");
        } else if (question.contains("geothermal")) {
            filters.put("fuelType", "GEOTHERMAL");
        }
        
        return filters.isEmpty() ? null : filters;
    }
}