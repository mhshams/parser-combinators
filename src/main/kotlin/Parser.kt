typealias Parser<T> = (State) -> Result<T>

data class State(
    val input: String = "",
    val line: Int = 0,
    val col: Int = 0,
    val pos: Int = 0
) {
    fun eof() = col >= input.length
    fun pos() = input[pos]
}

sealed class Result<T>
data class Success<T>(val value: T, val state: State) : Result<T>()
data class Failure<T>(val message: String) : Result<T>()


fun <T> run(parser: Parser<out T>, state: State): Result<out T> = parser(state)

//** Parsers **//

fun pChar(c: Char): Parser<Char> =
    { state ->
        when {
            state.eof() -> Failure("No more input")
            state.pos() == c -> Success(c, state.copy(col = state.col + 1, pos = state.pos + 1))
            else -> Failure("Expected $c. Got ${state.pos()}")
        }
    }

@Suppress("UNCHECKED_CAST")
fun <T, R> map(parser: Parser<out T>, transformer: (T) -> R): Parser<R> =
    { state ->
        when (val result = parser(state)) {
            is Success -> Success(transformer(result.value), result.state)
            else -> result as Failure<R>
        }
    }

@Suppress("UNCHECKED_CAST")
private fun concat(left: Any, right: Any): List<Any> =
    when {
        left is List<*> && right is List<*> -> left + right
        left is List<*> -> left + right
        right is List<*> -> listOf(left) + right
        else -> listOf(left, right)
    } as List<Any>

@Suppress("UNCHECKED_CAST")
fun andThen(left: Parser<out Any>, right: Parser<out Any>): Parser<List<Any>> =
    { state ->
        when (val leftResult = left(state)) {
            is Failure -> leftResult as Failure<List<Any>>
            is Success ->
                when (val rightResult = right(leftResult.state)) {
                    is Failure -> rightResult as Failure<List<Any>>
                    is Success -> Success(concat(leftResult.value, rightResult.value), rightResult.state)
                }
        }
    }

fun orElse(left: Parser<out Any>, right: Parser<out Any>): Parser<out Any> =
    { state ->
        when (val leftResult = left(state)) {
            is Success -> leftResult
            is Failure -> right(state)
        }
    }

fun andThenWithoutLeft(left: Parser<out Any>, right: Parser<out Any>): Parser<out Any> =
    map(andThen(left, right), { list -> list.last() })

fun andThenWithoutRight(left: Parser<out Any>, right: Parser<out Any>): Parser<out Any> =
    map(andThen(left, right), { list -> list.first() })

fun between(left: Parser<out Any>, middle: Parser<out Any>, right: Parser<out Any>): Parser<out Any> =
    andThenWithoutRight(andThenWithoutLeft(left, middle), right)

@Suppress("UNCHECKED_CAST")
fun <R : Any> reduce(
    reducer: (Parser<out Any>, Parser<out Any>) -> Parser<out R>,
    vararg parsers: Parser<out Any>
): Parser<R> =
    parsers.reduce { l, r -> reducer(l, r) } as Parser<R>

fun choice(vararg parsers: Parser<out Any>) = reduce(::orElse, *parsers)

fun anyOf(vararg chars: Char) = choice(*(chars.map { pChar(it) }.toTypedArray()))

fun pLowercase() = anyOf(*(('a'..'z').toList().toCharArray()))

fun pDigit() = anyOf(*(('0'..'9').toList().toCharArray()))

fun sequence(vararg parsers: Parser<out Any>): Parser<List<Any>> =
    reduce(::andThen, *parsers)

fun pString(string: String): Parser<String> =
    map(sequence(*(string.map(::pChar).toTypedArray())), { it.joinToString("") })

fun zeroOrMore(parser: Parser<out Any>): Parser<List<Any>> {
    fun zeroOrMoreParser(state: State): Success<List<Any>> =
        when (val first = parser(state)) {
            is Failure -> Success(emptyList(), state)
            is Success ->
                zeroOrMoreParser(first.state).let {
                    Success(concat(first.value, it.value), it.state)
                }
        }

    return ::zeroOrMoreParser
}

fun oneOrMore(parser: Parser<out Any>): Parser<List<Any>> =
    andThen(parser, zeroOrMore(parser))

fun zeroOrOne(parser: Parser<out Any>): Parser<out Any> =
    orElse(parser, { Success(Unit, it) })

fun whitespaceChar() = anyOf(' ', '\t', '\n')

fun whitespace() = oneOrMore(whitespaceChar())

fun pDoubleQuote() = pChar('"')

fun pDigits(): Parser<List<Any>> = oneOrMore(pDigit())

fun pInt(): Parser<Int> =
    map(pDigits(), { it.joinToString("").toInt() })

fun pQuotedInt() = between(pDoubleQuote(), pInt(), pDoubleQuote())
