package com.arvind.gdriveapp.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.LENGTH_SHORT
import com.arvind.gdriveapp.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.*
import java.text.SimpleDateFormat
import java.util.*


class GoogleDriveUtils(private val context: Context) {
    private var appFolderId: String = ""

    companion object {
        var driveFilePathId: String = ""
        const val RC_SIGN_IN = 101
    }

    private val preferenceHelper: IPreferenceHelper by lazy { PreferenceManager(context) }

    fun initializeGoogleClient() {

        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE), Scope(DriveScopes.DRIVE_APPDATA))
            .build()
        val client = GoogleSignIn.getClient(context, signInOptions)

        (context as? Activity)?.startActivityForResult(
            client.signInIntent,
           RC_SIGN_IN
        )

    }

    fun googleDriverIntegrate() {
        GoogleSignIn.getLastSignedInAccount(context)?.let { googleAccount ->
            val credential = GoogleAccountCredential.usingOAuth2(
                context, listOf(DriveScopes.DRIVE_FILE)
            )
            credential.selectedAccount = googleAccount.account!!

        }
    }

    fun getDriveService(): Drive? {
        GoogleSignIn.getLastSignedInAccount(context)?.let { googleAccount ->
            val credential = GoogleAccountCredential.usingOAuth2(
                context, listOf(DriveScopes.DRIVE_FILE)
            )

            credential.selectedAccount = googleAccount.account!!
            return Drive.Builder(
                AndroidHttp.newCompatibleTransport(),
                JacksonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName(context.getString(R.string.app_name))
                .build()

        }
        return null
    }


    fun accessDriveFiles() {
        getDriveService()?.let { googleDriveService ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    var pageToken: String?
                    do {
                        val result = googleDriveService.files().list().apply {
                            spaces = "drive"
                            fields = "nextPageToken, files(id, name)"
                            pageToken = this.pageToken
                        }.execute()

                        result.files.forEach { file ->
                            Log.d("FILE", ("name=${file.name} id=${file.id}"))
                            preferenceHelper.setFileName(file.id)

                        }
                    } while (pageToken != null)
                } catch (e: GoogleJsonResponseException) {
                    e.printStackTrace()
                    Log.e("Unable to create file:", "${e.details}")
                }

            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    fun uploadAppDataFolder(path: String?, sufix: String?): String {
        getDriveService()?.let { googleDriveService ->
            try {
                // File's metadata.
                val fileMetadata = com.google.api.services.drive.model.File()
                val formatter = SimpleDateFormat("yyyyMMddHHmmss")
                val dateString = formatter.format(Date())
                fileMetadata.name = dateString + sufix
                fileMetadata.parents = Collections.singletonList("appDataFolder")
                val filePath = File("$path")
                val mediaContent = FileContent("image/jpeg", filePath)
                val file = googleDriveService.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute()
                println("File ID: " + file.id)
                file.id
                driveFilePathId = file.id
                Log.e("driveFilePathId", driveFilePathId)
            } catch (e: GoogleJsonResponseException) {
                e.printStackTrace()
                Log.e("Unable to create file:", "${e.details}")
            }
        } ?: Toast.makeText(context, "Please Log In first!", LENGTH_LONG).show()

        return driveFilePathId

    }

    suspend fun uploadFileToGDrive(path: String?, sufix: String?): String {
        getDriveService()?.let { googleDriveService ->
            try {
                if (path != null) {
                    Log.e("pathGD", path)
                }

                googleDriveService.fetchOrCreateAppFolder(
                    context.getString(R.string.application_folder),
                    preferenceHelper
                )

                val encryptedData = AESEncryption().encryptFile("$path")
                Log.e("encryptedData", encryptedData)

                val actualFile = File(encryptedData)
                if (!actualFile.exists()) error("File $actualFile not exists.")
                val gFile = com.google.api.services.drive.model.File()
                // gFile.name = actualFile.name
                val formatter = SimpleDateFormat("yyyyMMddHHmmss")
                val dateString = formatter.format(Date())

                gFile.name = dateString + sufix
                gFile.parents = listOf(preferenceHelper.getFolderId())
                Log.e("prefFolderID", preferenceHelper.getFolderId())
                val fileContent = FileContent("image/jpeg", actualFile)
                val create = googleDriveService.files().create(gFile, fileContent)
                    .setFields("id, parents")
                    .execute()
                driveFilePathId = create.id

            } catch (exception: Exception) {
                exception.printStackTrace()
            }
        } ?: Toast.makeText(context, "Please Log In first!", LENGTH_LONG).show()

        return driveFilePathId

    }


    fun downloadFileFromGDrive(id: String) {
        getDriveService()?.let { googleDriveService ->
            CoroutineScope(Dispatchers.IO).launch {
                Log.e("idDownload", id)
                try {
                    val gDriveFile = googleDriveService.Files().get(id).execute()
                    createDirectoryAndSaveImagePackage(gDriveFile.id)
                } catch (e: Exception) {
                    println("!!! Handle Exception $e")
                }

            }
        } ?: Toast.makeText(context, "Please Log In first!", LENGTH_SHORT).show()
    }

    private fun createDirectoryAndSaveImagePackage(id: String?) {
        getDriveService()?.let { googleDriveService ->
            CoroutineScope(Dispatchers.IO).launch {
                val file = File(context.filesDir, "${id}.jpg")
                Log.e("fileEncryptedDirGD", "$file")
                try {
                    val outputStream = FileOutputStream(file)
                    googleDriveService.files()[id]
                        .executeMediaAndDownloadTo(outputStream)
                    if (id != null) {
                        googleDriveService.readFile(id)
                    }
                    val decryptedDataDir = AESEncryption().decryptFile("$file")
                    Log.e("decryptedDataDir", decryptedDataDir)
                    outputStream.flush()
                    outputStream.close()

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

    }

    fun deleteFileFromGDrive(id: String) {
        getDriveService()?.let { googleDriveService ->
            CoroutineScope(Dispatchers.IO).launch {
                googleDriveService.Files().delete(id).execute()
            }
        } ?: Toast.makeText(context, "Please Log In first!", LENGTH_SHORT).show()
    }

    fun updateFileFromGDrive(id: String) {
        getDriveService()?.let { googleDriveService ->
            CoroutineScope(Dispatchers.IO).launch {
                googleDriveService.readFile(id)
            }
        } ?: Toast.makeText(context, "Please Log In first!", LENGTH_SHORT).show()
    }

}
