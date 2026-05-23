package net.yukulab.fabpose

import kotlin.reflect.KProperty
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DelegatedLogger {
    private var logger: Logger? = null
    operator fun getValue(thisRef: Any, property: KProperty<*>): Logger = logger ?: run {
        val className = thisRef::class.java.name.removeSuffix("\$Companion")
        LoggerFactory.getLogger("$MOD_ID:$className").also { logger = it }
    }
}
