package top.hookvip.wxtablet.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Method

class MethodCacheTest {
    private val valid = String::class.java.getMethod("isEmpty")
    private val invalid = String::class.java.getMethod("length")
    private val matches: (Method) -> Boolean = { it.returnType == Boolean::class.javaPrimitiveType }

    @Test
    fun validCacheDoesNotScanOrWrite() {
        val method = resolveCachedMethod(
            cachedDescriptor = "cached",
            resolve = { valid },
            matches = matches,
            find = { error("A valid cache must not trigger a scan") },
            updateCache = { error("A valid cache must not be rewritten") },
        )

        assertEquals(valid, method)
    }

    @Test
    fun missingCachedMethodIsRemovedAndReplaced() {
        val writes = mutableListOf<String?>()
        var scans = 0
        val method = resolveCachedMethod(
            cachedDescriptor = "stale",
            resolve = { if (it == "stale") throw NoSuchMethodException(it) else valid },
            matches = matches,
            find = { scans++; "fresh" },
            updateCache = { writes.add(it) },
        )

        assertEquals(valid, method)
        assertEquals(1, scans)
        assertEquals(listOf(null, "fresh"), writes)
    }

    @Test
    fun changedCachedSignatureTriggersRescan() {
        val writes = mutableListOf<String?>()
        val method = resolveCachedMethod(
            cachedDescriptor = "old",
            resolve = { if (it == "old") invalid else valid },
            matches = matches,
            find = { "fresh" },
            updateCache = { writes.add(it) },
        )

        assertEquals(valid, method)
        assertEquals(listOf(null, "fresh"), writes)
    }

    @Test
    fun missingSearchResultDoesNotCacheFailure() {
        var wroteCache = false
        val method = resolveCachedMethod(
            cachedDescriptor = null,
            resolve = { error("There is no descriptor to resolve") },
            matches = matches,
            find = { null },
            updateCache = { wroteCache = true },
        )

        assertNull(method)
        assertFalse(wroteCache)
    }

    @Test
    fun invalidSearchResultIsNotPersisted() {
        var wroteCache = false
        val method = resolveCachedMethod(
            cachedDescriptor = null,
            resolve = { invalid },
            matches = matches,
            find = { "wrong-signature" },
            updateCache = { wroteCache = true },
        )

        assertNull(method)
        assertFalse(wroteCache)
    }

    @Test
    fun validSearchResultIsCached() {
        var wroteCache = false
        val method = resolveCachedMethod(
            cachedDescriptor = null,
            resolve = { valid },
            matches = matches,
            find = { "fresh" },
            updateCache = { wroteCache = it == "fresh" },
        )

        assertEquals(valid, method)
        assertTrue(wroteCache)
    }
}
