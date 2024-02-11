package net.yukulab.fabsit

import kotlin.reflect.KProperty
import net.fill1890.fabsit.FabSit
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DelegatedLogger {
    private var logger: Logger? = null
    operator fun getValue(thisRef: Any, property: KProperty<*>): Logger =
        logger ?: run {
            val className = thisRef::class.java.name.removeSuffix("\$Companion")
            LoggerFactory.getLogger("${FabSit.MOD_ID}:$className").also { logger = it }
        }
}
