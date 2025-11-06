package com.kush.math_note_solver;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class MathController {

    private final MathService service;

    public MathController(MathService service) {
        this.service = service;
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }

    // -------- Expression solver --------
    // GET: for quick manual tests in the browser (handles + because we strip spaces server-side)
    @GetMapping("/solve-expression")
    public ResponseEntity<Map<String, Object>> solveExpressionGet(@RequestParam("expression") String expr) {
        return ResponseEntity.ok(service.solveExpression(expr));
    }

    // POST JSON: for UI and clients (no URL encoding issues)
    @PostMapping("/solve-expression")
    public ResponseEntity<Map<String, Object>> solveExpressionPost(@RequestBody Map<String, String> body) {
        String expr = body.getOrDefault("expression", "");
        return ResponseEntity.ok(service.solveExpression(expr));
    }

    // -------- Equation solver --------
    @GetMapping("/solve-equation")
    public ResponseEntity<Map<String, Object>> solveEquationGet(@RequestParam("equation") String eq) {
        return ResponseEntity.ok(service.solveEquation(eq));
    }

    @PostMapping("/solve-equation")
    public ResponseEntity<Map<String, Object>> solveEquationPost(@RequestBody Map<String, String> body) {
        String eq = body.getOrDefault("equation", "");
        return ResponseEntity.ok(service.solveEquation(eq));
    }
}
