package com.cappielloantonio.tempo.navigation

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal fun encodeNavRouteValue(value: String): String =
    URLEncoder.encode(value, StandardCharsets.UTF_8.name())
        .replace("+", "%20")

