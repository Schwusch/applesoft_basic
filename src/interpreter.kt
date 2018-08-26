import ExprResult.*
import Result.Err
import Result.Ok
import BinaryOp.*
import Expression.*
import Command.*

sealed class ExprResult{
    data class ResStr(val value: String): ExprResult()
    data class ResInt(val value: Int): ExprResult()
}

class Interpreter {

    val variables = mutableMapOf<String, ExprResult>()

    fun interpretCommand(command: Command) {
        when(command) {
            is Print -> interpretPrint(command)
            is Assignment -> interpretAssignment(command)
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
                        when(res.value) {
                            is ResInt -> println(res.value.value)
                            is ResStr -> println(res.value.value)
                        }
                    }
                    is Err -> println(res.error)
                }
            }
            is ExpIdentifier -> {
                val res = variables[command.expression.value]
                when(res) {
                    is ResInt -> println(res.value)
                    is ResStr -> println(res.value)
                }
            }
            else -> println("*** Cannot interpret command: $command")
        }
    }

    private fun interpretExpression(expression: Expression): Result<ExprResult> = when(expression){
        is ExpInt -> Ok(ResInt(expression.value))
        is ExpStr -> Ok(ResStr(expression.value))
        is ExpBin -> interpretBinaryExpression(expression)
        is ExpUnr -> TODO()
        is ExpIdentifier -> {
            val res = variables[expression.value]
            when(res) {
                is ExprResult -> Ok(res)
                else -> Err("No variable with name ${expression.value}")
            }
        }
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

            Equal -> TODO()
            Less -> TODO()
            LessEq -> TODO()
            Great -> TODO()
            GreatEq -> TODO()
            Diff -> TODO()
            And -> TODO()
            Or -> TODO()
        }
    }
}
