package parser.core

data class State(
    val input: String = "",
    val line: Int = 0,
    val col: Int = 0,
    val pos: Int = 0
) {
    fun eof() = pos >= input.length
    fun char() = input[pos]

    fun next(): State {
        return if (char() != '\n')
            this.copy(col = this.col + 1, pos = this.pos + 1)
        else
            this.copy(line = this.line + 1, col = 0, pos = this.pos + 1)
    }
}

sealed class Result<T>

data class Success<T>(
    val value: T,
    val state: State
) : Result<T>()

data class Failure<T>(val error: ParserError) : Result<T>()

sealed class ParserError(open val label: String? = null) {
    abstract fun relabel(label: String?): ParserError
}

data class UnexpectedToken(
    override val label: String? = null,
    val char: Char,
    val line: Int,
    val col: Int
) : ParserError() {

    override fun relabel(label: String?) = this.copy(label = label)

    override fun toString() =
        """Error parsing ${label ?: "unknown"}. Unexpected '$char' at Line $line, Column $col"""
}

data class NoMoreInput(
    override val label: String? = null
) : ParserError() {

    override fun relabel(label: String?) = this.copy(label = label)

    override fun toString() =
        """Error parsing ${label ?: "unknown"}. No more input"""
}

class Parser<T : Any>(
    private val label: String? = null,
    private val parse: (State) -> Result<out T>
) {
    operator fun invoke(state: State): Result<out T> = parse(state)

    fun run(state: State): Result<out T> = this(state)

    infix fun label(label: String) =
        Parser(label) { state ->
            when (val result = this(state)) {
                is Success -> result
                is Failure -> Failure<T>(result.error.relabel(label))
            }
        }

    infix fun <R : Any> map(transformer: (T) -> R): Parser<R> =
        Parser { state ->
            when (val result = this.invoke(state)) {
                is Success -> Success(transformer(result.value), result.state)
                is Failure -> Failure<R>(result.error.relabel(label))
            }
        }

    infix fun and(that: Parser<out Any>): Parser<List<Any>> =
        Parser { state ->
            when (val thisResult = this(state)) {
                is Failure -> Failure<List<Any>>(error = thisResult.error)
                is Success ->
                    when (val thatResult = that(thisResult.state)) {
                        is Failure -> Failure(error = thatResult.error)
                        is Success -> Success(concat(thisResult.value as Any, thatResult.value), thatResult.state)
                    }
            }
        } label "${this.label} and ${that.label}"

    infix fun or(that: Parser<out Any>): Parser<out Any> =
        Parser { state ->
            when (val thisResult = this(state)) {
                is Success -> thisResult
                is Failure -> that(state)
            }
        } label "${this.label} or ${that.label}"

    infix fun andr(right: Parser<out Any>): Parser<out Any> =
        (this and right) map { list -> list.last() }

    infix fun andl(right: Parser<out Any>): Parser<out Any> =
        (this and right) map { list -> list.first() }

    private fun concat(left: Any, right: Any): List<Any> =
        listOf(left, right)
            .filter { it !is Unit }
            .map { if (it is List<*>) it.filterNotNull() else listOf(it) }
            .flatten()
}

//* Combinators *//

fun satisfy(label: String, predicate: (Char) -> Boolean): Parser<Char> =
    Parser { state ->
        when {
            state.eof() -> Failure<Char>(NoMoreInput(label))
            predicate(state.char()) -> Success(state.char(), state.next())
            else -> Failure(UnexpectedToken(label, state.char(), state.line, state.col))
        }
    } label label

fun any(parsers: List<Parser<out Any>>) = parsers.reduce { l, r -> l or r } label "any"

fun any(vararg parser: Parser<out Any>) = any(parser.toList())

fun all(parsers: List<Parser<out Any>>): Parser<List<Any>> {
    val emptyParser: Parser<List<Any>> = Parser { Success<List<Any>>(emptyList(), it) }

    return parsers.fold(emptyParser) { l, r -> l and r } label "all"
}

fun all(vararg parser: Parser<out Any>) = all(parser.toList())

fun optional(parser: Parser<out Any>): Parser<out Any> =
    parser or Parser { Success(Unit, it) } label "zero-or-more"

fun zeroOrMore(parser: Parser<out Any>): Parser<List<Any>> {
    fun concat(left: Any, right: Any): List<Any> =
        listOf(left, right)
            .filter { it !is Unit }
            .map { if (it is List<*>) it.filterNotNull() else listOf(it) }
            .flatten()

    fun zeroOrMoreParser(state: State): Success<List<Any>> =
        when (val first = parser(state)) {
            is Failure -> Success(emptyList(), state)
            is Success ->
                zeroOrMoreParser(first.state).let {
                    Success(concat(first.value, it.value), it.state)
                }
        }

    return Parser(parse = ::zeroOrMoreParser)
}

fun oneOrMore(parser: Parser<out Any>): Parser<List<Any>> =
    parser and zeroOrMore(parser) label "one-or-more"

//* Parsers *//

fun pBetween(left: Parser<out Any>, middle: Parser<out Any>, right: Parser<out Any>): Parser<out Any> =
    left andr middle andl right label "between"

fun pAnyOf(chars: List<Char>) = any(chars.map { pChar(it) }) label "any-of $chars"

fun pAnyOf(vararg chars: Char) = pAnyOf(chars.toList()) label "any-of $chars"

fun pLowercase() = pAnyOf(('a'..'z').toList()) label "lowercase"

fun pDigit() = pAnyOf(('0'..'9').toList()) label "digit"

fun pWhitespaceChar() = pAnyOf(' ', '\t', '\n') label "whitespace-char"

fun pWhitespace() = oneOrMore(pWhitespaceChar()) label "whitespace"

fun pDigits(): Parser<List<Any>> = oneOrMore(pDigit()) label "digits"

//** Primitive Type Parsers **//

fun pChar(c: Char): Parser<Char> =
    satisfy(c.toString()) { it == c }

fun pString(string: String): Parser<String> =
    all(string.map(::pChar)) map { it.joinToString("") } label string

fun pBoolean(): Parser<Boolean> =
    pString("true") or pString("false") map { it.toString().toBoolean() } label "boolean"

fun pSign(): Parser<Char> =
    pChar('+') or pChar('-') map { it as Char }

fun pInt(): Parser<Int> =
    (optional(pSign()) and pDigits()) map { it.joinToString("").toInt() } label "int"

fun pNumber(): Parser<Double> {
    fun e() = pChar('e') or pChar('E')
    fun optSign() = optional(pSign())
    fun intPart() = optSign() and pDigits()
    fun optFraction() = optional(pChar('.') and pDigits())
    fun optExponent() = optional(e() and optSign() and pDigits())

    return (intPart() and optFraction() and optExponent()) map { it.joinToString("").toDouble() } label "number"
}
