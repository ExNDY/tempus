package com.cappielloantonio.tempo.radiobrowser

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RadioBrowserRepository(private val httpClient: HttpClient) {
    private val baseUrl = "https://all.api.radio-browser.info/"

    suspend fun searchByName(name: String, limit: Int = 100): List<RadioBrowserStation> {
        return try {
            httpClient.get("${baseUrl}json/stations/search") {
                parameter("name", name)
                parameter("limit", limit)
                parameter("order", "votes")
                parameter("reverse", true)
            }.body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun searchByCountryExact(country: String, limit: Int = 100): List<RadioBrowserStation> {
        return try {
            httpClient.get("${baseUrl}json/stations/search") {
                parameter("countryexact", country)
                parameter("limit", limit)
                parameter("order", "votes")
                parameter("reverse", true)
            }.body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun searchAdvanced(name: String?, country: String?, language: String?, tag: String?): List<RadioBrowserStation> {
        return try {
            httpClient.get("${baseUrl}json/stations/search") {
                name?.let { parameter("name", it) }
                country?.let { parameter("country", it) }
                language?.let { parameter("language", it) }
                tag?.let { parameter("tag", it) }
                parameter("limit", 100)
                parameter("order", "votes")
                parameter("reverse", true)
            }.body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getCountries(): List<RadioBrowserCountry> {
        return try {
            httpClient.get("${baseUrl}json/countries").body()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
