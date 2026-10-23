package top.hookvip.wxtablet.utils

import java.lang.reflect.Method

internal fun resolveCachedMethod(
    cachedDescriptor: String?,
    resolve: (String) -> Method,
    matches: (Method) -> Boolean,
    find: () -> String?,
    updateCache: (String?) -> Unit,
): Method? {
    fun resolveIfValid(descriptor: String) = runCatching {
        resolve(descriptor).takeIf(matches)
    }.getOrNull()

    if (cachedDescriptor != null) {
        resolveIfValid(cachedDescriptor)?.let { return it }
        updateCache(null)
    }

    val descriptor = find() ?: return null
    val method = resolveIfValid(descriptor) ?: return null
    updateCache(descriptor)
    return method
}
