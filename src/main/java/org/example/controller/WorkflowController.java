package org.example.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WorkflowController {

    @GetMapping("/api/status/{workflowId}")
    String getWorkflowStatus(@PathVariable String workflowId) {

        return "string";
    }

}
