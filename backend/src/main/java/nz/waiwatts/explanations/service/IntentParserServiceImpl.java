package nz.waiwatts.explanations.service;

import nz.waiwatts.explanations.dto.ExplanationRequest;
import nz.waiwatts.explanations.dto.IntentParseResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Implementation of IntentParserService using structured LLM prompts.
 * 
 * Follows Phase 12 NL intent parsing contract:
 * - Maps NL to structured ExplanationRequest
 * - Returns deterministic refusals for ambiguous/unsupported inputs
 * - Never generates facts or accesses database
 */
@Service
public class IntentParserServiceImpl implements IntentParserService {
    
    private static final Logger logger = LoggerFactory.getLogger(IntentParserServiceImpl.class);
    
    // Supported question types from contract
    private static final List<String> SUPPORTED_QUESTION_TYPES = List.of(
        // MBIE Generation Question Types
        "renewable_generation_trend",
        "hydro_generation_trend", 
        "fuel_type_comparison",
        
        // LAWA Water Quality State Question Types
        "water_quality_overview",
        "excellent_sites_trend",
        "regional_water_quality",
        
        // LAWA Water Quality Trend Question Types
        "water_quality_trends",
        "improving_sites_trend",
        "regional_trend_comparison"
    );
    
    // Supported dataset sources from contract
    private static final List<String> SUPPORTED_DATASET_SOURCES = List.of(
        "mbie.generation.annual",
        "mbie.generation.quarterly", 
        "lawa.water_quality.state.multi_year",
        "lawa.water_quality.trend.multi_year"
    );
    
    @Override
    public IntentParseResponse parseQuestion(String question) {
        logger.info("Parsing natural language question: {}", question);
        
        try {
            // For now, implement rule-based parsing as a minimal implementation
            // In a full implementation, this would call an LLM with structured prompts
            ExplanationRequest request = parseWithRules(question);
            
            if (request == null) {
                logger.warn("Unable to parse question with rules: {}", question);
                return IntentParseResponse.refusal("AMBIGUOUS_INTENT", 
                    "I can't confidently map this question to a supported explanation type. Try specifying the dataset and a time range.");
            }
            
            logger.info("Successfully parsed question to request: questionType={}, datasetSource={}, filters={}", 
                request.getQuestionType(), request.getDatasetSource(), request.getFilters());
            
            return IntentParseResponse.success(request);
            
        } catch (Exception e) {
            logger.error("Error parsing question: {}", question, e);
            return IntentParseResponse.refusal("AMBIGUOUS_INTENT", 
                "I encountered an error while parsing your question. Please try rephrasing it.");
        }
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
        java.util.regex.Pattern yearPattern = java.util.regex.Pattern.compile("(\\d{4})");
        java.util.regex.Matcher matcher = yearPattern.matcher(question);
        java.util.List<Integer> years = new java.util.ArrayList<>();
        while (matcher.find()) {
            int year = Integer.parseInt(matcher.group(1));
            if (year >= 1974 && year <= 2030) { // Reasonable range
                years.add(year);
            }
        }
        
        if (years.size() >= 2) {
            years.sort(Integer::compareTo);
            filters.put("startYear", years.get(0));
            filters.put("endYear", years.get(years.size() - 1));
        } else if (years.size() == 1) {
            filters.put("startYear", years.get(0));
            filters.put("endYear", years.get(0));
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