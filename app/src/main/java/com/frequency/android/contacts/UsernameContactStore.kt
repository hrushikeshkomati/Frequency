package com.frequency.android.contacts

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

/** Local username-to-key contacts; usernames are not globally verified by this store. */
data class UsernameContact(val username: String, val key: String)

class UsernameContactStore(context: Context) {
    private val prefs = context.getSharedPreferences("frequency_username_contacts", Context.MODE_PRIVATE)

    fun contacts(): List<UsernameContact> = prefs.getString(KEY, null)?.let { encoded ->
        runCatching {
            val root = JSONArray(encoded)
            (0 until root.length()).mapNotNull {
                val item = root.getJSONObject(it)
                UsernameContact(item.getString("username"), item.getString("key"))
            }.sortedBy { it.username.lowercase() }
        }.getOrDefault(emptyList())
    } ?: emptyList()

    fun find(usernameOrKey: String): UsernameContact? {
        val query = usernameOrKey.trim().removePrefix("@").lowercase()
        return contacts().firstOrNull { it.username.lowercase() == query || it.key.equals(query, true) }
    }

    fun save(username: String, key: String) {
        val cleanUsername = username.trim().removePrefix("@").lowercase().filter { it.isLetterOrDigit() || it == '_' || it == '.' }
        require(cleanUsername.isNotEmpty()) { "Enter a username" }
        require(key.trim().isNotEmpty()) { "Enter a Frequency key" }
        val updated = contacts().filterNot { it.username.equals(cleanUsername, true) }.toMutableList()
        updated.add(UsernameContact(cleanUsername, key.trim()))
        val root = JSONArray()
        updated.forEach { contact ->
            root.put(JSONObject().put("username", contact.username).put("key", contact.key))
        }
        prefs.edit { putString(KEY, root.toString()) }
    }

    companion object { private const val KEY = "contacts" }
}
