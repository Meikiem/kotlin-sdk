package co.ingrow.android.kotlin.rest

import co.ingrow.android.kotlin.Const
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object RestClient {
    private val client = OkHttpClient()
    private const val API_URL = "https://event.ingrow.co/v1"
    private val JSON = "application/json; charset=utf-8".toMediaTypeOrNull()
    fun getInstance(apiKey: String, bodyJson: JSONObject): Call {
        val body = bodyJson.toString().toRequestBody(JSON)
        val request: Request = Request.Builder()
                .header(Const.API_KEY, apiKey)
                .header("Cache-Control", "no-cache")
                .post(body)
                .url(API_URL)
                .build()
        return client.newCall(request)
    }
}