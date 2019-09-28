package com.fourcode.tracking.standard.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.fourcode.tracking.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class StandardOptionsBottomDialogFragment: BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(
        R.layout.sheet_standard_options,
        container, false
    )

}