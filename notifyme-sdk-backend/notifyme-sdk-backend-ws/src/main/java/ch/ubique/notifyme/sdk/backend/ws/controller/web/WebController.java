package ch.ubique.notifyme.sdk.backend.ws.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("")
public class WebController {

    @GetMapping("/")
    public String web(final Model model) {
        model.addAttribute("name", "Vorname Nachname");
        return "home";
    }
}
