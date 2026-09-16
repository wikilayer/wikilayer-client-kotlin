package org.wikilayer.network.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class Account(
    val id: Long,
    @field:JsonProperty("display_name") val displayName: String = "",
    val email: String = "",
    @field:JsonProperty("avatar_url") val avatarUrl: String? = null,
    @field:JsonProperty("created_at") val createdAt: Instant? = null,
)

data class Credential(
    val token: String,
)

data class TokenGrant(
    @field:JsonProperty("access_token") val accessToken: String,
) {
    fun credential(): Credential = Credential(accessToken)
}
