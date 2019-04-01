package parser.json

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import parser.core.State
import parser.core.Success

class JsonTest {

    @Test
    fun `null input`() {
        "null" shouldBe JNull
    }

    @Test
    fun `boolean input`() {
        "false" shouldBe JBool(false)
    }

    @Test
    fun `string input`() {
        """"ABC"""" shouldBe JString("ABC")
    }

    @Test
    fun `number input`() {
        "123" shouldBe JNumber(123.0)
    }

    @Test
    fun `array input`() {
        "[  1 , 2 ]" shouldBe JArray(listOf(JNumber(1), JNumber(2)))
    }

    @Test
    fun `object input`() {
        """{"a": 2 }""" shouldBe JObject(mapOf("a" to JNumber(2)))
    }

    @Test
    fun `full json object`() {
        val input = """
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

        val output = JObject(
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

        assertThat(json().run(State(input)))
            .isEqualTo(Success(output, State(input = input, line = 12, col = 1, pos = input.length)))
    }

    private infix fun String.shouldBe(value: JValue) {
        assertThat(json().run(State(this)))
            .isEqualTo(Success(value, State(input = this, col = length, pos = length)))
    }
}
