package com.yopers.renee.utils

import io.paperdb.Paper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Database {
    fun write(key: String, value: MutableMap<String, String>) {
        GlobalScope.launch(Dispatchers.Main) {
            push(key, value)
        }
    }

    fun read(key: String) {
        GlobalScope.launch(Dispatchers.Main) {
            pull(key)
        }
    }

    private suspend fun push(key: String, value: MutableMap<String, String>): Boolean {
        return withContext(Dispatchers.IO) {
            Paper.book().write(key, value)
            true
        }
    }

    suspend fun pull(key: String): Map<String, String> {
        return withContext(Dispatchers.IO) {
            Paper.book().read<Map<String, String>>(key)
        }
    }
}