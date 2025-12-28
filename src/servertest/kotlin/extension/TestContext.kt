package extension

import net.minecraft.gametest.framework.GameTestHelper
import org.slf4j.Logger

fun GameTestHelper.addInstantFinalTask(logger: Logger, block: () -> Unit) {
    succeedWhen {
        runCatchingAssertion(logger, this, block)
    }
}

fun GameTestHelper.waitAndRun(ticks: Long, logger: Logger, block: () -> Unit) {
    runAfterDelay(ticks) {
        runCatchingAssertion(logger, this, block)
    }
}
