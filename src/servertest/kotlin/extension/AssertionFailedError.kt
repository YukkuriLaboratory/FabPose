package extension

import io.kotest.assertions.AssertionFailedError
import net.minecraft.test.GameTestException
import org.slf4j.Logger

fun runCatchingAssertion(logger: Logger, block: () -> Unit) {
    try {
        block()
    } catch (e: AssertionFailedError) {
        logger.error("Assertion failed", e)
        throw GameTestException("Assertion Error")
    } catch (e: AssertionError) {
        logger.error("Assertion failed", e)
        throw GameTestException("Assertion Error")
    }
}
