package com.evu.locator.ui.home

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
import com.evu.locator.databinding.FragmentHomeBinding
import com.evu.locator.utils.ContactDirectory
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: TravelerCardAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val myUid = (activity as MainActivity).myUid ?: return

        adapter = TravelerCardAdapter { contact ->
            val bundle = Bundle().apply {
                putString("uid", contact.otherUid)
                putString("name", contact.displayName)
            }
            findNavController().navigate(R.id.updateTimelineFragment, bundle)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            FirestoreRepo.listenContacts(myUid, "wellWishing").collect { contacts ->
                contacts.forEach { ContactDirectory.put(it.otherUid, it.displayName) }
                val homeItems = mutableListOf<HomeItem>()
                contacts.forEach { contact ->
                    FirestoreRepo.listenUserDoc(contact.otherUid).collect { user ->
                        val idx = homeItems.indexOfFirst { it.contact.otherUid == contact.otherUid }
                        if (idx >= 0) homeItems[idx] = HomeItem(contact, user)
                        else homeItems.add(HomeItem(contact, user))
                        adapter.submit(homeItems)
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