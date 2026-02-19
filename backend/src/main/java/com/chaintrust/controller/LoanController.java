package com.chaintrust.controller;

import com.chaintrust.model.LoanRequest;
import com.chaintrust.service.LoanService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/loan")
@CrossOrigin(origins = "*")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping("/evaluate")
    public ResponseEntity<Map<String, Object>> evaluate(@Valid @RequestBody LoanRequest request) {
        return ResponseEntity.ok(loanService.evaluate(request));
    }
}
