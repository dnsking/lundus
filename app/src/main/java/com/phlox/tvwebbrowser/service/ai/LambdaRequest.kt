package com.phlox.tvwebbrowser.service.ai
import com.google.gson.annotations.SerializedName

data class LambdaRequest(
    @SerializedName("image") val image: String,
    @SerializedName("position") val position: String,
    @SerializedName("prompt") val prompt: String
)

data class LambdaResponse(
    @SerializedName("result") val result: String?,
    @SerializedName("error") val error: String?
)
