package com.thub.outpost.common.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Slice test: @WebMvcTest starts only the web layer (no database, no Flyway), so it is fast.
 * A throwaway controller throws each exception type and we check status, content type and body shape.
 */
@WebMvcTest(controllers = GlobalExceptionHandlerTest.ThrowawayController.class) // only our test controller
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.ThrowawayController.class})
@AutoConfigureMockMvc(addFilters = false) // skip the security filters; we only test error mapping here
@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void notFound_returns404WithEntityCode() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(get("/test/not-found/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.code").value("GRANT_NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("Grant with id " + id + " was not found"))
                .andExpect(jsonPath("$.instance").value("/test/not-found/" + id));
    }

    @Test
    void conflict_returns409() throws Exception {
        mockMvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void validationException_returns400() throws Exception {
        mockMvc.perform(get("/test/validation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("STAGE_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void validationException_withFieldIssues_returnsThemInErrors() throws Exception {
        mockMvc.perform(get("/test/validation-fields"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("stageIds"))
                .andExpect(jsonPath("$.errors[0].message").value("unknown stage id(s)"));
    }

    @Test
    void forbiddenException_returns403() throws Exception {
        mockMvc.perform(get("/test/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void invalidBody_returns400WithPerFieldErrors() throws Exception {
        mockMvc.perform(post("/test/body").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\",\"city\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[?(@.field == 'name')].message").isNotEmpty())
                .andExpect(jsonPath("$.errors[?(@.field == 'city')].message").isNotEmpty());
    }

    @Test
    void unreadableBody_returns400() throws Exception {
        mockMvc.perform(post("/test/body").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.detail").value("Request body is missing or malformed"));
    }

    @Test
    void accessDenied_returns403() throws Exception {
        mockMvc.perform(get("/test/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void authenticationFailure_returns401() throws Exception {
        mockMvc.perform(get("/test/unauthenticated"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                // generic message: must not echo the exception's own message
                .andExpect(jsonPath("$.detail").value("Authentication is required or the credentials are invalid"));
    }

    @Test
    void unexpectedError_returns500WithoutLeakingDetails() throws Exception {
        mockMvc.perform(get("/test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("at com.thub"))));
    }

    @Test
    void unknownUrl_returns404ProblemDetail() throws Exception {
        mockMvc.perform(get("/test/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void serverError_isLoggedAtErrorWithStackTrace(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/test/boom")).andExpect(status().isInternalServerError());

        assertThat(output.getOut()).contains("ERROR").contains("java.lang.IllegalStateException: secret internal detail");
        assertThat(output.getOut()).contains("\tat com.thub.outpost");
    }

    @Test
    void clientError_isLoggedAtWarnWithoutStackTrace(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/test/conflict")).andExpect(status().isConflict());

        assertThat(output.getOut()).contains("WARN").contains("Request rejected on /test/conflict -> 409");
        assertThat(output.getOut()).doesNotContain("\tat ");
    }

    /** Only exists in tests. Each endpoint throws one kind of exception. */
    @RestController
    static class ThrowawayController {

        @GetMapping("/test/not-found/{id}")
        void notFound(@org.springframework.web.bind.annotation.PathVariable UUID id) {
            throw new NotFoundException("Grant", id);
        }

        @GetMapping("/test/conflict")
        void conflict() {
            throw new ConflictException("EMAIL_TAKEN", "Email is already registered");
        }

        @GetMapping("/test/validation")
        void validation() {
            throw new ValidationException("STAGE_NOT_FOUND", "Stage does not exist");
        }

        @GetMapping("/test/validation-fields")
        void validationWithFields() {
            throw new ValidationException("VALIDATION_FAILED", "Validation failed",
                    java.util.List.of(new FieldIssue("stageIds", "unknown stage id(s)")));
        }

        @GetMapping("/test/forbidden")
        void forbidden() {
            throw new ForbiddenException("EMAIL_NOT_VERIFIED", "Verify your email first");
        }

        @PostMapping("/test/body")
        void body(@Valid @RequestBody Body body) {
        }

        @GetMapping("/test/access-denied")
        void accessDenied() {
            throw new AccessDeniedException("nope");
        }

        @GetMapping("/test/unauthenticated")
        void unauthenticated() {
            throw new BadCredentialsException("user alice has the wrong password");
        }

        @GetMapping("/test/boom")
        void boom() {
            throw new IllegalStateException("secret internal detail");
        }
    }

    record Body(@NotBlank String name, @Size(min = 2) String city) {
    }
}
