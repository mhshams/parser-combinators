package parser.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ParserTest {

    @Test
    fun `Char Parser`() {
        assertThat((pChar('A').run(State("A"))))
            .isEqualTo(Success('A', State(input = "A", col = 1, pos = 1)))

        assertThat((pChar('A').run(State("Z"))))
            .isEqualTo(Failure(UnexpectedToken(label = "A", char = 'Z', line = 0, col = 0)))
    }

    @Test
    fun `Char Parser at given position`() {
        assertThat((pChar('A').run(State("BA", pos = 1, col = 1))))
            .isEqualTo(Success('A', State(input = "BA", col = 2, pos = 2)))

        assertThat((pChar('A').run(State("AB", pos = 1, col = 1))))
            .isEqualTo(Failure(UnexpectedToken(label = "A", char = 'B', line = 0, col = 1)))
    }

    @Test
    fun `Parser with Label`() {
        assertThat((pChar('A') label "Labeled").run(State("ZBC")))
            .isEqualTo(Failure(UnexpectedToken(label = "Labeled", char = 'Z', line = 0, col = 0)))
    }

    @Test
    fun `Parser with a transformer (map function)`() {
        val parser = pChar('a')

        assertThat((parser map { it.toUpperCase() }).run(State("ab")))
            .isEqualTo(Success('A', State(input = "ab", pos = 1, col = 1)))

        assertThat((parser map { it.toInt() }).run(State("ab")))
            .isEqualTo(Success(97, State(input = "ab", pos = 1, col = 1)))

        assertThat((parser map { it.toInt() }).run(State("2")))
            .isEqualTo(Failure(UnexpectedToken(label = "a", char = '2', line = 0, col = 0)))
    }

    @Test
    fun `Parser with a fixed transformer (map value)`() {

        assertThat((pChar('2') map (4)).run(State("23")))
            .isEqualTo(Success(4, State(input = "23", pos = 1, col = 1)))

        assertThat((pChar('T') map (true)).run(State("T")))
            .isEqualTo(Success(true, State(input = "T", pos = 1, col = 1)))

        assertThat((pChar('F') map (false)).run(State("T")))
            .isEqualTo(Failure(UnexpectedToken(label = "F", char = 'T', line = 0, col = 0)))
    }

    @Test
    fun `Newline parsing`() {
        val parser = pInt() and pChar('\n') and pDigits() and pChar('\n') and pString("AB")

        assertThat(parser.run(State("13\n4\nAB")))
            .isEqualTo(Success(listOf(13, '\n', '4', '\n', "AB"), State(input = "13\n4\nAB", line = 2, pos = 7, col = 2)))
    }
}
