package com.bocker.applesoft

import com.bocker.applesoft.ExprResult.*
import com.bocker.applesoft.Result.Err
import com.bocker.applesoft.Result.Ok
import com.bocker.applesoft.BinaryOp.*
import com.bocker.applesoft.Expression.*
import com.bocker.applesoft.Command.*

sealed class ExprResult{
    data class ResStr(val value: String): ExprResult()
    data class ResInt(val value: Int): ExprResult()
}

data class Line(val original: String, val command: Command)

class Interpreter {

    private val variables = mutableMapOf<String, ExprResult>()
    private val lines = mutableMapOf<Int, Line>()
    private var pc = -1
    private var runNext = true
    var onPrint: ((String) -> Unit)? = null

    fun interpretCommand(commandToInterpret: CommandResult): Result<Unit> {
        var command : Command? = null

        val res = when(commandToInterpret.command) {
            is Ok -> {
                command = commandToInterpret.command.value
                interpretLine(Line(
                        original = commandToInterpret.original,
                        command = commandToInterpret.command.value
                ))
            }
            is Err -> {
                runNext = true
                Err(commandToInterpret.command.error)
            }
        }

        if (res is Err && command !is Multiple) onPrint?.invoke(res.error)

        return res
    }

    private fun interpretLine(line: Line): Result<Unit> {
        return when(line.command) {
            is Print -> {
                runNext = true
                interpretPrint(line.command)
            }
            is Assignment -> {
                runNext = true
                interpretAssignment(line.command)
            }
            is StoreCommand -> {
                runNext = true
                lines[line.command.line] = Line(line.original, line.command.command)
                Ok(Unit)
            }
            is Run -> {
                runNext = true
                runProgram(line.command)
            }
            is ListCommand -> {
                runNext = true
                listProgramInMemory()
            }
            is GoTo -> {
                runNext = false
                pc = line.command.line
                Ok(Unit)
            }
            is GoSub -> {
                runNext = true
                lines[line.command.line]?.let { interpretLine(it) } ?: Err("No such line in memory")
            }
            is If -> {
                runNext = true
                interpretIf(line.command)
            }
            Return -> Err("Command 'RETURN' not implemented")
            Pop -> Err("Command 'POP' not implemented")
            ExpREM -> Ok(Unit)
            is Command.Multiple -> {
                for((index, command) in line.command.commands.withIndex()) {
                    val res = interpretCommand(CommandResult(original = command.original, command = command.command))
                    if (res is Err) {
                        val next = line.command.commands.getOrNull(index + 1)?.command
                        if(next is Ok && next.value !is OnErr) {
                            return res
                        }
                    }
                }
                return Ok(Unit)
            }
            is Command.OnErr -> interpretLine(Line(original = line.original, command = line.command.command))
        }
    }

    private fun listProgramInMemory(): Result<Unit> {
        val linesSorted = lines.keys.sorted()
        for (line in linesSorted) {
            lines[line]?.let {
                onPrint?.invoke(it.original)
            }
        }
        return Ok(Unit)
    }

    private fun runProgram(command: Run): Result<Unit> {
        val linesSorted = lines.keys.filter { it >= command.line  }.sorted()

        pc = linesSorted.first()

        while (true) {
            lines[pc]?.let {
                val res = interpretLine(it)
                if (res is Err) return Err("*** Error on line $pc *** \n\t${res.error}")
            } ?: return Err("No such line in memory: $pc")

            if (runNext) {
                val next = linesSorted.indexOf(pc)
                pc = linesSorted.getOrNull(if (next == -1) -2 else next + 1) ?: return Ok(Unit)
            }
        }
    }

    private fun interpretIf(command: If): Result<Unit> {
        val result = interpretExpression(command.eval)
        return when (result) {
            is Ok -> when (result.value) {
                is ResInt -> {
                    if (result.value.value > 0) interpretCommand(command.then) else Ok(Unit)
                }
                else -> Err("Cannot handle if statement that results other than integer")
            }
            is Err -> return Err(result.error)
        }
    }

    private fun interpretAssignment(command: Assignment): Result<Unit> {
        val res = interpretExpression(command.expression)
        when(res) {
            is Ok -> variables[command.identifier.value] = res.value
            is Err -> return Err(res.error)
        }
        return Ok(Unit)
    }

    private fun interpretPrint(command: Print): Result<Unit> {
        when(command.expression) {
            is ExpStr -> onPrint?.invoke(command.expression.value)
            is ExpInt -> onPrint?.invoke(command.expression.value.toString())
            is ExpBin -> {
                val res = interpretBinaryExpression(command.expression)
                when(res) {
                    is Ok -> {
                        val expRes = res.value
                        when(expRes) {
                            is ResInt -> onPrint?.invoke(expRes.value.toString())
                            is ResStr -> onPrint?.invoke(expRes.value)
                        }
                    }
                    is Err -> return Err(res.error)
                }
            }
            is ExpUnr -> {
                val res = interpretUnaryExpression(command.expression)
                when(res) {
                    is Ok -> {
                        val expRes = res.value
                        when(expRes) {
                            is ResInt -> onPrint?.invoke(expRes.value.toString())
                            is ResStr -> onPrint?.invoke(expRes.value)
                        }
                    }
                    is Err -> return Err(res.error)
                }
            }
            is ExpIdentifier -> {
                val res = interpretIdentifier(command.expression)
                when(res) {
                    is ResInt -> onPrint?.invoke(res.value.toString())
                    is ResStr -> onPrint?.invoke(res.value)
                }
            }
        }
        return Ok(Unit)
    }

    private fun interpretIdentifier(identifier: ExpIdentifier): ExprResult = variables.getOrPut(identifier.value) {
        ResInt(0)
    }

    private fun interpretExpression(expression: Expression): Result<ExprResult> = when(expression){
        is ExpInt -> Ok(ResInt(expression.value))
        is ExpStr -> Ok(ResStr(expression.value))
        is ExpBin -> interpretBinaryExpression(expression)
        is ExpUnr -> interpretUnaryExpression(expression)
        is ExpIdentifier -> Ok(interpretIdentifier(expression))
    }

    private fun interpretUnaryExpression(expression: ExpUnr): Result<ExprResult> {
        val resResult = interpretExpression(expression.expression)
        val res = when(resResult) {
            is Ok -> resResult.value
            is Err -> return resResult
        }
        return when(expression.op) {
            UnaryOp.UMinus -> when(res) {
                is ExprResult.ResStr -> Err("*** Can not negate string '${res.value}'")
                is ExprResult.ResInt -> Ok(ResInt(-res.value))
            }
            UnaryOp.Not -> when(res) {
                is ExprResult.ResStr -> Err("*** Can not NOT string '${res.value}'")
                is ExprResult.ResInt -> Ok(ResInt(if (res.value > 0) 0 else 1))
            }
        }

    }

    private fun interpretBinaryExpression(expression: ExpBin): Result<ExprResult> {
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
