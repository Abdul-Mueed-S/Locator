package com.evu.locator.ui.traveler

import android.R
import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import com.evu.locator.data.models.Contact
import com.evu.locator.databinding.DialogMakeUpdateBinding
import com.evu.locator.utils.cleanTrim
import com.evu.locator.utils.isBlankAfterClean
import java.util.Calendar

class MakeUpdateDialogFragment(
    private val wellWishers: List<Contact>,
    private val onSubmit: (update: String, description: String?, timestamp: Long, targetUid: String) -> Unit
) : DialogFragment() {

    private var hour = 0
    private var minute = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogMakeUpdateBinding.inflate(layoutInflater)
        val cal = Calendar.getInstance()
        hour = cal.get(Calendar.HOUR_OF_DAY)
        minute = cal.get(Calendar.MINUTE)
        binding.timeBtn.text = String.format("Time: %02d:%02d", hour, minute)

        binding.timeBtn.setOnClickListener {
            TimePickerDialog(requireContext(), { _, h, m ->
                hour = h; minute = m
                binding.timeBtn.text = String.format("Time: %02d:%02d", h, m)
            }, hour, minute, true).show()
        }

        binding.addDescriptionBtn.setOnClickListener {
            binding.descriptionLayout.visibility = android.view.View.VISIBLE
            binding.addDescriptionBtn.visibility = android.view.View.GONE
        }

        val spinnerLabels = mutableListOf("All Well-Wishers")
        spinnerLabels.addAll(wellWishers.map { it.displayName })
        binding.targetSpinner.adapter = ArrayAdapter(
            requireContext(), R.layout.simple_spinner_dropdown_item, spinnerLabels
        )

        binding.cancelBtn.setOnClickListener { dismiss() }
        binding.postBtn.setOnClickListener {
            val update = binding.updateInput.text.toString()
            if (update.isBlankAfterClean()) return@setOnClickListener

            val description = binding.descriptionInput.text.toString().cleanTrim()
            val finalDesc = if (description.isNotEmpty()) description else null

            val targetIndex = binding.targetSpinner.selectedItemPosition
            val targetUid = if (targetIndex == 0) "" else wellWishers[targetIndex - 1].otherUid

            val c = Calendar.getInstance()
            c.set(Calendar.HOUR_OF_DAY, hour)
            c.set(Calendar.MINUTE, minute)
            c.set(Calendar.SECOND, 0)

            onSubmit(update.cleanTrim(), finalDesc, c.timeInMillis, targetUid)
            dismiss()
        }

        return androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()
    }
}