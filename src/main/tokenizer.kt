package main
import main.Token.*

sealed class Token {
    data class Identifier(val value: String) : Token()
    data class StringLiteral(val value: String) : Token()
    data class NumberLiteral(val value: Int) : Token()
    data class Keyword(val value: String) : Token()
    data class Operator(val value: String) : Token()
    data class Illegal(val value: String) : Token()
    object EndOfLine : Token()
}

val keywords = setOf(
        "PRINT",
        "?",
        "REM",
        "IF",
        "THEN",
        "LET",
        "LIST",
        "RUN",
        "LIST",
        "GOTO",
        "GOSUB",
        "RETURN",
        "POP"
)

val operators = setOf(
        "+",
        "-",
        "*",
        "/",
        "!",
        "&",
        "|",
        "<",
        ">",
        "<>",
        "MOD",
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

    private fun readNumber(): Token.NumberLiteral {
        var number = "${input[index]}"
        index++
        while (index < input.length && input[index].isDigit()) {
            number += input[index]
            index++
        }

        return NumberLiteral(number.toInt())
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

        while (index < input.length && (input[index].isLetter() || input[index].isDigit() || input[index] == '_')) {
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