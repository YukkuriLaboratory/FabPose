package runner

import io.kotest.matchers.nulls.shouldNotBeNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.AccessibilityOnboardingScreen
import net.minecraft.client.gui.screen.GameMenuScreen
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.TitleScreen
import net.minecraft.client.gui.screen.world.CreateWorldScreen
import net.minecraft.client.gui.screen.world.LevelLoadingScreen
import net.minecraft.client.gui.screen.world.SelectWorldScreen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.CyclingButtonWidget
import net.minecraft.client.gui.widget.GridWidget
import net.minecraft.client.gui.widget.PressableWidget
import net.minecraft.client.option.Perspective
import net.minecraft.screen.ScreenTexts
import net.minecraft.text.Text
import net.minecraft.util.crash.CrashReport
import net.yukulab.fabpose.DelegatedLogger
import net.yukulab.fabpose.extension.accessor
import net.yukulab.fabpose.extension.loop
import org.spongepowered.asm.mixin.MixinEnvironment
import test.keybind.TestPoseKeybindings

class ClientTest : ClientModInitializer {
    override fun onInitializeClient() {
        val tests = listOf(
            TestPoseKeybindings::testSitKey,
            TestPoseKeybindings::testLayKey,
            TestPoseKeybindings::testSpinKey,
            TestPoseKeybindings::testSwimKey,
        )

        // Related: https://github.com/FabricMC/fabric/pull/2678
        CoroutineScope(Dispatchers.Default).launch {
            waitForLoadingComplete()

            if (MinecraftClient.getInstance().options.onboardAccessibility) {
                waitForScreen(AccessibilityOnboardingScreen::class.java)
                clickScreenButton(ScreenTexts.CONTINUE)
            }

            waitForScreen(TitleScreen::class.java)
            clickScreenButton("menu.singleplayer")

            if (FabricLoader.getInstance().gameDir.resolve("saves").toFile().listFiles()?.isNotEmpty() == true) {
                waitForScreen(SelectWorldScreen::class.java)
                clickScreenButton("selectWorld.create")
            }

            waitForScreen(CreateWorldScreen::class.java)
            // GameMode: creative
            clickScreenButton("selectWorld.gameMode")
            clickScreenButton("selectWorld.gameMode")

            // Difficulty: Peaceful
            clickScreenButton("options.difficulty")
            clickScreenButton("options.difficulty")

            waitFor("Click World Tab") {
                val createWorldScreen = it.currentScreen as CreateWorldScreen
                val tabNavigation = createWorldScreen.accessor.tabNavigation
                tabNavigation.selectTab(1, false)
                val targetTabText = Text.translatable("createWorld.tab.world.title")
                val tabManager = tabNavigation.accessor.tabManager
                tabManager.currentTab?.title?.string == targetTabText.string
            }
            // WorldType: Superflat
            clickScreenButton("selectWorld.mapType")

            clickScreenButton("selectWorld.create")

            // waitForScreen(ConfirmScreen::class.java)
            // clickScreenButton("gui.yes")

            MixinEnvironment.getCurrentEnvironment().audit()

            withContext(clientDispatcher) {
                MinecraftClient.getInstance().options.perspective = Perspective.THIRD_PERSON_BACK
            }
            waitForWorldTicks(100)
            val result = tests.map {
                runCatching {
                    it(MinecraftClient.getInstance())
                }.onFailure {
                    logger.error("Failed to execute client test", it)
                }
            }
            val success = result.count { it.isSuccess }
            val failure = result.size - success
            logger.info("======== Client Test =========")
            logger.info("Success: $success, Failure: $failure")
            logger.info("==============================")

            openGameMenu()
            clickScreenButton("menu.returnToMenu")

            waitForScreen(TitleScreen::class.java)
            if (failure != 0) {
                withContext(clientDispatcher) {
                    val crashReport =
                        CrashReport.create(RuntimeException("$failure Tests failed"), "$failure Tests failed")
                    MinecraftClient.getInstance().setCrashReportSupplier(crashReport)
                }
            }
            clickScreenButton("menu.quit")
        }
    }

    companion object {
        private val logger by DelegatedLogger()

        private suspend fun openGameMenu() {
            setScreen { GameMenuScreen(true) }
            waitForScreen(GameMenuScreen::class.java)
        }

        private suspend fun closeScreen() {
            setScreen { null }
        }

        private suspend fun setScreen(screen: (MinecraftClient) -> Screen?) = withContext(clientDispatcher) {
            val client = MinecraftClient.getInstance()
            client.setScreen(screen(client))
        }

        private suspend fun waitForLoadingComplete() {
            waitFor("Loading to complete", 5.minutes) {
                it.overlay == null
            }
        }

        private suspend fun waitForScreen(screen: Class<out Screen>) {
            waitFor("Screen ${screen.name}") {
                it.currentScreen?.javaClass == screen
            }
        }

        private suspend fun clickScreenButton(translationKey: String) {
            clickScreenButton(Text.translatable(translationKey))
        }

        private suspend fun clickScreenButton(translationKey: Text) {
            val buttonText = translationKey.string

            waitFor("Click button $buttonText") { client ->
                val screen = client.currentScreen ?: return@waitFor false

                screen.accessor.drawables.forEach { drawable ->
                    if (drawable is PressableWidget && pressMatchingButton(drawable, buttonText)) {
                        return@waitFor true
                    }

                    if (drawable is GridWidget) {
                        var result = false
                        drawable.forEachChild {
                            if (it is PressableWidget && pressMatchingButton(it, buttonText)) {
                                result = true
                            }
                        }
                        if (result) {
                            return@waitFor true
                        }
                    }
                }

                return@waitFor false
            }
        }

        private fun pressMatchingButton(widget: PressableWidget, text: String): Boolean {
            if (widget is ButtonWidget) {
                if (text == widget.message.string) {
                    widget.onPress()
                    return true
                }
            }
            if (widget is CyclingButtonWidget<*>) {
                if (text == widget.accessor.optionText.string) {
                    widget.onPress()
                    return true
                }
            }

            return false
        }

        private suspend fun waitForWorldTicks(ticks: Long) {
            waitFor("World load", 30.minutes) {
                it.world != null && it.currentScreen !is LevelLoadingScreen
            }
            val startTicks = withContext(clientDispatcher) {
                MinecraftClient.getInstance().world?.time.shouldNotBeNull()
            }
            waitFor("World load", 10.minutes) {
                it.world.shouldNotBeNull().time > startTicks + ticks
            }
        }

        private suspend fun waitFor(target: String, timeout: Duration = 10.seconds, block: suspend (MinecraftClient) -> Boolean) {
            withContext(clientDispatcher) {
                val client = MinecraftClient.getInstance()
                try {
                    withTimeout(timeout) {
                        loop(1.seconds) {
                            !block(client)
                        }
                    }
                } catch (e: Exception) {
                    client.setCrashReportSupplier(CrashReport.create(e, "Error occurred on waiting for $target"))
                }
            }
        }
    }
}
