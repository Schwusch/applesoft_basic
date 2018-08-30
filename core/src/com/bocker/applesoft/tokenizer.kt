package com.bocker.applesoft
import com.bocker.applesoft.Token.*

sealed class Token {
    data class Identifier(val value: String) : Token()
    data class StringLiteral(val value: String) : Token()
    data class NumberLiteral(val value: Float) : Token()
    data class Keyword(val value: String) : Token()
    data class Operator(val value: String, val unary: Boolean = false) : Token()
    data class Illegal(val value: String) : Token()
    object EndOfLine : Token()
}

val keywords = setOf(
        "PRINT",
        "?",
        "REM",
        "IF",
        "THEN",
        "TH",
        "LET",
        "LIST",
        "RUN",
        "LIST",
        "GOTO",
        "GOSUB",
        "RETURN",
        "POP",
        "ONERR",
        "LOAD"
)

val operators = setOf(
        "+",
        "-",
        "*",
        "/",
        "&",
        "AND",
        "OR",
        "|",
        "<",
        ">",
        "MOD",
        "NOT",
        "(",
        ")",
        "=",
        ":",
        ";"
)

data class TokenResult(val original: String, val tokens: List<Token>)

fun tokenizeLine(line: String) : TokenResult {
    val tokenizer = Tokenizer(line)
    val tokens = mutableListOf<Token>()

    loop@ while (true) {
        val tok = tokenizer.nextToken()

        tokens.add(tok)
        when(tok) {
            EndOfLine,
            is Illegal -> break@loop
        }
    }

    return TokenResult(line, tokens)
}



class Tokenizer(private val input: String, var index: Int = 0) {

    fun nextToken(): Token {
        skipWhitespace()

        val char = input.getOrNull(index)
        return when {
            char == '"' -> {
                index++
                readLiteral()
            }
            char?.isLetter() ?: false -> readIdentifier()
            char?.isDigit() ?: false -> readNumber()
            char != null && operators.contains(char.toString()) -> readOperator()
            char != null && keywords.contains(char.toString()) -> {
                index++
                Keyword(char.toString())
            }
            char == null -> EndOfLine
            else -> Illegal(char.toString())
        }
    }

    private fun skipWhitespace() {
        while (true) {
            if (index < input.length && input[index].isWhitespace()) index++
            else break
        }
    }

    private fun readNumber(): Token {
        var number = "${input[index]}"
        index++
        while (index < input.length && (input[index].isDigit() || input[index] == '.')) {
            number += input[index]
            index++
        }

        val dots = number.count { it == '.' }

        if (dots < 2) return NumberLiteral(number.toFloat())

        return Illegal(value = number)
    }

    private fun readOperator(): Token.Operator {
        var operator = "${input[index]}"
        index++
        while (
                index < input.length
                && operators.contains("${input[index]}")
                && Operator(operator + input[index]).getBinop() !is Result.Err
        ) {
            operator += input[index]
            index++
        }

        return Operator(operator)
    }

    private fun readIdentifier(): Token {
        var identifier = "${input[index]}"
        index++

        while (index < input.length && (input[index].isLetter() || input[index].isDigit() || input[index] == '_' || input[index] == '$')) {
            identifier += input[index]
            index++
        }

        return when {
            keywords.contains(identifier) -> Keyword(identifier)
            operators.contains(identifier) -> Operator(identifier)
            else -> Identifier(identifier)
        }
    }

    private fun readLiteral(): StringLiteral {
        var lit = ""
        while (index < input.length && input[index] != '"'  ) {
            lit += input[index]
            index++
        }
        index++
        return StringLiteral(lit)
    }
}