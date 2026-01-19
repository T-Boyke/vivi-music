package com.music.vivi.viewmodels

import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.innertube.models.WatchEndpoint
import com.music.innertube.pages.NextResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SwipeViewModelTest {

    private lateinit var viewModel: SwipeViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkObject(YouTube)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `initial load fetches quick picks or radio`() = runTest {
        // Given
        val mockSongs = listOf(
            SongItem(id = "1", title = "Song 1", artists = emptyList(), thumbnail = "thumb1"),
            SongItem(id = "2", title = "Song 2", artists = emptyList(), thumbnail = "thumb2")
        )
        // Mock a response for a "radio" or initial fetch.
        // For simplicity, let's assume valid start uses a specific endpoint or just QuickPicks logic if generic.
        // Or we can mock `YouTube.next` if we start a radio.
        
        // Let's assume the VM starts a radio based on a seed or generic.
        // For now, let's say it calls YouTube.next with a specific param or purely relies on HomeViewModel-like logic?
        // The plan said "Fetch initial Radio/Mix".
        // Let's mock `YouTube.next` returning a list of songs.
        val mockNextResult = mockk<NextResult>(relaxed = true)
        coEvery { mockNextResult.items } returns mockSongs
        coEvery { YouTube.next(any(), any()) } returns Result.success(mockNextResult)

        // When
        viewModel = SwipeViewModel()
        advanceUntilIdle()

        // Then
        assertEquals(mockSongs, viewModel.swipeStack.value)
    }

    @Test
    fun `swipe right triggers like and removes item from stack`() = runTest {
        // Given
        val songId = "123"
        val song = SongItem(id = songId, title = "Test Song", artists = emptyList(), thumbnail = "thumb")
        coEvery { YouTube.like(songId) } returns Result.success(Unit)
        
        viewModel = SwipeViewModel()
        viewModel.setStackForTest(listOf(song))

        // When
        viewModel.swipeRight(song)
        advanceUntilIdle()

        // Then
        coVerify { YouTube.like(songId) }
        assertEquals(emptyList<SongItem>(), viewModel.swipeStack.value)
    }

    @Test
    fun `swipe left triggers dislike and removes item from stack`() = runTest {
        // Given
        val songId = "456"
        val song = SongItem(id = songId, title = "Test Song", artists = emptyList(), thumbnail = "thumb")
        coEvery { YouTube.dislike(songId) } returns Result.success(Unit)

        viewModel = SwipeViewModel()
        viewModel.setStackForTest(listOf(song))

        // When
        viewModel.swipeLeft(song)
        advanceUntilIdle()

        // Then
        coVerify { YouTube.dislike(songId) }
        assertEquals(emptyList<SongItem>(), viewModel.swipeStack.value)
    }

    @Test
    fun `removing card triggers loadMore when stack is low`() = runTest {
        // Given: A stack with few items (less than 5) to trigger loadMore logic.
        // We need to simulate a valid continuation token first so loadMore actually tries to fetch.
        val song1 = SongItem(id = "1", title = "S1", artists = emptyList(), thumbnail = "")
        val song2 = SongItem(id = "2", title = "S2", artists = emptyList(), thumbnail = "")
        
        // Mock initial load to set continuation
        val initialResult = mockk<NextResult>(relaxed = true)
        coEvery { initialResult.items } returns listOf(song1, song2)
        coEvery { initialResult.continuation } returns "token"
        coEvery { YouTube.next(any<WatchEndpoint>(), null) } returns Result.success(initialResult)
        
        // Mock loadMore call
        val moreResult = mockk<NextResult>(relaxed = true)
        coEvery { moreResult.items } returns listOf(song1) // return something
        coEvery { YouTube.next(any<WatchEndpoint>(), "token") } returns Result.success(moreResult)
        
        viewModel = SwipeViewModel()
        advanceUntilIdle() // Process initial load
        
        // Now stack has 2 items. Removing one should trigger loadMore because 1 < 5.
        coEvery { YouTube.like("1") } returns Result.success(Unit)

        // When
        viewModel.swipeRight(song1)
        advanceUntilIdle()

        // Then
        coVerify { YouTube.next(any(), "token") }
    }

    @Test
    fun `initial load failure sets error message`() = runTest {
        // Given
        val errorMsg = "Network Error"
        coEvery { YouTube.next(any(), any()) } returns Result.failure(Exception(errorMsg))

        // When
        viewModel = SwipeViewModel()
        advanceUntilIdle()

        // Then
        assert(viewModel.errorMessage.value?.contains(errorMsg) == true)
    }
}
