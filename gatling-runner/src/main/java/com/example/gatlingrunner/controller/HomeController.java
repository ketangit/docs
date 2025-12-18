package com.example.gatlingrunner.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

  @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
  public String home() {
    return "<html><body><h1>Gatling Runner</h1><p><a href=\"/ui/scenarios\">View Scenarios</a></p></body></html>";
  }
}
