import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class FunctionalCombinatorTest {

    @Test
    fun `AndWithoutLeft Parser works as expected`() {
        val parser = andThenWithoutLeft(pChar('A'), pChar('2'))

        assertThat(run(parser, State("A2")))
            .isEqualTo(Success('2', State(input = "A2", pos = 2, col = 2)))
    }

    @Test
    fun `AndWithoutRight Parser works as expected`() {
        val parser = andThenWithoutRight(pChar('A'), pChar('2'))

        assertThat(run(parser, State("A2")))
            .isEqualTo(Success('A', State(input = "A2", pos = 2, col = 2)))
    }

    @Test
    fun `Between Parser works as expected`() {
        val parser = between(pChar('A'), pChar('B'), pChar('C'))

        assertThat(run(parser, State("ABC")))
            .isEqualTo(Success('B', State(input = "ABC", pos = 3, col = 3)))
    }

    @Test
    fun `Reduce and andThen Parser works as expected`() {
        val parsers = arrayOf(pChar('A'), pChar('B'), pChar('C'))

        assertThat(run(reduce(::andThen, *parsers), State("ABC")))
            .isEqualTo(Success(listOf('A', 'B', 'C'), State(input = "ABC", pos = 3, col = 3)))

        assertThat(run(reduce(::andThen, *parsers), State("ADC")))
            .isEqualTo(Failure<Any>("Expected B. Got D"))
    }

    @Test
    fun `Reduce and orElse Parser works as expected`() {
        val parsers = arrayOf(pChar('A'), pChar('B'), pChar('C'))

        assertThat(run(reduce(::orElse, *parsers), State("CDE")))
            .isEqualTo(Success('C', State(input = "CDE", pos = 1, col = 1)))

        assertThat(run(reduce(::orElse, *parsers), State("D")))
            .isEqualTo(Failure<Any>("Expected C. Got D"))
    }

    @Test
    fun `Lowercase Parser works as expected`() {
        val parser = pLowercase()

        assertThat(run(parser, State("aBC")))
            .isEqualTo(Success('a', State(input = "aBC", pos = 1, col = 1)))

        assertThat(run(parser, State("ABC")))
            .isEqualTo(Failure<Any>("Expected z. Got A"))
    }

    @Test
    fun `Digit Parser works as expected`() {
        val parser = pDigit()

        assertThat(run(parser, State("12")))
            .isEqualTo(Success('1', State(input = "12", pos = 1, col = 1)))

        assertThat(run(parser, State("false")))
            .isEqualTo(Failure<Any>("Expected 9. Got f"))
    }

    @Test
    fun `String Parser works as expected`() {
        val parser = pString("true")

        assertThat(run(parser, State("true")))
            .isEqualTo(Success("true", State(input = "true", pos = 4, col = 4)))

        assertThat(run(parser, State("false")))
            .isEqualTo(Failure<Any>("Expected t. Got f"))
    }

    @Test
    fun `OneOrMore Parser works as expected`() {
        val parser = oneOrMore(andThen(pChar('A'), pChar('B')))

        assertThat(run(parser, State("AB")))
            .isEqualTo(Success(listOf('A', 'B'), State(input = "AB", pos = 2, col = 2)))

        assertThat(run(parser, State("ABAB")))
            .isEqualTo(Success(listOf('A', 'B', 'A', 'B'), State(input = "ABAB", pos = 4, col = 4)))
    }

    @Test
    fun `ZeroOrOne Parser works as expected`() {
        val parser = zeroOrOne(andThen(pChar('A'), pChar('B')))

        assertThat(run(parser, State("")))
            .isEqualTo(Success(Unit, State(input = "", pos = 0, col = 0)))

        assertThat(run(parser, State("AB")))
            .isEqualTo(Success(listOf('A', 'B'), State(input = "AB", pos = 2, col = 2)))

        assertThat(run(parser, State("ABAB")))
            .isEqualTo(Success(listOf('A', 'B'), State(input = "ABAB", pos = 2, col = 2)))
    }

    @Test
    fun `Whitespace Parser works as expected`() {
        val parser = whitespace()

        assertThat(run(parser, State(" \t\n")))
            .isEqualTo(Success(listOf(' ', '\t', '\n'), State(input = " \t\n", pos = 3, col = 3)))
    }

    @Test
    fun `Int Parser works as expected`() {
        val parser = pInt()

        assertThat(run(parser, State("123A")))
            .isEqualTo(Success(123, State(input = "123A", pos = 3, col = 3)))
    }

    @Test
    fun `QuotedInt Parser works as expected`() {
        val parser = pQuotedInt()

        assertThat(run(parser, State("\"123\"")))
            .isEqualTo(Success(123, State(input = "\"123\"", pos = 5, col = 5)))
    }
}
