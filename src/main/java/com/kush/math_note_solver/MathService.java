package com.kush.math_note_solver;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class MathService {

    // ---------- PUBLIC API ----------

    public Map<String, Object> solveExpression(String expr) {
        Map<String, Object> out = new LinkedHashMap<>();
        List<String> steps = new ArrayList<>();
        try {
            double result = evaluateArithmetic(expr, steps);
            out.put("ok", true);
            out.put("expression", expr);
            out.put("steps", steps);
            out.put("result", result);
        } catch (IllegalArgumentException ex) {
            out.put("ok", false);
            out.put("error", ex.getMessage());
        }
        return out;
    }

    public Map<String, Object> solveEquation(String equation) {
        Map<String, Object> out = new LinkedHashMap<>();
        try {
            double x = solveLinearEquation(equation);
            out.put("ok", true);
            out.put("equation", equation);
            out.put("x", x);
        } catch (IllegalArgumentException ex) {
            out.put("ok", false);
            out.put("error", ex.getMessage());
        }
        return out;
    }

    // ARITHMETIC EVALUATOR
    private double evaluateArithmetic(String expression, List<String> steps) {
        if (expression == null) throw new IllegalArgumentException("Empty expression");
        String s = expression.replaceAll("\\s+", ""); // remove all spaces
        if (s.isEmpty()) throw new IllegalArgumentException("Empty expression");

        List<String> rpn = infixToRPN(s);
        return evalRPN(rpn, steps);
    }

    private List<String> infixToRPN(String s) {
        List<String> out = new ArrayList<>();
        Deque<String> ops = new ArrayDeque<>();

        for (int i = 0; i < s.length();) {
            char c = s.charAt(i);

            if (Character.isDigit(c) || c == '.') {
                int j = i + 1;
                boolean hasDot = (c == '.');
                while (j < s.length()) {
                    char d = s.charAt(j);
                    if (Character.isDigit(d)) { j++; continue; }
                    if (d == '.' && !hasDot) { hasDot = true; j++; continue; }
                    break;
                }
                out.add(s.substring(i, j));
                i = j;
                continue;
            }

            if (c == '(') { ops.push("("); i++; continue; }
            if (c == ')') {
                while (!ops.isEmpty() && !ops.peek().equals("(")) out.add(ops.pop());
                if (ops.isEmpty()) throw new IllegalArgumentException("Mismatched ')'");
                ops.pop(); i++; continue;
            }
            if (isOpChar(c)) {
                String op = String.valueOf(c);
                if (op.equals("-") && (i == 0 || isOpChar(s.charAt(i-1)) || s.charAt(i-1) == '(')) {
                    op = "u-";
                }

                while (!ops.isEmpty() && isOperator(ops.peek())) {
                    String top = ops.peek();
                    if ((isLeftAssoc(op) && prec(op) <= prec(top)) ||
                            (!isLeftAssoc(op) && prec(op) <  prec(top))) {
                        out.add(ops.pop());
                    } else break;
                }
                ops.push(op);
                i++; continue;
            }

            throw new IllegalArgumentException("Invalid character: " + c);
        }

        while (!ops.isEmpty()) {
            String op = ops.pop();
            if (op.equals("(")) throw new IllegalArgumentException("Mismatched '('");
            out.add(op);
        }
        return out;
    }

    private boolean isOpChar(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '^';
    }
    private boolean isOperator(String t) {
        return t.equals("+") || t.equals("-") || t.equals("*") || t.equals("/") || t.equals("^") || t.equals("u-");
    }
    private int prec(String op) {
        switch (op) {
            case "u-": return 4;
            case "^":  return 3;
            case "*":
            case "/":  return 2;
            case "+":
            case "-":  return 1;
            default:   return 0;
        }
    }
    private boolean isLeftAssoc(String op) {
        return !(op.equals("^") || op.equals("u-"));
    }

    private double evalRPN(List<String> rpn, List<String> steps) {
        Deque<Double> st = new ArrayDeque<>();
        for (String t : rpn) {
            if (isOperator(t)) {
                if (t.equals("u-")) {
                    if (st.isEmpty()) throw new IllegalArgumentException("Unary minus missing operand");
                    double a = st.pop();
                    double r = -a;
                    steps.add("negate(" + trim(a) + ") = " + trim(r));
                    st.push(r);
                } else {
                    if (st.size() < 2) throw new IllegalArgumentException("Operator missing operand");
                    double b = st.pop(), a = st.pop();
                    double r;
                    switch (t) {
                        case "+": r = a + b; steps.add(trim(a) + " + " + trim(b) + " = " + trim(r)); break;
                        case "-": r = a - b; steps.add(trim(a) + " - " + trim(b) + " = " + trim(r)); break;
                        case "*": r = a * b; steps.add(trim(a) + " * " + trim(b) + " = " + trim(r)); break;
                        case "/": r = a / b; steps.add(trim(a) + " / " + trim(b) + " = " + trim(r)); break;
                        case "^": r = Math.pow(a, b); steps.add(trim(a) + " ^ " + trim(b) + " = " + trim(r)); break;
                        default: throw new IllegalArgumentException("Bad op: " + t);
                    }
                    st.push(r);
                }
            } else {
                st.push(Double.parseDouble(t));
            }
        }
        if (st.size() != 1) throw new IllegalArgumentException("Invalid expression");
        return st.pop();
    }

    private String trim(double x) {
        if (Math.abs(x - Math.rint(x)) < 1e-9) return String.valueOf((long)Math.rint(x));
        return String.valueOf(x);
    }

    //LINEAR EQUATION SOLVER
    private double solveLinearEquation(String eq) {
        if (eq == null) throw new IllegalArgumentException("Empty equation");
        String s = eq.replaceAll("\\s+", "");
        int pos = s.indexOf('=');
        if (pos < 0) throw new IllegalArgumentException("No '=' found");

        String left = s.substring(0, pos);
        String right = s.substring(pos + 1);

        double rhs;
        try {
            rhs = Double.parseDouble(right);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Right side must be a number");
        }
        left = left.replace("-", "+-");
        String[] terms = left.split("\\+");

        double a = 0.0;
        double b = 0.0;

        for (String t : terms) {
            if (t.isEmpty()) continue;
            if (t.contains("x") || t.contains("X")) {
                String c = t.replace("X", "x").replace("x", "");
                if (c.equals("") || c.equals("+")) c = "1";
                else if (c.equals("-")) c = "-1";
                try {
                    a += Double.parseDouble(c);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid x term: " + t);
                }
            } else {
                try {
                    b += Double.parseDouble(t);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid constant term: " + t);
                }
            }
        }

        if (Math.abs(a) < 1e-12) {
            throw new IllegalArgumentException("No x term or infinite/no solutions");
        }

        return (rhs - b) / a;
    }
}
