package extension

import io.kotest.assertions.AssertionFailedError
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.network.chat.Component
import org.slf4j.Logger

fun runCatchingAssertion(logger: Logger, context: GameTestHelper, block: () -> Unit) {
    try {
        block()
    } catch (e: AssertionFailedError) {
        logger.error("Assertion failed", e)
        throw context.assertionException(Component.nullToEmpty(e.message ?: "Assertion Error"))
    } catch (e: AssertionError) {
        logger.error("Assertion failed", e)
        throw context.assertionException(Component.nullToEmpty(e.message ?: "Assertion Error"))
    } catch (e: Throwable) {
        logger.error("Failed to operate test", e)
        throw e
    }
}
