package parser.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class BaseCombinatorsTest {

    @Test
    fun `Label Combinator with label works as expected`() {

        assertThat((pChar('A') label "Labeled").run(State("ZBC")))
            .isEqualTo(Failure<Char>(UnexpectedToken(label = "Labeled", char = 'Z', line = 0, col = 0)))
    }

    @Test
    fun `Map Combinator works as expected`() {
        val parser = pChar('2') map { it.toInt() - 48 }

        assertThat(parser.run(State("23")))
            .isEqualTo(Success(2, State(input = "23", pos = 1, col = 1)))

        assertThat(parser.run(State("32")))
            .isEqualTo(Failure<Any>(UnexpectedToken(label = "2", char = '3', line = 0, col = 0)))
    }

    @Test
    fun `And Combinator works as expected`() {
        val parser = pChar('A') and pChar('2')

        assertThat(parser.run(State("A2")))
            .isEqualTo(Success(listOf('A', '2'), State(input = "A2", pos = 2, col = 2)))

        assertThat(parser.run(State("B2")))
            .isEqualTo(Failure<Any>(UnexpectedToken(label = "A and 2", char = 'B', line = 0, col = 0)))

        assertThat(parser.run(State("A3")))
            .isEqualTo(Failure<Any>(UnexpectedToken(label = "A and 2", char = '3', line = 0, col = 1)))
    }

    @Test
    fun `And Combinator works with more complex parsers`() {
        val parser = pChar('A') and pChar('2') and pChar('C')

        assertThat(parser.run(State("A2C")))
            .isEqualTo(Success(listOf('A', '2', 'C'), State(input = "A2C", pos = 3, col = 3)))
    }

    @Test
    fun `And Combinator works with other complex parsers`() {
        val parser = pChar('C') and pChar('A') and pChar('2')

        assertThat(parser.run(State("CA2")))
            .isEqualTo(Success(listOf('C', 'A', '2'), State(input = "CA2", pos = 3, col = 3)))
    }

    @Test
    fun `And Combinator works with even more complex parsers`() {
        val parser = pChar('A') and pChar('1') and pChar('B') and pChar('2')

        assertThat(parser.run(State("A1B2")))
            .isEqualTo(Success(listOf('A', '1', 'B', '2'), State(input = "A1B2", pos = 4, col = 4)))
    }

    @Test
    fun `Or Combinator works as expected`() {
        val parser = pChar('A') or pChar('2')

        assertThat(parser.run(State("A2")))
            .isEqualTo(Success('A', State(input = "A2", pos = 1, col = 1)))

        assertThat(parser.run(State("2A")))
            .isEqualTo(Success('2', State(input = "2A", pos = 1, col = 1)))

        assertThat(parser.run(State("B2")))
            .isEqualTo(Failure<Any>(UnexpectedToken(label = "A or 2", char = 'B', line = 0, col = 0)))
    }
}
