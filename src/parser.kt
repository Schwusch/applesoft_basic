sealed class UnaryOp {
    object UMinus : UnaryOp()
    object Not : UnaryOp()
}

sealed class BinaryOp {
    object Plus : BinaryOp()
    object Minus : BinaryOp()
    object Mult : BinaryOp()
    object Div : BinaryOp()
    object Mod : BinaryOp()
    object Equal : BinaryOp()
    object Less : BinaryOp()
    object LessEq : BinaryOp()
    object Great : BinaryOp()
    object GreatEq : BinaryOp()
    object Diff : BinaryOp()
    object And : BinaryOp()
    object Or : BinaryOp()
}

sealed class Expression {
    data class ExpInt(val value: Int) : Expression()
    data class ExpStr(val value: String) : Expression()
    data class ExpUnr(val op: UnaryOp, val expression: Expression) : Expression()
    data class ExpBin(val left: Expression, val op: BinaryOp, val right: Expression) : Expression()
}

sealed class Command {
    data class Print(val expression: Expression): Command()
}

fun parseToCommand(tokens: List<Token>): Result<Command> {
    return if (tokens.isNotEmpty()) {
        val head = tokens.first()
        when(head) {
            is Token.Keyword -> when(head.value) {
                "PRINT",
                "?" -> parseToPrintCommand(tokens.drop(1))
                else -> Result.Err("*** Unsupported command: $head")
            }
            else -> Result.Err("*** Not a keyword: $head")
        }

    } else {
        Result.Err("*** No valid expression ***")
    }
}

fun parseToPrintCommand(tokens: List<Token>): Result<Command.Print> {
    val expr = parseToExpression(tokens)
    return when(expr) {
        is Result.Ok -> Result.Ok(Command.Print(expr.value))
        is Result.Err -> Result.Err("*** Print error: ***\n${expr.error}")
    }
}

fun parseToExpression(tokens: List<Token>): Result<Expression> {
    return if (tokens.isNotEmpty()) {
        val first = tokens.first()
        when(first) {
            is Token.StringLiteral -> Result.Ok(Expression.ExpStr(first.value))
            is Token.NumberLiteral -> Result.Ok(Expression.ExpInt(first.value))
            else -> Result.Err("*** Unsupported expression: $first")
        }
    } else {
        Result.Err("*** No valid expression ***")
    }
}