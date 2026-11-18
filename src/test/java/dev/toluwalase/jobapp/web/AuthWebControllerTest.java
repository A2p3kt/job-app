package dev.toluwalase.jobapp.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import dev.toluwalase.jobapp.domain.AppUser;
import dev.toluwalase.jobapp.domain.AppUserRepository;
import dev.toluwalase.jobapp.domain.JobCategory;
import dev.toluwalase.jobapp.service.ProfileService;

/**
 * Controller slice unit tests for AuthWebController.
 * Validates Sign In, Sign Up, validation error branches, password hashing, and Sign Out.
 */
@ExtendWith(MockitoExtension.class)
public class AuthWebControllerTest {

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private ProfileService profileService;

    private MockMvc mockMvc;
    private AppUser existingUser;

    @BeforeEach
    void setUp() {
        AuthWebController controller = new AuthWebController(userRepository, profileService);

        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setViewResolvers(viewResolver)
                .build();

        String hashedPassword = SecurityConfig.hashPassword("secret123");
        existingUser = new AppUser("tolu", "tolu@example.com", hashedPassword, "LOCAL", "ROLE_USER");
        existingUser.setId(1L);
    }

    @Test
    @DisplayName("GET /signin renders signin view when unauthenticated")
    void testShowSignIn() throws Exception {
        mockMvc.perform(get("/signin"))
                .andExpect(status().isOk())
                .andExpect(view().name("signin"));
    }

    @Test
    @DisplayName("GET /signin redirects to /dashboard when already authenticated in session")
    void testShowSignInAlreadyAuthenticated() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("username", "tolu");

        mockMvc.perform(get("/signin").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @DisplayName("POST /signin with valid credentials sets session and redirects to /profile")
    void testProcessSignInSuccess() throws Exception {
        when(userRepository.findByUsername("tolu")).thenReturn(Optional.of(existingUser));

        mockMvc.perform(post("/signin")
                        .param("username", "tolu")
                        .param("password", "secret123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @DisplayName("POST /signin with valid email sets session and redirects to /profile")
    void testProcessSignInWithEmailSuccess() throws Exception {
        when(userRepository.findByUsername("tolu@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("tolu@example.com")).thenReturn(Optional.of(existingUser));

        mockMvc.perform(post("/signin")
                        .param("username", "tolu@example.com")
                        .param("password", "secret123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @DisplayName("POST /signin with blank fields re-renders signin with error")
    void testProcessSignInBlankCredentials() throws Exception {
        mockMvc.perform(post("/signin")
                        .param("username", "")
                        .param("password", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("signin"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("POST /signin with wrong password re-renders signin with error")
    void testProcessSignInWrongPassword() throws Exception {
        when(userRepository.findByUsername("tolu")).thenReturn(Optional.of(existingUser));

        mockMvc.perform(post("/signin")
                        .param("username", "tolu")
                        .param("password", "wrongPassword"))
                .andExpect(status().isOk())
                .andExpect(view().name("signin"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("POST /signin with unknown username re-renders signin with error")
    void testProcessSignInUnknownUser() throws Exception {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(post("/signin")
                        .param("username", "nonexistent")
                        .param("password", "secret123"))
                .andExpect(status().isOk())
                .andExpect(view().name("signin"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("GET /signup renders signup view with industry categories")
    void testShowSignUp() throws Exception {
        mockMvc.perform(get("/signup"))
                .andExpect(status().isOk())
                .andExpect(view().name("signup"))
                .andExpect(model().attributeExists("categories"));
    }

    @Test
    @DisplayName("GET /signup redirects to /dashboard when already authenticated in session")
    void testShowSignUpAlreadyAuthenticated() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("username", "tolu");

        mockMvc.perform(get("/signup").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @DisplayName("POST /signup with valid data creates user and profile, sets session and redirects to onboarding")
    void testProcessSignUpSuccess() throws Exception {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);

        AppUser created = new AppUser("newuser", "newuser@example.com", "hash", "LOCAL", "ROLE_USER");
        created.setId(2L);
        when(profileService.createUserWithProfile(eq("newuser"), eq("newuser@example.com"), any(), eq(JobCategory.MARKETING)))
                .thenReturn(created);

        mockMvc.perform(post("/signup")
                        .param("username", "newuser")
                        .param("email", "newuser@example.com")
                        .param("password", "secure123")
                        .param("confirmPassword", "secure123")
                        .param("specialization", "MARKETING"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile?onboarding=true"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(profileService).createUserWithProfile(eq("newuser"), eq("newuser@example.com"), any(), eq(JobCategory.MARKETING));
    }

    @Test
    @DisplayName("POST /signup with short username fails validation")
    void testProcessSignUpShortUsername() throws Exception {
        mockMvc.perform(post("/signup")
                        .param("username", "ab")
                        .param("email", "ab@example.com")
                        .param("password", "secure123")
                        .param("confirmPassword", "secure123"))
                .andExpect(status().isOk())
                .andExpect(view().name("signup"))
                .andExpect(model().attributeExists("errorMessage"));

        verify(profileService, never()).createUserWithProfile(any(), any(), any(), any());
    }

    @Test
    @DisplayName("POST /signup with mismatched passwords fails validation")
    void testProcessSignUpPasswordMismatch() throws Exception {
        mockMvc.perform(post("/signup")
                        .param("username", "alexdev")
                        .param("email", "alex@example.com")
                        .param("password", "secure123")
                        .param("confirmPassword", "different123"))
                .andExpect(status().isOk())
                .andExpect(view().name("signup"))
                .andExpect(model().attributeExists("errorMessage"));

        verify(profileService, never()).createUserWithProfile(any(), any(), any(), any());
    }

    @Test
    @DisplayName("POST /signup with duplicate username rejects registration")
    void testProcessSignUpDuplicateUsername() throws Exception {
        when(userRepository.existsByUsername("tolu")).thenReturn(true);

        mockMvc.perform(post("/signup")
                        .param("username", "tolu")
                        .param("email", "other@example.com")
                        .param("password", "secure123")
                        .param("confirmPassword", "secure123"))
                .andExpect(status().isOk())
                .andExpect(view().name("signup"))
                .andExpect(model().attributeExists("errorMessage"));

        verify(profileService, never()).createUserWithProfile(any(), any(), any(), any());
    }

    @Test
    @DisplayName("GET /signout invalidates session and redirects to /signin with feedback")
    void testSignOut() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("username", "tolu");

        mockMvc.perform(get("/signout").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/signin"))
                .andExpect(flash().attributeExists("successMessage"));
    }
}
