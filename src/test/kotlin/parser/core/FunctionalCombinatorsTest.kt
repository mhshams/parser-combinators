package parser.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.*

class FunctionalCombinatorsTest {

    @Test
    fun `Between Combinator`() {
        val parser = pBetween(pChar('A'), pChar('B'), pChar('C'))

        assertThat(parser.run(State("ABC")))
            .isEqualTo(Success('B', State(input = "ABC", pos = 3, col = 3)))
    }

    @Test
    fun `Sequence Combinator`() {
        val parser = sequence(pChar('A'), pChar('B'), pChar('C'))

        assertThat(parser.run(State("ABC")))
            .isEqualTo(Success(listOf('A', 'B', 'C'), State(input = "ABC", pos = 3, col = 3)))

        assertThat(sequence(parser).run(State("ADC")))
            .isEqualTo(Failure(UnexpectedToken(label = "sequence", char = 'D', line = 0, col = 1)))
    }

    @Test
    fun `Choice Combinator`() {
        val parsers = listOf(pChar('A'), pChar('B'), pChar('C'))

        assertThat(choice(parsers).run(State("CDE")))
            .isEqualTo(Success('C', State(input = "CDE", pos = 1, col = 1)))

        assertThat(choice(parsers).run(State("D")))
            .isEqualTo(Failure(UnexpectedToken(label = "choice", char = 'D', line = 0, col = 0)))
    }

    @Test
    fun `Lowercase Combinator`() {
        val parser = pLowercase()

        assertThat(parser.run(State("aBC")))
            .isEqualTo(Success('a', State(input = "aBC", pos = 1, col = 1)))

        assertThat(parser.run(State("ABC")))
            .isEqualTo(Failure(UnexpectedToken(label = "lowercase", char = 'A', line = 0, col = 0)))
    }

    @Test
    fun `Digit Combinator`() {
        val parser = pDigit()

        assertThat(parser.run(State("12")))
            .isEqualTo(Success('1', State(input = "12", pos = 1, col = 1)))

        assertThat(parser.run(State("false")))
            .isEqualTo(Failure(UnexpectedToken(label = "digit", char = 'f', line = 0, col = 0)))
    }

    @Test
    fun `String Combinator`() {
        val parser = pString("true")

        assertThat(parser.run(State("true")))
            .isEqualTo(Success("true", State(input = "true", pos = 4, col = 4)))

        assertThat(parser.run(State("false")))
            .isEqualTo(Failure(UnexpectedToken(label = "true", char = 'f', line = 0, col = 0)))
    }

    @Test
    fun `OneOrMore Combinator`() {
        val parser = oneOrMore(pChar('A') and pChar('B'))

        assertThat(parser.run(State("AB")))
            .isEqualTo(Success(listOf('A', 'B'), State(input = "AB", pos = 2, col = 2)))

        assertThat(parser.run(State("ABAB")))
            .isEqualTo(Success(listOf('A', 'B', 'A', 'B'), State(input = "ABAB", pos = 4, col = 4)))
    }

    @Test
    fun `Optional Combinator`() {
        val parser = optional(pChar('A') and pChar('B'))

        assertThat(parser.run(State("")))
            .isEqualTo(Success(Maybe.none<Char>(), State(input = "", pos = 0, col = 0)))

        assertThat(parser.run(State("AB")))
            .isEqualTo(Success(Maybe.just(listOf('A', 'B')), State(input = "AB", pos = 2, col = 2)))

        assertThat(parser.run(State("ABAB")))
            .isEqualTo(Success(Maybe.just(listOf('A', 'B')), State(input = "ABAB", pos = 2, col = 2)))
    }

    @Test
    fun `Whitespace Combinator`() {
        val parser = pWhitespace()

        assertThat(parser.run(State(" \t\n")))
            .isEqualTo(Success(listOf(' ', '\t', '\n'), State(input = " \t\n", line = 1, pos = 3)))

    }

    @Test
    fun `Digits Combinator`() {
        val parser = pDigits()

        assertThat(parser.run(State("123A")))
            .isEqualTo(Success(listOf('1', '2', '3'), State(input = "123A", pos = 3, col = 3)))
    }
}
