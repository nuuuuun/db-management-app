package com.example.db_management;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    @GetMapping({
        "/",
        "/login",
        "/about",
        "/users",
        "/masked-columns",
        "/csv-bulk",
        "/tables",
        "/tables/{p1}",
        "/tables/{p1}/import",
        "/tables/{p1}/edit/{p2}",
        "/tables/{p1}/edit/{p2}/confirm",
    })
    public String spa() {
        return "forward:/index.html";
    }
}
