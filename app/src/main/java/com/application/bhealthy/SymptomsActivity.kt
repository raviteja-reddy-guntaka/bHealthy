package com.application.bhealthy

import android.content.ContentValues
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.application.bhealthy.data.MyDatabaseHelper


class SymptomsActivity : AppCompatActivity() {

    val symptomsToColumns = mapOf(
        "Nausea" to "nausea",
        "Headache" to "headache",
        "Diarrhea" to "diarrhea",
        "Soar Throat" to "soarThroat",
        "Fever" to "fever",
        "Muscle Ache" to "muscleAche",
        "Loss of Smell or Taste" to "lossOfSmellOrTaste",
        "Cough" to "cough",
        "Shortness of Breath" to "shortnessOfBreath",
        "Feeling tired" to "feelingTired"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_symptoms)
        var symptoms = mutableMapOf<String, String>()

        var currentItemSelected: String = ""

        val symptomsDropdown = findViewById<Spinner>(R.id.symptomsDropdown);
        val symptomsDropdownAdaptor = ArrayAdapter.createFromResource(this,
            R.array.symptoms_dropdown_items,
            android.R.layout.simple_spinner_item)
        symptomsDropdownAdaptor.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        symptomsDropdown.adapter = symptomsDropdownAdaptor
        // When an item is selected from the dropdown
        symptomsDropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parentView: AdapterView<*>?,
                selectedItemView: View?,
                position: Int,
                id: Long
            ) {
                currentItemSelected = parentView?.getItemAtPosition(position).toString()
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {
                // Do nothing if nothing is selected

            }
        }

        val starRatingBar = findViewById<RatingBar>(R.id.starRatingBar)
        val starRatingValue = findViewById<TextView>(R.id.starRatingValue)
        // when a rating is changed
        starRatingBar.setOnRatingBarChangeListener { _, rating, fromUser ->
            // Update the TextView to show the current rating value
//            Log.d("RatingBar", "Rating changed to: $rating, fromUser: $fromUser")
            starRatingValue.text = String.format("%.1f", rating)
        }

        val recyclerView: RecyclerView = findViewById(R.id.mobile_list)
        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager

        val labelRatingList = mutableListOf<LabelRating>()
        val ratingAdapter = LabelRatingAdapter(labelRatingList)
        recyclerView.adapter = ratingAdapter

        val addSymptom = findViewById<Button>(R.id.addSymptomButton)
        // add the symptom to the list to display
        addSymptom.setOnClickListener {
            if (!currentItemSelected.equals("")) {
                symptoms[currentItemSelected] = starRatingBar.rating.toString()
            }

            labelRatingList.clear()
            for ((label, ratingStr) in symptoms) {
                val rating = ratingStr.toFloatOrNull() ?: 0.0f // Convert rating string to float
                val labelRating = LabelRating(label, rating)
                labelRatingList.add(labelRating)
            }
            ratingAdapter.notifyDataSetChanged()

            starRatingBar.rating = 0.0F
        }

        val dbHelper = MyDatabaseHelper(this)

        val uploadSymptoms = findViewById<Button>(R.id.uploadSymptoms)
        uploadSymptoms.setOnClickListener {
            val unique_id = intent.getStringExtra("unique_id")
            if (unique_id != null) {
                insertOrUpdateDatabaseEntry(dbHelper, unique_id, symptoms)
                Toast.makeText(baseContext, "Uploaded symptoms to database", Toast.LENGTH_SHORT).show()
            }
        }

    }

    fun insertOrUpdateDatabaseEntry(dbHelper: MyDatabaseHelper, vitalsId: String, symptoms: Map<String, String>) {
        val db = dbHelper.writableDatabase

        // Check if the entry with the given vitals_id exists
        val cursor = db.rawQuery("SELECT * FROM ${MyDatabaseHelper.TABLE_NAME} WHERE ${MyDatabaseHelper.COLUMN_NAME_VITALS_ID}=?", arrayOf(vitalsId))
        val values = ContentValues()
        for ((label, ratingStr) in symptoms) {
            val rating = ratingStr.toFloatOrNull() ?: 0.0f // Convert rating string to float
            values.put(symptomsToColumns[label], rating)
        }

        if (cursor.count > 0) {
            // Entry with the vitals_id already exists, update it
            db.update(
                MyDatabaseHelper.TABLE_NAME,
                values,
                "${MyDatabaseHelper.COLUMN_NAME_VITALS_ID}=?",
                arrayOf(vitalsId)
            )
        } else {
            values.put("vitals_id", vitalsId)
            // Entry with the vitals_id doesn't exist, insert a new record
            db.insert(MyDatabaseHelper.TABLE_NAME, null, values)
        }

        cursor.close()
        db.close()
    }
}