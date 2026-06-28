package com.cappielloantonio.tempo.util

import android.content.Context
import androidx.core.os.LocaleListCompat
import com.cappielloantonio.tempo.App
import com.cappielloantonio.tempo.R
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.AbstractMap
import java.util.Date
import java.util.Locale

object UIUtil {
    @JvmStatic
    fun getSpanCount(itemCount: Int, maxSpan: Int): Int {
        val itemSize = if (itemCount == 0) 1 else itemCount
        return if (itemSize / maxSpan > 0) {
            maxSpan
        } else {
            itemSize % maxSpan
        }
    }

    private fun getLocalesFromResources(context: Context): LocaleListCompat {
        val tagsList = ArrayList<String>()
        val xpp = context.resources.getXml(R.xml.locale_config)
        try {
            while (xpp.eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = xpp.name
                if (xpp.eventType == XmlPullParser.START_TAG) {
                    if ("locale" == tagName && xpp.attributeCount > 0 && xpp.getAttributeName(0) == "name") {
                        tagsList.add(xpp.getAttributeValue(0))
                    }
                }
                xpp.next()
            }
        } catch (e: XmlPullParserException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return LocaleListCompat.forLanguageTags(tagsList.joinToString(","))
    }

    @JvmStatic
    fun getLangPreferenceDropdownEntries(context: Context): Map<String, String> {
        val localeList = getLocalesFromResources(context)
        val localeArrayList = ArrayList<Map.Entry<String, String>>()
        val systemDefaultLabel = App.getContext().getString(R.string.settings_system_language)
        val systemDefaultValue = "default"

        for (i in 0 until localeList.size()) {
            val locale = localeList[i]
            if (locale != null) {
                localeArrayList.add(
                    AbstractMap.SimpleEntry(
                        Util.toPascalCase(locale.getDisplayName(locale)),
                        locale.toLanguageTag()
                    )
                )
            }
        }

        localeArrayList.sortWith { e1, e2 -> e1.key.compareTo(e2.key, ignoreCase = true) }

        val orderedMap = LinkedHashMap<String, String>()
        orderedMap[systemDefaultLabel] = systemDefaultValue
        for (entry in localeArrayList) {
            orderedMap[entry.key] = entry.value
        }
        return orderedMap
    }

    @JvmStatic
    fun getReadableDate(date: Date?): String {
        if (date == null) {
            return App.getContext().getString(R.string.share_no_expiration)
        }
        val formatter = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
        return formatter.format(date)
    }
}
