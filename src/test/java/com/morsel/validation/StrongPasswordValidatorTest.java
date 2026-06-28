package com.morsel.validation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Payload;
import java.lang.annotation.Annotation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("StrongPasswordValidator")
class StrongPasswordValidatorTest {

    private StrongPasswordValidator validator;

    @BeforeEach
    void setUp() {
        validator = new StrongPasswordValidator();
        validator.initialize(new StubStrongPassword());
    }

    @Test
    @DisplayName("returns true for valid password")
    void isValid_validPassword_returnsTrue() {
        assertThat(validator.isValid("Secure@123", null)).isTrue();
    }

    @Test
    @DisplayName("returns true for null value")
    void isValid_null_returnsTrue() {
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @Test
    @DisplayName("returns false when too short")
    void isValid_tooShort_returnsFalse() {
        assertThat(validator.isValid("Sec@1a", null)).isFalse();
    }

    @Test
    @DisplayName("returns false when missing uppercase")
    void isValid_noUppercase_returnsFalse() {
        assertThat(validator.isValid("secure@123", null)).isFalse();
    }

    @Test
    @DisplayName("returns false when missing lowercase")
    void isValid_noLowercase_returnsFalse() {
        assertThat(validator.isValid("SECURE@123", null)).isFalse();
    }

    @Test
    @DisplayName("returns false when missing digit")
    void isValid_noDigit_returnsFalse() {
        assertThat(validator.isValid("Secure@abc", null)).isFalse();
    }

    @Test
    @DisplayName("returns false when missing special character")
    void isValid_noSpecialChar_returnsFalse() {
        assertThat(validator.isValid("SecureA123", null)).isFalse();
    }

    @Test
    @DisplayName("accepts hyphen as special character")
    void isValid_hyphen_returnsTrue() {
        assertThat(validator.isValid("Secure-123", null)).isTrue();
    }

    @Test
    @DisplayName("accepts underscore as special character")
    void isValid_underscore_returnsTrue() {
        assertThat(validator.isValid("Secure_123", null)).isTrue();
    }

    @Test
    @DisplayName("accepts tilde as special character")
    void isValid_tilde_returnsTrue() {
        assertThat(validator.isValid("Secure~123", null)).isTrue();
    }

    @Test
    @DisplayName("accepts plus as special character")
    void isValid_plus_returnsTrue() {
        assertThat(validator.isValid("Secure+123", null)).isTrue();
    }

    @Test
    @DisplayName("accepts equals as special character")
    void isValid_equals_returnsTrue() {
        assertThat(validator.isValid("Secure=123", null)).isTrue();
    }

    @Test
    @DisplayName("accepts square brackets as special character")
    void isValid_squareBrackets_returnsTrue() {
        assertThat(validator.isValid("Secure[1]23", null)).isTrue();
    }

    @Test
    @DisplayName("accepts semicolon as special character")
    void isValid_semicolon_returnsTrue() {
        assertThat(validator.isValid("Secure;123", null)).isTrue();
    }

    @Test
    @DisplayName("accepts single quote as special character")
    void isValid_singleQuote_returnsTrue() {
        assertThat(validator.isValid("Secure'123", null)).isTrue();
    }

    @Test
    @DisplayName("accepts forward slash as special character")
    void isValid_forwardSlash_returnsTrue() {
        assertThat(validator.isValid("Secure/123", null)).isTrue();
    }

    @Test
    @DisplayName("accepts backtick as special character")
    void isValid_backtick_returnsTrue() {
        assertThat(validator.isValid("Secure`123", null)).isTrue();
    }

    @Test
    @DisplayName("accepts backslash as special character")
    void isValid_backslash_returnsTrue() {
        assertThat(validator.isValid("Secure\\123", null)).isTrue();
    }

    @Test
    @DisplayName("respects custom minLength")
    void isValid_customMinLength() {
        StrongPasswordValidator customValidator = new StrongPasswordValidator();
        customValidator.initialize(new StubStrongPassword(12));

        assertThat(customValidator.isValid("Short@1a", null)).isFalse();
        assertThat(customValidator.isValid("LongEnough@1a", null)).isTrue();
    }

    record StubStrongPassword(int minLength) implements StrongPassword {
        StubStrongPassword() {
            this(8);
        }

        @Override
        public String message() {
            return "";
        }

        @Override
        public Class<?>[] groups() {
            return new Class<?>[0];
        }

        @SuppressWarnings("unchecked")
        @Override
        public Class<? extends Payload>[] payload() {
            return new Class[0];
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return StrongPassword.class;
        }
    }
}
