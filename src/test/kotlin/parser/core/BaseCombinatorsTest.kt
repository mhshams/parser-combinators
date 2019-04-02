package parser.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class BaseCombinatorsTest {

    @Test
    fun `And Combinator`() {
        val parser = pChar('A') and pChar('2')

        assertThat(parser.run(State("A2")))
            .isEqualTo(Success(listOf('A', '2'), State(input = "A2", pos = 2, col = 2)))

        assertThat(parser.run(State("B2")))
            .isEqualTo(Failure(UnexpectedToken(label = "A and 2", char = 'B', line = 0, col = 0)))

        assertThat(parser.run(State("A3")))
            .isEqualTo(Failure(UnexpectedToken(label = "A and 2", char = '3', line = 0, col = 1)))
    }

    @Test
    fun `Multiple And Combinator`() {
        val parser = pChar('A') and pChar('2') and pChar('C')

        assertThat(parser.run(State("A2C")))
            .isEqualTo(Success(listOf('A', '2', 'C'), State(input = "A2C", pos = 3, col = 3)))
    }

    @Test
    fun `Another example of Multiple And Combinator`() {
        val parser = pChar('C') and pChar('A') and pChar('2')

        assertThat(parser.run(State("CA2")))
            .isEqualTo(Success(listOf('C', 'A', '2'), State(input = "CA2", pos = 3, col = 3)))
    }

    @Test
    fun `Even more example of Multiple And Combinator`() {
        val parser = pChar('A') and pChar('1') and pChar('B') and pChar('2')

        assertThat(parser.run(State("A1B2")))
            .isEqualTo(Success(listOf('A', '1', 'B', '2'), State(input = "A1B2", pos = 4, col = 4)))
    }

    @Test
    fun `Or Combinator`() {
        val parser = pChar('A') or pChar('2')

        assertThat(parser.run(State("A2")))
            .isEqualTo(Success('A', State(input = "A2", pos = 1, col = 1)))

        assertThat(parser.run(State("2A")))
            .isEqualTo(Success('2', State(input = "2A", pos = 1, col = 1)))

        assertThat(parser.run(State("B2")))
            .isEqualTo(Failure(UnexpectedToken(label = "A or 2", char = 'B', line = 0, col = 0)))
    }

    @Test
    fun `AndWithoutLeft Combinator`() {
        val parser = pChar('A') andr pChar('2')

        assertThat(parser.run(State("A2")))
            .isEqualTo(Success('2', State(input = "A2", pos = 2, col = 2)))
    }

    @Test
    fun `AndWithoutRight Combinator`() {
        val parser = pChar('A') andl pChar('2')

        assertThat(parser.run(State("A2")))
            .isEqualTo(Success('A', State(input = "A2", pos = 2, col = 2)))
    }
}
