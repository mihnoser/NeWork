package ru.netology.nework.ui

import android.os.Bundle
import android.view.*
import android.widget.Toast
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

    private var draftOrganization: String? = null
    private var draftPosition: String? = null
    private var draftStartDate: String? = null
    private var draftEndDate: String? = null
    private var draftLink: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        fragmentBinding = binding

        draftOrganization = savedInstanceState?.getString("draft_organization")
        draftPosition = savedInstanceState?.getString("draft_position")
        draftStartDate = savedInstanceState?.getString("draft_start_date")
        draftEndDate = savedInstanceState?.getString("draft_end_date")
        draftLink = savedInstanceState?.getString("draft_link")

        lifecycleScope.launchWhenCreated {
            viewModel.data.collectLatest { list ->
                job = list.find { job ->
                    job.id == viewModel.getEditedId()
                } ?: emptyJob

                updateUiWithJob()
            }
        }

        with(binding) {
            if (viewModel.getEditedId() == 0L) {
                draftOrganization?.let { jobOrganizationInput.setText(it) }
                draftPosition?.let { jobPositionInput.setText(it) }
                draftStartDate?.let { dateStartWorkingInput.setText(it) }
                draftEndDate?.let { dateEndWorkingInput.setText(it) }
                draftLink?.let { inputLink.setText(it) }
            }

            requireActivity().addMenuProvider(object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.menu_new_post, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                    when (menuItem.itemId) {
                        R.id.save -> {
                            saveJob()
                            true
                        }
                        else -> false
                    }

            }, viewLifecycleOwner)

            return root
        }
    }

    private fun updateUiWithJob() {
        if (job != emptyJob) {
            binding.jobOrganizationInput.setText(job.name)
            binding.jobPositionInput.setText(job.position)

            try {
                val sourceFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val targetFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

                val formattedStart: String = targetFormat.format(sourceFormat.parse(job.start)!!)
                binding.dateStartWorkingInput.setText(formattedStart)

                job.finish?.let {
                    val formattedFinish: String = targetFormat.format(sourceFormat.parse(it)!!)
                    binding.dateEndWorkingInput.setText(formattedFinish)
                }

                binding.inputLink.setText(job.link)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Ошибка формата даты", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveJob() {
        fragmentBinding?.let {
            if (it.jobOrganizationInput.text.toString().isBlank()) {
                Toast.makeText(requireContext(), "Введите название организации", Toast.LENGTH_SHORT).show()
                return
            }

            if (it.jobPositionInput.text.toString().isBlank()) {
                Toast.makeText(requireContext(), "Введите должность", Toast.LENGTH_SHORT).show()
                return
            }

            if (it.dateStartWorkingInput.text.toString().isBlank()) {
                Toast.makeText(requireContext(), "Введите дату начала", Toast.LENGTH_SHORT).show()
                return
            }

            viewModel.changeContent(
                it.jobOrganizationInput.text.toString(),
                it.jobPositionInput.text.toString(),
                it.dateStartWorkingInput.text.toString(),
                it.dateEndWorkingInput.text.toString().ifEmpty { null },
                it.inputLink.text.toString().ifEmpty { null },
            )

            viewModel.save()

            clearDrafts()

            hideKeyboard(requireView())
            findNavController().navigate(R.id.action_newJobFragment_to_profileFragment)
        }
    }

    private fun clearDrafts() {
        draftOrganization = null
        draftPosition = null
        draftStartDate = null
        draftEndDate = null
        draftLink = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (viewModel.getEditedId() == 0L) {
            outState.putString("draft_organization", binding.jobOrganizationInput.text.toString())
            outState.putString("draft_position", binding.jobPositionInput.text.toString())
            outState.putString("draft_start_date", binding.dateStartWorkingInput.text.toString())
            outState.putString("draft_end_date", binding.dateEndWorkingInput.text.toString())
            outState.putString("draft_link", binding.inputLink.text.toString())
        }
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onDestroyView() {
        if (viewModel.getEditedId() == 0L) {
            if (binding.jobOrganizationInput.text.toString().isNotBlank() ||
                binding.jobPositionInput.text.toString().isNotBlank() ||
                binding.dateStartWorkingInput.text.toString().isNotBlank() ||
                binding.dateEndWorkingInput.text.toString().isNotBlank() ||
                binding.inputLink.text.toString().isNotBlank()) {

                draftOrganization = binding.jobOrganizationInput.text.toString()
                draftPosition = binding.jobPositionInput.text.toString()
                draftStartDate = binding.dateStartWorkingInput.text.toString()
                draftEndDate = binding.dateEndWorkingInput.text.toString()
                draftLink = binding.inputLink.text.toString()
            }
        }
        fragmentBinding = null
        super.onDestroyView()
    }
}