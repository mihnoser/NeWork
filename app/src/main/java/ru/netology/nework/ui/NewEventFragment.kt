package ru.netology.nework.ui


import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toFile
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.github.dhaval2404.imagepicker.ImagePicker
import com.github.dhaval2404.imagepicker.constant.ImageProvider
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import ru.netology.nework.R
import ru.netology.nework.adapters.OnInteractionListenerUsers
import ru.netology.nework.adapters.UsersAdapter
import ru.netology.nework.util.AndroidUtils.hideKeyboard
import ru.netology.nework.util.ConstantValues.emptyEvent
import ru.netology.nework.util.FloatingValue.getExtensionFromUri
import ru.netology.nework.databinding.FragmentNewEventBinding
import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.AttachmentType
import ru.netology.nework.dto.EventType
import ru.netology.nework.dto.User
import ru.netology.nework.util.Factory.Companion.textArg
import ru.netology.nework.viewmodel.EventViewModel
import ru.netology.nework.viewmodel.UsersViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

@AndroidEntryPoint
class NewEventFragment : Fragment() {

    private val binding by lazy { FragmentNewEventBinding.inflate(layoutInflater) }
    private val viewModel: EventViewModel by activityViewModels()
    private val userViewModel: UsersViewModel by viewModels()
    private var event = emptyEvent
    private var fragmentBinding: FragmentNewEventBinding? = null
    private var type: AttachmentType? = null
    private var attachRes: Attachment? = null
    private var speakersIds:MutableList<Long> = mutableListOf()
    private var typeEvent:EventType = EventType.ONLINE
    private var draftText: String? = null
    private var adapter = UsersAdapter(object : OnInteractionListenerUsers {
        override fun onTap(user: User) {
            speakersIds.add(user.id)
            binding.countMentions.text = speakersIds.size.toString()
        }
    })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        fragmentBinding = binding

        draftText = savedInstanceState?.getString("draft_text")

        lifecycleScope.launchWhenCreated {
            viewModel.data.collectLatest { list ->
                event = list.find { event ->
                    event.id == viewModel.getEditedId()
                } ?: emptyEvent
            }
        }

