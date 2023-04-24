package org.dhis2.helper

import android.annotation.SuppressLint
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import com.google.gson.GsonBuilder
import timber.log.Timber
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
            "Immunization" -> {
                var cursor = db.rawQuery(
                    "SELECT d.formName as formName,d.shortName as name, tedv.value as value FROM DataElement d JOIN TrackedEntityDataValue tedv ON tedv.dataelement = d.uid WHERE tedv.event = '$eventUID' AND d.uid IN (SELECT dataElement FROM ProgramStageDataElement WHERE programStage IN (SELECT programStage FROM Event WHERE uid = '$eventUID'));",
                    null
                )
                val program = db.rawQuery(
                    "SELECT Program.name as name FROM Program JOIN Event ON Event.program = Program.uid WHERE Event.uid = '$eventUID'",
                    null
                )
                val programName = program.getString(0)

                if (programName == "COVAC - COVID-19 Vaccination Registry") {
                    cursor = db.rawQuery(
                        "SELECT d.displayName as formName,d.shortName as name, tedv.value as value FROM DataElement d JOIN TrackedEntityDataValue tedv ON tedv.dataelement = d.uid WHERE tedv.event = '$eventUID' AND d.uid IN (SELECT dataElement FROM ProgramStageDataElement WHERE programStage IN (SELECT programStage FROM Event WHERE uid = '$eventUID'));",
                        null
                    )
                }
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
                    Timber.e("getAllUsers: " + e.message)
                    // handle the exception here, e.g. by showing an error dialog to the user
                } finally {
                    cursor.close()
                }
                val arrayJson = convertArrayToArrayJSON(dataList, type)
                return mapEventToJsonString(arrayJson, type, programName)
            }

            "Demographic" -> {
                val cursor = db.rawQuery(
                    "SELECT TrackedEntityAttributeValue.value as value, TrackedEntityAttribute.formName as formName, OrganisationUnit.name as name FROM TrackedEntityAttributeValue INNER JOIN TrackedEntityAttribute ON TrackedEntityAttributeValue.trackedEntityAttribute = TrackedEntityAttribute.uid INNER JOIN Enrollment ON TrackedEntityAttributeValue.trackedEntityInstance = Enrollment.trackedEntityInstance INNER JOIN OrganisationUnit ON Enrollment.organisationUnit = OrganisationUnit.uid WHERE Enrollment.uid = '$eventUID'",
                    null
                )
                val program = db.rawQuery(
                    "SELECT Program.name as name FROM Program JOIN Enrollment ON Enrollment.program = Program.uid WHERE Enrollment.uid = '$eventUID'",
                    null
                )
                val programName = program.getString(0)
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
                    Timber.tag("DatabaseHelper").e("getAllUsers: %s", e.message)
                    // handle the exception here, e.g. by showing an error dialog to the user
                } finally {
                    cursor.close()
                }
                val arrayJson = convertArrayToArrayJSON(dataList, type)
                return mapEventToJsonString(arrayJson, type, programName)
            }

            else -> ""
        }
    }

    private fun mapEventToJsonString(
        event: Map<String, Any>, type: String, programName: String
    ): String? {
        return when (programName) {

            "Electronic Immunization Registry" -> {
                return when (type) {
                    "Immunization" -> {
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
                    "Demographic" -> {
                        val vaccinationMap = mapOf(
                            "idNumber" to event["Serial Number"],
                            "name" to mapOf(
                                "firstName" to event["Region"],
                                "middleName" to event["Region"],
                                "lastName" to event["Region"],
                            ),
                            "sex" to event["Serial Number"],
                            "dateOfBirth" to event["Serial Number"],
                            "passport" to event["Serial Number"],
                            "phoneNumber" to event["Serial Number"],
                            "occupation" to event["Serial Number"],
                            "address" to mapOf(
                                "region" to event["Region"],
                                "zone/sub-city" to event["Region"],
                                "woreda" to event["Region"],
                                "kebele/specific_area " to event["Region"],
                                "village/got" to event["Region"],
                                "houseNumber" to event["Region"],
                            ),
                            "healthFacility" to event["Serial Number"],
                        )
                        val demographicRoutineList = listOf(vaccinationMap)
                        val demographicRoutineMap =
                            mapOf(type.toLowerCase() to demographicRoutineList)
                        GsonBuilder().setPrettyPrinting().create().toJson(demographicRoutineMap)
                    }
                    else -> ""
                }

            }
            "COVAC - COVID-19 Vaccination Registry" -> {
                return when (type) {
                    "Covax" -> {
                        val vaccinationMap = mapOf(
                            "underlyingCondition" to false,
                            "chronicHeartDisease" to false,
                            "hypertension" to false,
                            "diabetes" to false,
                            "cancer" to false,
                            "HepaticFailure" to false,
                            "pulmonaryDiseases" to false,
                            "renalFailure" to false,
                            "autoimmuneDiseases" to false,
                            "morbidObesity" to false,
                            "HIV_AIDS" to false,
                            "mentalIllness" to false,
                            "underlyingConditionOther" to false,
                            "nameOfVaccinationPost_HF_IDP_RefugeeCamp" to "-",
                            "routine" to mapOf(
                                "vaccineName" to "-",
                                "batchNumber" to event["COVAC- Batch Number"],
                                "batchNumber2" to "-",
                                "batchNumber3" to "-",
                                "doseNumber" to event["COVAC - Dose Number"],
                                "totalDoses" to event["COVAC - Total doses"],
                                "suggestedDateForNextDose" to event["COVAC Suggested date for next dose"],
                                "vaccineDate1" to "-",
                                "vaccineDate2" to "-",
                                "vaccineDate3" to "-",
                                "allergicReactionAfterFirstDose" to false,
                                "AEFIsPresent" to false
                            ),
                        )
                        val routineList = listOf(vaccinationMap)
                        val routineMap = mapOf(type.toLowerCase() to routineList)
                        GsonBuilder().setPrettyPrinting().create().toJson(routineMap)
                    }
                    "Demographic" -> {
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
                        val demographicRoutineMap =
                            mapOf(type.toLowerCase() to demographicRoutineList)
                        GsonBuilder().setPrettyPrinting().create().toJson(demographicRoutineMap)
                    }
                    else -> ""
                }

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