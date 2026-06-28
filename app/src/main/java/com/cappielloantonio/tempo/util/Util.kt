package com.cappielloantonio.tempo.util

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Function
import java.util.function.Predicate

object Util {
    @JvmStatic
    fun <T> distinctByKey(keyExtractor: Function<in T, Any>): Predicate<T>? {
        return try {
            val uniqueMap = ConcurrentHashMap<Any, Boolean>()
            Predicate { t: T ->
                uniqueMap.putIfAbsent(keyExtractor.apply(t), true) == null
            }
        } catch (_: NullPointerException) {
            null
        }
    }

    @JvmStatic
    fun toPascalCase(name: String?): String? {
        if (name.isNullOrEmpty()) {
            return name
        }

        val pascalCase = StringBuilder()
        var toUpper = false
        val charArray = name.toCharArray()

        for (ctr in charArray.indices) {
            if (ctr == 0) {
                pascalCase.append(charArray[ctr].uppercaseChar())
                continue
            }

            if (charArray[ctr] == '_') {
                toUpper = true
                continue
            }

            if (toUpper) {
                pascalCase.append(charArray[ctr].uppercaseChar())
                toUpper = false
                continue
            }

            pascalCase.append(charArray[ctr])
        }

        return pascalCase.toString()
    }

    @JvmStatic
    fun encode(value: String): String {
        return try {
            URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
        } catch (_: Exception) {
            value
        }
    }
}
