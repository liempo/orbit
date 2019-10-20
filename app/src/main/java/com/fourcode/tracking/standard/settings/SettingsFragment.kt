package com.fourcode.tracking.standard.settings

import android.os.Bundle
import android.text.InputType
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import com.fourcode.tracking.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?
    ) {
        setPreferencesFromResource(R.xml.preferences_standard, rootKey)

        findPreference<EditTextPreference>(getString(R.string.pref_key_fuel_efficiency))?.
            setOnBindEditTextListener { it.inputType = InputType.TYPE_CLASS_NUMBER }

        findPreference<EditTextPreference>(getString(R.string.pref_key_fuel_price))?.
            setOnBindEditTextListener { it.inputType = InputType.TYPE_CLASS_NUMBER }
    }

}