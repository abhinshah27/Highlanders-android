/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.highlanders_app.utility

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import rs.highlande.highlanders_app.utility.caches.*
import rs.highlande.highlanders_app.utility.helpers.MediaHelper
import java.util.concurrent.LinkedBlockingQueue

/**
 * @author mbaldrighi on 11/15/2018.
 */
class DownloadReceiver: BroadcastReceiver() {

    var picturesCache: PicturesCache? = null
    var audioVideoCache: AudioVideoCache? = null

    companion object {

        private val LOG_TAG = DownloadReceiver::class.java.canonicalName

        var linkedBlockingQueue: LinkedBlockingQueue<Long> = LinkedBlockingQueue()
        var currentDownloads = mutableSetOf<String>()

        private var instance: DownloadReceiver? = null
        fun getInstance(): DownloadReceiver {
            if (instance == null)
                instance = DownloadReceiver()

            return instance as DownloadReceiver
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        try {
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0)
            val query = DownloadManager.Query()
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val enqueue = linkedBlockingQueue.take()
            query.setFilterById(enqueue)
            val cursor = downloadManager.query(query)
            cursor.moveToFirst()
            while (!cursor.isAfterLast) {
                val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    val downloadFileLocalUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))

                    val mime = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE))
                    val size = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))

                    if (picturesCache != null && mime.isPictureType())
                        picturesCache!!.addToMemoryCache(downloadFileLocalUri, size.toLong())
                    else if (audioVideoCache != null && (mime.isAudioType() || mime.isVideoType()))
                        audioVideoCache!!.addToMemoryCache(downloadFileLocalUri, size.toLong())

                    val fileName = MediaHelper.getFileNameFromPath(downloadFileLocalUri)
                    if (!fileName.isNullOrBlank())
                        currentDownloads.remove(fileName)

                    LogUtils.d(LOG_TAG, "Download SUCCESS: $downloadFileLocalUri")
                }
                cursor.moveToNext()
            }
        } catch (e: InterruptedException) {
            LogUtils.e(LOG_TAG, e.toString())
        }
    }

}