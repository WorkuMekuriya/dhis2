package org.dhis2.helper

import android.annotation.SuppressLint
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.google.gson.GsonBuilder
import timber.log.Timber
import java.util.*

class Middleware(context: Context, val type: String) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME =
            "196-191-212-226-8090_test1_unencrypted.db"
        private const val DATABASE_VERSION = 135
    }
    data class MyData(val type: String, val dataList: String?)

    override fun onCreate(db: SQLiteDatabase?) {

    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {}

    @SuppressLint("Range")
    fun fetchData(eventUID: String, type: String): Any? {
        val dataList = mutableListOf<Data>()
        val db = readableDatabase
        var Type : String? = null
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
                Type = "Routine"
                val programName: String = if (program.moveToFirst()) {
                    program.getString(program.getColumnIndex("name"))
                } else {
                    ""
                }
                program.close()

                Log.d("SHARED", "programName: $programName")

                if (programName == "COVAC - COVID-19 Vaccination Registry") {
                    cursor = db.rawQuery(
                        "SELECT d.displayName as formName,d.shortName as name, tedv.value as value FROM DataElement d JOIN TrackedEntityDataValue tedv ON tedv.dataelement = d.uid WHERE tedv.event = '$eventUID' AND d.uid IN (SELECT dataElement FROM ProgramStageDataElement WHERE programStage IN (SELECT programStage FROM Event WHERE uid = '$eventUID'));",
                        null
                    )
                    Type = "Covax"

                }

                var Vaccination_name: String? = null // declare the variable outside the loop

                try {
                    if (cursor.moveToFirst()) {
                        do {
                            val vaccination = cursor.getString(cursor.getColumnIndex("name"))
                            val formName = cursor.getString(cursor.getColumnIndex("formName"))
                            val value = cursor.getString(cursor.getColumnIndex("value"))
                            Vaccination_name = vaccination.replace(formName, "").trim() // assign the value to the variable
                            Log.d("Vaccination", Vaccination_name)
                            val data = Data(Vaccination_name, formName, value)
                            dataList.add(data)

                            // Subtract the formName from the name to create the vaccination string

                        } while (cursor.moveToNext())
                    }
                } catch (e: SQLiteException) {
                    Timber.e("getAllUsers: " + e.message)
                    // handle the exception here, e.g. by showing an error dialog to the user
                } finally {
                    cursor.close()
                }

                val arrayJson = convertArrayToArrayJSON(dataList, type)
                val jsonString = mapEventToJsonString(arrayJson, type, programName, Vaccination_name)
                return MyData(Type, jsonString)
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
                Type = "RoutineDemographic"

                val programName: String = if (program.moveToFirst()) {
                    program.getString(program.getColumnIndex("name"))
                } else {
                    ""
                }
                program.close()
                if (programName == "COVAC - COVID-19 Vaccination Registry") {
                    Type = "CovaxDemographic"
                }
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
                val jsonString = mapEventToJsonString(arrayJson, type, programName)
                return MyData(Type, jsonString)
            }

            else -> ""
        }
    }

    private fun mapEventToJsonString(
        event: Map<String, Any>, type: String, programName: String, Vaccination_name: String? = null
    ): String? {
        return when (programName) {

            "Electronic Immunization Registry" -> {
                return when (type) {
                    "Immunization" -> {
                        val vaccinationMap = mapOf(
                            "typeOfVaccination" to Vaccination_name,
                            "childWeight" to 0, // replace with the actual value
                            "dateGiven" to event["Date Given"],
                            "nextAppointment" to (event["Next Appointment"] ?: "-")
                        )
                        val routineList = mutableListOf<Map<String, Any>>()
                        routineList.add(vaccinationMap as Map<String, Any>)
                        val routineMap = mapOf("routine1" to routineList)
                        GsonBuilder().setPrettyPrinting().create().toJson(routineMap)
                    }
                    "Demographic" -> {
                        val type1 = "DemographicRoutine"
                        val vaccinationMap = mapOf(
                            "DemographicRoutine" to mapOf(
                            "serialNumber" to event["Serial Number"],
                            "cardNo" to event["National ID"],
                                "nameOfInfant" to event["First name"],
                                "nameOfMother" to event["Mother's Name"],
                                "nameOfBabysFather" to event["Father's name/Middle name"],
                            "sex" to event["Sex"],
                            "dateOfBirth" to event["Date of birth (age)"],
                                "dateOfBirthOfMother" to "-",
                                "birthWeight/birthHeight" to 0,
                                "parentPhoneNo" to event["Mother/Caregiver's contact number"],
                            "address" to mapOf(
                                "region" to event["Region"],
                                "zone" to event["Zone/Sub-city"],
                                "woreda" to event["Woreda"],
                                "kebele" to event["Woreda"],
                                "ketena/got" to event["Village/Got"],
                                "houseNumber" to event["House Number"],
                            ),
                            "healthFacility" to event["Serial Number"],
                        )
                        )
                        val demographicRoutineList = listOf(vaccinationMap)
                        val firstList = demographicRoutineList[0]
                        GsonBuilder().setPrettyPrinting().create().toJson(firstList)
                    }
                    else -> ""
                }

            }
            "COVAC - COVID-19 Vaccination Registry" -> {
                return when (type) {
                    "Immunization" -> {
                        val vaccinationMap = mapOf(
                            "Covax" to mapOf(
                            "underlyingCondition_comorbidity" to false,
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
                                "batchNumber1" to event["COVAC- Batch Number"],
                                "batchNumber2" to "-",
                                "batchNumber3" to "-",
                                "doseNumber" to 1,
                                "totalDoses" to (event["COVAC - Total doses"].toString().toInt() ?: 0),
                                "suggestedDateForNextDose" to event["COVAC Suggested date for next dose"],
                                "vaccineDate1" to "-",
                                "vaccineDate2" to "-",
                                "vaccineDate3" to "-",
                                "allergicReactionAfterFirstDose" to false,
                                "AEFIsPresent" to false
                            ),
                        )
                        )
                        val routineList = listOf(vaccinationMap)
                        val firstList = routineList[0]
                        GsonBuilder().setPrettyPrinting().create().toJson(firstList)
                    }
                    "Demographic" -> {

                        val vaccinationMap = mapOf(
                            "Demographic" to mapOf(
                        "idNumber" to event["ID Number"].toString().toInt(),
                                "cardNo" to event["Unique System Identifier (EPI)"],
                                "name" to mapOf(
                                    "firstName" to event["First name"],
                                    "middleName" to event["Father's name/Middle name"],
                                    "lastName" to event["Grandfather's name/Last name"]
                                ),
                                "sex" to event["Sex"],
                                "dateOfBirth" to event["Date of birth (age)"],
                                "passport" to event["Passport"],
                                "phoneNumber" to event["Mobile phone number"],
                                "occupation" to (event["Occupation"] ?: "-"),
                                "address" to mapOf(
                                    "region" to (event["Region"] ?: "-"),
                                    "zone/sub-city" to (event["Zone"] ?: "-"),
                                    "woreda" to (event["Woreda"] ?: "-"),
                                    "kebele/specific_area" to (event["Village/Got"] ?: "-"),
                                    "village/got" to (event["Village/Got"] ?: "-"),
                                    "houseNumber" to event["House Number"].toString().toInt()
                                ),
                                "healthFacility" to "Ministry of Health"
                            )
                        )


                        val demographicRoutineList = listOf(vaccinationMap)
                        val firstItem =demographicRoutineList[0]
                        GsonBuilder().setPrettyPrinting().create().toJson(firstItem)
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
            "Immunization" -> data.associate { it.formName to it.value }
            "Demographic" -> data.associate { it.formName to it.value }
            else -> emptyMap()
        }
    }
}