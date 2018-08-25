
sealed class ExprResult{
    data class ResStr(val value: String): ExprResult()
    data class ResInt(val value: Int): ExprResult()
}

fun interpretCommand(command: Command) {
    when(command) {
        is Command.Print -> when(command.expression) {
            is Expression.ExpStr -> println(command.expression.value)
            is Expression.ExpInt -> println(command.expression.value)
            else -> println("*** Cannot interpret command: $command")
        }
    }
}