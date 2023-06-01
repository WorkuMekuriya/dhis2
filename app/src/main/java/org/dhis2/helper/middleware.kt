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
        private const val DATABASE_NAME = "196-191-212-226-8090_test1_unencrypted.db"
        private const val DATABASE_VERSION = 135
    }

    data class MyData(
        val type: String,
        val type2: String,
        val dataList: String?,
        val dataList2: String?,
        val majorType: String,
        val name: String? = null,
        val cardNo: String? = null,
        val phoneNo: String? = null,
        val password: String? = null,
    )

    override fun onCreate(db: SQLiteDatabase?) {

    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {}

    @SuppressLint("Range")
    fun fetchData(eventUID: String, type: String): Any? {
        val dataList = mutableListOf<Data>()
        val dataList2 = mutableListOf<Data>()
        val db = readableDatabase
        var Type: String? = null

        val program = db.rawQuery(
            "SELECT Program.name as name FROM Program JOIN Event ON Event.program = Program.uid WHERE Event.uid = '$eventUID'",
            null
        )
        val programName: String = if (program.moveToFirst()) {
            program.getString(program.getColumnIndex("name"))
        } else {
            ""
        }
        program.close()
        var cursor = if (programName == "COVAC - COVID-19 Vaccination Registry") {
            db.rawQuery(
                "SELECT d.displayName as formName,d.shortName as name, tedv.value as value FROM DataElement d JOIN TrackedEntityDataValue tedv ON tedv.dataelement = d.uid WHERE tedv.event = '$eventUID' AND d.uid IN (SELECT dataElement FROM ProgramStageDataElement WHERE programStage IN (SELECT programStage FROM Event WHERE uid = '$eventUID'));",
                null
            )
        }else {
             db.rawQuery(
                "SELECT d.formName as formName,d.shortName as name, tedv.value as value FROM DataElement d JOIN TrackedEntityDataValue tedv ON tedv.dataelement = d.uid WHERE tedv.event = '$eventUID' AND d.uid IN (SELECT dataElement FROM ProgramStageDataElement WHERE programStage IN (SELECT programStage FROM Event WHERE uid = '$eventUID'));",
                null
            )
        }
                var cursor2 = db.rawQuery(
                    "SELECT TrackedEntityAttributeValue.value AS value, TrackedEntityAttribute.formName AS formName, OrganisationUnit.name AS name\n" +
                            "FROM TrackedEntityAttributeValue\n" +
                            "INNER JOIN TrackedEntityAttribute ON TrackedEntityAttributeValue.trackedEntityAttribute = TrackedEntityAttribute.uid\n" +
                            "INNER JOIN Enrollment ON TrackedEntityAttributeValue.trackedEntityInstance = Enrollment.trackedEntityInstance\n" +
                            "INNER JOIN OrganisationUnit ON Enrollment.organisationUnit = OrganisationUnit.uid\n" +
                            "WHERE Enrollment.uid = (SELECT enrollment FROM Event WHERE uid = '$eventUID')",
                    null
                )



                Type = "Routine"


                Log.d("SHARED", "programName: $programName")


                var Vaccination_name: String? = null // declare the variable outside the loop

                try {
                    if (cursor.moveToFirst()) {
                        do {
                            val vaccination = cursor.getString(cursor.getColumnIndex("name"))
                            val formName =  cursor.getString(cursor.getColumnIndex("formName"))
                            val value = cursor.getString(cursor.getColumnIndex("value"))
                            Vaccination_name = vaccination.replace(formName.trim(), "").trim() // assign the value to the variable
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

                var attributeValue: String? = null

                try {
                    if (cursor2.moveToFirst()) {
                        do {
                            val value = cursor2.getString(cursor2.getColumnIndex("value"))
                            val formName = cursor2.getString(cursor2.getColumnIndex("formName"))
                            val name = cursor2.getString(cursor2.getColumnIndex("name"))
                            // Add the fetched values to your data structure or process them as needed
                            // For example, create a Data object and add it to the dataList
                            val data = Data(name, formName, value)
                            dataList2.add(data)
                        } while (cursor2.moveToNext())
                    }
                } catch (e: SQLiteException) {
                    Timber.e("fetchData: " + e.message)
                    // Handle the exception here
                } finally {
                    cursor2.close()
                }

                val arrayJson = convertArrayToArrayJSON(dataList, type)
                val arrayJson2 = convertArrayToArrayJSON(dataList2, type)
                return mapEventToJsonString(arrayJson, arrayJson2, type, programName, Vaccination_name)

    }

    private fun mapEventToJsonString(
        event: Map<String, Any>, event2: Map<String, Any>, type: String, programName: String, Vaccination_name: String? = null
    ): Any? {
        return when (programName) {
            "Electronic Immunization Registry" -> {
                val type1 = "Routine"
                val vaccinationMap1 = mapOf(
                    "typeOfVaccination" to Vaccination_name,
                    "childsweight" to 0, // replace with the actual value
                    "dateGiven" to event["Date Given"],
                    "nextAppointment" to (event["Next Appointment"] ?: "-")
                )
                val routineList = mutableListOf<Map<String, Any>>()
                routineList.add(vaccinationMap1 as Map<String, Any>)
                val routineMap = mapOf("routine1" to routineList)
                val jsonString1 = GsonBuilder().setPrettyPrinting().create().toJson(routineMap)
                val majorType = "Routine"

                val type2 = "RoutineDemographic"
                val firstName = event2["First name"]
                val fatherName = event2["Father's name/Middle name"]
                val name = "$firstName $fatherName"
                val cardNo = event2["National ID"]
                val phoneNo = event2["Mother/Caregiver's contact number"]
                val password = event2["PIN"]
                val vaccinationMap2 = mapOf(
                    "DemographicRoutine" to mapOf(
                        "serialNumber" to event2["Serial Number"],
                        "cardNo" to event2["National ID"],
                        "nameOfInfant" to event2["First name"],
                        "nameOfMother" to event2["Mother's Name"],
                        "nameOfBabysFather" to event2["Father's name/Middle name"],
                        "sex" to event2["Sex"],
                        "dateOfBirth" to event2["Date of birth (age)"],
                        "dateOfBirthOfMother" to "-",
                        "birthWeight/birthHeight" to 0,
                        "parentPhoneNo" to event2["Mother/Caregiver's contact number"],
                        "address" to mapOf(
                            "region" to event2["Region"],
                            "zone" to event2["Zone/Sub-city"],
                            "woreda" to event2["Woreda"],
                            "kebele" to event2["Woreda"],
                            "ketena/got" to event2["Village/Got"],
                            "houseNumber" to event2["House Number"],
                        ),
                        "healthFacility" to event2["Serial Number"],
                    )
                )
                val demographicRoutineList = listOf(vaccinationMap2)
                val firstList = demographicRoutineList[0]
                val jsonString2 = GsonBuilder().setPrettyPrinting().create().toJson(firstList)

                return MyData(
                    type1,
                    type2,
                    jsonString1,
                    jsonString2,
                    majorType,
                    name,
                    cardNo as String?,
                    phoneNo as String?,
                    password as String?,
                )
            }
            "COVAC - COVID-19 Vaccination Registry" -> {
                val type1 = "Covax"
                val vaccinationMap1 = mapOf(
                    "Covax" to mapOf(
                        "underlyingCondition_comorbidity" to false,
                        "chronicHeartDisease" to (event["COVAC - Chronic Heart Disease"] == "true"),
                        "hypertension" to (event["COVAC - Hepatic Failure"] == "true"),
                        "diabetes" to (event["COVAC - Diabetes"] == "true"),
                        "cancer" to (event["COVAC - Cancer"] == "true"),
                        "HepaticFailure" to (event["COVAC - Hepatic Failure"] == "true"),
                        "pulmonaryDiseases" to (event["COVAC - Pulmonary diseases"] == "true"),
                        "renalFailure" to (event["COVAC - Renal Failure"] == "true"),
                        "autoimmuneDiseases" to (event["COVAC - Autoimmune diseases"] == "true"),
                        "morbidObesity" to (event["COVAC - Morbid obesity"] == "true"),
                        "HIV_AIDS" to (event["COVAC - HIV/AIDS"] == "true"),
                        "mentalIllness" to (event["COVAC - Mental Illness"] == "true"),
                        "underlyingConditionOther" to (event["COVAC - Underlying condition Other"] == "true"),
                        "nameOfVaccinationPost_HF_IDP_RefugeeCamp" to "-",
                        "routine" to mapOf(
                            "vaccineName" to event["COVAC - Vaccine Name"],
                            "batchNumber1" to event["COVAC- Batch Number"],
                            "batchNumber2" to "-",
                            "batchNumber3" to "-",
                            "doseNumber" to 1,
                            "totalDoses" to (event["COVAC - Total doses"].toString().toIntOrNull()
                                ?: 0),
                            "suggestedDateForNextDose" to (event["COVAC Suggested date for next dose"]
                                ?: "-"),
                            "vaccineDate1" to (event["COVAC Suggested date for next dose"] ?: "-"),
                            "vaccineDate2" to "-",
                            "vaccineDate3" to "-",
                            "allergicReactionAfterFirstDose" to false,
                            "AEFIsPresent" to false
                        ),
                    )
                )
                val routineList = listOf(vaccinationMap1)
                val firstList = routineList[0]
                val jsonString1 = GsonBuilder().setPrettyPrinting().create().toJson(firstList)
                val majorType = "COVAX"

                val type2 = "CovaxDemographic"
                val firstName = event2["First name"]
                val middleName = event2["Father's name/Middle name"]
                val lastName = event2["Grandfather's name/Last name"]
                val name = "$firstName $middleName $lastName"
                val cardNo = event2["ID Number"]
                val phoneNo = event2["Mobile phone number"]
                val password = event2["PIN"]
                val vaccinationMap2 = mapOf(
                    "Demographic" to mapOf(
                        "idNumber" to event2["ID Number"].toString().toIntOrNull(),
                        "cardNo" to event2["Unique System Identifier (EPI)"],
                        "name" to mapOf(
                            "firstName" to event2["First name"],
                            "middleName" to event2["Father's name/Middle name"],
                            "lastName" to event2["Grandfather's name/Last name"]
                        ),
                        "sex" to event2["Sex"],
                        "dateOfBirth" to event2["Date of birth (age)"],
                        "passport" to event2["Passport"],
                        "phoneNumber" to event2["Mobile phone number"],
                        "occupation" to (event2["Occupation"] ?: "-"),
                        "address" to mapOf(
                            "region" to (event2["Region"] ?: "-"),
                            "zone/sub-city" to (event2["Zone"] ?: "-"),
                            "woreda" to (event2["Woreda"] ?: "-"),
                            "kebele/specific_area" to (event2["Village/Got"] ?: "-"),
                            "village/got" to (event2["Village/Got"] ?: "-"),
                            "houseNumber" to 0
                        ),
                        "healthFacility" to "Ministry of Health"
                    )
                )
                val demographicRoutineList = listOf(vaccinationMap2)
                val firstItem = demographicRoutineList[0]
                val jsonString2 = GsonBuilder().setPrettyPrinting().create().toJson(firstItem)

                return MyData(
                    type1,
                    type2,
                    jsonString1,
                    jsonString2,
                    majorType,
                    name,
                    cardNo as String?,
                    phoneNo as String?,
                    password as String?,
                )
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
