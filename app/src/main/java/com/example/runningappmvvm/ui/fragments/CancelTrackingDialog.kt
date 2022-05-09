package com.example.runningappmvvm.ui.fragments

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.example.runningappmvvm.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class CancelTrackingDialog: DialogFragment() {

    private var onYesClickListener: (() -> Unit)? = null

    fun onYesClickListener(listener: () -> Unit){
        onYesClickListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
            .setTitle("Cancel the Run?")
            .setMessage("Are you sure to cancel te current run and delete all its data?")
            .setIcon(R.drawable.ic_delete)
            .setPositiveButton("Yes"){ _, _ ->
                onYesClickListener?.let {
                    it()
                }
            }
            .setNegativeButton("No"){ dialog, _ ->
                dialog.cancel()
            }
            .create()
    }
}