package com.arvind.gdriveapp.utils

interface IPreferenceHelper {
    fun setFolderID(folderID: String)
    fun getFolderId(): String
    fun setFileName(filename: String)
    fun getFileName(): String


    fun clearPrefs()
}