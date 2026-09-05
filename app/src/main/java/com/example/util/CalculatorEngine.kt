package com.example.util

object CalculatorEngine {

    fun evaluate(expression: String): Double {
        if (expression.isBlank()) return 0.0
        val sanitized = expression.replace(" ", "").replace("×", "*").replace("÷", "/")
        return try {
            evalExpr(sanitized)
        } catch (e: Exception) {
            0.0
        }
    }

    private fun evalExpr(expr: String): Double {
        if (expr.isEmpty()) return 0.0
        // Simple recursive descent parser for +, -, *, /
        var pos = -1
        var ch = 0

        fun nextChar() {
            ch = if (++pos < expr.length) expr[pos].code else -1
        }

        fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parseFactor(): Double {
            if (eat('+'.code)) return parseFactor()
            if (eat('-'.code)) return -parseFactor()

            var x: Double
            val startPos = pos
            if (eat('('.code)) {
                x = parseFactor() // will recursively evaluate inside
                while (ch != ')'.code && ch != -1) {
                    if (eat('+'.code)) x += parseFactor()
                    else if (eat('-'.code)) x -= parseFactor()
                    else if (eat('*'.code)) x *= parseFactor()
                    else if (eat('/'.code)) {
                        val d = parseFactor()
                        x = if (d != 0.0) x / d else 0.0
                    } else break
                }
                eat(')'.code)
            } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                val numStr = expr.substring(startPos, pos)
                x = numStr.toDoubleOrNull() ?: 0.0
            } else {
                x = 0.0
            }
            return x
        }

        fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                when {
                    eat('*'.code) -> x *= parseFactor()
                    eat('/'.code) -> {
                        val d = parseFactor()
                        x = if (d != 0.0) x / d else 0.0
                    }
                    else -> return x
                }
            }
        }

        fun parseExpression(): Double {
            nextChar()
            var x = parseTerm()
            while (true) {
                when {
                    eat('+'.code) -> x += parseTerm()
                    eat('-'.code) -> x -= parseTerm()
                    else -> return x
                }
            }
        }

        return parseExpression()
    }

    fun formatResult(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            String.format("%.2f", value)
        }
    }
}
