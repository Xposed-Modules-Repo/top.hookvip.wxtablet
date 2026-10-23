package top.hookvip.wxtablet.utils

import io.fastkv.FastKV

class FastKv(id: String = "WeChatTablet") {
    private val kv by lazy { FastKV.Builder(storePath, id).build() }

    init {
        check(storePath.isNotEmpty()) { "FastKv.initialize(path) must be called first" }
    }

    companion object {
        private var storePath = ""

        @JvmStatic
        fun initialize(path: String) {
            storePath = path
        }
    }

    fun getString(key: String): String? = kv.getString(key)

    fun putString(key: String, value: String) {
        kv.putString(key, value)
    }

    fun remove(key: String) {
        kv.remove(key)
    }
}
