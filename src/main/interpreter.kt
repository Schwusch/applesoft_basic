package main

import main.ExprResult.*
import main.Result.Err
import main.Result.Ok
import main.BinaryOp.*
import main.Expression.*
import main.Command.*

sealed class ExprResult{
    data class ResStr(val value: String): ExprResult()
    data class ResInt(val value: Int): ExprResult()
}

data class Line(val original: String, val command: Command)

class Interpreter {

    private val variables = mutableMapOf<String, ExprResult>()
    private val lines = mutableMapOf<Int, Line>()
    private var pc = -1

    fun interpretCommand(command: CommandResult): Boolean {

        return when(command.command) {
            is Ok -> interpretLine(Line(
                    original = command.original,
                    command = command.command.value
            ))
            is Err -> {
                println(command.command.error)
                true
            }
        }
    }

    private fun interpretLine(line: Line): Boolean {
        return when(line.command) {
            is Print -> {
                interpretPrint(line.command)
                true
            }
            is Assignment -> {
                interpretAssignment(line.command)
                true
            }
            is StoreCommand -> {
                lines[line.command.line] = Line(line.original, line.command.command)
                true
            }
            is Run -> {
                runProgram(line.command)
                true
            }
            is ListCommand -> {
                listProgramInMemory()
                true
            }
            is GoTo -> {
                pc = line.command.line
                false
            }
            is Command.GoSub -> {
                lines[line.command.line]?.let { interpretLine(it) }
                true
            }
            is Command.If -> {
                interpretIf(line.command)
            }
            Command.Return -> true
            Command.Pop -> false
            Command.ExpREM -> true
            is Command.Multiple -> {
                line.command.commands.forEach {
                    interpretCommand(CommandResult(original = it.original, command = it.command))
                    Unit
                }
                true
            }
        }
    }

    private fun listProgramInMemory() {
        val linesSorted = lines.keys.sorted()
        for (line in linesSorted) {
            lines[line]?.let {
                println(it.original)
            }
        }
    }

    private fun runProgram(command: Run) {
        val linesSorted = lines.keys.filter { it >= command.line  }.sorted()

        pc = linesSorted.first()

        while (true) {
            val ordinary = lines[pc]?.let { interpretLine(it) } ?: break

            if (ordinary) {
                pc = linesSorted.getOrNull(linesSorted.indexOf(pc)  + 1) ?: break
            }
        }
    }

    private fun interpretIf(command: If): Boolean {
        val result = interpretExpression(command.eval)
        return when (result) {
            is Ok -> when (result.value) {
                is ResInt -> if (result.value.value > 0) interpretCommand(command.then) else true
                else -> true
            }
            else -> true
        }
    }

    private fun interpretAssignment(command: Assignment) {
        val res = interpretExpression(command.expression)
        when(res) {
            is Ok -> variables[command.identifier.value] = res.value
            is Err -> println(res.error)
        }
    }

    private fun interpretPrint(command: Print) {
        when(command.expression) {
            is ExpStr -> println(command.expression.value)
            is ExpInt -> println(command.expression.value)
            is ExpBin -> {
                val res = interpretBinaryExpression(command.expression)
                when(res) {
                    is Ok -> {
                        val expRes = res.value
                        when(expRes) {
                            is ResInt -> println(expRes.value)
                            is ResStr -> println(expRes.value)
                        }
                    }
                    is Err -> println(res.error)
                }
            }
            is ExpIdentifier -> {
                val res = interpretIdentifier(command.expression)
                when(res) {
                    is ResInt -> println(res.value)
                    is ResStr -> println(res.value)
                }
            }
            else -> println("*** Cannot interpret command: $command")
        }
    }

    private fun interpretIdentifier(identifier: ExpIdentifier): ExprResult = variables.getOrPut(identifier.value) {
        ResInt(0)
    }

    private fun interpretExpression(expression: Expression): Result<ExprResult> = when(expression){
        is ExpInt -> Ok(ResInt(expression.value))
        is ExpStr -> Ok(ResStr(expression.value))
        is ExpBin -> interpretBinaryExpression(expression)
        is ExpUnr -> TODO()
        is ExpIdentifier -> Ok(interpretIdentifier(expression))
    }

