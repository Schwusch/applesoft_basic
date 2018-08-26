import java.util.*
import Result.Err
import Result.Ok
import BinaryOp.*
import UnaryOp.*
import Token.*
import Expression.*

sealed class UnaryOp {
    object UMinus : UnaryOp()
    object Not : UnaryOp()
}

fun UnaryOp.priorityUop(): Int {
    return when(this) {
        UMinus -> 7
        Not -> 1
    }
}

fun Token.Operator.getUnop(): Result<UnaryOp> {
    return when(this.value) {
        "-" -> Ok(UMinus)
        "!" -> Ok(Not)
        else -> Err("*** '${this.value}' is not a valid unary operator ***")
    }
}

fun Token.Operator.getBinop(): Result<BinaryOp> {
    val res =  when(this.value) {
        "+" -> Plus
        "-" -> Minus
        "*" -> Mult
        "/" -> Div
        "MOD" -> Mod
        "=" -> Equal
        "<" -> Less
        "<=" -> LessEq
        ">" -> Great
        ">=" -> GreatEq
        "<>" -> Diff
        "&" -> And
        "|" -> Or
        else -> null
    }

    return when(res) {
        is BinaryOp -> Ok(res)
        else -> Err("*** '${this.value}' is not a valid binary operator ***")
    }
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

fun BinaryOp.priorityBinop(): Int {
    return when(this) {
        Mult, BinaryOp.Div -> 6
        Plus, BinaryOp.Minus -> 5
        Mod -> 4
        Equal, Less, LessEq, Great, GreatEq, Diff -> 3
        And,
        Or -> 2
    }
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
                "PRINT", "?" -> parseToPrintCommand(tokens.drop(1))
                else -> Err("*** Unsupported command: $head")
            }
            else -> Err("*** Not a keyword: $head")
        }

    } else {
        Err("*** No valid expression ***")
    }
}

fun parseToPrintCommand(tokens: List<Token>): Result<Command.Print> {
    val expr = parseToExpression(tokens)
    return when(expr) {
        is Ok -> Ok(Command.Print(expr.value))
        is Err -> Err("*** Print error: ***\n\t${expr.error}")
    }
}

fun parseToExpression(tokens: List<Token>): Result<Expression> {
    return if (tokens.isNotEmpty()) {
        val first = tokens.first()
        when(first) {
            is Token.StringLiteral -> Ok(ExpStr(first.value))
            is Token.NumberLiteral -> parseShuntingYard(tokens)

            else -> Err("*** Unsupported expression: $first")
        }
    } else {
        Err("*** No valid expression ***")
    }
}

fun parseShuntingYard(tokens: List<Token>): Result<Expression> {
    val output = Stack<Token>()
    val operators = Stack<Token.Operator>()

    for (token in tokens) {
        when(token) {
            is NumberLiteral -> output.push(token)
            is Operator -> {
                val tokOpRes = token.getBinop()
                val tokBinop = when(tokOpRes) {
                    is Ok -> tokOpRes.value
                    is Err -> return Err("*** Something went wrong 1:\n\t${tokOpRes.error}")
                }

                while (operators.isNotEmpty()) {
                    val opStackResult = operators.peek().getBinop()
                    val opStackBinop = when(opStackResult) {
                        is Ok -> opStackResult.value
                        is Err -> return Err("*** Something went wrong 2:\n\t${opStackResult.error}")
                    }
                    if (tokBinop.priorityBinop() <= opStackBinop.priorityBinop()) {
                        output.push(operators.pop())
                    } else break
                }

                operators.push(token)
            }
            is Token.EndOfLine -> {}
            else -> return Err("*** Shunting yard can not handle: $token")
        }
    }
    operators.forEach { output.push(it) }

    return parseReversePolishNotation(output)
}

fun parseReversePolishNotation(tokens: Stack<Token>): Result<Expression> {
    val expressionStack = Stack<Expression>()
    val operatorStack = Stack<Token.Operator>()

    var pendingOperand = false
    while (tokens.isNotEmpty()) {
        val token = tokens.pop()
        when(token) {
            is Operator -> {
                operatorStack.push(token)
                pendingOperand = false
            }
            is NumberLiteral -> {
                var expression: Expression = ExpInt(token.value)
                if (pendingOperand) {
                    while (expressionStack.isNotEmpty()) {
                        val operand = expressionStack.pop()
                        val operatorResult = operatorStack.pop().getBinop()
                        val operator = when(operatorResult) {
                            is Ok -> operatorResult.value
                            is Err -> return Err("*** Something went wrong 3:\n\t${operatorResult.error}")
                        }

                        expression = ExpBin(left = expression, op = operator, right = operand)
                    }
                }
                expressionStack.push(expression)
                pendingOperand = true
            }
        }
    }
    return Ok(expressionStack.pop())
}
