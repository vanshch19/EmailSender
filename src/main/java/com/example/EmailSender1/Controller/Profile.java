package com.example.EmailSender1.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class Profile {

    @GetMapping("/profile")
    public String profile() {
        return "profile";
    }

    @GetMapping("/send")
    public String smtp(){
        return "send";
    }

    // @GetMapping("/compose")
    // public String composeMessage(Model model) {
    //     model.addAttribute("subject", "");
    //     // Add attributes to the model as needed
    //     return "compose"; // Returns the template named "compose.html"
    // }

      @GetMapping("/compose")
    public String composeMessage(Model model) {
        // Ensure that your form-backing object is correctly set here
        model.addAttribute("subject", "");
        model.addAttribute("to", "");
        model.addAttribute("charset", "Windows-1252");
        model.addAttribute("message", "");
        model.addAttribute("plainText", "");
        return "compose"; // Make sure this matches the template name
    }

    @GetMapping("/upload")
    public String composeMessage3(){
        return "upload";
    }
}