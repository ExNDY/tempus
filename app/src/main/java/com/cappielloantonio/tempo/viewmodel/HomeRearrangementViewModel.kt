package com.cappielloantonio.tempo.viewmodel

import androidx.lifecycle.ViewModel
import com.cappielloantonio.tempo.model.HomeSector
import com.cappielloantonio.tempo.util.Preferences
import com.google.common.reflect.TypeToken
import com.google.gson.Gson

class HomeRearrangementViewModel : ViewModel() {
    private var sectors: MutableList<HomeSector> = mutableListOf()

    fun getHomeSectorList(defaultSectors: MutableList<HomeSector>): MutableList<HomeSector> {
        if (sectors.isNotEmpty()) return sectors

        val storedHomeSectorList = Preferences.getHomeSectorList()
        sectors = if (!storedHomeSectorList.isNullOrEmpty() && storedHomeSectorList != "null") {
            Gson().fromJson<List<HomeSector>>(
                storedHomeSectorList,
                object : TypeToken<List<HomeSector>>() {}.type,
            ).toMutableList()
        } else {
            defaultSectors
        }

        return sectors
    }

    fun orderSectorLiveListAfterSwap(sectors: MutableList<HomeSector>) {
        this.sectors = sectors
    }

    fun saveHomeSectorList(sectors: List<HomeSector>) {
        Preferences.setHomeSectorList(sectors)
    }

    fun resetHomeSectorList() {
        Preferences.setHomeSectorList(null)
    }

    fun closeDialog() {
        sectors = mutableListOf()
    }
}
