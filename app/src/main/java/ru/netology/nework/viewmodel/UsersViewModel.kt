package ru.netology.nework.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import ru.netology.nework.dto.User
import ru.netology.nework.repository.Repository
import javax.inject.Inject

@HiltViewModel
class UsersViewModel @Inject constructor(
    application: Application,
    private val repository: Repository,
) : AndroidViewModel(application) {
    val dataUsersList
        get() = flow {
            while (true) {
                getData()
                emit(_dataUsersList)
                delay(1_000)
            }
        }

    private var _dataUsersList: List<User> = listOf()

    private fun getData() = viewModelScope.launch  {
        try {
            repository.getUsers()
        } catch (_: Exception) { }

        repository.dataUsers.collectLatest {
            _dataUsersList = it
        }
    }

}