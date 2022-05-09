package com.example.runningappmvvm.ui.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.runningappmvvm.R
import com.example.runningappmvvm.other.Constants
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_settings.*
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment: Fragment(R.layout.fragment_settings) {

    @Inject
    lateinit var sharedPref: SharedPreferences

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadFieldsFromSharedPref()

        btnApplyChanges.setOnClickListener {
            if (applyChangesToSharedPref())
                Snackbar.make(requireView(), "Saved Changes", Snackbar.LENGTH_SHORT).show()
            else
                Snackbar.make(requireView(), "Please fill out all the fields", Snackbar.LENGTH_SHORT).show()
        }

    }

    private fun loadFieldsFromSharedPref(){
        etName.setText(sharedPref.getString(Constants.KEY_NAME, ""))
        etWeight.setText(sharedPref.getFloat(Constants.KEY_WEIGHT, 80f).toString())
    }

    private fun applyChangesToSharedPref(): Boolean{
        val nameText = etName.text.toString()
        val weightText = etWeight.text.toString()

        if (nameText.isEmpty() || weightText.isEmpty())
            return false

        sharedPref.edit()
            .putString(Constants.KEY_NAME, nameText)
            .putFloat(Constants.KEY_WEIGHT, weightText.toFloat())
            .apply()

        val toolbarText = "Let's go, $nameText"
        requireActivity().tvToolbarTitle.text = toolbarText
        return true
    }
}