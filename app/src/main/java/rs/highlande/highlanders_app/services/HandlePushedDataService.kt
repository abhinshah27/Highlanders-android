/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.highlanders_app.services

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.os.Handler
import io.realm.Realm
import org.json.JSONArray
import rs.highlande.highlanders_app.models.HLPosts
import rs.highlande.highlanders_app.utility.Constants
import rs.highlande.highlanders_app.utility.LogUtils
import rs.highlande.highlanders_app.utility.realm.RealmUtils

/**
 * An [IntentService] subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
class HandlePushedDataService : IntentService(HandlePushedDataService::class.qualifiedName) {

    companion object {

        var mHandler: Handler? = null

        @JvmStatic
        fun startService(context: Context, handler: Handler, jsonString: String) {
            try {
                mHandler = handler
                context.startService(
                        Intent(context, HandlePushedDataService::class.java).apply { putExtra(Constants.EXTRA_PARAM_1, jsonString) }
                )
            } catch (e: IllegalStateException) {
                LogUtils.e(HandleChatsUpdateService.LOG_TAG, "Cannot start background service: " + e.message, e)
            }
        }
    }

    var deleteLambda = { jsonArray: JSONArray?, realm: Realm ->
        if (jsonArray != null && jsonArray.length() > 0) {
            val posts = HLPosts.getInstance()

            for (i in 0 until jsonArray.length()) {
                posts.deletePost(jsonArray.optString(i), realm, true)
            }
        }
    }

    var updateInsertLambda = { jsonArray: JSONArray?, realm: Realm ->
        if (jsonArray != null && jsonArray.length() > 0) {
            val posts = HLPosts.getInstance()
            posts.setPosts(jsonArray, realm, true)
        }
    }

    var updateHeartsLambda = { jsonArray: JSONArray? ->
        if (jsonArray != null && jsonArray.length() > 0) {
            val posts = HLPosts.getInstance()
            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.optJSONObject(i)
                if (json != null && json.length() > 0 && json.has("userID") && json.has("totHearts")) {
                    val uid = json.optString("userID")
                    val hearts = json.optInt("totHearts")
                    posts.updateAuthorHeartsForAllPosts(uid, hearts, -1)
                }
            }
        }
    }

    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) return

        val jsonArray =
                if (intent.hasExtra(Constants.EXTRA_PARAM_1)) JSONArray(intent.getStringExtra(Constants.EXTRA_PARAM_1))
                else JSONArray()

        if (jsonArray.length() > 0) {
            var realm: Realm? = null
            try {
                realm = RealmUtils.getCheckedRealm()

                val jsonObject = jsonArray.optJSONObject(0)
                if (jsonObject != null && jsonObject.length() > 0) {
                    var hasInsert = false
                    for (it in jsonObject.keys()) {
                        val newArray = jsonObject.optJSONArray(it)
                        when (it) {
                            "arrayDelete" -> deleteLambda(newArray, realm)
                            "arrayInsert" -> {
                                hasInsert = newArray.length() > 0
                                updateInsertLambda(newArray, realm)
                            }
                            "arrayUpdate" -> updateInsertLambda(newArray, realm)
                            "arrayUpdateHearts" -> updateHeartsLambda(newArray)
                        }
                    }

                    mHandler?.sendEmptyMessage( if (hasInsert) 1 else 0 )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                realm?.close()
            }
        }
    }

}
