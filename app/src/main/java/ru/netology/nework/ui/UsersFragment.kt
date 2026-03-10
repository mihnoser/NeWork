package ru.netology.nework.ui

import android.app.AlertDialog
import ru.netology.nework.adapters.OnInteractionListenerUsers
import ru.netology.nework.dto.User
import android.os.Bundle
import android.view.*
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import ru.netology.nework.R
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import ru.netology.nework.adapters.UsersAdapter
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.databinding.FragmentUsersBinding
import ru.netology.nework.util.Factory.Companion.textArg
import ru.netology.nework.util.Factory.Companion.userId
import ru.netology.nework.viewmodel.AuthViewModel
import ru.netology.nework.viewmodel.UsersViewModel
import javax.inject.Inject

@AndroidEntryPoint
class UsersFragment : Fragment() {

    @Inject
    lateinit var appAuth: AppAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentUsersBinding.inflate(layoutInflater)
        val viewModel: UsersViewModel by activityViewModels()

        val authViewModel: AuthViewModel by viewModels()

        var menuProvider: MenuProvider? = null

        val adapter = UsersAdapter(    object : OnInteractionListenerUsers {
            override fun onTap(user: User) {
                findNavController().navigate(
                    R.id.action_usersFragment_to_profileFragment,
                    Bundle().apply {
                        userId = user.id
                    }
                )
            }
        }
        )

        authViewModel.data.observe(viewLifecycleOwner) {
            menuProvider?.let(requireActivity()::removeMenuProvider)
            requireActivity().addMenuProvider(object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.menu_main, menu)

                    menu.setGroupVisible(R.id.unauthenticated, !authViewModel.authenticated)
                    menu.setGroupVisible(R.id.authenticated, authViewModel.authenticated)

                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.signin -> {
                            findNavController().navigate(
                                R.id.action_usersFragment_to_authFragment,
                                Bundle().apply {
                                    textArg = getString(R.string.sign_in)
                                }
                            )
                            true
                        }
                        R.id.signup -> {
                            findNavController().navigate(
                                R.id.action_usersFragment_to_authFragment,
                                Bundle().apply {
                                    textArg = getString(R.string.sign_up)
                                }
                            )
                            true
                        }
                        R.id.signout -> {
                            AlertDialog.Builder(requireActivity())
                                .setTitle(R.string.are_you_suare)
                                .setPositiveButton(R.string.yes) { _, _ ->
                                    appAuth.removeAuth()
                                }
                                .setCancelable(true)
                                .setNegativeButton(R.string.no, null)
                                .create()
                                .show()
                            true
                        }
                        R.id.navigation_profile -> {
                            findNavController().navigate(R.id.action_usersFragment_to_profileFragment)
                            true
                        }
                        else -> false
                    }
                }
            }.apply {
                menuProvider = this
            }, viewLifecycleOwner)
        }
        binding.mainNavView.setOnItemSelectedListener{ menuItem ->
            when (menuItem.itemId) {
                R.id.navigation_posts -> {
                    findNavController().navigate(R.id.action_usersFragment_to_feedFragment)
                    true
                }
                R.id.navigation_events -> {
                    findNavController().navigate(R.id.action_usersFragment_to_eventsFragment)
                    true
                }
                R.id.navigation_users -> {
                    true
                }

                else -> false
            }
        }
        binding.mainNavView.selectedItemId = R.id.navigation_users
        binding.listUsers.adapter = adapter
        lifecycleScope.launchWhenCreated {
            viewModel.dataUsersList.collectLatest {
                adapter.submitList(it)
            }
        }
        return binding.root
    }

    override fun onStart() {
        super.onStart()
    }
}