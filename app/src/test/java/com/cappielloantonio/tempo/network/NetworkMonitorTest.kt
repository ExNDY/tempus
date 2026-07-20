package com.cappielloantonio.tempo.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkMonitorTest {

    @Test
    fun initialSuccessfulPingEmitsAvailableState() = runBlocking {
        var pingCount = 0
        val monitor = monitor(
            checker = {
                pingCount++
                true
            }
        )

        monitor.setSelectedServer("server")

        val states = collectStates(monitor, count = 1)

        assertEquals(listOf(ConnectionState(isNetworkAvailable = true, isServerAvailable = true)), states)
        assertEquals(1, pingCount)
    }

    @Test
    fun failedPingRetriesAndEmitsAvailableWhenServerReturns() = runBlocking {
        var pingCount = 0
        val monitor = monitor(
            retryIntervalMillis = 10L,
            checker = {
                pingCount++
                pingCount >= 2
            }
        )

        monitor.setSelectedServer("server")

        val states = collectStates(monitor, count = 2)

        assertEquals(
            listOf(
                ConnectionState(isNetworkAvailable = true, isServerAvailable = false),
                ConnectionState(isNetworkAvailable = true, isServerAvailable = true),
            ),
            states
        )
        assertEquals(2, pingCount)
    }

    @Test
    fun manualRefreshRetriesWithoutWaitingForRetryInterval() = runBlocking {
        var pingCount = 0
        val monitor = monitor(
            retryIntervalMillis = 5_000L,
            checker = {
                pingCount++
                pingCount >= 2
            }
        )
        val states = mutableListOf<ConnectionState>()

        monitor.setSelectedServer("server")

        val collection = launch {
            monitor.connectionState.take(2).toList(states)
        }
        waitUntil { states.isNotEmpty() }

        monitor.refresh()

        withTimeout(1_000L) {
            collection.join()
        }
        assertEquals(
            listOf(
                ConnectionState(isNetworkAvailable = true, isServerAvailable = false),
                ConnectionState(isNetworkAvailable = true, isServerAvailable = true),
            ),
            states
        )
        assertEquals(2, pingCount)
    }

    @Test
    fun noInternetDoesNotPingServer() = runBlocking {
        var pingCount = 0
        val network = MutableStateFlow(false)
        val monitor = monitor(
            systemNetworkFlow = network,
            checker = {
                pingCount++
                true
            }
        )

        monitor.setSelectedServer("server")

        val states = collectStates(monitor, count = 1)

        assertEquals(listOf(ConnectionState(isNetworkAvailable = false, isServerAvailable = false)), states)
        assertEquals(0, pingCount)
    }

    @Test
    fun blankServerDoesNotPingServer() = runBlocking {
        var pingCount = 0
        val monitor = monitor(
            checker = {
                pingCount++
                true
            }
        )

        monitor.setSelectedServer("")

        val states = collectStates(monitor, count = 1)

        assertEquals(listOf(ConnectionState(isNetworkAvailable = true, isServerAvailable = false)), states)
        assertEquals(0, pingCount)
    }

    private fun monitor(
        systemNetworkFlow: MutableStateFlow<Boolean> = MutableStateFlow(true),
        retryIntervalMillis: Long = 10L,
        checker: suspend (String) -> Boolean,
    ): NetworkMonitor {
        return NetworkMonitor(
            systemNetworkFlow = systemNetworkFlow,
            retryIntervalMillis = retryIntervalMillis,
            ioDispatcher = Dispatchers.Unconfined,
            serverReachabilityChecker = checker,
        )
    }

    private suspend fun collectStates(
        monitor: NetworkMonitor,
        count: Int,
    ): List<ConnectionState> = withTimeout(1_000L) {
        monitor.connectionState.take(count).toList()
    }

    private suspend fun waitUntil(predicate: () -> Boolean) {
        withTimeout(1_000L) {
            while (!predicate()) {
                delay(1L)
            }
        }
    }
}
