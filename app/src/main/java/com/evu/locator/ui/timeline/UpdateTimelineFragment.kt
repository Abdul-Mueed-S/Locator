package com.evu.locator.ui.timeline

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.evu.locator.MainActivity
import com.evu.locator.data.FirestoreRepo
import com.evu.locator.databinding.FragmentUpdateTimelineBinding
import kotlinx.coroutines.launch

class UpdateTimelineFragment : Fragment() {

    private var _binding: FragmentUpdateTimelineBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentUpdateTimelineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val uid = arguments?.getString("uid") ?: return
        val name = arguments?.getString("name") ?: ""
        val myUid = (activity as MainActivity).myUid ?: return

        binding.nameText.text = name
        val adapter = TimelineAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            FirestoreRepo.listenUpdatesForViewer(uid, myUid).collect { updates ->
                adapter.submit(updates)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}