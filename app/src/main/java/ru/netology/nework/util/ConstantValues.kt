package ru.netology.nework.util

import ru.netology.nework.dto.*
import ru.netology.nework.model.MediaModel

object ConstantValues {
    const val POST_CONTENT = "content"
    const val POST_LINK = "link"
    const val POST_MENTIONS_COUNT = "count mentions in post"



    val emptyPost = Post(
        id = 0,
        authorId = 0,
        content = "",
        author = "",
        likeOwnerIds = emptyList(),
        countShared = 0,
        mentionIds = emptyList(),
        published = ""
    )

    val noPhoto = MediaModel()
}