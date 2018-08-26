import ExprResult.*
import Result.Err
import Result.Ok
import BinaryOp.*
import Expression.*

sealed class ExprResult{
    data class ResStr(val value: String): ExprResult()
    data class ResInt(val value: Int): ExprResult()
}

fun interpretCommand(command: Command) {
    when(command) {
        is Command.Print -> when(command.expression) {
            is ExpStr -> println(command.expression.value)
            is ExpInt -> println(command.expression.value)
            is ExpBin -> println(interpretBinaryExpression(command.expression))
            else -> println("*** Cannot interpret command: $command")
        }
    }
}

fun interpretExpression(expression: Expression): Result<ExprResult> = when(expression){
    is ExpInt -> Ok(ResInt(expression.value))
    is ExpStr -> Ok(ResStr(expression.value))
    is ExpUnr -> TODO()
    is ExpBin -> interpretBinaryExpression(expression)
}

fun interpretBinaryExpression(expression: Expression.ExpBin): Result<ExprResult> {
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