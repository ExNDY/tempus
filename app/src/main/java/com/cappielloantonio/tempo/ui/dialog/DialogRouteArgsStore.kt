package com.cappielloantonio.tempo.ui.dialog

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

typealias DialogRouteContent = @Composable (navController: NavController, onClose: () -> Unit) -> Unit
typealias BottomSheetRouteContent = @Composable (navController: NavController, onClose: suspend () -> Unit) -> Unit

object DialogRouteArgsStore {
    private val dialogArgs = ConcurrentHashMap<String, DialogRouteContent>()
    private val bottomSheetArgs = ConcurrentHashMap<String, BottomSheetRouteContent>()

    fun putDialog(content: DialogRouteContent): String {
        val token = UUID.randomUUID().toString()
        dialogArgs[token] = content
        return token
    }

    fun getDialog(token: String?): DialogRouteContent? {
        if (token.isNullOrBlank()) return null
        return dialogArgs[token]
    }

    fun removeDialog(token: String?) {
        if (!token.isNullOrBlank()) {
            dialogArgs.remove(token)
        }
    }

    fun putBottomSheet(content: BottomSheetRouteContent): String {
        val token = UUID.randomUUID().toString()
        bottomSheetArgs[token] = content
        return token
    }

    fun getBottomSheet(token: String?): BottomSheetRouteContent? {
        if (token.isNullOrBlank()) return null
        return bottomSheetArgs[token]
    }

    fun removeBottomSheet(token: String?) {
        if (!token.isNullOrBlank()) {
            bottomSheetArgs.remove(token)
        }
    }
}
