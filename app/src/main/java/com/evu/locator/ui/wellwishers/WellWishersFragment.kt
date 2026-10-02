package com.evu.locator.ui.wellwishers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.evu.locator.MainActivity
import com.evu.locator.data.FirestoreRepo
import com.evu.locator.databinding.FragmentWellWishersBinding
import com.evu.locator.ui.common.ContactAdapter
import com.evu.locator.ui.common.RenameDialogFragment
import kotlinx.coroutines.launch

class WellWishersFragment : Fragment() {

    private var _binding: FragmentWellWishersBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWellWishersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val myUid = (activity as MainActivity).myUid ?: return

        val adapter = ContactAdapter(
            onRename = { contact ->
                RenameDialogFragment.newInstance(contact.otherUid, contact.displayName)
                    .show(parentFragmentManager, "rename")
            },
            onRemove = { contact ->
                FirestoreRepo.removeContact(myUid, contact.otherUid)
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            FirestoreRepo.listenContacts(myUid, "wellWisher").collect { adapter.submit(it) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}