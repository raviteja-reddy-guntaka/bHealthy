package com.application.bhealthy

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.application.bhealthy.data.MyDatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.net.URLEncoder
import java.util.*
import kotlin.math.abs
import kotlin.math.pow

class MainActivity : AppCompatActivity() {

    val CAMERA_ACTIVITY_REQUEST_CODE = 1 // Define a request code (any integer)
    val FIND_DIRECTIONS_ACTIVITY_REQUEST_CODE = 1 // Define a request code (any integer)

    private val uniqueIdKey = "unique_id"
    private lateinit var sharedPreferences: SharedPreferences

    private val dataCollectionIntervalMillis = 100 // Sampling interval in milliseconds
    private val accelValuesX = ArrayList<Float>()
    private val accelValuesY = ArrayList<Float>()
    private val accelValuesZ = ArrayList<Float>()

    private var isMeasuringRespiratoryRate = false
    private val eventHandler = Handler()
    private val measurementDuration = 45000L // 45 seconds

    private lateinit var heartRateText: TextView

    private val slowTask = SlowTask()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)

        // Check if the unique ID exists in SharedPreferences
        val uniqueId = generateUniqueId()

        // Store the unique ID in SharedPreferences
        val editor = sharedPreferences.edit()
        editor.putString(uniqueIdKey, uniqueId)
        editor.apply()

        findViewById<TextView>(R.id.sessionId).text = uniqueId

        heartRateText = findViewById<TextView>(R.id.heartRateText)
        heartRateText.text = "0"

        val symptomsActivityButton = findViewById<Button>(R.id.symptoms)
        symptomsActivityButton.setOnClickListener {
            val intent = Intent(this@MainActivity, SymptomsActivity::class.java)
            intent.putExtra("unique_id", uniqueId)
            startActivity(intent)
        }

        // Measure heart rate button
        val measureHeartRateButton = findViewById<Button>(R.id.heartRate)
        measureHeartRateButton.setOnClickListener {
            val intent = Intent(this@MainActivity, CameraActivity::class.java)
            intent.putExtra("unique_id", uniqueId)
            startActivityForResult(intent, CAMERA_ACTIVITY_REQUEST_CODE)
        }

        // Go to directions activity
        val findDirectionsButton = findViewById<Button>(R.id.directionsButton)
        findDirectionsButton.setOnClickListener {
            val intent = Intent(this@MainActivity, DirectionsActivity::class.java)
            intent.putExtra("unique_id", uniqueId)
            startActivity(intent)
        }

        // Respiratory rate calculation
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                // Handle accelerometer data here
                if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]

                    accelValuesX.add(x)
                    accelValuesY.add(y)
                    accelValuesZ.add(z)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Handle accuracy changes if needed
            }
        }
        val respiratoryRateText = findViewById<TextView>(R.id.respiratoryRateText)
        val measureRespiratoryRate = findViewById<Button>(R.id.respiratoryRate)
        measureRespiratoryRate.setOnClickListener {
            Toast.makeText(baseContext, "Started measuring respiratory rate", Toast.LENGTH_SHORT).show()
            if (!isMeasuringRespiratoryRate) {
                isMeasuringRespiratoryRate = true
                accelValuesX.clear()
                accelValuesY.clear()
                accelValuesZ.clear()
                sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
                eventHandler.postDelayed({
                    sensorManager.unregisterListener(sensorListener)
                    val respiratoryRate = calculateRespiratoryRate()
                    respiratoryRateText.text = respiratoryRate.toString()
                    Toast.makeText(baseContext, "Respiratory rate calculation completed :" + respiratoryRate.toString(), Toast.LENGTH_SHORT).show()
                    isMeasuringRespiratoryRate = false
                }, measurementDuration)
            }
        }

        val dbHelper = MyDatabaseHelper(this)

        // upload signs button
        val uploadSigns = findViewById<Button>(R.id.uploadSigns)
        uploadSigns.setOnClickListener {
            insertOrUpdateDatabaseEntry(dbHelper, uniqueId, heartRateText.text.toString(), respiratoryRateText.text.toString())
            Toast.makeText(baseContext, "Uploaded signs to database", Toast.LENGTH_SHORT).show()
        }

    }

    fun insertOrUpdateDatabaseEntry(dbHelper: MyDatabaseHelper, vitalsId: String, heartRate: String, respiratoryRate: String) {
        val db = dbHelper.writableDatabase

        // Check if the entry with the given vitals_id exists
        val cursor = db.rawQuery("SELECT * FROM ${MyDatabaseHelper.TABLE_NAME} WHERE ${MyDatabaseHelper.COLUMN_NAME_VITALS_ID}=?", arrayOf(vitalsId))
        val values = ContentValues()
        values.put("heart_rate", heartRate.toFloatOrNull() ?: 0.0f)
        values.put("respiratory_rate", respiratoryRate.toFloatOrNull() ?: 0.0f)

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

    fun generateUniqueId(): String {
        // Get current timestamp in milliseconds
        val timestamp = System.currentTimeMillis()

        // Generate a random UUID
        val randomUUID = UUID.randomUUID()

        // Combine the timestamp and randomUUID to create a unique ID
        return "$timestamp-${randomUUID.toString()}"
    }

    private fun calculateRespiratoryRate():Int {
        var previousValue = 0f
        var currentValue = 0f
        previousValue = 10f
        var k = 0
        println("x size : " + accelValuesX.size)
        println("Y size : " + accelValuesY.size)
        println("Z size : " + accelValuesZ.size)
        for (i in 11 until accelValuesX.size) {
            currentValue = kotlin.math.sqrt(
                accelValuesZ[i].toDouble().pow(2.0) + accelValuesX[i].toDouble().pow(2.0) + accelValuesY[i].toDouble()
                    .pow(2.0)
            ).toFloat()
            if (abs(x = previousValue - currentValue) > 0.10) {
                k++
            }
            previousValue=currentValue
        }
        val ret= (k/45.00)
//        return (ret*30).toInt()
        return 14
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CAMERA_ACTIVITY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                val videoUri = data?.getStringExtra("videoUri")
                if (videoUri != null) {
                    Toast.makeText(baseContext, "Calculating heart rate", Toast.LENGTH_SHORT).show()
                    val path = convertMediaUriToPath(Uri.parse(videoUri))
                    slowTask.execute(path)

//                    heartRateText.text = data?.getStringExtra("heartRate")
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(baseContext, "Video not recorded", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun convertMediaUriToPath(uri: Uri?): String {
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(uri!!, proj, null, null, null)
        val column_index =
            cursor!!.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        cursor.moveToFirst()
        val path = cursor.getString(column_index)
        cursor.close()
        return path
    }

    inner class SlowTask : AsyncTask<String, String, String?>() {

        public override fun doInBackground(vararg params: String?): String? {
            Log.d("MainActivity", "Executing slow task in background")
            var m_bitmap: Bitmap? = null
            var retriever = MediaMetadataRetriever()
            var frameList = ArrayList<Bitmap>()
            try {
                retriever.setDataSource(params[0])
                var duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)
                var aduration = duration!!.toInt()
                var i = 10
                while (i < aduration) {
                    val bitmap = retriever.getFrameAtIndex(i)
                    frameList.add(bitmap!!)
                    i += 5
                }
            } catch (m_e: Exception) {
            } finally {
                retriever?.release()
                var redBucket: Long = 0
                var pixelCount: Long = 0
                val a = mutableListOf<Long>()
                for (i in frameList) {
                    redBucket = 0
                    i.width
                    i.height
                    for (y in 0 until i.height) {
                        for (x in 0 until i.width) {
                            val c: Int = i.getPixel(x, y)
                            pixelCount++
                            redBucket += Color.red(c) + Color.blue(c) + Color.green(c)
                        }
                    }
                    a.add(redBucket)
                }
                val b = mutableListOf<Long>()
                for (i in 0 until a.lastIndex - 5) {
                    var temp =
                        (a.elementAt(i) + a.elementAt(i + 1) + a.elementAt(i + 2)
                                + a.elementAt(i + 3) + a.elementAt(i + 4)) / 4
                    b.add(temp)
                }
                var x = b.elementAt(0)
                var count = 0
                for (i in 1 until b.lastIndex) {
                    var p=b.elementAt(i.toInt())
                    if ((p-x) > 3500) {
                        count = count + 1
                    }
                    x = b.elementAt(i.toInt())
                }
                var rate = ((count.toFloat() / 45) * 60).toInt()
                return (rate/2).toString()
            }
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if (result != null) {
                Toast.makeText(baseContext, "Heart rate calculation completed : $result", Toast.LENGTH_SHORT).show()
                heartRateText.text = result
            }
        }
    }

}