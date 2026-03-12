package ru.netology.nework.ui

import android.app.AlertDialog
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.*
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import ru.netology.nework.R
import ru.netology.nework.adapters.*
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.util.ConstantValues.emptyUser
import ru.netology.nework.databinding.FragmentProfileBinding
import ru.netology.nework.dto.*
import ru.netology.nework.util.BundleArguments.Companion.eventId
import ru.netology.nework.util.BundleArguments.Companion.eventRequestType
import ru.netology.nework.util.BundleArguments.Companion.linkArg
import ru.netology.nework.util.BundleArguments.Companion.mentionsCountArg
import ru.netology.nework.util.BundleArguments.Companion.textArg
import ru.netology.nework.util.BundleArguments.Companion.userId
import ru.netology.nework.viewmodel.*
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    val postViewModel: PostViewModel by activityViewModels()

    val eventViewModel: EventViewModel by activityViewModels()

    val jobViewModel: JobViewModel by activityViewModels()

    private val usersViewModel: UsersViewModel by activityViewModels()

    val authViewModel: AuthViewModel by viewModels()

    @Inject
    lateinit var appAuth: AppAuth

    val mediaPlayer = MediaPlayer()

    private val interactionListenerEvent = object : OnInteractionListenerEvent {

        override fun onLike(event: EventResponse) {
            if (authViewModel.authenticated) {
                eventViewModel.likeById(event)
            } else {
                AlertDialog.Builder(context)
                    .setMessage(R.string.action_not_allowed)
                    .setPositiveButton(R.string.sign_up) { _, _ ->
                        findNavController().navigate(
                            R.id.action_profileFragment_to_authFragment,
                            Bundle().apply {
                                textArg = getString(R.string.sign_up)
                            }
                        )
                    }
                    .setNeutralButton(R.string.sign_in) { _, _ ->
                        findNavController().navigate(
                            R.id.action_profileFragment_to_authFragment,
                            Bundle().apply {
                                textArg = getString(R.string.sign_in)
                            }
                        )
                    }
                    .setNegativeButton(R.string.no, null)
                    .setCancelable(true)
                    .create()
                    .show()
            }
        }

        override fun onShare(event: EventResponse) {
            val intent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, event.content)
            }

            val shareIntent =
                Intent.createChooser(intent, getString(R.string.chooser_share_post))
            startActivity(shareIntent)
        }

        override fun onRemove(event: EventResponse) {
            eventViewModel.removeById(event.id)
        }

        override fun onEdit(event: EventResponse) {
            eventViewModel.edit(event)
            findNavController().navigate(R.id.action_profileFragment_to_newEventFragment)
        }

        override fun onPlayPost(event: EventResponse, videoView: VideoView?) {
            if (event.attachment?.type == AttachmentType.VIDEO) {
                videoView?.isVisible = true
                val uri = Uri.parse(event.attachment.url)
                videoView?.apply {
                    setMediaController(MediaController(requireContext()))
                    setVideoURI(uri)
                    setOnPreparedListener {
                        videoView.layoutParams?.height =
                            (resources.displayMetrics.widthPixels * (it.videoHeight.toDouble() / it.videoWidth)).toInt()
                        start()
                    }
                    setOnCompletionListener {

                        if (videoView.layoutParams?.width != null) {
                            videoView.layoutParams?.width = resources.displayMetrics.widthPixels
                            videoView.layoutParams?.height =
                                (videoView.layoutParams?.width!! * 0.5625).toInt()
                        }
                        stopPlayback()

                    }

                }
            }
            if (event.attachment?.type == AttachmentType.AUDIO) {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                } else {
                    mediaPlayer.reset()
                    mediaPlayer.setDataSource(event.attachment.url)
                    mediaPlayer.prepare()
                    mediaPlayer.start()
                }
            }
        }

        override fun onLink(event: EventResponse) {
            val intent =
                if (event.link?.contains("https://") == true || event.link?.contains("http://") == true) {
                    Intent(Intent.ACTION_VIEW, Uri.parse(event.link))
                } else {
                    Intent(Intent.ACTION_VIEW, Uri.parse("http://${event.link}"))
                }
            startActivity(intent)
        }

        override fun onPreviewAttachment(event: EventResponse) {
            findNavController().navigate(
                R.id.action_profileFragment_to_viewImageAttach,
                Bundle().apply {
                    textArg = event.attachment?.url
                })
        }

        override fun onSpeakersAction(event: EventResponse) {
            if (event.speakerIds.isNotEmpty()) {
                findNavController().navigate(
                    R.id.action_profileFragment_to_bottomSheetFragment,
                    Bundle().apply {
                        eventId = event.id
                        eventRequestType = "speakers"
                    }
                )
            } else {
                Toast.makeText(requireContext(), R.string.not_value_event, Toast.LENGTH_SHORT)
                    .show()
            }

        }

        override fun onPartyAction(event: EventResponse) {
            if (event.participantsIds.isNotEmpty()) {
                findNavController().navigate(
                    R.id.action_profileFragment_to_bottomSheetFragment,
                    Bundle().apply {
                        eventId = event.id
                        eventRequestType = "party"
                    }
                )
            } else {
                Toast.makeText(requireContext(), R.string.not_value_event, Toast.LENGTH_SHORT)
                    .show()
            }
        }

        override fun onJoinAction(event: EventResponse) {
            eventViewModel.joinById(event)
        }
    }

    private val interactionListenerPost = object : OnInteractionListener {

        override fun onLike(post: Post) {
            if (authViewModel.authenticated) {
                postViewModel.likeById(post)
            } else {
                AlertDialog.Builder(context)
                    .setMessage(R.string.action_not_allowed)
                    .setPositiveButton(R.string.sign_up) { _, _ ->
                        findNavController().navigate(
                            R.id.action_profileFragment_to_authFragment,
                            Bundle().apply {
                                textArg = getString(R.string.sign_up)
                            }
                        )
                    }
                    .setNeutralButton(R.string.sign_in) { _, _ ->
                        findNavController().navigate(
                            R.id.action_profileFragment_to_authFragment,
                            Bundle().apply {
                                textArg = getString(R.string.sign_in)
                            }
                        )
                    }
                    .setNegativeButton(R.string.no, null)
                    .setCancelable(true)
                    .create()
                    .show()
            }
        }

        override fun onShare(post: Post) {
            val intent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, post.content)
            }

            val shareIntent =
                Intent.createChooser(intent, getString(R.string.chooser_share_post))
            startActivity(shareIntent)
        }

        override fun onRemove(post: Post) {
            postViewModel.removeById(post.id)
        }

        override fun onEdit(post: Post) {
            postViewModel.edit(post)
            findNavController().navigate(
                R.id.action_profileFragment_to_newPostFragment,
                Bundle().apply {
                    textArg = post.content
                    linkArg = post.link
                    mentionsCountArg = post.mentionIds?.size?.toLong() ?: 0L
                })

        }

        override fun onPlayPost(post: Post, videoView: VideoView?) {
            if (post.attachment?.type == AttachmentType.VIDEO) {
                videoView?.isVisible = true
                val uri = Uri.parse(post.attachment.url)
                videoView?.apply {
                    setMediaController(MediaController(requireContext()))
                    setVideoURI(uri)
                    setOnPreparedListener {
                        videoView.layoutParams?.height =
                            (resources.displayMetrics.widthPixels * (it.videoHeight.toDouble() / it.videoWidth)).toInt()
                        start()
                    }
                    setOnCompletionListener {

                        if (videoView.layoutParams?.width != null) {
                            videoView.layoutParams?.width = resources.displayMetrics.widthPixels
                            videoView.layoutParams?.height =
                                (videoView.layoutParams?.width!! * 0.5625).toInt()
                        }
                        stopPlayback()

                    }

                }
            }
            if (post.attachment?.type == AttachmentType.AUDIO) {
                mediaPlayer.reset()
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                } else {
                    mediaPlayer.setDataSource(post.attachment.url)
                    mediaPlayer.prepare()
                    mediaPlayer.start()
                }
            }
        }

        override fun onLink(post: Post) {
            val intent =
                if (post.link?.contains("https://") == true || post.link?.contains("http://") == true) {
                    Intent(Intent.ACTION_VIEW, Uri.parse(post.link))
                } else {
                    Intent(Intent.ACTION_VIEW, Uri.parse("http://${post.link}"))
                }
            startActivity(intent)
        }

        override fun onPreviewAttachment(post: Post) {
            findNavController().navigate(
                R.id.action_profileFragment_to_viewImageAttach,
                Bundle().apply {
                    textArg = post.attachment?.url
                })
        }
    }

    private val interactionListenerJob = object : OnInteractionListenerJob {

        override fun onRemove(job: Job) {
            jobViewModel.removeById(job.id)
        }

        override fun onEdit(job: Job) {
            jobViewModel.edit(job)
            findNavController().navigate(R.id.action_profileFragment_to_newJobFragment)
        }

        override fun onLink(job: Job) {
            val intent =
                if (job.link?.contains("https://") == true || job.link?.contains("http://") == true) {
                    Intent(Intent.ACTION_VIEW, Uri.parse(job.link))
                } else {
                    Intent(Intent.ACTION_VIEW, Uri.parse("http://${job.link}"))
                }
            startActivity(intent)
        }

        override fun myOrNo(job: Job): Boolean {
            return job.ownerId == appAuth.authStateFlow.value.id
        }
    }

    private lateinit var binding: FragmentProfileBinding
    private lateinit var adapterJob: JobAdapter
    private lateinit var adapterEvent: EventAdapter
    private lateinit var adapterPost: PostAdapter

    private lateinit var user: User

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentProfileBinding.inflate(layoutInflater)
        adapterJob = JobAdapter(interactionListenerJob)
        adapterEvent = EventAdapter(interactionListenerEvent)
        adapterPost = PostAdapter(interactionListenerPost)

        binding.listJob.adapter = adapterJob
        binding.listEvent.adapter = adapterEvent
        binding.listPosts.adapter = adapterPost

        lifecycleScope.launchWhenCreated {
            usersViewModel.dataUsersList.collectLatest { listUsers ->
                user = listUsers.find {
                    it.id == (arguments?.userId ?: appAuth.authStateFlow.value.id)
                }
                    ?: emptyUser
                Glide.with(binding.avatar)
                    .load(user.avatar)
                    .placeholder(R.drawable.ic_image_not_supported_24)
                    .error(R.drawable.ic_not_avatars_24)
                    .circleCrop()
                    .timeout(10_000)
                    .into(binding.avatar)

                if (user.id<0){
                    binding.idUser.text = getString(R.string.not_auth)
                }
                else{
                    binding.idUser.text = user.id.toString()
                }
                binding.nameUser.text = user.name
                binding.loginUser.text = user.login
            }
        }

        if (arguments?.userId != null) {
            jobViewModel.loadUserJobs(arguments?.userId!!)
        } else {
            jobViewModel.loadMyJobs()
        }


        lifecycleScope.launchWhenCreated {
            jobViewModel.data.collectLatest {
                adapterJob.submitList(it.filter { job ->
                    job.ownerId == (arguments?.userId ?: appAuth.authStateFlow.value.id)
                })
            }
            binding.jobTitle.text = if (adapterJob.itemCount == 0) {
                getString(R.string.no_jobs)
            } else {
                getString(R.string.job_description)

            }
        }

        lifecycleScope.launchWhenCreated {
            eventViewModel.data.collectLatest {
                adapterEvent.submitList(it.filter { event ->
                    event.speakerIds.contains((arguments?.userId ?: appAuth.authStateFlow.value.id))
                            || event.participantsIds.contains(
                        (arguments?.userId ?: appAuth.authStateFlow.value.id)
                    )
                })
                binding.eventTitle.text = if (adapterEvent.itemCount == 0) {
                    getString(R.string.no_events)
                } else {
                    getString(R.string.event_description)
                }
            }
        }

        postViewModel.data.observe(viewLifecycleOwner) {
            adapterPost.submitList(it.posts.filter { post ->
                post.mentionIds?.contains(
                    (arguments?.userId ?: appAuth.authStateFlow.value.id)
                ) ?: false
                        || post.authorId == (arguments?.userId ?: appAuth.authStateFlow.value.id)
            })
            binding.postTitle.text = if (adapterPost.itemCount == 0) {
                getString(R.string.no_posts)
            } else {
                getString(R.string.post_description)
            }
        }

        binding.addJob.isVisible =
            (arguments?.userId == appAuth.authStateFlow.value.id || arguments?.userId == null)
        binding.mainNavView.selectedItemId = R.id.navigation_profile


        var menuProvider: MenuProvider? = null

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
                                R.id.action_feedFragment_to_authFragment,
                                Bundle().apply {
                                    textArg = getString(R.string.sign_in)
                                }
                            )
                            true
                        }

                        R.id.signup -> {
                            findNavController().navigate(
                                R.id.action_feedFragment_to_authFragment,
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
                            arguments?.userId?.let {
                                findNavController().navigate(R.id.action_profileFragment_self)
                            }
                            true
                        }
                        else -> false
                    }
                }
            }.apply {
                menuProvider = this
            }, viewLifecycleOwner)
        }
        binding.mainNavView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navigation_posts -> {
                    findNavController().navigate(R.id.action_profileFragment_to_feedFragment)
                    true
                }

                R.id.navigation_events -> {
                    findNavController().navigate(R.id.action_profileFragment_to_feedFragment)
                    findNavController().navigate(R.id.action_feedFragment_to_eventsFragment)
                    true
                }

                R.id.navigation_users -> {
                    findNavController().navigate(R.id.action_profileFragment_to_feedFragment)
                    findNavController().navigate(R.id.action_feedFragment_to_usersFragment)
                    true
                }



                else -> false
            }
        }

        binding.addJob.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_newJobFragment)
        }

        return binding.root
    }
}