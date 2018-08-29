package main
import main.Result.Err
import main.Result.Ok
import main.BinaryOp.*
import main.UnaryOp.*
import main.Token.*
import main.Expression.*
import main.Command.*

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
        "NOT" -> Ok(Not)
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
        "<=", "=<" -> LessEq
        ">" -> Great
        ">=", "=>" -> GreatEq
        "<>", "><" -> Diff
        "&", "AND" -> And
        "|", "OR" -> Or
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
    data class OnErr(val command: Command) : Command()
    data class Multiple(val commands: List<CommandResult>): Command()
    object Return: Command()
    object Pop: Command()
    object ExpREM: Command()
    object ListCommand: Command()
}

data class CommandResult(val original: String, val command: Result<Command>)

fun parseToCommand(tokenRes: TokenResult): CommandResult {
    val tokens = tokenRes.tokens
    val semi = tokens.indexOfFirst { it is Operator && it.value == ":" }

    val result: Result<Command> = when {
        semi > -1 -> {
            val head = tokens.subList(0, semi)
            val tail = tokens.drop(semi + 1)
            Ok(Multiple(
                    listOf(
                            parseToCommand(TokenResult(
                                    original = tokenRes.original,
                                    tokens = head
                            )),
                            parseToCommand(TokenResult(
                                    original = tokenRes.original,
                                    tokens = tail
                            ))
                    )
            ))
        }
        tokens.isNotEmpty() -> {
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
                    "ONERR" -> {
                        val res = parseToCommand(TokenResult(original = tokenRes.original, tokens = tokens.drop(1)))
                        return when(res.command) {
                            is Ok -> CommandResult(
                                    original = tokenRes.original,
                                    command = Ok(OnErr(res.command.value))
                            )
                            is Err -> res
                        }
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

        }
        else -> Err("*** No valid tokens ***\n\t original: ${tokenRes.original}")
    }

    return CommandResult(original = tokenRes.original, command = result)
}

fun parseToAssignment(tokens: List<Token>): Result<Assignment> {
    return if (tokens.size > 2) {
        val head = tokens.first()
        if (head !is Identifier) return Err("*** $head is not a valid identifier ***")
        val hopefullyEqualSign = tokens[1]
        if (hopefullyEqualSign !is Operator || hopefullyEqualSign.value != "=") return Err("*** no equal sign in assignment to $head ***")

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
            is Operator -> parseShuntingYard(tokens)
            else -> Err("*** Unsupported expression: $first")
        }
    } else {
        Err("*** No valid expression ***")
    }
}

fun parseShuntingYard(tokens: List<Token>): Result<Expression> {
    val output = mutableListOf<Token>()
    val operators = mutableListOf<Operator>()

    tokens.forEachIndexed { index, token ->
        when(token) {
            is Identifier,
            is StringLiteral,
            is NumberLiteral -> output.push(token)
            is Operator -> {
                if(token.value == "(") operators.push(token)
                else if (token.value == ")") {
                    while (operators.peek()?.value != "(") operators.pop()?.let { output.push(it) } ?: return Err("*** No matching '(' ***")
                    if (operators.peek()?.value == "(") operators.pop()
                } else {
                    val tokenBefore = tokens.getOrNull(index - 1)
                    if (index == 0 || (tokenBefore is Operator && tokenBefore.value != ")")) {
                        val tokOpRes = token.getUnop()
                        when (tokOpRes) {
                            is Ok -> {
                                operators.push(Operator(token.value, true))
                                Unit
                            }
                            is Err -> return Err("*** Error in expression parsing ***\n\t${tokOpRes.error}")
                        }
                    } else {
                        val tokOpRes = token.getBinop()
                        val tokBinop = when (tokOpRes) {
                            is Ok -> tokOpRes.value
                            is Err -> return Err("*** Error in expression parsing ***\n\t${tokOpRes.error}")
                        }

                        while (operators.isNotEmpty() && operators.peek()?.value?.isParanthesis()?.not() == true) {
                            val opStackResult = operators.peek()?.getBinop()
                                    ?: return Err("*** No operators on shunting yard stack ***")
                            val opStackBinop = when (opStackResult) {
                                is Ok -> opStackResult.value
                                is Err -> return Err("*** Something went wrong 2:\n\t${opStackResult.error}")
                            }

                            if (tokBinop.priorityBinop() <= opStackBinop.priorityBinop()) {
                                operators.pop()?.let { output.push(it) }
                            } else break
                        }

                        operators.push(token)
                    }
                }
            }
            is Token.EndOfLine -> {}
            else -> return Err("*** Shunting yard can not handle: $token")
        }
    }
    while (operators.isNotEmpty()) operators.pop()?.let { output.push(it) }

    return parseReversePolishNotation(output)
}

fun parseReversePolishNotation(tokens: MutableList<Token>): Result<Expression> {
    val expressionStack = mutableListOf<Expression>()
    val operatorStack = mutableListOf<Operator>()

    var pendingOperand = false
    while (tokens.isNotEmpty()) {
        val token = tokens.pop()
        when(token) {
            is Operator -> {
                operatorStack.push(token)
                pendingOperand = token.unary
            }
            is Identifier,
            is StringLiteral,
            is NumberLiteral -> {
                var expression: Expression = when(token) {
                    is NumberLiteral -> ExpInt(token.value)
                    is StringLiteral -> ExpStr(token.value)
                    is Identifier -> ExpIdentifier(token.value)
                    else -> return Err("*** Compiler has failed us: $token ***")
                }
                if (pendingOperand) {
                    while (operatorStack.isNotEmpty() && operatorStack.peek()?.unary == true) {
                        val operatorResult = operatorStack.pop()!!.getUnop()
                        val operator = when(operatorResult) {
                            is Ok -> operatorResult.value
                            is Err -> return Err("*** Something went wrong 3:\n\t${operatorResult.error}")
                        }
                        expression = ExpUnr(op = operator, expression = expression)
                    }
                    while (expressionStack.isNotEmpty()) {
                        val operatorResult = operatorStack.pop()?.getBinop() ?: return Err("*** No operator ***")
                        val rightOperand = expressionStack.pop() ?: return Err("*** No expression ***")

                        val operator = when(operatorResult) {
                            is Ok -> operatorResult.value
                            is Err -> return Err("*** Something went wrong 3:\n\t${operatorResult.error}")
                        }

                        expression = ExpBin(left = expression, op = operator, right = rightOperand)
                    }
                }
                expressionStack.push(expression)
                pendingOperand = true
            }
        }
    }
    return expressionStack.pop()?.let{ Ok(it) } ?: return Err("*** No expression ***")
}

fun String.isParanthesis(): Boolean = this == "(" || this == ")"

fun <T>MutableList<T>.push(item: T) = add(item)
fun <T>MutableList<T>.peek(): T? = lastOrNull()
fun <T>MutableList<T>.pop(): T? {
    val item = lastOrNull()
    if (isNotEmpty()){
        removeAt(size -1)
    }
    return item
}
