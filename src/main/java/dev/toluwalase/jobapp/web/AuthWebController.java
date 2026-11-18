package dev.toluwalase.jobapp.web;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import dev.toluwalase.jobapp.domain.AppUser;
import dev.toluwalase.jobapp.domain.AppUserRepository;
import dev.toluwalase.jobapp.domain.JobCategory;
import dev.toluwalase.jobapp.service.ProfileService;
import jakarta.servlet.http.HttpSession;

/**
 * Controller handling user authentication workflows:
 * Sign In, Sign Up (registration with industry specialization), and Sign Out.
 * Stores authenticated user identity in the HTTP session.
 */
@Controller
public class AuthWebController {

    private static final Logger log = LoggerFactory.getLogger(AuthWebController.class);

    private final AppUserRepository userRepository;
    private final ProfileService profileService;

    public AuthWebController(AppUserRepository userRepository, ProfileService profileService) {
        this.userRepository = userRepository;
        this.profileService = profileService;
    }

    /**
     * Renders the Sign In page.
     * Redirects to /dashboard if user is already authenticated.
     */
    @GetMapping({"/signin", "/login"})
    public String showSignIn(Model model, HttpSession session) {
        if (session != null && session.getAttribute("username") != null) {
            return "redirect:/dashboard";
        }
        return "signin";
    }

    /**
     * Authenticates candidate credentials using PBKDF2 salted hash verification.
     */
    @PostMapping({"/signin", "/login"})
    public String processSignIn(@RequestParam("username") String username,
                                @RequestParam("password") String password,
                                HttpSession session,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            model.addAttribute("errorMessage", "Please provide both username/email and password.");
            model.addAttribute("username", username);
            return "signin";
        }

        String identifier = username.trim();
        Optional<AppUser> userOpt = userRepository.findByUsername(identifier);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(identifier);
        }

        if (userOpt.isEmpty() || !SecurityConfig.verifyPassword(password, userOpt.get().getPasswordHash())) {
            log.warn("Failed sign-in attempt for identifier '{}'", identifier);
            model.addAttribute("errorMessage", "Invalid username or password. Please try again.");
            model.addAttribute("username", identifier);
            return "signin";
        }

        AppUser user = userOpt.get();
        session.setAttribute("username", user.getUsername());
        session.setAttribute("currentUser", user);

        log.info("User '{}' signed in successfully.", user.getUsername());
        redirectAttributes.addFlashAttribute("successMessage",
                "Welcome back, " + user.getUsername() + "!");
        return "redirect:/dashboard";
    }

    /**
     * Renders the Sign Up registration page with industry specialization selection.
     */
    @GetMapping({"/signup", "/register"})
    public String showSignUp(Model model, HttpSession session) {
        if (session != null && session.getAttribute("username") != null) {
            return "redirect:/dashboard";
        }
        model.addAttribute("categories", JobCategory.values());
        return "signup";
    }

    /**
     * Registers a new AppUser along with their initial MasterProfile and chosen specialization.
     */
    @PostMapping({"/signup", "/register"})
    public String processSignUp(@RequestParam("username") String username,
                                @RequestParam("email") String email,
                                @RequestParam("password") String password,
                                @RequestParam("confirmPassword") String confirmPassword,
                                @RequestParam(value = "specialization", required = false) JobCategory specialization,
                                HttpSession session,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        model.addAttribute("categories", JobCategory.values());

        // Validate username
        if (username == null || username.trim().length() < 3) {
            model.addAttribute("errorMessage", "Username must be at least 3 characters long.");
            populateForm(model, username, email, specialization);
            return "signup";
        }

        // Validate email
        if (email == null || email.isBlank() || !email.contains("@") || !email.contains(".")) {
            model.addAttribute("errorMessage", "Please provide a valid email address.");
            populateForm(model, username, email, specialization);
            return "signup";
        }

        // Validate password
        if (password == null || password.length() < 6) {
            model.addAttribute("errorMessage", "Password must be at least 6 characters long.");
            populateForm(model, username, email, specialization);
            return "signup";
        }

        // Validate password confirmation
        if (!password.equals(confirmPassword)) {
            model.addAttribute("errorMessage", "Passwords do not match. Please re-enter.");
            populateForm(model, username, email, specialization);
            return "signup";
        }

        String cleanUsername = username.trim();
        String cleanEmail = email.trim().toLowerCase();

        // Check uniqueness
        if (userRepository.existsByUsername(cleanUsername)) {
            model.addAttribute("errorMessage", "Username '" + cleanUsername + "' is already taken. Please pick another.");
            populateForm(model, username, email, specialization);
            return "signup";
        }

        if (userRepository.existsByEmail(cleanEmail)) {
            model.addAttribute("errorMessage", "An account with email '" + cleanEmail + "' already exists.");
            populateForm(model, username, email, specialization);
            return "signup";
        }

        JobCategory category = (specialization != null) ? specialization : JobCategory.TECHNOLOGY;
        String passwordHash = SecurityConfig.hashPassword(password);

        AppUser createdUser = profileService.createUserWithProfile(cleanUsername, cleanEmail, passwordHash, category);

        session.setAttribute("username", createdUser.getUsername());
        session.setAttribute("currentUser", createdUser);

        log.info("New candidate registered: username='{}', category='{}'", cleanUsername, category);
        redirectAttributes.addFlashAttribute("successMessage",
                "Account created successfully! Welcome to JobApp, " + createdUser.getUsername() + ". Please fill in your CV details below to start tailoring applications.");
        return "redirect:/profile?onboarding=true";
    }

    /**
     * Signs out the current user by invalidating the HTTP session.
     */
    @GetMapping({"/signout", "/logout"})
    public String signOut(HttpSession session, RedirectAttributes redirectAttributes) {
        if (session != null) {
            session.invalidate();
        }
        redirectAttributes.addFlashAttribute("successMessage", "You have been signed out successfully.");
        return "redirect:/signin";
    }

    private void populateForm(Model model, String username, String email, JobCategory specialization) {
        model.addAttribute("username", username);
        model.addAttribute("email", email);
        model.addAttribute("selectedCategory", specialization);
    }
}
