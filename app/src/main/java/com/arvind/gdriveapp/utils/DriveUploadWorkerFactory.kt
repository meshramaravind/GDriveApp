package com.arvind.gdriveapp.utils

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.google.api.services.drive.Drive

class DriveUploadWorkerFactory(
    private val driveService: Drive
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {

        return when (workerClassName) {
            DriveUploadWorker::class.java.name ->
                DriveUploadWorker(appContext, workerParameters, driveService)
            else ->
                // Return null, so that the base class can delegate to the default WorkerFactory.
                null
        }
    }
}