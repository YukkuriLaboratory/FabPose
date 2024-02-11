package extension

import net.minecraft.test.TestContext
import org.slf4j.Logger

fun TestContext.addInstantFinalTask(logger: Logger, block: () -> Unit) {
    addInstantFinalTask {
        runCatchingAssertion(logger, block)
    }
}
