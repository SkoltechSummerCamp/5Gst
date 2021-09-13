package ru.scoltech.openran.speedtest.task

import java.lang.Exception
import java.lang.RuntimeException

class FatalException(message: String, cause: Exception? = null) : RuntimeException(message, cause)
