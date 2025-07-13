package extension

import io.kotest.assertions.AssertionFailedError
import net.minecraft.test.TestContext
import net.minecraft.text.Text
import org.slf4j.Logger

fun runCatchingAssertion(logger: Logger, context: TestContext, block: () -> Unit) {
    try {
        block()
    } catch (e: AssertionFailedError) {
        logger.error("Assertion failed", e)
        throw context.createError(Text.of(e.message ?: "Assertion Error"))
    } catch (e: AssertionError) {
        logger.error("Assertion failed", e)
        throw context.createError(Text.of(e.message ?: "Assertion Error"))
    }
}
