import java.util.*
import Result.Err
import Result.Ok
import BinaryOp.*
import UnaryOp.*
import Token.*
import Expression.*
import Command.*

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
    data class ExpIdentifier(val value: String) : Expression()
    data class ExpUnr(val op: UnaryOp, val expression: Expression) : Expression()
    data class ExpBin(val left: Expression, val op: BinaryOp, val right: Expression) : Expression()
}

sealed class Command {
    data class Print(val expression: Expression): Command()
    data class Assignment(val identifier: ExpIdentifier, val expression: Expression): Command()
    data class StoreCommand(val line: Int, val command: Command): Command()
    data class Run(val line: Int = 0): Command()
    data class GoTo(val line: Int): Command()
    data class GoSub(val line: Int): Command()
    data class If(val eval: Expression, val then: CommandResult) : Command()
    data class Multiple(val commands: List<Command>): Command()
    object Return: Command()
    object Pop: Command()
    object ExpREM: Command()
    object ListCommand: Command()
}

data class CommandResult(val original: String, val command: Result<Command>)

fun parseToCommand(tokenRes: TokenResult): CommandResult {
    val tokens = tokenRes.tokens
    val result: Result<Command> =  if (tokens.isNotEmpty()) {
        val head = tokens.first()
        when(head) {
            is Keyword -> when(head.value) {
                "PRINT", "?" -> parseToPrintCommand(tokens.drop(1))
                "REM" -> Ok(ExpREM)
                "LET" -> parseToAssignment(tokens.drop(1))
                "RUN" -> {
                    val line = tokens.getOrNull(1)?.let {
                        (it as? NumberLiteral)?.value ?: 0
                    } ?: 0
                    Ok(Run(line))
                }
                "LIST" ->  Ok(ListCommand)
                "GOTO" -> {
                    val line = tokens.getOrNull(1)?.let {
                        (it as? NumberLiteral)?.value ?: 0
                    } ?: 0
                    Ok(GoTo(line))
                }
                /*
                ]10 ? hej
                ]20 hej = hej + 1
                ]30 IF hej < 100 THEN GOTO 10
                */
                "IF" -> {
                    val thenIndex = tokens.indexOfFirst { it is Keyword && it.value == "THEN" }
                    val ifExpRes = parseShuntingYard(tokens.subList(1, thenIndex))
                    val thenExpRes = parseToCommand(TokenResult(original = tokenRes.original, tokens = tokens.drop(thenIndex + 1)))
                    if (ifExpRes !is Ok || thenExpRes.command !is Ok)
                        return CommandResult(
                                original = tokenRes.original,
                                command = Err("*** No valid expression ***\n" + "\t original: ${tokenRes.original}")
                        )
                    Ok(If(eval = ifExpRes.value, then = thenExpRes))
                }
                else -> Err("*** Unsupported command: $head")
            }
            is NumberLiteral -> {
                val command = parseToCommand(TokenResult(original = tokenRes.original, tokens = tokens.drop(1)))
                when(command.command) {
                    is Ok -> Ok(StoreCommand(head.value, command = command.command.value))
                    else -> command.command
                }
            }
            is Identifier -> parseToAssignment(tokens)
            else -> Err("*** Not a keyword: $head")
        }

    } else {
        Err("*** No valid tokens ***\n\t original: ${tokenRes.original}")
    }

    return CommandResult(original = tokenRes.original, command = result)
}

fun parseToAssignment(tokens: List<Token>): Result<Assignment> {
    return if (tokens.size > 2) {
        val head = tokens.first()
        if (head !is Identifier) return Err("*** $head is not a valid identifier ***")
        val hopefullyEqualSign = tokens[1]
        if (hopefullyEqualSign !is Operator || hopefullyEqualSign.value != "=") return Err("*** no equal sign in assignment ***")

        val res = parseToExpression(tokens.drop(2))
        when(res) {
            is Ok -> Ok(Assignment(identifier = ExpIdentifier(head.value), expression = res.value))
            is Err -> Err(res.error)
        }

    } else {
        Err("*** Not enough tokens in $tokens ***")
    }
}

fun parseToPrintCommand(tokens: List<Token>): Result<Print> {
    val expr = parseToExpression(tokens)
    return when(expr) {
        is Ok -> Ok(Print(expr.value))
        is Err -> Err("*** Print error: ***\n\t${expr.error}")
    }
}

fun parseToExpression(tokens: List<Token>): Result<Expression> {
    return if (tokens.isNotEmpty()) {
        val first = tokens.first()
        when(first) {
            is StringLiteral,
            is NumberLiteral,
            is Identifier,
            is Operator-> parseShuntingYard(tokens)

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
            is Identifier,
            is StringLiteral,
            is NumberLiteral -> output.push(token)
            is Operator -> {
                if(token.value == "(") operators.push(token)
                else if (token.value == ")") {
                    while (operators.peek().value != "(") output.push(operators.pop())
                    if (operators.peek().value == "(") operators.pop()
                } else {
                    val tokOpRes = token.getBinop()
                    val tokBinop = when(tokOpRes) {
                        is Ok -> tokOpRes.value
                        is Err -> return Err("*** Something went wrong 1:\n\t${tokOpRes.error}")
                    }

                    while (operators.isNotEmpty() && operators.peek().value.isParanthesis().not()) {
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
            is Identifier,
            is StringLiteral,
            is NumberLiteral -> {
                var expression: Expression = when(token) {
                    is NumberLiteral -> ExpInt(token.value)
                    is StringLiteral -> ExpStr(token.value)
                    is Identifier -> ExpIdentifier(token.value)
                    else -> return Err("*** Something went really wrong ***")
                }
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

fun String.isParanthesis(): Boolean = this == "(" || this == ")"
