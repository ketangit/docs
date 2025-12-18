package com.example.gatlingrunner.controller;

import com.example.gatlingrunner.service.ExecutionService;
import com.example.gatlingrunner.service.S3Service;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ui")
public class UiController {

  private final ExecutionService executionService;
  private final S3Service s3Service;

  public UiController(ExecutionService executionService, S3Service s3Service) {
    this.executionService = executionService;
    this.s3Service = s3Service;
  }

  @GetMapping(value = "/scenarios", produces = MediaType.TEXT_HTML_VALUE)
  public String listScenarios() {
    List<String> scenarios = executionService.listScenarios();
    StringBuilder sb = new StringBuilder();
    sb.append("<div>");
    sb.append("<h3>Available Scenarios</h3>");
    sb.append("<ul>");
    for (String s : scenarios) {
      sb.append("<li>");
      sb.append("<button hx-post=\"/ui/run\" hx-vals='{\"scenario\":\"").append(escape(s)).append("\"}' ");
      sb.append("hx-swap=\"outerHTML\">Run ").append(escape(s)).append("</button>");
      sb.append("</li>");
    }
    sb.append("</ul>");
    sb.append("</div>");
    return sb.toString();
  }

  @PostMapping(value = "/run", produces = MediaType.TEXT_HTML_VALUE)
  public String startRun(@RequestParam String scenario) {
    String runId = executionService.startScenario(scenario);
    StringBuilder sb = new StringBuilder();
    sb.append("<div id=\"run-").append(runId).append("\">");
    sb.append("<div>Started run ").append(runId).append(" for scenario ").append(escape(scenario)).append("</div>");
    sb.append("<div id=\"log-").append(runId).append("\" hx-get=\"/ui/log/").append(runId)
      .append("\" hx-trigger=\"every 1s\" hx-swap=\"outerHTML\">Loading logs...</div>");
    sb.append("<div id=\"report-").append(runId).append("\"></div>");
    sb.append("</div>");
    return sb.toString();
  }

  @GetMapping(value = "/log/{runId}", produces = MediaType.TEXT_HTML_VALUE)
  public String pollLog(@PathVariable String runId) {
    String tail = executionService.tailLog(runId);
    boolean finished = executionService.isFinished(runId);
    StringBuilder sb = new StringBuilder();
    sb.append("<pre>").append(escape(tail)).append("</pre>");
    if (finished) {
      String reportLink = s3Service.presignReportUrl(runId);
      sb.append("<div><a href=\"").append(reportLink).append("\" target=\"_blank\">Open Gatling Report</a></div>");
    }
    return sb.toString();
  }

  @GetMapping(value = "/report/{runId}", produces = MediaType.TEXT_HTML_VALUE)
  public String report(@PathVariable String runId) {
    String url = s3Service.presignReportUrl(runId);
    return "<a href=\"" + url + "\" target=\"_blank\">Open Gatling Report</a>";
  }

  private String escape(String s) {
    if (s == null) return "";
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
      .replace("\"", "&quot;").replace("'", "&#x27;");
  }
}
