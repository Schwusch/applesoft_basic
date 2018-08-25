

sealed class Token {
    data class Identifier(val value: String) : Token()
    data class StringLiteral(val value: String) : Token()
    data class NumberLiteral(val value: Int) : Token()
    data class Keyword(val value: String) : Token()
    data class Operator(val value: String) : Token()
    object EndOfLine : Token()
    object Illegal : Token()
}

val keywords = setOf(
        "PRINT",
        "?"
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
        ")"
)

fun tokenize_line(line: String) : List<Token> {
    val tokenizer = Tokenizer(line)
    val tokens = mutableListOf<Token>()

    loop@ while (true) {
        val tok = tokenizer.nextToken()

        tokens.add(tok)
        when(tok) {
            Token.EndOfLine,
            Token.Illegal -> break@loop
        }
    }

    return tokens
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
            char?.let { operators.contains("$char") } ?: false -> readOperator()
            char == null -> Token.EndOfLine
            else -> Token.Illegal
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

        return Token.NumberLiteral(number.toInt())
    }

    private fun readOperator(): Token.Operator {
        var operator = "${input[index]}"
        index++
        while (index < input.length && operators.contains("${input[index]}")) {
            operator += input[index]
            index++
        }

        return Token.Operator(operator)
    }

    private fun readIdentifier(): Token {
        var identifier = "${input[index]}"
        index++

        while (index < input.length && (input[index].isLetter() || input[index].isDigit() || input[index] == '_')) {
            identifier += input[index]
            index++
        }

        return when {
            keywords.contains(identifier) -> Token.Keyword(identifier)
            operators.contains(identifier) -> Token.Operator(identifier)
            else -> Token.Identifier(identifier)
        }
    }

    private fun readLiteral(): Token.StringLiteral {
        var lit = ""
        while (index < input.length && input[index] != '"'  ) {
            lit += input[index]
            index++
        }
        index++
        return Token.StringLiteral(lit)
    }
}