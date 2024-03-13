package edu.temple.convoy

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.android.volley.AuthFailureError
import com.android.volley.NetworkResponse
import com.android.volley.ParseError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.io.UnsupportedEncodingException

/**
 * A helper class to store all functions relating to:
 * API Control
 * User Management
 */
class Helper {

    object api {

        val ENDPOINT_CONVOY = "convoy.php"
        val ENDPOINT_USER = "account.php"

        val API_BASE = "https://kamorris.com/lab/convoy/"

        fun interface Response {
            fun processResponse(response: JSONObject)
        }

        fun createAccount(context: Context, user: User, password: String, response: Response?){
            val params = mutableMapOf(
                Pair("action", "REGISTER"),
                Pair("username", user.username),
                Pair("password", password),
                Pair("firstname", user.firstname!!),
                Pair("lastname", user.lastname!!)
            )
            makeRequest(context, ENDPOINT_USER, params, response)
        }

        fun login(context: Context, user: User, password: String, response: Response?) {
            val params = mutableMapOf(
                Pair("action", "LOGIN"),
                Pair("username", user.username),
                Pair("password", password)
            )
            makeRequest(context, ENDPOINT_USER, params, response)
        }

        fun createConvoy(context: Context, user: User, sessionKey: String, response: Response?) {
            val params = mutableMapOf(
                Pair("action", "CREATE"),
                Pair("username", user.username),
                Pair("session_key", sessionKey)
            )
            makeRequest(context, ENDPOINT_CONVOY, params, response)
        }

        fun joinConvoy(context: Context, user: User, sessionKey: String, convoyId: String, response: Response?) {
            val params = mutableMapOf(
                Pair("action", "JOIN"),
                Pair("username", user.username),
                Pair("session_key", sessionKey),
                Pair("convoy_id", convoyId)
            )
            makeRequest(context, ENDPOINT_CONVOY, params, response)
        }

        fun leaveConvoy(context: Context, user: User, sessionKey: String, convoyId: String, response: Response?) {
            val params = mutableMapOf(
                Pair("action", "LEAVE"),
                Pair("username", user.username),
                Pair("session_key", sessionKey),
                Pair("convoy_id", convoyId)
            )
            makeRequest(context, ENDPOINT_CONVOY, params, response)
        }

        fun closeConvoy(context: Context, user: User, sessionKey: String, convoyId: String, response: Response?) {
            val params = mutableMapOf(
                Pair("action", "END"),
                Pair("username", user.username),
                Pair("session_key", sessionKey),
                Pair("convoy_id", convoyId)
            )
            makeRequest(context, ENDPOINT_CONVOY, params, response)
        }

        fun queryStatus(context: Context, user:User, sessionKey: String, response: Response?) {
            val params = mutableMapOf(
                Pair("action", "QUERY"),
                Pair("username", user.username),
                Pair("session_key", sessionKey),
            )
            makeRequest(context, ENDPOINT_CONVOY, params, response)
        }

        fun updateFCMToken(context: Context, user:User, sessionKey: String, fcmToken: String, response: Response?) {
            val params = mutableMapOf(
                Pair("action", "UPDATE"),
                Pair("username", user.username),
                Pair("session_key", sessionKey),
                Pair("fcm_token", fcmToken),
            )
            makeRequest(context, ENDPOINT_USER, params, response)
        }

        fun updateUserLocation(context: Context, user:User, sessionKey: String, convoyId: String, lat: String, long: String, response: Response?) {
            val params = mutableMapOf(
                Pair("action", "UPDATE"),
                Pair("username", user.username),
                Pair("session_key", sessionKey),
                Pair("convoy_id", convoyId),
                Pair("latitude", lat),
                Pair("longitude", long),
            )
            makeRequest(context, ENDPOINT_CONVOY, params, response)
        }

        fun sendAudioMessage(context: Context, user:User, sessionKey: String, convoyId: String, audioFile: File, response: Response?){
            val params = mutableMapOf(
                Pair("action", "UPDATE"),
                Pair("username", user.username),
                Pair("session_key", sessionKey),
                Pair("convoy_id", convoyId),
            )
            makeMultiPartRequest(context, ENDPOINT_CONVOY, params, audioFile, response)
        }

        private fun makeRequest(context: Context, endPoint: String, params: MutableMap<String, String>, responseCallback: Response?) {
            Volley.newRequestQueue(context)
                .add(object: StringRequest(Request.Method.POST, API_BASE + endPoint, {
                    Log.d("Server Response", it)
                    responseCallback?.processResponse(JSONObject(it))
                }, {}){
                    override fun getParams(): MutableMap<String, String> {
                            return params;
                    }
                })
        }

        private fun makeMultiPartRequest(context: Context, endPoint: String, params: MutableMap<String,String>, file: File, responseCallback: Response?) {
            Volley.newRequestQueue(context)
                .add(
                    MultipartRequest(API_BASE + endPoint, params, file.readBytes(), {
                        Log.d("Server Response", it)
                        responseCallback?.processResponse(JSONObject(it))
                    }, {
                        Log.d("Multipart error",it.toString())
                    })
                )
        }

