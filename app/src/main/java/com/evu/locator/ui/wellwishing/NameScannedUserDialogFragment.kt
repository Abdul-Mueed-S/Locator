package com.evu.locator.ui.wellwishing

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.evu.locator.data.FirestoreRepo
import com.evu.locator.databinding.DialogNameScannedUserBinding
import com.evu.locator.utils.cleanTrim
import com.evu.locator.utils.isBlankAfterClean

class NameScannedUserDialogFragment : DialogFragment() {

    companion object {
        private const val ARG_SCANNED_UID = "scannedUid"
        private const val ARG_MY_UID = "myUid"

        fun newInstance(scannedUid: String, myUid: String): NameScannedUserDialogFragment {
            val fragment = NameScannedUserDialogFragment()
            fragment.arguments = Bundle().apply {
                putString(ARG_SCANNED_UID, scannedUid)
                putString(ARG_MY_UID, myUid)
            }
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val scannedUid = requireArguments().getString(ARG_SCANNED_UID, "")
        val myUid = requireArguments().getString(ARG_MY_UID, "")

        val binding = DialogNameScannedUserBinding.inflate(layoutInflater)

        binding.cancelBtn.setOnClickListener { dismiss() }
        binding.addBtn.setOnClickListener {
            val name = binding.nameInput.text.toString()
            if (!name.isBlankAfterClean()) {
                FirestoreRepo.connect(myUid, scannedUid, name.cleanTrim()) {}
                dismiss()
            }
        }

        return androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()
    }
}