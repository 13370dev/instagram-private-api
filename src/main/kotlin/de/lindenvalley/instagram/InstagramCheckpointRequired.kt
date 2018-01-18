package de.lindenvalley.instagram

import com.google.gson.annotations.SerializedName

data class InstagramCheckpointRequired(
        val message: String,
        @SerializedName("checkpoint_url") val checkpointUrl: String,
        val status: String,
        @SerializedName("error_type") val errorType: String
)
