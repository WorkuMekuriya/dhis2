package org.dhis2.usescases.login

import androidx.annotation.UiThread
import co.infinum.goldfinger.Goldfinger
import org.dhis2.usescases.general.AbstractActivityContracts
import org.hisp.dhis.android.core.user.openid.IntentWithRequestCode

class LoginContracts {

    interface View : AbstractActivityContracts.View {
        @UiThread
        fun showUnlockButton()

        @UiThread
        fun onUnlockClick(android: android.view.View)

        @UiThread
        fun onLogoutClick(android: android.view.View)

        @UiThread
        fun setAutocompleteAdapters()

        @UiThread
        fun saveUsersData(isInitialSyncDone: Boolean)

        fun handleLogout()

        fun setLoginVisibility(isVisible: Boolean)

        fun showLoginProgress(showLogin: Boolean)

        fun goToNextScreen()

        fun setUrl(url: String)

        fun setUser(user: String)

        fun showCrashlyticsDialog()

        fun navigateToQRActivity()

        @UiThread
        fun renderError(throwable: Throwable)

        // FingerPrintAuth

        fun showBiometricButton()

        fun openAccountRecovery()

        fun alreadyAuthenticated()
        fun showCredentialsData(type: Goldfinger.Type, vararg args: String)
        fun showEmptyCredentialsMessage()
        fun setTestingCredentials()
        fun getDefaultServerProtocol(): String
        fun getPromptParams(): Goldfinger.PromptParams
        fun isNetworkAvailable(): Boolean
        fun openOpenIDActivity(it: IntentWithRequestCode?)
        fun openAccountsActivity()
        fun showNoConnectionDialog()
    }
}
