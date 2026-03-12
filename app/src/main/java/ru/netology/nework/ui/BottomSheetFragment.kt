package ru.netology.nework.ui

import android.os.Bundle
import android.view.LayoutInflater
import androidx.navigation.fragment.findNavController
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import ru.netology.nework.R
import ru.netology.nework.adapters.OnInteractionListenerUsers
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import ru.netology.nework.adapters.UsersAdapter
import ru.netology.nework.dto.User
import ru.netology.nework.databinding.FragmentBottomSheetBinding
import ru.netology.nework.util.BundleArguments.Companion.eventId
import ru.netology.nework.util.BundleArguments.Companion.eventRequestType
import ru.netology.nework.util.BundleArguments.Companion.userId
import ru.netology.nework.viewmodel.EventViewModel
import ru.netology.nework.viewmodel.UsersViewModel

@AndroidEntryPoint
class BottomSheetFragment : BottomSheetDialogFragment() {

    private val usersViewModel: UsersViewModel by activityViewModels()
    val viewModel: EventViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val binding = FragmentBottomSheetBinding.inflate(inflater, container, false)
        val adapter = UsersAdapter(object : OnInteractionListenerUsers {
            override fun onTap(user: User) {
                findNavController().navigate(
                    R.id.action_bottomSheetFragment_to_profileFragment,
                    Bundle().apply { userId = user.id }
                )
            }
        })
        var filteredList: List<Long> = emptyList()
        binding.list.adapter = adapter
        lifecycleScope.launchWhenCreated {
            viewModel.data.collectLatest {
                when (arguments?.eventRequestType) {
                    "speakers" -> filteredList = it.find { event ->
                        event.id == arguments?.eventId
                    }?.speakerIds ?: emptyList()
                    "party" -> filteredList = it.find { event ->
                        event.id == arguments?.eventId
                    }?.participantsIds ?: emptyList()
                    else -> emptyList<Long>()
                }
            }
        }

        lifecycleScope.launchWhenCreated {
            usersViewModel.dataUsersList.collectLatest {
                adapter.submitList(it.filter { user ->
                    filteredList.contains(user.id)
                })
            }
        }
        return binding.root
    }
}