package com.music.vivi.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing the state and logic of the Swipe Mode feature.
 * It handles fetching an infinite stream of songs (similar to a radio) and processing
 * user interactions such as liking (swiping right) and disliking (swiping left).
 */
class SwipeViewModel : ViewModel() {

    // Internal mutable state flow to hold the current stack of songs.
    private val _swipeStack = MutableStateFlow<List<SongItem>>(emptyList())
    
    /**
     * Publicly exposed state flow of the song stack.
     * The UI observes this to render the swipeable cards.
     */
    val swipeStack: StateFlow<List<SongItem>> = _swipeStack

    init {
        loadInitialData()
    }

    private var continuation: String? = null
    private var isLoading = false

    // Internal mutable state flow for error messages
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    /**
     * Loads the initial batch of songs for the swipe session.
     * Currently configured to fetch "My Supermix" (or a similar radio) to provide
     * a personalized starting point.
     */
    private fun loadInitialData() {
        viewModelScope.launch {
            _errorMessage.value = null
            val result = YouTube.next(
                WatchEndpoint(
                    playlistId = "RDTMAK5uy_kset8DisdE7LSD4TNjEVvrKRTmG7a56sY", // 'My Supermix' typical ID
                    params = "wAEB" // Radio params
                )
            )
            
            result.onSuccess { nextResult ->
                continuation = nextResult.continuation
                _swipeStack.value = nextResult.items
            }.onFailure {
                it.printStackTrace()
                _errorMessage.value = "Failed to load music: ${it.message}"
            }
        }
    }

    /**
     * Handles the "Like" action (Swipe Right).
     * Sends a like request to the YouTube API for the given song and removes it from the stack.
     *
     * @param song The song item that was liked.
     */
    fun swipeRight(song: SongItem) {
        viewModelScope.launch {
            YouTube.like(song.id)
            removeTopCard()
        }
    }

    /**
     * Handles the "Dislike" action (Swipe Left).
     * Sends a dislike request to the YouTube API for the given song and removes it from the stack.
     *
     * @param song The song item that was disliked.
     */
    fun swipeLeft(song: SongItem) {
        viewModelScope.launch {
            YouTube.dislike(song.id)
            removeTopCard()
        }
    }

    /**
     * Removes the top card from the stack.
     * Automatically triggers loading of more songs if the stack size falls below a threshold (5 items).
     */
    private fun removeTopCard() {
        _swipeStack.value = _swipeStack.value.drop(1)
        if (_swipeStack.value.size < 5) {
            loadMore()
        }
    }

    /**
     * Fetches more songs from the YouTube API using the continuation token.
     * Ensures an infinite stream of music by appending new items to the stack.
     * Prevents multiple simultaneous load requests.
     */
    private fun loadMore() {
        if (isLoading || continuation == null) return
        isLoading = true
         viewModelScope.launch {
             YouTube.next(WatchEndpoint(videoId = null), continuation = continuation).onSuccess { nextResult ->
                 continuation = nextResult.continuation
                 // Append new items to the existing list
                 _swipeStack.value += nextResult.items
                 isLoading = false
             }.onFailure {
                 isLoading = false
                 _errorMessage.value = "Failed to load more songs: ${it.message}"
             }
         }
    }
    
    // Test helper
    fun setStackForTest(songs: List<SongItem>) {
        _swipeStack.value = songs
    }
}
