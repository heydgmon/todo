package com.example.todo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/blog")
public class BlogController {

    @GetMapping
    public String blogIndex() {
        return "blog/index";
    }

    @GetMapping("/{slug}")
    public String blogPost(@PathVariable String slug) {
        return "blog/" + slug;
    }
}