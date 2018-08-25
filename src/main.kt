
sealed class Result<out V : Any> {
    data class Ok<out V : Any>(val value: V) : Result<V>()
    data class Err<out V : Any>(val error: String) : Result<V>()
}

fun main(args: Array<String>) {
    while (true) {
        print(">>> ")
        readLine()?.let { input ->
            val tokens = tokenize_line(input)
            println(tokens)
            val parseResult = parseToCommand(tokens)
            println(parseResult)
            when(parseResult) {
                is Result.Err -> println(parseResult.error)
                is Result.Ok -> interpretCommand(parseResult.value)
            }
        }
    }
}