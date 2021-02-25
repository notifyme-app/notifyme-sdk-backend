package ch.ubique.notifyme.sdk.backend.ws.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("")
public class WebController {

    @GetMapping("/")
    public String web() {
        return "home";
    }
}
