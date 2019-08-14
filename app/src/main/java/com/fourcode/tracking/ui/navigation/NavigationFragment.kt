package com.fourcode.tracking.ui.navigation

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.fourcode.tracking.R

class NavigationFragment : Fragment() {

    private lateinit var model: NavigationViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(
        R.layout.navigation_fragment,
        container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        model = ViewModelProviders.of(this)
            .get(NavigationViewModel::class.java)
    }

    companion object {
        fun newInstance() = NavigationFragment()
    }

}
