package ru.netology.nework.ui

import android.os.Bundle
import android.view.*
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import ru.netology.nework.R
import ru.netology.nework.util.AndroidUtils.hideKeyboard
import ru.netology.nework.util.ConstantValues.emptyJob
import ru.netology.nework.util.FloatingValue.currentFragment
import ru.netology.nework.databinding.FragmentNewJobBinding
import ru.netology.nework.viewmodel.JobViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@AndroidEntryPoint
class NewJobFragment : Fragment() {

    private val binding by lazy { FragmentNewJobBinding.inflate(layoutInflater) }
    private val viewModel: JobViewModel by activityViewModels()
    private var job = emptyJob
    private var fragmentBinding: FragmentNewJobBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        fragmentBinding = binding

        lifecycleScope.launchWhenCreated {
            viewModel.data.collectLatest { list ->
                job = list.find { job ->
                    job.id == viewModel.getEditedId()
                } ?: emptyJob
            }
        }

        with(binding) {
            if (job != emptyJob) {
                jobOrganizationInput.setText(job.name)
                jobPositionInput.setText(job.position)

                val sourceFormat =  SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val targetFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

                val formattedStart: String = targetFormat.format(sourceFormat.parse(job.start)!!)
                val formattedFinish: String = targetFormat.format(job.finish?.let {
                    sourceFormat.parse(
                        it
                    )
                }!!)

                dateStartWorkingInput.setText(formattedStart)
                dateEndWorkingInput.setText(formattedFinish)


                inputLink.setText(job.link)
            }

            requireActivity().addMenuProvider(object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.menu_new_post, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                    when (menuItem.itemId) {
                        R.id.save -> {
                            fragmentBinding?.let {
                                viewModel.changeContent(
                                    it.jobOrganizationInput.text.toString(),
                                    it.jobPositionInput.text.toString(),
                                    it.dateStartWorkingInput.text.toString(),
                                    it.dateEndWorkingInput.text.toString().ifEmpty { null },
                                    it.inputLink.text.toString().ifEmpty { null },
                                )
                                viewModel.save()
                                hideKeyboard(requireView())
                                findNavController().navigate(R.id.action_newJobFragment_to_profileFragment)
                            }
                            true
                        }
                        else -> false
                    }

            }, viewLifecycleOwner)

            return root
        }
    }

    override fun onStart() {
        currentFragment = javaClass.simpleName
        super.onStart()
    }

    override fun onDestroyView() {
        fragmentBinding = null
        super.onDestroyView()
    }
}