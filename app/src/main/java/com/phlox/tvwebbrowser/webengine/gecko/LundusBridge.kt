package com.phlox.tvwebbrowser.webengine.gecko

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import org.mozilla.geckoview.WebExtension

object LundusBridge {

    private const val TAG = "lundusbrowser"
    private var port: WebExtension.Port? = null
    private val pending = ArrayDeque<String>()

    /** Register on the extension returned by `ensureBuiltIn`. Idempotent. */
    fun register(p: WebExtension.Port) {

        Log.i(TAG, "register")
        Log.i(TAG, "bridge port connected")
        port = p
        p.setDelegate(object : WebExtension.PortDelegate {
            override fun onPortMessage(message: Any, port: WebExtension.Port) {

                Log.i(TAG, "onPortMessage received $message")
            }
            override fun onDisconnect(disconnected: WebExtension.Port) {
                if (port === disconnected) port = null
            }
        })
        while (pending.isNotEmpty()) post(p, pending.removeFirst())
        Log.i(TAG, "bridge delegate registered")
    }

    /** Evaluate [script] in the active page; queued if the bridge isn't connected yet. */
    fun eval(script: String) {
        val p = port
        if (p != null) {
            post(p, script)
        } else {
            Log.i(TAG, "bridge not connected; queueing eval (${pending.size + 1} pending)")
            pending.addLast(script)
            while (pending.size > 20) pending.removeFirst()
        }
    }

    private fun post(p: WebExtension.Port, script: String) {
        try {
            p.postMessage(JSONObject().put("type", "eval").put("code", script))
        } catch (e: Exception) {
            Log.i(TAG, "eval post error", e)
        }
    }
    fun executeWorkflow(workflow: JSONArray){
        port?.postMessage(
            JSONObject().apply {
                put("type", "runWorkflow")
                put("steps", workflow)
            }
        )
       // port?.postMessage(workflow);
    }
}
