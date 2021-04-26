package co.ingrow.android.kotlin

import java.util.logging.Level
import java.util.logging.Logger
import java.util.logging.SimpleFormatter
import java.util.logging.StreamHandler

object InGrowLogging {
    private val LOGGER: Logger = Logger.getLogger(InGrowLogging::class.java.name)
    private val HANDLER: StreamHandler = StreamHandler(System.out, SimpleFormatter())
    fun log(msg: String?) {
        if (isLoggingEnabled) {
            LOGGER.log(Level.FINER, msg)
            HANDLER.flush()
        }
    }

    fun log(msg: String?, throwable: Throwable?) {
        if (isLoggingEnabled) {
            LOGGER.log(Level.FINER, msg, throwable)
            HANDLER.flush()
        }
    }

    fun enableLogging() {
        setLogLevel(Level.FINER)
    }

    fun disableLogging() {
        setLogLevel(Level.OFF)
    }

    private val isLoggingEnabled: Boolean
        get() = LOGGER.level === Level.FINER

    private fun setLogLevel(newLevel: Level) {
        LOGGER.level = newLevel
        for (handler in LOGGER.handlers) {
            try {
                handler.level = newLevel
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }

    init {
        LOGGER.addHandler(HANDLER)
        disableLogging()
    }
}