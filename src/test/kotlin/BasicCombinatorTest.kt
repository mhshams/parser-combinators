import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class BasicCombinatorTest {

    @Test
    fun `Map Parser works as expected`() {
        val parser = map(pChar('2'), { it.toInt() - 48 })

        assertThat(run(parser, State("23")))
            .isEqualTo(Success(2, State(input = "23", pos = 1, col = 1)))

        assertThat(run(parser, State("32")))
            .isEqualTo(Failure<Any>("Expected 2. Got 3"))
    }

    @Test
    fun `And Parser works as expected`() {
        val parser = andThen(pChar('A'), pChar('2'))

        assertThat(run(parser, State("A2")))
            .isEqualTo(Success(listOf('A', '2'), State(input = "A2", pos = 2, col = 2)))

        assertThat(run(parser, State("B2")))
            .isEqualTo(Failure<Any>("Expected A. Got B"))

        assertThat(run(parser, State("A3")))
            .isEqualTo(Failure<Any>("Expected 2. Got 3"))
    }

    @Test
    fun `And Parser works with more complex parsers`() {
        val parser = andThen(andThen(pChar('A'), pChar('2')), pChar('C'))

        assertThat(run(parser, State("A2C")))
            .isEqualTo(Success(listOf('A', '2', 'C'), State(input = "A2C", pos = 3, col = 3)))
    }

    @Test
    fun `And Parser works with other complex parsers`() {
        val parser = andThen(pChar('C'), andThen(pChar('A'), pChar('2')))

        assertThat(run(parser, State("CA2")))
            .isEqualTo(Success(listOf('C', 'A', '2'), State(input = "CA2", pos = 3, col = 3)))
    }

    @Test
    fun `And Parser works with even more complex parsers`() {
        val parser = andThen(andThen(pChar('A'), pChar('1')), andThen(pChar('B'), pChar('2')))

        assertThat(run(parser, State("A1B2")))
            .isEqualTo(Success(listOf('A', '1', 'B', '2'), State(input = "A1B2", pos = 4, col = 4)))
    }

    @Test
    fun `Or Parser works as expected`() {
        val parser = orElse(pChar('A'), pChar('2'))

        assertThat(run(parser, State("A2")))
            .isEqualTo(Success('A', State(input = "A2", pos = 1, col = 1)))

        assertThat(run(parser, State("2A")))
            .isEqualTo(Success('2', State(input = "2A", pos = 1, col = 1)))

        assertThat(run(parser, State("B2")))
            .isEqualTo(Failure<Any>("Expected 2. Got B"))
    }
}