    private fun interpretBinaryExpression(expression: Expression.ExpBin): Result<ExprResult> {
        val leftResResult = interpretExpression(expression.left)
        val rightResResult = interpretExpression(expression.right)

        val leftRes = when(leftResResult) {
            is Ok -> leftResResult.value
            is Err -> return leftResResult
        }
        val rightRes = when(rightResResult) {
            is Ok -> rightResResult.value
            is Err -> return rightResResult
        }

        return when(expression.op) {
            Plus -> if (leftRes is ResInt && rightRes is ResInt ) {
                Ok(ResInt(leftRes.value + rightRes.value))
            } else if (leftRes is ResStr && rightRes is ResStr ) {
                Ok(ResStr(leftRes.value + rightRes.value))
            } else Err<ExprResult>("Cannot add $leftRes to $rightRes")

            Minus ->  if (leftRes is ResInt && rightRes is ResInt ) {
                Ok(ResInt(leftRes.value - rightRes.value))
            } else Err("Cannot subtract $leftRes to $rightRes")

            Mult -> if (leftRes is ResInt && rightRes is ResInt ) {
                Ok(ResInt(leftRes.value * rightRes.value))
            } else Err("Cannot multiply $leftRes to $rightRes")

            Div -> if (leftRes is ResInt && rightRes is ResInt ) {
                Ok(ResInt(leftRes.value / rightRes.value))
            } else Err("Cannot divide $leftRes to $rightRes")

            Mod -> if (leftRes is ResInt && rightRes is ResInt ) {
                Ok(ResInt(leftRes.value % rightRes.value))
            } else Err("Cannot modulus $leftRes to $rightRes")

            Equal -> if (leftRes is ResInt && rightRes is ResInt ) {
                Ok(ResInt(
                        if (leftRes.value == rightRes.value) 1 else 0
                ))
            } else if (leftRes is ResStr && rightRes is ResStr ) {
                Ok(ResInt(
                        if (leftRes.value == rightRes.value) 1 else 0
                ))
            } else Err<ExprResult>("Cannot compare $leftRes = $rightRes")

            Less -> if (leftRes is ResInt && rightRes is ResInt ) {
                Ok(ResInt(
                        if(leftRes.value - rightRes.value < 0) 1 else 0
                ))
            } else Err("Cannot compare $leftRes < $rightRes")

            LessEq -> if (leftRes is ResInt && rightRes is ResInt ) {
                Ok(ResInt(
                        if(leftRes.value - rightRes.value <= 0) 1 else 0
                ))
            } else Err("Cannot compare $leftRes <= $rightRes")

            Great ->  if (leftRes is ResInt && rightRes is ResInt ) {
                Ok(ResInt(
                        if(leftRes.value - rightRes.value > 0) 1 else 0
                ))
            } else Err("Cannot compare $leftRes > $rightRes")

            GreatEq -> if (leftRes is ResInt && rightRes is ResInt ) {
                Ok(ResInt(
                        if (leftRes.value - rightRes.value >= 0) 1 else 0
                ))
            } else Err("Cannot compare $leftRes >= $rightRes")

            Diff -> if (leftRes is ResInt && rightRes is ResInt ) {
                Ok(ResInt(
                        if (leftRes.value != rightRes.value) 1 else 0
                ))
            }  else if (leftRes is ResStr && rightRes is ResStr ) {
                Ok(ResInt(
                        if (leftRes.value != rightRes.value) 1 else 0
                ))
            } else Err<ExprResult>("Cannot compare $leftRes <> $rightRes")

            And -> if (leftRes is ResInt && rightRes is ResInt ) {
                Ok(ResInt(
                        if (leftRes.value > 0 && rightRes.value > 0) 1 else 0
                ))
            } else Err("Cannot compare $leftRes & $rightRes")
            Or -> if (leftRes is ResInt && rightRes is ResInt ) {
                Ok(ResInt(
                        if (leftRes.value > 0 || rightRes.value > 0) 1 else 0
                ))
            } else Err("Cannot compare $leftRes & $rightRes")
        }
    }
}
