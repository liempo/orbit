package com.fourcode.tracking.standard.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.fourcode.tracking.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.sheet_standard_options.*

class OptionsDialogFragment : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(
        R.layout.sheet_standard_options,
        container, false
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navigation_view.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.menu_settings -> findNavController()
                    .navigate(HomeFragmentDirections.openSettings())

                R.id.menu_logout -> findNavController()
                    .navigate(R.id.action_logout)
            }

            dismiss()

            true
        }
    }

}