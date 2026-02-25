package nz.waiwatts.explanations.service;

import nz.waiwatts.explanations.dataset.DatasetCatalog;
import nz.waiwatts.explanations.dataset.DatasetDescriptor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionTypeCatalogTest {

    @Test
    void supportedDescriptionsCoverAllCatalogQuestionTypes() {
        DatasetCatalog datasetCatalog = new DatasetCatalog();
        QuestionTypeCatalog catalog = new QuestionTypeCatalog(datasetCatalog);

        var descriptions = catalog.supportedDescriptions();
        for (DatasetDescriptor descriptor : datasetCatalog.getDatasets()) {
            for (String questionType : descriptor.supportedQuestionTypes()) {
                assertTrue(descriptions.containsKey(questionType),
                    "Missing description for question type: " + questionType);
                String description = descriptions.get(questionType);
                assertNotNull(description, "Description must not be null: " + questionType);
                assertFalse(description.isBlank(), "Description must not be blank: " + questionType);
            }
        }
    }

    @Test
    void generatesSafeDefaultDescriptionWhenNoOverrideExists() {
        DatasetCatalog customCatalog = new DatasetCatalog() {
            @Override
            public List<DatasetDescriptor> getDatasets() {
                return List.of(new DatasetDescriptor(
                    "custom.dataset",
                    "Custom Dataset",
                    "CUSTOM",
                    "annual",
                    List.of("novel_intent_without_override"),
                    Set.of("startYear", "endYear")
                ));
            }
        };

        QuestionTypeCatalog catalog = new QuestionTypeCatalog(customCatalog);
        String description = catalog.supportedDescriptions().get("novel_intent_without_override");

        assertNotNull(description);
        assertFalse(description.isBlank());
        assertTrue(description.toLowerCase().contains("novel intent without override"));
    }
}

