package com.example.animelist.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.animelist.data.network.AnimeApiService
import com.example.animelist.di.database.Anime
import com.example.animelist.di.database.AnimeDao
import com.example.animelist.toEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

// Лучше вынести в пакет с моделями
enum class AnimeApiStatus { LOADING, ERROR, DONE }

@HiltViewModel
class HomeViewModel @Inject constructor(
    val animeApi: AnimeApiService,
    val animeDao: AnimeDao,
) : ViewModel() {
    // Можно обойтись одной live data, создав вместо enum иерархию классов, которые будут уже у себя
    //  инкапсулировать статус, данные, позицию и проч.
    private val _animeList = MutableLiveData<MutableList<Anime>>(mutableListOf())
    private val _status = MutableLiveData<AnimeApiStatus>()

    var currentPage: Int = 1

    private var _anime = MutableLiveData<Anime?>()
    val anime: LiveData<Anime?> = _anime

    val status: LiveData<AnimeApiStatus> = _status
    val animeList: LiveData<MutableList<Anime>> = _animeList

    private var _query: String = ""

    init {
        getAnimeList()
    }

    fun clearAnime() {
        _anime.value = null
    }

    fun updateQuery(query: String) {
        currentPage = 1
        _query = query
        _animeList.value?.clear()
        getAnimeList()
    }

    fun nextPage() {
        currentPage++
        getAnimeList()
    }

    fun getAnime(malId: Int) {
        viewModelScope.launch {
            _status.value = AnimeApiStatus.LOADING
            // TODO возвращать из юз кейса статус + данные + убрать трай кэч
            try {
                val response = animeApi.getAnime(malId)
                _anime.value = response.data.toEntity()
                _status.value = AnimeApiStatus.DONE
            } catch (exception: Exception) {
                _status.value = AnimeApiStatus.ERROR
            }
        }
    }

    private fun getAnimeList() {
        // IO операции запускать на Dispatchers.IO
        viewModelScope.launch {
            _status.value = AnimeApiStatus.LOADING
            try {
                val response = animeApi.getAnimeList(currentPage, _query)
                addToList(response.data.map { it.toEntity() })
                _status.value = AnimeApiStatus.DONE
            } catch (e: Exception) {
                _status.value = AnimeApiStatus.ERROR
                addToList(listOf())
            }
        }
    }

    private fun addToList(items: Iterable<Anime>) {
        val currentList = _animeList.value
        currentList?.addAll(items)
        _animeList.value = currentList!!
    }
}