        fun isSuccess(response: JSONObject): Boolean {
            return response.getString("status").equals("SUCCESS")
        }

        fun getErrorMessage(response: JSONObject): String {
            return response.getString("message")
        }

    }

    object user {
        private val SHARED_PREFERENCES_FILE = "shared_prefs"
        private val KEY_SESSION_KEY = "session_key"
        private val KEY_USERNAME = "username"
        private val KEY_FIRSTNAME = "firstname"
        private val KEY_LASTNAME = "lastname"
        private val KEY_CONVOY_ID = "convoy_id"
        private val KEY_FCM_TOKEN = "fcm_token"
        private val KEY_JOINED = "joined"

        fun saveSessionData(context: Context, sessionKey: String) {
            getSP(context).edit()
                .putString(KEY_SESSION_KEY, sessionKey)
                .apply()
        }

        fun saveConvoyId(context: Context, groupId: String) {
            getSP(context).edit()
                .putString(KEY_CONVOY_ID, groupId)
                .apply()
        }

        fun saveJoinedState(context: Context, joined: Boolean?){
            getSP(context).edit()
                .putString(KEY_JOINED, joined.toString())
                .apply()
        }

        fun saveFCMToken(context: Context, token: String) {
            getSP(context).edit()
                .putString(KEY_FCM_TOKEN, token)
                .apply()
        }

        fun getConvoyId(context: Context): String? {
            return getSP(context).getString(KEY_CONVOY_ID, null)
        }

        fun getJoinedState(context: Context): Boolean? {
            val stringValue = getSP(context).getString(KEY_JOINED, "null")
            return if (stringValue == "null") null else stringValue.toBoolean()
        }

        fun getFCMToken(context: Context): String? {
            return getSP(context).getString(KEY_FCM_TOKEN, null)
        }

        fun clearConvoyId(context: Context) {
            getSP(context).edit().remove(KEY_CONVOY_ID)
                .apply()
        }

        fun clearJoinedState(context: Context){
            getSP(context).edit().remove(KEY_JOINED)
                .apply()
        }

        fun clearSessionData(context: Context) {
            getSP(context).edit().remove(KEY_SESSION_KEY)
                .apply()
        }

        fun getSessionKey(context: Context): String? {
            return getSP(context).getString(KEY_SESSION_KEY, null)
        }

        fun saveUser(context: Context, user: User) {
            getSP(context).edit()
                .putString(KEY_USERNAME, user.username)
                .putString(KEY_FIRSTNAME, user.firstname)
                .putString(KEY_LASTNAME, user.lastname)
                .apply()
        }

        fun get(context: Context) : User {
            return User (
                        getSP(context).getString(KEY_USERNAME, "")!!,
                        getSP(context).getString(KEY_FIRSTNAME, ""),
                        getSP(context).getString(KEY_LASTNAME, ""),
                    )
        }
        private fun getSP (context: Context) : SharedPreferences {
            return context.getSharedPreferences(SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE)
        }
    }


}

class MultipartRequest(
    url: String,
    private val params: Map<String, String>?,
    private val file: ByteArray,
    private val listener: Response.Listener<String>,
    private val errorListener: Response.ErrorListener
) : Request<String>(Method.POST, url, errorListener) {

    private val boundary = "*****" // You can change this to any random string

    override fun getBodyContentType(): String {
        return "multipart/form-data; boundary=$boundary"
    }

    @Throws(AuthFailureError::class)
    override fun getBody(): ByteArray {
        val bos = ByteArrayOutputStream()
        val dos = DataOutputStream(bos)

        try {
            // Adding parameters
            params?.let {
                for ((key, value) in it) {
                    writeFormField(dos, key, value)
                }
            }

            // Adding files
            writeFilePart(dos, "audioFile", "file.jpg", file)

            // Adding end boundary
            dos.writeBytes("--$boundary--\r\n")

            return bos.toByteArray()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                dos.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return ByteArray(0)
    }

    private fun writeFormField(dos: DataOutputStream, fieldName: String, value: String) {
        dos.writeBytes("--$boundary\r\n")
        dos.writeBytes("Content-Disposition: form-data; name=\"$fieldName\"\r\n\r\n")
        dos.writeBytes(value + "\r\n")
    }

    private fun writeFilePart(dos: DataOutputStream, fieldName: String, fileName: String, data: ByteArray) {
        dos.writeBytes("--$boundary\r\n")
        dos.writeBytes("Content-Disposition: form-data; name=\"$fieldName\"; filename=\"$fileName\"\r\n")
        dos.writeBytes("Content-Type: application/octet-stream\r\n\r\n")
        dos.write(data)
        dos.writeBytes("\r\n")
    }

    override fun parseNetworkResponse(response: NetworkResponse): Response<String> {
        return try {
            val data = String(response.data, charset(HttpHeaderParser.parseCharset(response.headers)))
            Response.success(data, HttpHeaderParser.parseCacheHeaders(response))
        } catch (e: UnsupportedEncodingException) {
            Response.error(ParseError(e))
        }
    }

    override fun deliverResponse(response: String) {
        listener.onResponse(response)
    }
}