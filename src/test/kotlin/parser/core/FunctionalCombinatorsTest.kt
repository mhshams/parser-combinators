package parser.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class FunctionalCombinatorsTest {

    @Test
    fun `AndWithoutLeft Combinator works as expected`() {
        val parser = pChar('A') andr pChar('2')

        assertThat(parser.run(State("A2")))
            .isEqualTo(Success('2', State(input = "A2", pos = 2, col = 2)))
    }

    @Test
    fun `AndWithoutRight Combinator works as expected`() {
        val parser = pChar('A') andl pChar('2')

        assertThat(parser.run(State("A2")))
            .isEqualTo(Success('A', State(input = "A2", pos = 2, col = 2)))
    }

    @Test
    fun `Between Combinator works as expected`() {
        val parser = pBetween(pChar('A'), pChar('B'), pChar('C'))

        assertThat(parser.run(State("ABC")))
            .isEqualTo(Success('B', State(input = "ABC", pos = 3, col = 3)))
    }

    @Test
    fun `All Combinator works as expected`() {
        val parsers = listOf(pChar('A'), pChar('B'), pChar('C'))

        assertThat(all(parsers).run(State("ABC")))
            .isEqualTo(Success(listOf('A', 'B', 'C'), State(input = "ABC", pos = 3, col = 3)))

        assertThat(all(parsers).run(State("ADC")))
            .isEqualTo(Failure<Any>(UnexpectedToken(label = "all", char = 'D', line = 0, col = 1)))
    }

    @Test
    fun `Any Combinator works as expected`() {
        val parsers = listOf(pChar('A'), pChar('B'), pChar('C'))

        assertThat(any(parsers).run(State("CDE")))
            .isEqualTo(Success('C', State(input = "CDE", pos = 1, col = 1)))

        assertThat(any(parsers).run(State("D")))
            .isEqualTo(Failure<Any>(UnexpectedToken(label = "any", char = 'D', line = 0, col = 0)))
    }

    @Test
    fun `Lowercase Combinator works as expected`() {
        val parser = pLowercase()

        assertThat(parser.run(State("aBC")))
            .isEqualTo(Success('a', State(input = "aBC", pos = 1, col = 1)))

        assertThat(parser.run(State("ABC")))
            .isEqualTo(Failure<Any>(UnexpectedToken(label = "lowercase", char = 'A', line = 0, col = 0)))
    }

    @Test
    fun `Digit Combinator works as expected`() {
        val parser = pDigit()

        assertThat(parser.run(State("12")))
            .isEqualTo(Success('1', State(input = "12", pos = 1, col = 1)))

        assertThat(parser.run(State("false")))
            .isEqualTo(Failure<Any>(UnexpectedToken(label = "digit", char = 'f', line = 0, col = 0)))
    }

    @Test
    fun `String Combinator works as expected`() {
        val parser = pString("true")

        assertThat(parser.run(State("true")))
            .isEqualTo(Success("true", State(input = "true", pos = 4, col = 4)))

        assertThat(parser.run(State("false")))
            .isEqualTo(Failure<Any>(UnexpectedToken(label = "true", char = 'f', line = 0, col = 0)))
    }

    @Test
    fun `OneOrMore Combinator works as expected`() {
        val parser = oneOrMore(pChar('A') and pChar('B'))

        assertThat(parser.run(State("AB")))
            .isEqualTo(Success(listOf('A', 'B'), State(input = "AB", pos = 2, col = 2)))

        assertThat(parser.run(State("ABAB")))
            .isEqualTo(Success(listOf('A', 'B', 'A', 'B'), State(input = "ABAB", pos = 4, col = 4)))
    }

    @Test
    fun `Optional Combinator works as expected`() {
        val parser = optional(pChar('A') and pChar('B'))

        assertThat(parser.run(State("")))
            .isEqualTo(Success(Unit, State(input = "", pos = 0, col = 0)))

        assertThat(parser.run(State("AB")))
            .isEqualTo(Success(listOf('A', 'B'), State(input = "AB", pos = 2, col = 2)))

        assertThat(parser.run(State("ABAB")))
            .isEqualTo(Success(listOf('A', 'B'), State(input = "ABAB", pos = 2, col = 2)))
    }

    @Test
    fun `Whitespace Combinator works as expected`() {
        val parser = pWhitespace()

        assertThat(parser.run(State(" \t\n")))
            .isEqualTo(Success(listOf(' ', '\t', '\n'), State(input = " \t\n", line = 1, pos = 3)))

    }

    @Test
    fun `Newline parsing works as expected`() {
        val parser = pInt() and pChar('\n') and pDigits() and pChar('\n') and pString("AB")

        assertThat(parser.run(State("13\n4\nAB")))
            .isEqualTo(Success(listOf(13, '\n', '4', '\n', "AB"), State(input = "13\n4\nAB", line = 2, pos = 7, col = 2)))
    }

    @Test
    fun `Digits Combinator works as expected`() {
        val parser = pDigits()

        assertThat(parser.run(State("123A")))
            .isEqualTo(Success(listOf('1', '2', '3'), State(input = "123A", pos = 3, col = 3)))
    }
}