        with(binding) {
            if (event != emptyEvent) {
                edit.setText(event.content)
                inputLink.setText(event.link)
                countMentions.text = event.speakerIds.size.toString()

                val sourceFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                val targetFormat =  SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                val formattedStart: String = targetFormat.format(sourceFormat.parse(event.datetime.take(10))!!)



                dateEventInput.setText(formattedStart)
                timeEventInput.setText(event.datetime.subSequence(11,16))
            }

            if (edit.text.isNullOrBlank()) {
                draftText?.let { edit.setText(it) }
            }

            edit.requestFocus()
            attachRes = viewModel.getEditedEventAttachment()
            Glide.with(photo)
                .load(attachRes?.url)
                .placeholder(
                    when (attachRes?.type) {
                        AttachmentType.AUDIO -> {
                            R.drawable.ic_baseline_audio_file_500
                        }
                        AttachmentType.VIDEO -> {
                            R.drawable.ic_baseline_video_library_500
                        }
                        else -> {
                            R.drawable.not_image_500
                        }
                    }
                )
                .timeout(10_000)
                .into(photo)


            if (!attachRes?.url.isNullOrBlank() && arguments?.textArg != null) {
                binding.photoContainer.visibility = View.VISIBLE
            }

            viewModel.media.observe(viewLifecycleOwner) {
                if (it.uri == null && attachRes?.url.isNullOrBlank()) {
                    binding.photoContainer.visibility = View.GONE
                    return@observe
                } else {
                    binding.photoContainer.visibility = View.VISIBLE
                    if (it.attachmentType == AttachmentType.IMAGE) {
                        binding.photo.setImageURI(it.uri)
                    }
                }
            }

            requireActivity().addMenuProvider(object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.menu_new_post, menu)
                }
                private fun formatDate(inputDate: String): String {
                    val sourceFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                    val targetFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    return targetFormat.format(sourceFormat.parse(inputDate)!!)
                }
                override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                    when (menuItem.itemId) {
                        R.id.save -> {
                            fragmentBinding?.let {
                                val formattedDate = formatDate(it.dateEventInput.text.toString())
                                val formattedDateTime = "$formattedDate ${it.timeEventInput.text.toString()}"


                                viewModel.changeContent(
                                    it.edit.text.toString(),
                                    it.inputLink.text.toString().ifEmpty { null },
                                    formattedDateTime,
                                    typeEvent,
                                    speakersIds
                                )
                                viewModel.save()
                                draftText = null
                                hideKeyboard(requireView())
                            }
                            true
                        }
                        else -> false
                    }

            }, viewLifecycleOwner)
            binding.listUsers.adapter = adapter
            clickListeners()

            viewModel.saveError.observe(viewLifecycleOwner) { errorMessage ->
                errorMessage?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                    viewModel.clearSaveError()
                }
            }

            return root
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("draft_text", binding.edit.text.toString())
    }

    override fun onStart() {
        super.onStart()
    }

    @SuppressLint("IntentReset")
    private fun clickListeners() {
        binding.typeOnline.isClickable = false
        binding.typeOnline.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                binding.typeOffline.toggle()
                binding.typeOffline.isClickable = true
                binding.typeOnline.isClickable = false
                typeEvent = EventType.ONLINE
            }
        }

        binding.typeOffline.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                binding.typeOnline.toggle()
                binding.typeOnline.isClickable = true
                binding.typeOffline.isClickable = false
                typeEvent = EventType.OFFLINE
            }
        }

        binding.countMentions.setOnLongClickListener {
            speakersIds = mutableListOf()
            binding.countMentions.text = speakersIds.size.toString()
            true
        }

        val pickPhotoLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                when (it.resultCode) {
                    ImagePicker.RESULT_ERROR -> {
                        Snackbar.make(
                            binding.root,
                            ImagePicker.getError(it.data),
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                    Activity.RESULT_OK -> {
                        val uri: Uri? = it.data?.data
                        viewModel.changeMedia(uri, uri?.toFile(), AttachmentType.IMAGE)
                    }
                }
            }
        binding.pickPhoto.setOnClickListener {
            ImagePicker.with(this)
                .crop()
                .compress(2048)
                .provider(ImageProvider.GALLERY)
                .galleryMimeTypes(
                    arrayOf(
                        "image/png",
                        "image/jpeg",
                    )
                )
                .createIntent(pickPhotoLauncher::launch)
        }

        binding.takePhoto.setOnClickListener {
            ImagePicker.with(this)
                .crop()
                .compress(2048)
                .provider(ImageProvider.CAMERA)
                .createIntent(pickPhotoLauncher::launch)
        }

        val pickMediaLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                when (it.resultCode) {
                    Activity.RESULT_OK -> {
                        val uri: Uri? = it.data?.data
                        val contentResolver = context?.contentResolver
                        val inputStream = uri?.let { it1 -> contentResolver?.openInputStream(it1) }
                        val audioBytes = inputStream?.readBytes()
                        if (uri != null && contentResolver != null) {
                            val extension = getExtensionFromUri(uri, contentResolver)
                            val file = File(context?.getExternalFilesDir(null), "input.$extension")
                            FileOutputStream(file).use { outputStream ->
                                outputStream.write(audioBytes)
                                outputStream.flush()
                            }
                            viewModel.changeMedia(uri, file, type)
                        }
                    }
                    else -> {
                        Snackbar.make(
                            binding.root,
                            getString(R.string.error_upload),
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            }

        binding.uploadAudio.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
            intent.type = "audio/*"
            type = AttachmentType.AUDIO
            attachRes = attachRes?.copy(type = type!!)
            pickMediaLauncher.launch(intent)
        }

        binding.uploadVideo.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            intent.type = "video/*"
            type = AttachmentType.VIDEO
            attachRes = attachRes?.copy(type = type!!)
            pickMediaLauncher.launch(intent)
        }


        binding.removePhoto.setOnClickListener {
            viewModel.deleteAttachment()
            attachRes = null
            viewModel.changeMedia(null, null, null)
        }


        binding.addMentions.setOnClickListener {
            binding.listUsers.isVisible = !binding.listUsers.isVisible
            lifecycleScope.launchWhenCreated {
                userViewModel.dataUsersList.collectLatest {
                    adapter.submitList(it)
                }
            }
        }


        with(binding) {

            viewModel.eventCreated.observe(viewLifecycleOwner) {
                viewModel.loadEvents()
                findNavController().navigateUp()
            }

            fabCancel.setOnClickListener {
                if (viewModel.getEditedId() == 0L) {
                    draftText = edit.text.toString()
                } else {
                    edit.text?.clear()
                    viewModel.save()
                }
                hideKeyboard(root)
                findNavController().navigateUp()
            }

        }
    }

    override fun onDestroyView() {
        if (viewModel.getEditedId() == 0L && binding.edit.text.toString().isNotBlank()) {
            draftText = binding.edit.text.toString()
        }
        fragmentBinding = null
        super.onDestroyView()
    }
}