package com.fourcode.tracking.standard.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.fourcode.tracking.R

class SettingsFragment: PreferenceFragmentCompat() {

    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_standard, rootKey)
    }

}