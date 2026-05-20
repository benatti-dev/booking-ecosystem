package com.booker.catalog;

import com.booker.catalog.entity.AttributeFieldType;
import com.booker.catalog.entity.ServiceAttributeDefinition;
import com.booker.catalog.service.AttributeDefinitionService;
import com.booker.catalog.service.ServiceAttributeValidator;
import com.booker.shared.exception.BookerException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ServiceAttributeValidator — Unit Tests")
class ServiceAttributeValidatorUnitTest {

    @Mock AttributeDefinitionService definitionService;

    @InjectMocks ServiceAttributeValidator validator;

    private static final long CATEGORY_ID = 1L;

    private ServiceAttributeDefinition required(String key, String label, AttributeFieldType type) {
        return ServiceAttributeDefinition.builder()
                .fieldKey(key)
                .fieldLabel(label)
                .fieldType(type)
                .isRequired(true)
                .sortOrder(0)
                .build();
    }

    private ServiceAttributeDefinition optional(String key, String label, AttributeFieldType type) {
        return ServiceAttributeDefinition.builder()
                .fieldKey(key)
                .fieldLabel(label)
                .fieldType(type)
                .isRequired(false)
                .sortOrder(0)
                .build();
    }

    private ServiceAttributeDefinition selectDef(String key, boolean required, List<String> options) {
        return ServiceAttributeDefinition.builder()
                .fieldKey(key)
                .fieldLabel(key)
                .fieldType(AttributeFieldType.SELECT)
                .options(options)
                .isRequired(required)
                .sortOrder(0)
                .build();
    }

    private ServiceAttributeDefinition multiSelectDef(String key, boolean required, List<String> options) {
        return ServiceAttributeDefinition.builder()
                .fieldKey(key)
                .fieldLabel(key)
                .fieldType(AttributeFieldType.MULTI_SELECT)
                .options(options)
                .isRequired(required)
                .sortOrder(0)
                .build();
    }

    // ── No definitions ──────────────────────────────────────────────────────

    @Test
    @DisplayName("no definitions for category → always passes")
    void noDefinitions_passes() {
        when(definitionService.getDefinitions(CATEGORY_ID)).thenReturn(List.of());
        assertThatCode(() -> validator.validate(CATEGORY_ID, Map.of("anything", "value")))
                .doesNotThrowAnyException();
    }

    // ── Required fields ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Required fields")
    class RequiredFields {

        @Test
        @DisplayName("required TEXT field absent → throws 400")
        void requiredTextField_absent_throws() {
            when(definitionService.getDefinitions(CATEGORY_ID))
                    .thenReturn(List.of(required("color", "Color", AttributeFieldType.TEXT)));

            assertThatThrownBy(() -> validator.validate(CATEGORY_ID, Map.of()))
                    .isInstanceOf(BookerException.class)
                    .hasMessageContaining("Color");
        }

        @Test
        @DisplayName("required TEXT field blank → throws 400")
        void requiredTextField_blank_throws() {
            when(definitionService.getDefinitions(CATEGORY_ID))
                    .thenReturn(List.of(required("color", "Color", AttributeFieldType.TEXT)));

            assertThatThrownBy(() -> validator.validate(CATEGORY_ID, Map.of("color", "  ")))
                    .isInstanceOf(BookerException.class)
                    .hasMessageContaining("Color");
        }

        @Test
        @DisplayName("optional field absent → passes")
        void optionalField_absent_passes() {
            when(definitionService.getDefinitions(CATEGORY_ID))
                    .thenReturn(List.of(optional("note", "Note", AttributeFieldType.TEXT)));

            assertThatCode(() -> validator.validate(CATEGORY_ID, Map.of()))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("null attributes map treated as empty → required field triggers error")
        void nullAttributeMap_requiredFieldThrows() {
            when(definitionService.getDefinitions(CATEGORY_ID))
                    .thenReturn(List.of(required("color", "Color", AttributeFieldType.TEXT)));

            assertThatThrownBy(() -> validator.validate(CATEGORY_ID, null))
                    .isInstanceOf(BookerException.class);
        }
    }

    // ── Type validation ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Type validation")
    class TypeValidation {

