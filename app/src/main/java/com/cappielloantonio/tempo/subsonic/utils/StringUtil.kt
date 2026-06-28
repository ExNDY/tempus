package com.cappielloantonio.tempo.subsonic.utils

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

object StringUtil {
    @JvmStatic
    fun tokenize(s: String): String {
        val md5 = "MD5"
        return try {
            val digest = MessageDigest.getInstance(md5)
            digest.update(s.toByteArray())
            val messageDigest = digest.digest()
            val hexString = StringBuilder()
            for (aMessageDigest in messageDigest) {
                var h = Integer.toHexString(0xFF and aMessageDigest.toInt())
                while (h.length < 2) {
                    h = "0$h"
                }
                hexString.append(h)
            }
            hexString.toString()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            ""
        }
    }
}
