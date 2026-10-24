package com.conan.translator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

class NetworkTest {
    @Test fun timeoutBecomesTimeout() {
        assertSame(NetworkError.Timeout, convertException(SocketTimeoutException("Read timed out")))
    }

    @Test fun unknownHostRefusedOrNoRouteBecomesNoConnection() {
        for (e in listOf(UnknownHostException("x"), ConnectException("refused"), NoRouteToHostException("route"))) {
            assertSame(e.toString(), NetworkError.NoConnection, convertException(e))
        }
    }

    @Test fun tlsHandshakeFailureBecomesNoConnection() {
        assertSame(NetworkError.NoConnection, convertException(SSLHandshakeException("handshake failed")))
    }

    @Test fun connectionResetBecomesNoConnection() {
        assertSame(NetworkError.NoConnection, convertException(SocketException("Connection reset")))
    }

    @Test fun alreadyConvertedNetworkErrorPassesThrough() {
        val http = NetworkError.Http(503)
        assertSame(http, convertException(http))
    }

    @Test fun restBecomesOtherWithDetail() {
        val r = convertException(IOException("boom"))
        assertTrue(r is NetworkError.Other)
        assertEquals("boom", (r as NetworkError.Other).detail)
        assertEquals("IllegalStateException", (convertException(IllegalStateException()) as NetworkError.Other).detail)
    }
}
