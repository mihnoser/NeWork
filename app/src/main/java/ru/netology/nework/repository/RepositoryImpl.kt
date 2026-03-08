package ru.netology.nework.repository

import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okio.IOException
import ru.netology.nework.api.*
import ru.netology.nework.dao.*
import ru.netology.nework.dto.*
import ru.netology.nework.error.ApiError
import ru.netology.nework.error.AppError
import ru.netology.nework.error.NetworkError
import ru.netology.nework.error.UnknownError
import javax.inject.Inject


class RepositoryImpl @Inject constructor(
    private val dao: PostDaoRoom,
    private val apiService: ApiService,
) : Repository {

    private val newerPostsId = mutableListOf<Long>()

    override val data = dao.getAll()
        .map(List<PostEntity>::toDto)
        .flowOn(Dispatchers.Default)

    override val dataUsers = dao.getUsers()
        .map(List<UserEntity>::toDto)
        .flowOn(Dispatchers.Default)

    override val dataEvents: Flow<List<EventResponse>> = dao.getAllEvents()
        .map(List<EventsEntity>::toDto)
        .flowOn(Dispatchers.Default)

    override val dataJobs: Flow<List<Job>> = dao.getJobs()
        .map(List<JobEntity>::toDto)
        .flowOn(Dispatchers.Default)


    override fun getNewerCount(id: Long): Flow<Int> = flow {
        while (true) {
            delay(10_000L)
            val response = apiService.getNewer(id)
            if (!response.isSuccessful) {
                throw ApiError(response.message())
            }

            val body = response.body() ?: throw ApiError(response.message())
            dao.insert(body.toEntity(isNew = true))
            body.forEach {
                newerPostsId.add(it.id)
            }
            emit(body.size)
        }
    }

    override suspend fun showNewPosts() {
        dao.showNewPosts()
    }

    override suspend fun getAllAsync() {

        try {
            val response = apiService.getAll()

            if (!response.isSuccessful) {
                throw ApiError(response.message())
            }

            val body = response.body() ?: throw ApiError(response.message())
            dao.insert(body.toEntity())
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun removeByIdAsync(id: Long) {
        try {
            val response = apiService.removeById(id)

            if (!response.isSuccessful) {
                throw ApiError(response.message())
            }
            dao.removeById(id)

        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun saveAsync(post: Post) {
        try {
            val postRequest = PostCreateRequest(
                post.id,
                post.content,
                post.coords,
                post.link,
                post.attachment,
                post.mentionIds
            )
            val response = apiService.save(postRequest)

            if (!response.isSuccessful) {
                throw ApiError(response.message())
            }
            val postResponse =
                response.body() ?: throw ApiError(response.message())
            val postDaoSaved = Post(
                id = postResponse.id,
                authorId = postResponse.authorId,
                author = postResponse.author,
                authorAvatar = postResponse.authorAvatar,
                content = postResponse.content,
                published = postResponse.published,
                likedByMe = postResponse.likedByMe,
                likeOwnerIds = postResponse.likeOwnerIds,
                attachment = postResponse.attachment,
                ownedByMe = postResponse.ownedByMe
            )
            dao.save(PostEntity.fromDto(postDaoSaved))
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun saveWithAttachment(
        post: Post,
        upload: MediaUpload,
        attachmentType: AttachmentType
    ) {
        try {
            val media = upload(upload)
            val postWithAttachment = post.copy(
                attachment =
                    Attachment(
                        url = media.url,
                        type = attachmentType,
                    )
            )
            saveAsync(postWithAttachment)
        } catch (e: AppError) {
            throw e
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun upload(upload: MediaUpload): MediaResponse {
        try {
            val media = MultipartBody.Part.createFormData(
                "file", upload.file.name, upload.file.asRequestBody()
            )

            val response = apiService.upload(media)
            if (!response.isSuccessful) {
                throw ApiError(response.message())
            }

            return response.body() ?: throw ApiError(response.message())
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }


    override suspend fun likeByIdAsync(post: Post) {
        dao.likeById(post.id)
        try {
            val response = if (post.likedByMe) {
                apiService.dislikeById(post.id)
            } else {
                apiService.likeById(post.id)

            }
            if (!response.isSuccessful) {
                throw ApiError(response.message())
            }
            val body = response.body() ?: throw ApiError(response.message())
            dao.insert(PostEntity.fromDto(body))
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun edit(post: Post) {
        saveAsync(post)
    }
}