package parser.json

import parser.core.*

sealed class JValue

object JNull : JValue()

data class JBool(val value: Boolean) : JValue()

data class JString(val value: String) : JValue()

data class JNumber(val value: Double) : JValue() {
    constructor(value: Int) : this(value.toDouble())
}

data class JArray(val value: List<JValue>) : JValue()

data class JObject(val value: Map<String, JValue>) : JValue()

fun jValue() = any(
    jNull(),
    jBool(),
    jNumber(),
    jString(),
    Parser { jArray()(it) },
    Parser { jObject()(it) }
)

fun jNull() =
    pString("null") map (JNull) label "null"

fun jBool() =
    pBoolean() map { JBool(it) } label "bool"

fun jUnescapedChar() =
    satisfy("char") { it != '\\' && it != '\"' }

fun jEscapedChar() =
    any(listOf(
        Pair("\\\"", '\"'),      // quote
        Pair("\\\\", '\\'),      // reverse solidus
        Pair("\\/", '/'),        // solidus
        Pair("\\b", '\b'),       // backspace
        Pair("\\f", 'f'),        // form feed
        Pair("\\n", '\n'),       // newline
        Pair("\\r", '\r'),       // cr
        Pair("\\t", '\t')        // tab
    ).map { pair ->
        pString(pair.first) map { pair.second }
    }) label "escaped-char"

fun jUnicodeChar(): Parser<Char> {
    fun backslash() = pChar('\\')
    fun uChar() = pChar('u')
    fun hexDigit() = pAnyOf(('0'..'9').toList() + ('A'..'F').toList() + ('a'..'f').toList())

    return backslash() andr uChar() andr hexDigit() and hexDigit() and hexDigit() and hexDigit() map {
        it.joinToString("").toInt(16).toChar()
    }
}

fun jString() =
    quotedString() map { JString(it) } label "quoted string"

fun jNumber() = pNumber() map { JNumber(it) }

fun jArray(): Parser<JArray> {
    fun left() = pChar('[') andl spaces()
    fun right() = pChar(']') andl spaces()
    fun values() = separatedBy(value(), comma())

    return pBetween(left(), values(), right()) map { values ->
        values
            .map { it as JValue }
            .let { JArray(it) }
    } label "array"
}

fun jObject(): Parser<JObject> {
    fun left() = pChar('{') andl spaces()
    fun right() = pChar('}') andl spaces()
    fun colon() = pChar(':') andl spaces()
    fun key() = quotedString() andl spaces()

    fun keyValue() = key() andl colon() and value()
    fun keyValues() = separatedBy(keyValue(), comma())

    return pBetween(left(), keyValues(), right()) map { pairs ->
        pairs.chunked(2).map { pair ->
            Pair(pair.first() as String, pair.last() as JValue)
        }.toMap().let { JObject(it) }
    } label "object"
}

private fun separatedBy(parser: Parser<out Any>, sep: Parser<out Any>): Parser<List<Any>> =
    parser and zeroOrMore(sep andr parser)

private fun spaces() = zeroOrMore(pWhitespace())

private fun comma() = pChar(',') andl spaces()

private fun value() = jValue() andl spaces()

private fun quotedString(): Parser<String> {
    fun quote() = pChar('\"') label "quote"
    fun jChar() = jUnescapedChar() or jEscapedChar() or jUnicodeChar()

    return quote() andr (zeroOrMore(jChar())) andl quote() map { it.joinToString("") }
}
