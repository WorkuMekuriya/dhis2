package org.dhis2.helper

import android.annotation.SuppressLint
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import org.json.JSONObject

class Middleware(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME =
            "dhis-immunization-k8s-sandboxaddis-com_Test_unencrypted_db"
        private const val DATABASE_VERSION = 1
    }

    override fun onCreate(db: SQLiteDatabase?) {

    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {}

    @SuppressLint("Range")
    fun fetchData(eventUID: String): List<Data> {
        val userList = mutableListOf<Data>()
        val db = readableDatabase
        val cursor = db.rawQuery(
//            "SELECT d.formName, tedv.value FROM DataElement d JOIN TrackedEntityDataValue tedv ON tedv.dataelement = d.uid WHERE tedv.event = $eventUID AND d.uid IN (SELECT dataElement FROM ProgramStageDataElement WHERE programStage IN (SELECT programStage FROM Event WHERE uid = $eventUID));",
            "SELECT * FROM Event WHERE uid = $eventUID", null
        )
        try {
            if (cursor.moveToFirst()) {
                do {
                    val formName = cursor.getString(cursor.getColumnIndex("formName"))
                    val value = cursor.getString(cursor.getColumnIndex("value"))
                    val user = Data(formName, value)
                    userList.add(user)
                } while (cursor.moveToNext())
            }
        } catch (e: SQLiteException) {
            Log.e("DatabaseHelper", "getAllUsers: ${e.message}")
            // handle the exception here, e.g. by showing an error dialog to the user
        } finally {
            cursor.close()
        }
        return userList
    }

    fun convertEventToJson(event: JSONObject): JSONObject {
        val json = JSONObject()
//        try {
//            json.put("eventName", event.name)
//        } catch (e: JSONException) {
//            e.printStackTrace()
//        }
        return json
    }

    fun convertAnotherEventToJson(event: JSONObject): JSONObject {
        val json = JSONObject()
//        try {
//            json.put("eventName", event.name)
//        } catch (e: JSONException) {
//            e.printStackTrace()
//        }
        return json
    }
}
