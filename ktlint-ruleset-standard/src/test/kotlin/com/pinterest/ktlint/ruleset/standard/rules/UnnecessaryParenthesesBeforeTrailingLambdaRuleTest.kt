package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import org.junit.jupiter.api.Test

class UnnecessaryParenthesesBeforeTrailingLambdaRuleTest {
    private val unnecessaryParenthesesBeforeTrailingLambdaRuleAssertThat =
        assertThatRule { UnnecessaryParenthesesBeforeTrailingLambdaRule() }

    @Test
    fun `Remove unnecessary parentheses in function call followed by lambda`() {
        val code =
            """
            fun countDash(input: String) =
                "some-string".count() { it == '-' }
            """.trimIndent()
        val formattedCode =
            """
            fun countDash(input: String) =
                "some-string".count { it == '-' }
            """.trimIndent()
        unnecessaryParenthesesBeforeTrailingLambdaRuleAssertThat(code)
            .hasLintViolation(2, 24, "Empty parentheses in function call followed by lambda are unnecessary")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 2884 - Given some a call expression ending with a lambda argument, followed by an empty argument list followed by another lambda argument then do not remove empty parameter list`() {
        val code =
            """
            fun fooBar(foo: () -> String): (() -> String) -> String = { bar -> foo().plus("  ").plus(bar()) }

            val foobar = fooBar { "Hello" }() { "world" }
            """.trimIndent()
        unnecessaryParenthesesBeforeTrailingLambdaRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 3016 - Given some nested call expressions, and outmost call expression does not have arguments, and ending with a lambda argument then do not remove empty parameter list`() {
        val code =
            """
            class Foo
            class Bar
            class Baz
            operator fun Foo.invoke() = Bar()
            operator fun Bar.invoke() = Baz()
            inline operator fun Baz.invoke(function: () -> Unit) {}

            fun main() {
                val foo = Foo()
                foo()()() {} // Do not remove empty parameter list
                val bar = Bar()
                bar()() {} // Do not remove empty parameter list
                val baz = Baz()
                baz() {} // Empty parameter list is to be removed
            }
            """.trimIndent()
        val formattedCode =
            """
            class Foo
            class Bar
            class Baz
            operator fun Foo.invoke() = Bar()
            operator fun Bar.invoke() = Baz()
            inline operator fun Baz.invoke(function: () -> Unit) {}

            fun main() {
                val foo = Foo()
                foo()()() {} // Do not remove empty parameter list
                val bar = Bar()
                bar()() {} // Do not remove empty parameter list
                val baz = Baz()
                baz {} // Empty parameter list is to be removed
            }
            """.trimIndent()
        unnecessaryParenthesesBeforeTrailingLambdaRuleAssertThat(code)
            .hasLintViolation(14, 8, "Empty parentheses in function call followed by lambda are unnecessary")
            .isFormattedAs(formattedCode)
    }
}
