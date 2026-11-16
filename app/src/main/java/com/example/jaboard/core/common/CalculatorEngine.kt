package com.example.jaboard.core.common

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

object CalculatorEngine {

  sealed interface EvalResult {
    data class Success(val value: Double, val formatted: String) : EvalResult

    data class Error(val reason: String) : EvalResult
  }

  fun evaluate(rawExpression: String): EvalResult {
    val sanitized =
      rawExpression
        .trim()
        .replace("×", "*")
        .replace("÷", "/")
        .replace("−", "-")
        .replace("π", PI.toString())
        .replace("√(", "sqrt(")

    if (sanitized.isBlank()) return EvalResult.Error("Empty expression")

    return try {
      val tokens = tokenize(sanitized)
      val rpn = toRpn(tokens)
      val value = evalRpn(rpn)
      if (value.isNaN() || value.isInfinite()) {
        EvalResult.Error("Math error")
      } else {
        EvalResult.Success(value, formatNumber(value))
      }
    } catch (e: IllegalArgumentException) {
      EvalResult.Error(e.message ?: "Invalid expression")
    } catch (_: Exception) {
      EvalResult.Error("Invalid expression")
    }
  }

  private sealed interface Token {
    data class Number(val value: Double) : Token

    data class Operator(val symbol: Char, val precedence: Int, val rightAssoc: Boolean = false) :
      Token

    data class Function(val name: String) : Token

    data object LeftParen : Token

    data object RightParen : Token
  }

  private fun tokenize(expr: String): List<Token> {
    val tokens = mutableListOf<Token>()
    var i = 0
    while (i < expr.length) {
      val ch = expr[i]
      when {
        ch.isWhitespace() -> i++
        ch.isDigit() || ch == '.' -> {
          val start = i
          var dotCount = 0
          while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
            if (expr[i] == '.') dotCount++
            if (dotCount > 1) throw IllegalArgumentException("Malformed number")
            i++
          }
          val num = expr.substring(start, i).toDouble()
          tokens.add(Token.Number(num))
        }
        ch.isLetter() -> {
          val start = i
          while (i < expr.length && expr[i].isLetter()) i++
          val word = expr.substring(start, i).lowercase()
          when (word) {
            "e" -> tokens.add(Token.Number(kotlin.math.E))
            "pi" -> tokens.add(Token.Number(PI))
            "sin",
            "cos",
            "tan",
            "sqrt",
            "ln",
            "log" -> tokens.add(Token.Function(word))
            else -> throw IllegalArgumentException("Unknown symbol: $word")
          }
        }
        ch == '(' -> {
          // Support implicit multiplication like 2(3+4)
          val prev = tokens.lastOrNull()
          if (prev is Token.Number || prev is Token.RightParen) {
            tokens.add(Token.Operator('*', 2))
          }
          tokens.add(Token.LeftParen)
          i++
        }
        ch == ')' -> {
          tokens.add(Token.RightParen)
          i++
        }
        ch == '%' -> {
          // Convert postfix % to * 0.01
          tokens.add(Token.Operator('*', 2))
          tokens.add(Token.Number(0.01))
          i++
        }
        ch in charArrayOf('+', '-', '*', '/', '^') -> {
          val prev = tokens.lastOrNull()
          val isUnaryMinus =
            ch == '-' && (prev == null || prev is Token.Operator || prev is Token.LeftParen)
          if (isUnaryMinus) {
            tokens.add(Token.Number(0.0))
            tokens.add(Token.Operator('-', 3, rightAssoc = true))
          } else {
            val prec =
              when (ch) {
                '+',
                '-' -> 1
                '*',
                '/' -> 2
                '^' -> 4
                else -> 1
              }
            tokens.add(Token.Operator(ch, prec, rightAssoc = ch == '^'))
          }
          i++
        }
        else -> throw IllegalArgumentException("Unexpected character: $ch")
      }
    }
    return tokens
  }

  private fun toRpn(tokens: List<Token>): List<Token> {
    val output = mutableListOf<Token>()
    val stack = ArrayDeque<Token>()

    for (token in tokens) {
      when (token) {
        is Token.Number -> output.add(token)
        is Token.Function -> stack.addLast(token)
        is Token.Operator -> {
          while (stack.isNotEmpty()) {
            val top = stack.last()
            if (
              top is Token.Operator &&
                ((!token.rightAssoc && token.precedence <= top.precedence) ||
                  (token.rightAssoc && token.precedence < top.precedence))
            ) {
              output.add(stack.removeLast())
            } else if (top is Token.Function) {
              output.add(stack.removeLast())
            } else {
              break
            }
          }
          stack.addLast(token)
        }
        Token.LeftParen -> stack.addLast(token)
        Token.RightParen -> {
          var foundLeft = false
          while (stack.isNotEmpty()) {
            val top = stack.removeLast()
            if (top is Token.LeftParen) {
              foundLeft = true
              break
            }
            output.add(top)
          }
          if (!foundLeft) throw IllegalArgumentException("Mismatched parentheses")
          if (stack.lastOrNull() is Token.Function) {
            output.add(stack.removeLast())
          }
        }
      }
    }

    while (stack.isNotEmpty()) {
      val top = stack.removeLast()
      if (top is Token.LeftParen || top is Token.RightParen) {
        throw IllegalArgumentException("Mismatched parentheses")
      }
      output.add(top)
    }

    return output
  }

  private fun evalRpn(rpn: List<Token>): Double {
    val stack = ArrayDeque<Double>()
    for (token in rpn) {
      when (token) {
        is Token.Number -> stack.addLast(token.value)
        is Token.Operator -> {
          if (stack.size < 2) throw IllegalArgumentException("Incomplete expression")
          val b = stack.removeLast()
          val a = stack.removeLast()
          val res =
            when (token.symbol) {
              '+' -> a + b
              '-' -> a - b
              '*' -> a * b
              '/' -> {
                if (b == 0.0) throw IllegalArgumentException("Cannot divide by zero")
                a / b
              }
              '^' -> a.pow(b)
              else -> throw IllegalArgumentException("Unknown operator")
            }
          stack.addLast(res)
        }
        is Token.Function -> {
          if (stack.isEmpty()) throw IllegalArgumentException("Missing function argument")
          val a = stack.removeLast()
          val res =
            when (token.name) {
              "sin" -> sin(Math.toRadians(a))
              "cos" -> cos(Math.toRadians(a))
              "tan" -> tan(Math.toRadians(a))
              "sqrt" -> {
                if (a < 0.0) throw IllegalArgumentException("Negative square root")
                sqrt(a)
              }
              "ln" -> {
                if (a <= 0.0) throw IllegalArgumentException("Invalid logarithm")
                ln(a)
              }
              "log" -> {
                if (a <= 0.0) throw IllegalArgumentException("Invalid logarithm")
                log10(a)
              }
              else -> throw IllegalArgumentException("Unknown function")
            }
          stack.addLast(res)
        }
        Token.LeftParen,
        Token.RightParen -> Unit
      }
    }
    if (stack.size != 1) throw IllegalArgumentException("Malformed expression")
    return stack.single()
  }

  fun formatNumber(value: Double): String {
    val cleaned = if (kotlin.math.abs(value) < 1e-12) 0.0 else value
    val longVal = cleaned.toLong()
    return if (cleaned == longVal.toDouble()) {
      longVal.toString()
    } else {
      val str = String.format(java.util.Locale.US, "%.8f", cleaned)
      str.trimEnd('0').trimEnd('.')
    }
  }
}
