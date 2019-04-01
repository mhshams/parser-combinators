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

fun jValue() = choice(
    Parser<JValue> { jNull()(it) },
    Parser<JValue> { jBool()(it) },
    Parser<JValue> { jNumber()(it) },
    Parser<JValue> { jString()(it) },
    Parser<JValue> { jArray()(it) },
    Parser<JValue> { jObject()(it) }
)

fun jNull() =
    pString("null") map (JNull) label "null"

fun jBool() =
    pBoolean() map { JBool(it) } label "bool"

fun jString() =
    quotedString() map { JString(it) } label "quoted string"

fun jNumber() = pNumber() map { JNumber(it) }

fun jArray(): Parser<JArray> {
    val left = pChar('[') andl spaces()
    val right = pChar(']') andl spaces()
    val values = separatedBy(value(), comma())

    return pBetween(left, values, right) map { elements ->
        elements
            .map { it as JValue }
            .let { JArray(it) }
    } label "array"
}

fun jObject(): Parser<JObject> {
    val left = pChar('{') andl spaces()
    val right = pChar('}') andl spaces()
    val colon = pChar(':') andl spaces()
    val key = quotedString() andl spaces()

    val keyValue = key andl colon and value()
    val keyValues = separatedBy(keyValue, comma())

    return pBetween(left, keyValues, right) map { pairs ->
        pairs.chunked(2).map { pair ->
            Pair(pair.first() as String, pair.last() as JValue)
        }.toMap().let { JObject(it) }
    } label "object"
}

fun json(): Parser<out JValue> = jValue() label "json"

private fun separatedBy(parser: Parser<out Any>, sep: Parser<out Any>): Parser<List<Any>> =
    parser and zeroOrMore(sep andr parser)

private fun spaces() = zeroOrMore(pWhitespace())

private fun comma() = pChar(',') andl spaces()

private fun value() = jValue() andl spaces()

private fun jUnescapedChar(): Parser<Char> =
    satisfy("char") { it != '\\' && it != '\"' }

private fun jEscapedChar(): Parser<out Char> =
    choice(listOf(
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

private fun jUnicodeChar(): Parser<out Char> {
    val backslash = pChar('\\')
    val uChar = pChar('u')
    val hexDigit = pAnyOf(('0'..'9').toList() + ('A'..'F').toList() + ('a'..'f').toList())

    return backslash andr uChar andr hexDigit and hexDigit and hexDigit and hexDigit map {
        it.joinToString("").toInt(16).toChar()
    }
}

private fun quotedString(): Parser<String> {
    val quote = pChar('\"') label "quote"
    val jChar = jUnescapedChar() or jEscapedChar() or jUnicodeChar()

    return quote andr (zeroOrMore(jChar)) andl quote map { it.joinToString("") }
}
