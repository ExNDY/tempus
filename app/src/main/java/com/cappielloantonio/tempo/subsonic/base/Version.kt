package com.cappielloantonio.tempo.subsonic.base

class Version private constructor(val versionString: String) : Comparable<Version> {

    fun isLowerThan(version: Version): Boolean {
        return compareTo(version) < 0
    }

    override fun compareTo(other: Version): Int {
        val thisParts = this.versionString.split(".").map { it.toInt() }
        val thatParts = other.versionString.split(".").map { it.toInt() }

        val length = maxOf(thisParts.size, thatParts.size)

        for (i in 0 until length) {
            val thisPart = if (i < thisParts.size) thisParts[i] else 0
            val thatPart = if (i < thatParts.size) thatParts[i] else 0

            if (thisPart < thatPart) return -1
            if (thisPart > thatPart) return 1
        }
        return 0
    }

    override fun toString(): String {
        return versionString
    }

    companion object {
        private const val VERSION_PATTERN = "\\d+(\\.\\d+)*"

        @JvmStatic
        fun of(versionString: String?): Version {
            if (versionString == null || !versionString.matches(Regex(VERSION_PATTERN))) {
                throw IllegalArgumentException("Invalid version format")
            }
            return Version(versionString)
        }
    }
}
