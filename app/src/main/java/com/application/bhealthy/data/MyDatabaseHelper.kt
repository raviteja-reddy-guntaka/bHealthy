package com.application.bhealthy.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns

class MyDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION){

    // Table contents are grouped together in an anonymous object.
    companion object VitalsInformation : BaseColumns {
        const val DATABASE_NAME = "health_monitoring"
        const val DATABASE_VERSION = 1

        const val TABLE_NAME = "vitalsInformation_0"

        const val COLUMN_NAME_VITALS_ID = "vitals_id"
        const val COLUMN_NAME_HEART_RATE = "heart_rate"
        const val COLUMN_NAME_RESPIRATORY_RATE = "respiratory_rate"
        const val COLUMN_NAME_NAUSEA = "nausea"
        const val COLUMN_NAME_HEADACHE = "headache"
        const val COLUMN_NAME_DIARRHEA = "diarrhea"
        const val COLUMN_NAME_SOAR_THROAT = "soarThroat"
        const val COLUMN_NAME_FEVER = "fever"
        const val COLUMN_NAME_MUSCLE_ACHE = "muscleAche"
        const val COLUMN_NAME_LOSS_OF_SMELL_OR_TASTE = "lossOfSmellOrTaste"
        const val COLUMN_NAME_COUGH = "cough"
        const val COLUMN_NAME_SHORTNESS_OF_BREATH = "shortnessOfBreath"
        const val COLUMN_NAME_FEELING_TIRED = "feelingTired"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTableQuery = "CREATE TABLE $TABLE_NAME (" +
                "$COLUMN_NAME_VITALS_ID TEXT PRIMARY KEY, " +
                "$COLUMN_NAME_HEART_RATE REAL," +
                "$COLUMN_NAME_RESPIRATORY_RATE REAL," +
                "$COLUMN_NAME_NAUSEA REAL," +
                "$COLUMN_NAME_HEADACHE REAL," +
                "$COLUMN_NAME_DIARRHEA REAL," +
                "$COLUMN_NAME_SOAR_THROAT REAL," +
                "$COLUMN_NAME_FEVER REAL," +
                "$COLUMN_NAME_MUSCLE_ACHE REAL," +
                "$COLUMN_NAME_LOSS_OF_SMELL_OR_TASTE REAL," +
                "$COLUMN_NAME_COUGH REAL," +
                "$COLUMN_NAME_SHORTNESS_OF_BREATH REAL," +
                "$COLUMN_NAME_FEELING_TIRED REAL)"
        db?.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

}