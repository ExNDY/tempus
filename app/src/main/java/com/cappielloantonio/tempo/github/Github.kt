package com.cappielloantonio.tempo.github

object Github {
    private const val OWNER = "eddyizm"
    private const val REPO = "Tempus"

    @JvmStatic
    fun getUrl(): String {
        return "https://api.github.com/"
    }

    @JvmStatic
    fun getOwner(): String {
        return OWNER
    }

    @JvmStatic
    fun getRepo(): String {
        return REPO
    }
}
