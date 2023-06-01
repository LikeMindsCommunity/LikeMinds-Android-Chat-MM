package com.likeminds.chatmm.utils

import android.app.Application
import com.likeminds.chatmm.utils.sharedpreferences.BasePreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SDKPreferences @Inject constructor(
    application: Application,
) : BasePreferences(SDK_PREFS, application) {
    companion object {
        const val SDK_PREFS = "sdk_prefs"

        private const val API_KEY = "API_KEY"
        private const val IS_GUEST = "IS_GUEST"
    }

    fun setAPIKey(apiKey: String) {
        putPreference(API_KEY, apiKey)
    }

    fun getAPIKey(): String {
        return getPreference(API_KEY, "") ?: ""
    }

    fun setIsGuestUser(isGuest: Boolean?) {
        putPreference(IS_GUEST, isGuest ?: false)
    }

    fun getIsGuestUser(): Boolean {
        return getPreference(IS_GUEST, false)
    }
}