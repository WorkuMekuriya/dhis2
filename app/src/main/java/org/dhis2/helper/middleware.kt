package org.dhis2.helper

import android.annotation.SuppressLint
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
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
    fun fetchData(eventUID: String, type: String): String? {
        val dataList = mutableListOf<Data>()
        val db = readableDatabase
        return when (type) {
            "Routine" -> {
                val cursor = db.rawQuery(
                    "SELECT d.formName as formName,d.shortName as name, tedv.value as value FROM DataElement d JOIN TrackedEntityDataValue tedv ON tedv.dataelement = d.uid WHERE tedv.event = '$eventUID' AND d.uid IN (SELECT dataElement FROM ProgramStageDataElement WHERE programStage IN (SELECT programStage FROM Event WHERE uid = '$eventUID'));",
                    null
                )
                try {
                    if (cursor.moveToFirst()) {
                        do {
                            val name = cursor.getString(cursor.getColumnIndex("name"))
                            val formName = cursor.getString(cursor.getColumnIndex("formName"))
                            val value = cursor.getString(cursor.getColumnIndex("value"))
                            val data = Data(name, formName, value)
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

            "DemographicRoutine" -> {
                val cursor = db.rawQuery(
                        "SELECT TrackedEntityAttributeValue.value as value, TrackedEntityAttribute.formName as formName, OrganisationUnit.name as name FROM TrackedEntityAttributeValue INNER JOIN TrackedEntityAttribute ON TrackedEntityAttributeValue.trackedEntityAttribute = TrackedEntityAttribute.uid INNER JOIN Enrollment ON TrackedEntityAttributeValue.trackedEntityInstance = Enrollment.trackedEntityInstance INNER JOIN OrganisationUnit ON Enrollment.organisationUnit = OrganisationUnit.uid WHERE Enrollment.uid = '$eventUID'",
                        null
                )
                try {
                    if (cursor.moveToFirst()) {
                        do {
                            val formName = cursor.getString(cursor.getColumnIndex("formName"))
                            val value = cursor.getString(cursor.getColumnIndex("value"))
                            val name = cursor.getString(cursor.getColumnIndex("name"))
                            val data = Data(name, formName, value)
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

            else -> ""
        }
    }

    private fun mapEventToJsonString(
        event: Map<String, Any>, type: String
    ): String? {
        return when (type) {
            "Routine" -> {
                val vaccinationMap = mapOf(
                    "typeOfVaccination" to event["name"],
                    "childWeight" to 0,
                    "dateGiven" to event["DateGiven"],
                    "nextAppointment" to event["Next Appointment"],
                )
                val routineList = listOf(vaccinationMap)
                val routineMap = mapOf(type.toLowerCase() to routineList)
                GsonBuilder().setPrettyPrinting().create().toJson(routineMap)
            }

            "DemographicRoutine" -> {
                val vaccinationMap = mapOf(
                        "serialNumber" to event["Serial Number"],
                        "cardNo" to event["Unique System Identifier (EPI)"],
                        "nameOfInfant" to event["First Name"],
                        "nameOfMother" to event["Mother's Name"],
                        "nameOfBabysFather" to event["Middle name"],
                        "sex" to event["Sex"],
                        "dateOfBirth" to event["Date of Birth"],
                        "dateOfBirthOfMother" to "-",
                        "birthWeight/birthHeight" to event["Next Appointment"],
                        "parentPhoneNo" to event["Caregiver's contact number"],
                        "address" to mapOf(
                                "region" to event["Region"],
                                "zone" to event["Zone"],
                                "woreda" to event["Woreda"],
                                "kebele" to event["Kebele"],
                                "ketena/got" to "-",
                                "houseNumber" to event["House Number"],
                        ),
                        "healthFacility" to "Ministry of Health"
                )
                val demographicRoutineList = listOf(vaccinationMap)
                val demographicRoutineMap = mapOf(type.toLowerCase() to demographicRoutineList)
                GsonBuilder().setPrettyPrinting().create().toJson(demographicRoutineMap)
            }

            else -> ""
        }
    }

    private fun convertArrayToArrayJSON(
        data: MutableList<Data>, type: String
    ): Map<String, Any> {
        return when (type) {
            "Routine" -> data.associate { it.formName to it.value }
            "DemographicRoutine" -> data.associate { it.formName to it.value }
            else -> emptyMap()
        }
    }
}