package com.evu.locator.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.evu.locator.MainActivity
import com.evu.locator.databinding.FragmentSettingsBinding
import com.evu.locator.utils.ThemeManager

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val myUid = (activity as MainActivity).myUid
        binding.idText.text = "Your ID: $myUid"

        when (ThemeManager.getSavedTheme(requireContext())) {
            ThemeManager.LIGHT_GREEN -> binding.lightGreenRadio.isChecked = true
            ThemeManager.DARK_BLUE -> binding.darkBlueRadio.isChecked = true
            ThemeManager.DARK_GREEN -> binding.darkGreenRadio.isChecked = true
            else -> binding.lightBlueRadio.isChecked = true
        }

        binding.themeGroup.setOnCheckedChangeListener { _, checkedId ->
            val theme = when (checkedId) {
                binding.lightGreenRadio.id -> ThemeManager.LIGHT_GREEN
                binding.darkBlueRadio.id -> ThemeManager.DARK_BLUE
                binding.darkGreenRadio.id -> ThemeManager.DARK_GREEN
                else -> ThemeManager.LIGHT_BLUE
            }
            ThemeManager.setTheme(requireContext(), theme)
            activity?.recreate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}