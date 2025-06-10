package itu.mg.erp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import itu.mg.erp.features.request.LoginRequest;
import itu.mg.erp.response.ApiSuccessLoginResult;
import itu.mg.erp.service.SecurityService;
import jakarta.servlet.http.HttpSession;

@Controller
public class SecurityController {

    private final SecurityService _securityService;

    public SecurityController(SecurityService securityService) {
        this._securityService = securityService;
    }

    @GetMapping("/")
    public String showLoginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(
        @RequestParam String email,
        @RequestParam String password,
        HttpSession session,
        RedirectAttributes redirectAttributes,
        Model model
    ) {
        LoginRequest request = new LoginRequest();
        request.setUsr(email);
        request.setPwd(password);
        ApiSuccessLoginResult<Object> result = _securityService.login(request);
        if (result != null && result.getFull_name() != null ) {
            session.setAttribute("user", result.getFull_name());
            redirectAttributes.addFlashAttribute("action", true);
            return "redirect:/invoices";
        } else {
            redirectAttributes.addFlashAttribute("error", "Invalid login credentials.");
            return "redirect:/"; 
        }

    }

    @GetMapping("/logout")
    public String logout() {
        _securityService.logout();
        return "login";
    }
}

