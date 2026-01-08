package com.refreshme

import com.refreshme.data.Stylist
import com.refreshme.data.StylistRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class StylistProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: StylistProfileViewModel
    private val repository: StylistRepository = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = StylistProfileViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `fetchStylist updates stylist state flow`() = runTest {
        // Given
        val stylistId = "123"
        val stylist = Stylist(id = stylistId, name = "Test Stylist")
        coEvery { repository.getStylist(stylistId) } returns stylist

        // When
        viewModel.fetchStylist(stylistId)
        testDispatcher.scheduler.advanceUntilIdle() // Advances the coroutine execution

        // Then
        val result = viewModel.stylist.first()
        assertEquals(stylist, result)
    }
}