        @Test
        @DisplayName("TEXT accepts any string value")
        void text_anyString_passes() {
            when(definitionService.getDefinitions(CATEGORY_ID))
                    .thenReturn(List.of(required("note", "Note", AttributeFieldType.TEXT)));

            assertThatCode(() -> validator.validate(CATEGORY_ID, Map.of("note", "hello world")))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("NUMBER with Integer value passes")
        void number_integerValue_passes() {
            when(definitionService.getDefinitions(CATEGORY_ID))
                    .thenReturn(List.of(required("capacity", "Capacity", AttributeFieldType.NUMBER)));

            assertThatCode(() -> validator.validate(CATEGORY_ID, Map.of("capacity", 10)))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("NUMBER with numeric string passes")
        void number_numericString_passes() {
            when(definitionService.getDefinitions(CATEGORY_ID))
                    .thenReturn(List.of(required("capacity", "Capacity", AttributeFieldType.NUMBER)));

            assertThatCode(() -> validator.validate(CATEGORY_ID, Map.of("capacity", "42.5")))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("NUMBER with non-numeric string → throws 400")
        void number_nonNumericString_throws() {
            when(definitionService.getDefinitions(CATEGORY_ID))
                    .thenReturn(List.of(required("capacity", "Capacity", AttributeFieldType.NUMBER)));

            assertThatThrownBy(() -> validator.validate(CATEGORY_ID, Map.of("capacity", "abc")))
                    .isInstanceOf(BookerException.class)
                    .hasMessageContaining("Capacity");
        }

        @Test
        @DisplayName("BOOLEAN with Boolean true passes")
        void boolean_true_passes() {
            when(definitionService.getDefinitions(CATEGORY_ID))
                    .thenReturn(List.of(required("active", "Active", AttributeFieldType.BOOLEAN)));

            assertThatCode(() -> validator.validate(CATEGORY_ID, Map.of("active", true)))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("BOOLEAN with string → throws 400")
        void boolean_string_throws() {
            when(definitionService.getDefinitions(CATEGORY_ID))
                    .thenReturn(List.of(required("active", "Active", AttributeFieldType.BOOLEAN)));

            assertThatThrownBy(() -> validator.validate(CATEGORY_ID, Map.of("active", "yes")))
                    .isInstanceOf(BookerException.class)
                    .hasMessageContaining("Active");
        }
    }

    // ── SELECT / MULTI_SELECT ───────────────────────────────────────────────

    @Nested
    @DisplayName("SELECT and MULTI_SELECT")
    class SelectTypes {

        @Test
        @DisplayName("SELECT with valid option passes")
        void select_validOption_passes() {
            when(definitionService.getDefinitions(CATEGORY_ID))
                    .thenReturn(List.of(selectDef("color", true, List.of("red", "blue"))));

            assertThatCode(() -> validator.validate(CATEGORY_ID, Map.of("color", "red")))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("SELECT with invalid option → throws 400")
        void select_invalidOption_throws() {
            when(definitionService.getDefinitions(CATEGORY_ID))
                    .thenReturn(List.of(selectDef("color", true, List.of("red", "blue"))));

            assertThatThrownBy(() -> validator.validate(CATEGORY_ID, Map.of("color", "green")))
                    .isInstanceOf(BookerException.class)
                    .hasMessageContaining("red");
        }

        @Test
        @DisplayName("MULTI_SELECT with all valid options passes")
        void multiSelect_allValid_passes() {
            when(definitionService.getDefinitions(CATEGORY_ID))
                    .thenReturn(List.of(multiSelectDef("features", true, List.of("wifi", "parking", "ac"))));

            assertThatCode(() -> validator.validate(CATEGORY_ID, Map.of("features", List.of("wifi", "parking"))))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("MULTI_SELECT with one invalid option → throws 400")
        void multiSelect_invalidOption_throws() {
            when(definitionService.getDefinitions(CATEGORY_ID))
                    .thenReturn(List.of(multiSelectDef("features", true, List.of("wifi", "parking"))));

            assertThatThrownBy(() -> validator.validate(CATEGORY_ID, Map.of("features", List.of("wifi", "pool"))))
                    .isInstanceOf(BookerException.class)
                    .hasMessageContaining("pool");
        }

        @Test
        @DisplayName("MULTI_SELECT with non-list value → throws 400")
        void multiSelect_nonList_throws() {
            when(definitionService.getDefinitions(CATEGORY_ID))
                    .thenReturn(List.of(multiSelectDef("features", true, List.of("wifi"))));

            assertThatThrownBy(() -> validator.validate(CATEGORY_ID, Map.of("features", "wifi")))
                    .isInstanceOf(BookerException.class);
        }
    }
}
