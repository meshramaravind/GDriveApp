package com.arvind.gdriveapp.utils

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.api.services.drive.Drive
import kotlinx.coroutines.coroutineScope

class DriveUploadWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val driveService: Drive
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val fileName = inputData.getString(KEY_NAME_ARG)!!
        val contents = inputData.getString(KEY_CONTENTS_ARG)!!
        val folderId = inputData.getString(KEY_CONTENTS_FOLDER_ID)!!
        return coroutineScope {
//            val fileId = driveService.createFile(folderId, fileName)
//            driveService.saveFile(fileId, fileName, contents)
            Result.success()
        }
    }

    companion object {
        const val KEY_NAME_ARG = "name"
        const val KEY_CONTENTS_ARG = "contents"
        const val KEY_CONTENTS_FOLDER_ID = "folder_id"
    }
}