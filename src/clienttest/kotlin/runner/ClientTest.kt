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
import net.minecraft.CrashReport
import net.minecraft.client.CameraType
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.AbstractButton
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.CycleButton
import net.minecraft.client.gui.layouts.GridLayout
import net.minecraft.client.gui.layouts.LayoutElement
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen
import net.minecraft.client.gui.screens.LevelLoadingScreen
import net.minecraft.client.gui.screens.PauseScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.input.MouseButtonInfo
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
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

            if (Minecraft.getInstance().options.onboardAccessibility) {
                waitForScreen(AccessibilityOnboardingScreen::class.java)
                clickScreenButton(CommonComponents.GUI_CONTINUE)
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
                val createWorldScreen = it.screen as CreateWorldScreen
                val tabNavigation = createWorldScreen.accessor.tabNavigationBar
                tabNavigation.selectTab(1, false)
                val targetTabText = Component.translatable("createWorld.tab.world.title")
                val tabManager = tabNavigation.accessor.tabManager
                tabManager.currentTab?.tabTitle?.string == targetTabText.string
            }
            // WorldType: Superflat
            clickScreenButton("selectWorld.mapType")

            clickScreenButton("selectWorld.create")

            // waitForScreen(ConfirmScreen::class.java)
            // clickScreenButton("gui.yes")

            MixinEnvironment.getCurrentEnvironment().audit()

            withContext(clientDispatcher) {
                Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_BACK)
            }
            waitForWorldTicks(100)
            val result = tests.map {
                runCatching {
                    it(Minecraft.getInstance())
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
                        CrashReport.forThrowable(RuntimeException("$failure Tests failed"), "$failure Tests failed")
                    Minecraft.getInstance().delayCrashRaw(crashReport)
                }
            }
            clickScreenButton("menu.quit")
        }
    }

    companion object {
        private val logger by DelegatedLogger()

        private suspend fun openGameMenu() {
            setScreen { PauseScreen(true) }
            waitForScreen(PauseScreen::class.java)
        }

        private suspend fun closeScreen() {
            setScreen { null }
        }

        private suspend fun setScreen(screen: (Minecraft) -> Screen?) = withContext(clientDispatcher) {
            val client = Minecraft.getInstance()
            client.setScreen(screen(client))
        }

        private suspend fun waitForLoadingComplete() {
            waitFor("Loading to complete", 5.minutes) {
                it.overlay == null
            }
        }

        private suspend fun waitForScreen(screen: Class<out Screen>) {
            waitFor("Screen ${screen.name}") {
                it.screen?.javaClass == screen
            }
        }

        private suspend fun clickScreenButton(translationKey: String) {
            clickScreenButton(Component.translatable(translationKey))
        }

        private suspend fun clickScreenButton(translationKey: Component) {
            val buttonText = translationKey.string

            waitFor("Click button $buttonText") { client ->
                val screen = client.screen ?: return@waitFor false

                screen.accessor.renderables.forEach { drawable ->
                    if (drawable is AbstractButton && pressMatchingButton(drawable, buttonText)) {
                        return@waitFor true
                    }

                    if (drawable is GridLayout) {
                        var result = false
                        drawable.visitWidgets {
                            if (it is AbstractButton && pressMatchingButton(it, buttonText)) {
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

        @Suppress("ktlint:standard:function-naming")
        private fun Click(widget: LayoutElement) = MouseButtonEvent(widget.x.toDouble(), widget.y.toDouble(), MouseButtonInfo(1, 0))

        private fun pressMatchingButton(widget: AbstractButton, text: String): Boolean {
            if (widget is Button) {
                if (text == widget.message.string) {
                    widget.onPress(Click(widget))
                    return true
                }
            }
            if (widget is CycleButton<*>) {
                if (text == widget.accessor.name.string) {
                    widget.onPress(Click(widget))
                    return true
                }
            }

            return false
        }

        private suspend fun waitForWorldTicks(ticks: Long) {
            waitFor("World load", 30.minutes) {
                it.level != null && it.screen !is LevelLoadingScreen
            }
            val startTicks = withContext(clientDispatcher) {
                Minecraft.getInstance().level?.gameTime.shouldNotBeNull()
            }
            waitFor("World load", 10.minutes) {
                it.level.shouldNotBeNull().gameTime > startTicks + ticks
            }
        }

        private suspend fun waitFor(target: String, timeout: Duration = 10.seconds, block: suspend (Minecraft) -> Boolean) {
            withContext(clientDispatcher) {
                val client = Minecraft.getInstance()
                try {
                    withTimeout(timeout) {
                        loop(1.seconds) {
                            !block(client)
                        }
                    }
                } catch (e: Exception) {
                    client.delayCrashRaw(CrashReport.forThrowable(e, "Error occurred on waiting for $target"))
                }
            }
        }
    }
}
