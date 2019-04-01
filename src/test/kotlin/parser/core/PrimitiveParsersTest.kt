package parser.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PrimitiveParsersTest {

    @Test
    fun `Char Parser`() {

        assertThat(pChar('A').run(State("ABC")))
            .isEqualTo(Success('A', State(input = "ABC", col = 1, pos = 1)))

        assertThat(pChar('A').run(State("ZBC")))
            .isEqualTo(Failure(UnexpectedToken(label = "A", char = 'Z', line = 0, col = 0)))
    }

    @Test
    fun `String Parser`() {

        assertThat(pString("Foo").run(State("Foo")))
            .isEqualTo(Success("Foo", State(input = "Foo", col = 3, pos = 3)))

        assertThat(pString("Foo").run(State("FoD")))
            .isEqualTo(Failure(UnexpectedToken(label = "Foo", char = 'D', line = 0, col = 2)))
    }

    @Test
    fun `Boolean Parser`() {

        assertThat(pBoolean().run(State("true")))
            .isEqualTo(Success(true, State(input = "true", col = 4, pos = 4)))

        assertThat(pBoolean().run(State("false")))
            .isEqualTo(Success(false, State(input = "false", col = 5, pos = 5)))

        assertThat(pBoolean().run(State("falsh")))
            .isEqualTo(Failure(UnexpectedToken(label = "boolean", char = 'h', line = 0, col = 4)))
    }

    @Test
    fun `Int Parser`() {

        assertThat(pInt().run(State("123")))
            .isEqualTo(Success(123, State(input = "123", col = 3, pos = 3)))

        assertThat(pInt().run(State("123A")))
            .isEqualTo(Success(123, State(input = "123A", col = 3, pos = 3)))

        assertThat(pInt().run(State("+0")))
            .isEqualTo(Success(0, State(input = "+0", col = 2, pos = 2)))

        assertThat(pInt().run(State("-0")))
            .isEqualTo(Success(0, State(input = "-0", col = 2, pos = 2)))

        assertThat(pInt().run(State("-1")))
            .isEqualTo(Success(-1, State(input = "-1", col = 2, pos = 2)))

        assertThat(pInt().run(State("+A")))
            .isEqualTo(Failure(UnexpectedToken(label = "int", char = 'A', line = 0, col = 1)))
    }

    @Test
    fun `Number Parser`() {

//        assertThat(pNumber().run(State("12.34")))
//            .isEqualTo(Success(12.34, State(input = "12.34", col = 5, pos = 5)))
//
//        assertThat(pNumber().run(State("0.34")))
//            .isEqualTo(Success(0.34, State(input = "0.34", col = 4, pos = 4)))

        assertThat(pNumber().run(State("-0.34")))
            .isEqualTo(Success(-0.34, State(input = "-0.34", col = 5, pos = 5)))
//
//        assertThat(pNumber().run(State("-12.34")))
//            .isEqualTo(Success(-12.34, State(input = "-12.34", col = 6, pos = 6)))
//
//        assertThat(pNumber().run(State("12.34E5")))
//            .isEqualTo(Success(1234000.0, State(input = "12.34E5", col = 7, pos = 7)))
//
//        assertThat(pNumber().run(State("-12.34e5")))
//            .isEqualTo(Success(-1234000.0, State(input = "-12.34e5", col = 8, pos = 8)))
//
//        assertThat(pNumber().run(State("-12.34e-7AB")))
//            .isEqualTo(Success(-1.234e-6, State(input = "-12.34e-7AB", col = 9, pos = 9)))
//
//        assertThat(pNumber().run(State("+A")))
//            .isEqualTo(Failure(UnexpectedToken(label = "number", char = 'A', line = 0, col = 1)))
    }
}
