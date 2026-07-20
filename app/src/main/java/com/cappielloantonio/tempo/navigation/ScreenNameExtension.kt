package com.cappielloantonio.tempo.navigation

import com.cappielloantonio.tempo.navigation.DefaultScreenNameExtension.defaultScreenName

object ScreenNameExtension {
    lateinit var allScreens: List<Screen>

    /**
     * Return screen name from screen list of application
     * @return The string path of default screen name
     */
    @Suppress("UnsafeCallOnNullableType")
    inline fun <reified S : Screen> getScreenName(): String {
        return allScreens.find { it is S }!!.screenName
    }

    /**
     * Return navigation path for screen with required parameters
     *
     * @param params The navigation arguments for screen path
     * @return The string path of default screen name with required parameters of navigation
     */
    fun Screen.screenNameWithParams(vararg params: Any): String {
        return defaultScreenName() + params.joinToString(separator = "/", prefix = "/")
    }

    /**
     * Return navigation path for screen with required and optional parameters
     *
     * @param params The navigation arguments for screen path
     * @param optionalParams The optional arguments that will be used for screen path
     * @return The string path of default screen name with required parameters of navigation,
     * path can contains optional parameters
     */
    fun Screen.screenNameWithOptionalParams(
        params: List<Any>,
        optionalParams: List<Pair<String, Any?>>
    ): String {
        val mandatoryPart: String = if (params.isNotEmpty()) {
            params.joinToString(prefix = "/", separator = "/") { it.toString() }
        } else {
            ""
        }
        val validOptionals: List<Pair<String, Any?>> = optionalParams.filter { it.second != null }
        val optionalPart: String = if (validOptionals.isNotEmpty()) {
            validOptionals.joinToString(prefix = "?", separator = "&") {
                "${it.first}=${it.second}"
            }
        } else {
            ""
        }

        return defaultScreenName() + mandatoryPart + optionalPart
    }

    /**
     * Return navigation path for screen only with optional parameters
     *
     * @param optionalParams The optional arguments that will be used for screen path
     * @return The string path of default screen name with optional parameters of navigation,
     */
    fun Screen.screenNameWithOptionalParams(optionalParams: List<Pair<String, Any>>): String {
        if (optionalParams.isEmpty()) return defaultScreenName()

        val optionalPart: String = optionalParams.joinToString(prefix = "?", separator = "&") {
            "${it.first}=${it.second}"
        }

        return defaultScreenName() + optionalPart
    }
}
