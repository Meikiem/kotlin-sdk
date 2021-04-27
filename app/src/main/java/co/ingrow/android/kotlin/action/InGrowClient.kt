package co.ingrow.android.kotlin.action

import android.app.Application
import android.util.Log
import co.ingrow.android.kotlin.Const
import co.ingrow.android.kotlin.InGrowLogging
import co.ingrow.android.kotlin.rest.RestClient
import co.ingrow.android.kotlin.util.NetworkStatusHandler
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.HashMap

class InGrowClient private constructor(
    private val application: Application,
    private val project: String,
    private val stream: String,
    private val apiKey: String,
    var anonymousId: String? = null,
    var userId: String? = null,
    var isDebugMode: Boolean = false
) {

    data class Builder(
        var application: Application? = null,
        var project: String? = null,
        var stream: String? = null,
        var apiKey: String? = null,
        var isLoggingEnable: Boolean = false,
        var anonymousId: String? = null,
        var userId: String? = null,
        var isDebugMode: Boolean = false) {

        fun application(application: Application) = apply { this.application = application }
        fun project(project: String) = apply { this.project = project }
        fun stream(stream: String) = apply { this.stream = stream }
        fun apiKey(apiKey: String) = apply { this.apiKey = apiKey }
        fun isLoggingEnable(isLoggingEnable: Boolean) = apply { this.isLoggingEnable = isLoggingEnable }
        fun anonymousId(anonymousId: String) = apply { this.anonymousId = anonymousId }
        fun userId(userId: String) = apply { this.userId = userId }
        fun isDebugMode(isDebugMode: Boolean) = apply { this.isDebugMode = isDebugMode }
        fun build() = apply {
            checkNotNull(application) { "Application must be filled before calling Build()" }
            checkNotNull(project) { "Project must be defined before calling Build()" }
            checkNotNull(stream) { "Stream must be defined before calling Build()" }
            checkNotNull(apiKey) { "api_key must be defined before calling Build()" }
            checkNotNull(apiKey) { "api_key must be defined before calling Build()" }
            if (isLoggingEnable) InGrowLogging.enableLogging() else InGrowLogging.disableLogging()
            ClientSingleton.INSTANCE.client = InGrowClient(application!!, project!!, stream!!, apiKey!!, anonymousId, userId, isDebugMode)
        }

    }

    companion object {
        fun client(): InGrowClient {
            checkNotNull(ClientSingleton.INSTANCE.client) { "Please call InGrowClient.Builder() before requesting the client." }
            return ClientSingleton.INSTANCE.client!!
        }
    }

    fun setEnrichment(userId: String) {
        checkNotNull(ClientSingleton.INSTANCE.client) { "Please call InGrowClient.initialize() before requesting the enrichmentBySession." }
        checkNotNull(ClientSingleton.INSTANCE.client!!.anonymousId) { "You had to set Anonymous ID while you were initializing InGrow." }
        ClientSingleton.INSTANCE.client!!.userId = userId
    }

    fun logEvents(events: HashMap<*, *>) {
        if (events.isEmpty()) {
            handleFailure(Exception("Events must not be null"))
            return
        }
        if (!isNetworkConnected()) {
            InGrowLogging.log("Couldn't send events because of no network connection.")
            handleFailure(Exception("Network's not connected."))
            return
        }
        val main = JSONObject()
        val inGrowObject = JSONObject()
        val eventObject = JSONObject()
        val enrichmentSessionObject = JSONObject()
        val enrichmentIPObject = JSONObject()
        val enrichmentArray = JSONArray()
        val inputObject = JSONObject()
        val inputIPObject = JSONObject()
        try {
            inGrowObject.put(Const.PROJECT, this.project)
            inGrowObject.put(Const.STREAM, this.stream)
            for (key in events.keys) {
                eventObject.put(key.toString(), events[key])
            }
            // Events stream would always have IP ENRICHMENT and would be filled automatically
            inputIPObject.put(Const.IP, Const.AUTO_FILL)
            enrichmentIPObject.put(Const.NAME, Const.IP)
            enrichmentIPObject.put(Const.INPUT, inputIPObject)
            enrichmentArray.put(enrichmentIPObject)
            if (this.anonymousId != null) {
                inputObject.put(EnrichmentKey.ANONYMOUS_ID.id, this.anonymousId)
                inputObject.put(EnrichmentKey.USER_ID.id, if (this.userId != null) this.userId else "")
                enrichmentSessionObject.put(Const.NAME, Const.SESSION)
                enrichmentSessionObject.put(Const.INPUT, inputObject)
                enrichmentArray.put(enrichmentSessionObject)
            }
            main.put(Const.ENRICHMENT, enrichmentArray)
            main.put(Const.INGROW, inGrowObject)
            main.put(Const.EVENT, eventObject)
        } catch (e: JSONException) {
            handleFailure(e)
        }
        sendRequest(main)
    }


    private enum class ClientSingleton {
        INSTANCE;
        var client: InGrowClient? = null
    }

    enum class EnrichmentKey(internal val id: String) {
        ANONYMOUS_ID("anonymous_id"), USER_ID("user_id");
    }

    private fun handleFailure(e: Exception) {
        if (isDebugMode) {
            if (e is RuntimeException) {
                throw e
            } else {
                throw RuntimeException(e)
            }
        } else {
            InGrowLogging.log("Encountered error: " + e.message)
        }
    }

    private fun isNetworkConnected(): Boolean {
        return NetworkStatusHandler(this.application.baseContext).isNetworkConnected()
    }

    private fun sendRequest(main: JSONObject) {

        RestClient.getInstance(ClientSingleton.INSTANCE.client!!.apiKey, main).enqueue(object :
            Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d("ERROR SENDING EVENTS:", e.toString())
                Log.d("EVENTS:", main.toString())
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d("SEND EVENTS SUCCEEDED:", response.toString())
            }
        })
    }

}