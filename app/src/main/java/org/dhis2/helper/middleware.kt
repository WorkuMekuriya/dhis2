package org.dhis2.helper

import android.annotation.SuppressLint
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import androidx.compose.ui.text.toLowerCase
import com.google.gson.GsonBuilder
import java.util.*

class Middleware(context: Context, val type: String) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME =
            "dhis-immunization-k8s-sandboxaddis-com_Test_unencrypted.db"
        private const val DATABASE_VERSION = 135
    }

    override fun onCreate(db: SQLiteDatabase?) {

    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {}

    @SuppressLint("Range")
    fun fetchData(eventUID: String): String? {
        val dataList = mutableListOf<Data>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT d.formName as formName,d.shortName as name, tedv.value as value " + "FROM DataElement d " + "JOIN TrackedEntityDataValue tedv ON tedv.dataelement = d.uid " + "WHERE tedv.event = '$eventUID' AND d.uid " + "IN (SELECT dataElement FROM ProgramStageDataElement " + "WHERE programStage " + "IN (SELECT programStage FROM Event WHERE uid = '$eventUID'));",
            null
        )
        try {
            if (cursor.moveToFirst()) {
                do {
                    val Name = cursor.getString(cursor.getColumnIndex("name"))
                    val formName = cursor.getString(cursor.getColumnIndex("formName"))
                    val value = cursor.getString(cursor.getColumnIndex("value"))
                    val data = Data(Name,formName, value)
                    dataList.add(data)
                } while (cursor.moveToNext())
            }
        } catch (e: SQLiteException) {
            Log.e("DatabaseHelper", "getAllUsers: ${e.message}")
            // handle the exception here, e.g. by showing an error dialog to the user
        } finally {
            cursor.close()
        }
        val arrayJson = convertArrayToArrayJSON(dataList, type)
        return mapEventToJsonString(arrayJson, type)
    }

    private fun mapEventToJsonString(
        event: Map<String, Any>, typeOfVaccine: String
    ): String? {
        return when (typeOfVaccine) {
            "Routine" -> {
                val vaccinationMap = mapOf(
                    "nextAppointment" to event["Next Appointment"],
                    "dateGiven" to event["DateGiven"],
                    "typeOfVaccination" to typeOfVaccine,
                    //TODO: add other fields
                )
                val routineList = listOf(vaccinationMap)
                val routineMap = mapOf(type.toLowerCase() to routineList)
                GsonBuilder().setPrettyPrinting().create().toJson(routineMap)
            }
            else -> ""
        }
    }

    private fun convertArrayToArrayJSON(
        data: MutableList<Data>, typeOfVaccine: String
    ): Map<String, Any> {
        return when (typeOfVaccine) {
            "Routine" -> data.associate { it.formName to it.value }
            else -> emptyMap()
        }
    }
}