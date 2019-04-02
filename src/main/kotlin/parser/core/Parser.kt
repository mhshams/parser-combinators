package parser.core

import kotlin.NoSuchElementException

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

data class Failure(val error: ParserError) : Result<Nothing>()

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

sealed class Maybe<T : Any> {
    companion object {
        fun <T : Any> just(value: T): Maybe<T> = Just(value)

        @Suppress("UNCHECKED_CAST")
        fun <T : Any> none(): Maybe<T> = None as Maybe<T>
    }

    abstract fun get(): T
}

data class Just<T : Any>(private val value: T) : Maybe<T>() {
    override fun get() = value
}

object None : Maybe<Nothing>() {
    override fun get() = throw NoSuchElementException("")
}

class Parser<T : Any>(
    private val label: String? = null,
    private val parse: (State) -> Result<out T>
) {
    companion object {
        fun concat(left: Any, right: Any): List<Any> =
            listOf(left, right)
                .filter { it !is None }
                .map { if (it is Just<*>) it.get() else it }
                .map { if (it is List<*>) it.filterNotNull() else listOf(it) }
                .flatten()
    }

    operator fun invoke(state: State): Result<out T> = parse(state)

    fun run(state: State): Result<out T> = this(state)

    infix fun label(label: String) =
        Parser(label) { state ->
            when (val result = this(state)) {
                is Success -> result
                is Failure -> Failure(result.error.relabel(label))
            }
        }

    infix fun <R : Any> map(transformer: (T) -> R): Parser<R> =
        Parser { state ->
            when (val result = this.invoke(state)) {
                is Success -> Success(transformer(result.value), result.state)
                is Failure -> Failure(result.error.relabel(label))
            }
        }

    infix fun <R : Any> map(r: R): Parser<R> =
        Parser { state ->
            when (val result = this.invoke(state)) {
                is Success -> Success(r, result.state)
                is Failure -> Failure(result.error.relabel(label))
            }
        }

    private fun <R : Any> andThen(that: Parser<R>): Parser<Pair<T, R>> =
        Parser { state ->
            when (val thisResult = this(state)) {
                is Failure -> Failure(error = thisResult.error)
                is Success ->
                    when (val thatResult = that(thisResult.state)) {
                        is Failure -> Failure(error = thatResult.error)
                        is Success -> Success(Pair(thisResult.value, thatResult.value), thatResult.state)
                    }
            }
        }

    infix fun and(that: Parser<out Any>): Parser<List<Any>> =
        andThen(that) map { concat(it.first, it.second) } label "${this.label} and ${that.label}"

    infix fun <R : Any> andr(that: Parser<R>): Parser<R> =
        andThen(that) map { it.second } label "${this.label} andr ${that.label}"

    infix fun andl(that: Parser<out Any>): Parser<T> =
        andThen(that) map { it.first } label "${this.label} andl ${that.label}"

    infix fun or(that: Parser<out T>): Parser<T> =
        Parser { state ->
            when (val thisResult = this(state)) {
                is Success -> thisResult
                is Failure -> that(state)
            }
        } label "${this.label} or ${that.label}"
}

//* Combinators *//

fun satisfy(label: String, predicate: (Char) -> Boolean): Parser<Char> =
    Parser { state ->
        when {
            state.eof() -> Failure(NoMoreInput(label))
            predicate(state.char()) -> Success(state.char(), state.next())
            else -> Failure(UnexpectedToken(label, state.char(), state.line, state.col))
        }
    } label label

fun <T : Any> choice(parsers: List<Parser<T>>): Parser<out T> = parsers.reduce { l, r -> l or r } label "choice"

fun <T : Any> choice(vararg parser: Parser<T>) = choice(parser.toList())

fun sequence(parsers: List<Parser<out Any>>): Parser<List<Any>> {
    val emptyParser: Parser<List<Any>> = Parser { Success<List<Any>>(emptyList(), it) }

    return parsers.fold(emptyParser) { l, r -> l and r } label "sequence"
}

fun sequence(vararg parser: Parser<out Any>) = sequence(parser.toList())

fun <T : Any> optional(parser: Parser<T>): Parser<Maybe<T>> {
    val none = Parser { Success(Maybe.none<T>(), it) }
    val just = parser map { Maybe.just(it) }

    return just or none label "zero-or-more"
}

fun zeroOrMore(parser: Parser<out Any>): Parser<List<Any>> {

    fun zeroOrMoreParser(state: State): Success<List<Any>> =
        when (val first = parser(state)) {
            is Failure -> Success(emptyList(), state)
            is Success ->
                zeroOrMoreParser(first.state).let {
                    Success(Parser.concat(first.value, it.value), it.state)
                }
        }

    return Parser(parse = ::zeroOrMoreParser)
}

fun oneOrMore(parser: Parser<out Any>): Parser<List<Any>> =
    parser and zeroOrMore(parser) label "one-or-more"

//* Parsers *//

fun pChar(c: Char): Parser<Char> =
    satisfy(c.toString()) { it == c }

fun <T : Any> pBetween(left: Parser<out Any>, middle: Parser<T>, right: Parser<out Any>): Parser<T> =
    left andr middle andl right label "between"

fun pAnyOf(chars: List<Char>) = choice(chars.map { pChar(it) }) label "choice-of $chars"

fun pAnyOf(vararg chars: Char) = pAnyOf(chars.toList()) label "choice-of $chars"

fun pLowercase() = pAnyOf(('a'..'z').toList()) label "lowercase"

fun pDigit() = pAnyOf(('0'..'9').toList()) label "digit"

fun pWhitespaceChar() = pAnyOf(' ', '\t', '\n') label "whitespace-char"

fun pWhitespace() = oneOrMore(pWhitespaceChar()) label "whitespace"

fun pDigits(): Parser<List<Any>> = oneOrMore(pDigit()) label "digits"

//** Primitive Type Parsers **//

fun pString(string: String): Parser<String> =
    sequence(string.map(::pChar)) map { it.joinToString("") } label string

fun pBoolean(): Parser<Boolean> {
    val pTrue = pString("true") map true
    val pFalse = pString("false") map false

    return pTrue or pFalse label "boolean"
}

fun pSign(): Parser<out Char> = pAnyOf('-', '+')

fun pInt(): Parser<Int> =
    (optional(pSign()) and pDigits()) map { it.joinToString("").toInt() } label "int"

fun pNumber(): Parser<Double> {
    val e = pAnyOf('e', 'E')
    val optSign = optional(pSign())
    val intPart = optSign and pDigits()
    val optFraction = optional(pChar('.') and pDigits())
    val optExponent = optional(e and intPart)

    return (intPart and optFraction and optExponent) map { it.joinToString("").toDouble() } label "number"
}
