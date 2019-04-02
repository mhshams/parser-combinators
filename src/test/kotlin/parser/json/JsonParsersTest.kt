package parser.json

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import parser.core.Failure
import parser.core.State
import parser.core.Success
import parser.core.UnexpectedToken

class JsonParsersTest {

    @Test
    fun `jNull Parser`() {

        assertThat(jNull().run(State("null")))
            .isEqualTo(Success(JNull, State(input = "null", col = 4, pos = 4)))

        assertThat(jNull().run(State("nulp")))
            .isEqualTo(Failure(UnexpectedToken(label = "null", char = 'p', line = 0, col = 3)))
    }

    @Test
    fun `jBool Parser`() {

        assertThat(jBool().run(State("true")))
            .isEqualTo(Success(JBool(true), State(input = "true", col = 4, pos = 4)))

        assertThat(jBool().run(State("false")))
            .isEqualTo(Success(JBool(false), State(input = "false", col = 5, pos = 5)))

        assertThat(jBool().run(State("truf")))
            .isEqualTo(Failure(UnexpectedToken(label = "bool", char = 't', line = 0, col = 0)))
    }

    @Test
    fun `jString Parser`() {
        assertThat(jString().run(State("\"ABC\"")))
            .isEqualTo(Success(JString("ABC"), State(input = "\"ABC\"", col = 5, pos = 5)))

        assertThat(jString().run(State("\"\"")))
            .isEqualTo(Success(JString(""), State(input = "\"\"", col = 2, pos = 2)))

        assertThat(jString().run(State("\"a\"")))
            .isEqualTo(Success(JString("a"), State(input = "\"a\"", col = 3, pos = 3)))

        assertThat(jString().run(State("\"ab\"")))
            .isEqualTo(Success(JString("ab"), State(input = "\"ab\"", col = 4, pos = 4)))

        assertThat(jString().run(State("\"ab\\tde\"")))
            .isEqualTo(Success(JString("ab\tde"), State(input = "\"ab\\tde\"", col = 8, pos = 8)))

        assertThat(jString().run(State("\"ab\\u263Ade\"")))
            .isEqualTo(Success(JString("ab☺de"), State(input = "\"ab\\u263Ade\"", col = 12, pos = 12)))

    }

    @Test
    fun `jString Parser with more cases`() {

        val backSlash = "\"\\\\\""
        assertThat(jString().run(State(backSlash)))
            .isEqualTo(Success(JString("\\"), State(input = backSlash, col = 4, pos = 4)))

        val tab = "\"\\t\""
        assertThat(jString().run(State(tab)))
            .isEqualTo(Success(JString("\t"), State(input = tab, col = 4, pos = 4)))

        val unicode = "\"\\u263A\""
        assertThat(jString().run(State(unicode)))
            .isEqualTo(Success(JString("\u263A"), State(input = unicode, col = 8, pos = 8)))
    }

    @Test
    fun `jNumber Parser`() {
        assertThat(jNumber().run(State("123")))
            .isEqualTo(Success(JNumber(123.0), State(input = "123", col = 3, pos = 3)))

        assertThat(jNumber().run(State("-123")))
            .isEqualTo(Success(JNumber(-123.0), State(input = "-123", col = 4, pos = 4)))

        assertThat(jNumber().run(State("123.4")))
            .isEqualTo(Success(JNumber(123.4), State(input = "123.4", col = 5, pos = 5)))
    }

    @Test
    fun `jArray Parser`() {
        assertThat(jArray().run(State("[1, 2 ]")))
            .isEqualTo(Success(JArray(listOf(JNumber(1), JNumber(2))), State(input = "[1, 2 ]", col = 7, pos = 7)))

        assertThat(jArray().run(State("[ true\t,     false   ]")))
            .isEqualTo(
                Success(
                    JArray(listOf(JBool(true), JBool(false))),
                    State(input = "[ true\t,     false   ]", col = 22, pos = 22)
                )
            )

        assertThat(jArray().run(State("[\"person\", null]")))
            .isEqualTo(
                Success(
                    JArray(listOf(JString("person"), JNull)),
                    State(input = "[\"person\", null]", col = 16, pos = 16)
                )
            )

        assertThat(jArray().run(State("[[1, 2]]")))
            .isEqualTo(
                Success(
                    JArray(listOf(JArray(listOf(JNumber(1), JNumber(2))))),
                    State(input = "[[1, 2]]", col = 8, pos = 8)
                )
            )

        assertThat(jArray().run(State("[1, 2, ]")))
            .isEqualTo(Failure(UnexpectedToken("array", ',', 0, 5)))

    }

    @Test
    fun `jObject Parser`() {
        """{"a": 2 }""".let { input ->
            assertThat(jObject().run(State(input)))
                .isEqualTo(
                    Success(
                        JObject(
                            mapOf(
                                "a" to JNumber(2)
                            )
                        ), State(input = input, col = 9, pos = 9)
                    )
                )
        }

        """{"a": 2, "foo": "bar" }""".let { input ->
            assertThat(jObject().run(State(input)))
                .isEqualTo(
                    Success(
                        JObject(
                            mapOf(
                                "a" to JNumber(2),
                                "foo" to JString("bar")
                            )
                        ), State(input = input, col = 23, pos = 23)
                    )
                )
        }
    }

    @Test
    fun `full json object`() {
        val json = """
            {
                "name" : "Scott",
                "isMale" : true,
                "bday" : {
                    "year":2001,
                    "month":12,
                    "day":25
                },
                "favouriteColors" : [
                    "blue",
                    "green"
                ]
            }""".trimIndent()

        val expectedObject = JObject(
            mapOf(
                "name" to JString("Scott"),
                "isMale" to JBool(true),
                "bday" to JObject(
                    mapOf(
                        "year" to JNumber(2001),
                        "month" to JNumber(12),
                        "day" to JNumber(25)
                    )
                ),
                "favouriteColors" to JArray(
                    listOf(
                        JString("blue"),
                        JString("green")
                    )
                )
            )
        )

        assertThat(jObject().run(State(json)))
            .isEqualTo(Success(expectedObject, State(json, 12, 1, 190)))
    }
}
