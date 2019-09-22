package com.fourcode.tracking.standard.home


import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment

import com.fourcode.tracking.R

class StandardFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(
            R.layout.fragment_standard,
            container, false)
}
