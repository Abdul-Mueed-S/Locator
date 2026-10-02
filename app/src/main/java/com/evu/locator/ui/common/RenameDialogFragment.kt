package com.evu.locator.ui.common

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.evu.locator.data.FirestoreRepo
import com.evu.locator.databinding.DialogRenameBinding
import com.evu.locator.utils.cleanTrim
import com.evu.locator.utils.isBlankAfterClean

class RenameDialogFragment : DialogFragment() {

    companion object {
        private const val ARG_OTHER_UID = "otherUid"
        private const val ARG_CURRENT_NAME = "currentName"

        fun newInstance(otherUid: String, currentName: String): RenameDialogFragment {
            val fragment = RenameDialogFragment()
            fragment.arguments = Bundle().apply {
                putString(ARG_OTHER_UID, otherUid)
                putString(ARG_CURRENT_NAME, currentName)
            }
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val otherUid = requireArguments().getString(ARG_OTHER_UID, "")
        val currentName = requireArguments().getString(ARG_CURRENT_NAME, "")

        val binding = DialogRenameBinding.inflate(layoutInflater)
        binding.nameInput.setText(currentName)

        binding.cancelBtn.setOnClickListener { dismiss() }
        binding.saveBtn.setOnClickListener {
            val name = binding.nameInput.text.toString()
            if (!name.isBlankAfterClean()) {
                val myUid = com.evu.locator.data.FirestoreRepo.myUid
                if (myUid != null) {
                    FirestoreRepo.renameContact(myUid, otherUid, name.cleanTrim())
                }
                dismiss()
            }
        }

        return androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()
    }
}