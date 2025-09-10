package com.nmr.image_api.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ImageGuiController {

    @GetMapping("/images/ui")
    public String gui() {
        return "images"; // maps to templates/images.html
    }
}
