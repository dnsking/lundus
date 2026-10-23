package com.phlox.tvwebbrowser.service.ai

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phlox.tvwebbrowser.utils.Utils.bitmapToBase64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray

// Sealed interface representing UI state
sealed interface AnalysisState {
    data object Idle : AnalysisState
    data object Loading : AnalysisState
    data class Success(val result: List<SearchResult>) : AnalysisState
    data class Error(val message: String) : AnalysisState
}

class ImageAnalysisViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<AnalysisState>(AnalysisState.Idle)
    val uiState: StateFlow<AnalysisState> = _uiState.asStateFlow()
    private fun parseJson(jsonStr: String): List<SearchResult> {
        val list = mutableListOf<SearchResult>()

        // --- THE FIX: Unwrap the double-escaped string ---
        var cleanJsonStr = jsonStr.trim()
        if (cleanJsonStr.startsWith("\"") && cleanJsonStr.endsWith("\"")) {
            // JSONTokener safely parses the outer string, un-escaping \" and \n
            // to give you the actual raw JSON array string underneath.
            cleanJsonStr = org.json.JSONTokener(cleanJsonStr).nextValue() as String
        }
        // -------------------------------------------------

        // Now it is safe to parse as an array
        val jsonArray = JSONArray(cleanJsonStr)

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
         //   val pos = obj.getJSONObject("position")
            list.add(
                SearchResult(
                    item = obj.getString("item"),
                    findable = obj.getString("findable"),
                    color = obj.getString("color"),
                    brand = obj.getString("brand"),
                    model = obj.getString("model"),
                    type = obj.getString("type"),
                    x = obj.getDouble("x").toFloat(),
                    y = obj.getDouble("y").toFloat()
                )
            )
        }
        return list
    }
    fun analyze(bitmap: Bitmap, position: String, prompt: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = AnalysisState.Loading

            try {
                val base64Image = bitmapToBase64(bitmap, quality = 80)
                val requestPayload = LambdaRequest(
                    image = base64Image,
                    position = position,
                    prompt = prompt
                )

                val response = ApiClient.service.analyzeImage(requestPayload)

                if (response.isSuccessful) {

                   // Log.i("Full", "Scan results fulle ${response.body()?.string()}")
                    val resultText = response.body()?.string()
                    Log.i("Full", "Scan results fulle ${resultText}")
                    _uiState.value = AnalysisState.Success(parseJson(resultText!!))
                } else {
                    val errorText = response.errorBody()?.string() ?: "Unknown error"
                    _uiState.value = AnalysisState.Error("Server error (${response.code()}): $errorText")
                }
            } catch (e: Exception) {
                _uiState.value = AnalysisState.Error(e.localizedMessage ?: "Network request failed")
            }
        }
    }
}