package com.example.jarvis
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class MainActivity() : AppCompatActivity(), Parcelable {

    private val client = OkHttpClient()

    private lateinit var typingTextView: TextView
    private lateinit var outputText: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val textToDisplay = "Jarvis at your service, How can I help you?"
    private var index = 0

    constructor(parcel: Parcel) : this() {
        index = parcel.readInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val inputText=findViewById<EditText>(R.id.inputTextView)
        val sendBtn=findViewById<ImageButton>(R.id.sendButton)
        outputText=findViewById(R.id.outputText)


        sendBtn.setOnClickListener {
            val ques=inputText.text.toString()
            getResponse(ques){response->
                runOnUiThread {
                    outputText.text=response
                }

            }

        }

        typingTextView = findViewById(R.id.typingTextView)
        // Set the custom font programmatically
        val typeface = ResourcesCompat.getFont(this, R.font.roboto_mono)
        typingTextView.typeface = typeface

        startTypingAnimation()
    }

    private fun startTypingAnimation() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (index < textToDisplay.length) {
                    typingTextView.text = textToDisplay.substring(0, index + 1)
                    index++
                    handler.postDelayed(this, 100) // Adjust typing speed here
                }
            }
        }, 100)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(index)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MainActivity> {
        override fun createFromParcel(parcel: Parcel): MainActivity {
            return MainActivity(parcel)
        }

        override fun newArray(size: Int): Array<MainActivity?> {
            return arrayOfNulls(size)
        }
    }

    private fun getResponse(question: String, callback: (String) -> Unit){
       val apiKey="sk-2IHcho2J1Xw6i8sgP0NNe1sTU1I70cow7QHw6V6awhT3BlbkFJtRu5d-ETJ_sZZnaC3ACPTvlsttNiFW7GsxLCFHKUUA"
       val url="https://api.openai.com/v1/completions"
        val requestBody = """ 
        {
        "model": "gpt-3.5-turbo",
        "prompt": "$question",
        "max_tokens": 50,
        "temperature": 0.7
        }
    """.trimIndent().trim()

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody.toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("error", "API Failed", e)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (body != null) {
                    Log.v("Data", body)
                    try {
                        val jsonObject = JSONObject(body)
                        if (jsonObject.has("choices")) {
                            val jsonArray = jsonObject.getJSONArray("choices")
                            val text = jsonArray.getJSONObject(0).getString("text")
                            runOnUiThread {
                                outputText.text = text
                            }
                        } else if (jsonObject.has("error")) {
                            val errorMessage = jsonObject.getJSONObject("error").getString("message")
                            Log.e("API Error", errorMessage)
                            runOnUiThread {
                                // Set error message in outputText
                                outputText.text = "API Error: $errorMessage"
                            }
                        } else {
                            Log.e("Data", "Unexpected response format")
                            runOnUiThread {
                                // Set unexpected response format message in outputText
                                outputText.text = "Unexpected response format"
                            }
                        }
                    } catch (e: JSONException) {
                        Log.e("Data", "Error parsing JSON", e)
                        runOnUiThread {
                            // Set JSON parsing error message in outputText
                            outputText.text = "Error parsing JSON: ${e.message}"
                        }
                    }
                } else {
                    Log.v("Data", "Response body is empty")
                    runOnUiThread {
                        // Set empty response message in outputText
                        outputText.text = "Response body is empty"
                    }
                }
            }
        })
    }
}

