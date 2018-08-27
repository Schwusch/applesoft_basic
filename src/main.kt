
sealed class Result<out V : Any> {
    data class Ok<out V : Any>(val value: V) : Result<V>()
    data class Err<out V : Any>(val error: String) : Result<V>()
}

fun main(args: Array<String>) {
    val interpreter = Interpreter()

    while (true) {
        print("]")
        readLine()?.let { input ->
            interpreter.interpretCommand(
                    parseToCommand(
                            tokenizeLine(input)
                    )
            )
        }
    }
}