package com.evu.locator.ui.wellwishing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.evu.locator.MainActivity
import com.evu.locator.R
import com.evu.locator.data.FirestoreRepo
import com.evu.locator.databinding.FragmentWellWishingBinding
import com.evu.locator.ui.common.RenameDialogFragment
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch

class WellWishingFragment : Fragment() {

    private var _binding: FragmentWellWishingBinding? = null
    private val binding get() = _binding!!

    private val scanLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            val myUid = (activity as MainActivity).myUid ?: return@registerForActivityResult
            NameScannedUserDialogFragment.newInstance(result.contents, myUid)
                .show(parentFragmentManager, "nameScanned")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWellWishingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val myUid = (activity as MainActivity).myUid ?: return

        val adapter = WellWishingAdapter(
            onRename = { contact ->
                RenameDialogFragment.newInstance(contact.otherUid, contact.displayName)
                    .show(parentFragmentManager, "rename")
            },
            onRemove = { contact ->
                FirestoreRepo.removeContact(myUid, contact.otherUid)
            },
            onView = { contact ->
                val bundle = Bundle().apply {
                    putString("uid", contact.otherUid)
                    putString("name", contact.displayName)
                }
                findNavController().navigate(R.id.updateTimelineFragment, bundle)
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.scanBtn.setOnClickListener {
            val options = ScanOptions()
            options.setPrompt("Scan traveler's QR code")
            options.setBeepEnabled(true)
            scanLauncher.launch(options)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            FirestoreRepo.listenContacts(myUid, "wellWishing").collect { contacts ->
                val items = mutableListOf<WellWishingItem>()
                contacts.forEach { contact ->
                    FirestoreRepo.listenUserDoc(contact.otherUid).collect { user ->
                        val idx = items.indexOfFirst { it.contact.otherUid == contact.otherUid }
                        if (idx >= 0) items[idx] = WellWishingItem(contact, user)
                        else items.add(WellWishingItem(contact, user))
                        adapter.submit(items)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}