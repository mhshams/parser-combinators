import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ParserTest {

    @Test
    fun `Char Parser works as expected`() {

        assertThat(run(pChar('A'), State("ZBC")))
            .isEqualTo(Failure<Char>("Expected A. Got Z"))

        assertThat(run(pChar('A'), State("ABC")))
            .isEqualTo(Success('A', State(input = "ABC", col = 1, pos = 1)))

        assertThat(run(pChar('B'), State(input = "ABC", col = 1, pos = 1)))
            .isEqualTo(Success('B', State(input = "ABC", col = 2, pos = 2)))
    }
}
