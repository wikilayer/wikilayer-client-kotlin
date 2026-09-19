package org.wikilayer.network

import android.net.Uri
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.wikilayer.network.model.Account
import org.wikilayer.network.model.Credential
import org.wikilayer.network.model.TokenGrant

/** Authentication and account operations for a shared host pool. */
class AuthApi internal constructor(
    private val hosts: WikiHostPool,
    client: OkHttpClient,
    private val mapper: ObjectMapper,
    private val clientTheServerRegistered: OAuthClient,
) {
    constructor(
        clientTheServerRegistered: OAuthClient,
        hosts: WikiHostPool = WikiHostConfiguration.bundled.pool(),
    ) : this(hosts, ClientDefaults.http(), ClientDefaults.json(), clientTheServerRegistered)

    internal constructor(
        baseUrl: String,
        client: OkHttpClient,
        mapper: ObjectMapper,
        clientTheServerRegistered: OAuthClient,
    ) : this(WikiHostPool(baseUrl), client, mapper, clientTheServerRegistered)

    private val transport = JsonTransport(client, mapper)

    /** Exchanges a native provider token once on the selected host. */
    suspend fun signIn(
        identityToken: String,
        provider: NativeProvider = NativeProvider.GOOGLE,
        nameOfferedOnce: String = "",
    ): Credential =
        onSelectedHost(hosts) { host ->
            transport
                .value(
                    TokenGrant::class.java,
                    Request
                        .Builder()
                        .url(address(host, "api/auth/${provider.wireName}"))
                        .post(
                            mapper
                                .writeValueAsBytes(mapOf("id_token" to identityToken, "name" to nameOfferedOnce))
                                .toRequestBody(JSON),
                        ).build(),
                ).credential()
        }

    /** Selects a reachable host before the caller obtains a one-use provider token. */
    suspend fun prepareHost() {
        onAvailableHost(hosts) { host ->
            transport.data(
                Request
                    .Builder()
                    .url(address(host, "api/wikis"))
                    .get()
                    .build(),
            )
        }
    }

    /** Creates a browser authorization request on the selected host. */
    fun authorizationRequest(
        provider: String,
        state: String,
        challenge: String,
    ): AuthorizationRequest? =
        hosts.candidates().firstOrNull()?.let { host ->
            AuthorizationRequest(
                Uri
                    .parse(host)
                    .buildUpon()
                    .appendEncodedPath("oauth/authorize")
                    .appendQueryParameter("client_id", clientTheServerRegistered.id)
                    .appendQueryParameter("redirect_uri", clientTheServerRegistered.redirectUri)
                    .appendQueryParameter("response_type", "code")
                    .appendQueryParameter("scope", clientTheServerRegistered.scope)
                    .appendQueryParameter("provider", provider)
                    .appendQueryParameter("state", state)
                    .appendQueryParameter("code_challenge", challenge)
                    .appendQueryParameter("code_challenge_method", "S256")
                    .build()
                    .toString(),
                host,
            )
        }

    /** Exchanges an OAuth code on the host that issued [authorization]. */
    suspend fun exchange(
        code: String,
        verifier: String,
        authorization: AuthorizationRequest,
    ): Credential =
        try {
            transport
                .value(
                    TokenGrant::class.java,
                    Request
                        .Builder()
                        .url(address(authorization.host, "oauth/token"))
                        .post(
                            FormBody
                                .Builder()
                                .add("grant_type", "authorization_code")
                                .add("code", code)
                                .add("code_verifier", verifier)
                                .add("client_id", clientTheServerRegistered.id)
                                .add("redirect_uri", clientTheServerRegistered.redirectUri)
                                .build(),
                        ).build(),
                ).credential()
        } catch (api: WikiApiError) {
            throw api
        } catch (network: java.io.IOException) {
            throw WikiApiError.Unreachable(
                listOf(
                    WikiHostFailure(
                        authorization.host,
                        WikiHostFailure.Reason.Network(network.kind()),
                    ),
                ),
                network,
            )
        }

    /** Returns the account represented by [credential]. */
    suspend fun account(credential: Credential): Account =
        onAvailableHost(hosts) { host ->
            transport.value(Account::class.java, signed(host, "api/me", credential).get().build())
        }

    /** Changes the display name and returns the updated account. */
    suspend fun rename(
        to: String,
        credential: Credential,
    ): Account =
        onAvailableHost(hosts) { host ->
            transport.value(
                Account::class.java,
                signed(host, "api/me", credential)
                    .patch(mapper.writeValueAsBytes(mapOf("display_name" to to)).toRequestBody(JSON))
                    .build(),
            )
        }

    /** Permanently closes the account represented by [credential]. */
    suspend fun deleteAccount(
        reason: String,
        credential: Credential,
    ) {
        try {
            onSelectedHost(hosts) { host ->
                transport.data(
                    signed(host, "api/me", credential)
                        .delete(mapper.writeValueAsBytes(mapOf("reason" to reason)).toRequestBody(JSON))
                        .build(),
                )
            }
        } catch (refused: WikiApiError.Status) {
            if (refused.code == CONFLICT) throw AccountDeletionError.LiveWikis(refused)
            throw refused
        }
    }

    /** Revokes [credential] on its selected host. */
    suspend fun signOut(credential: Credential) {
        onSelectedHost(hosts) { host ->
            transport.data(
                signed(host, "api/auth/signout", credential)
                    .post(ByteArray(0).toRequestBody(null))
                    .build(),
            )
        }
    }

    private fun signed(
        host: String,
        path: String,
        credential: Credential,
    ): Request.Builder =
        Request
            .Builder()
            .url(address(host, path))
            .header("Authorization", "Bearer ${credential.token}")

    private fun address(
        host: String,
        path: String,
    ): String =
        Uri
            .parse(host)
            .buildUpon()
            .appendEncodedPath(path)
            .build()
            .toString()

    companion object {
        private val JSON = "application/json".toMediaType()
        private const val CONFLICT = 409
    }
}

sealed class AccountDeletionError(
    cause: Throwable,
) : Exception(cause) {
    class LiveWikis(
        cause: Throwable,
    ) : AccountDeletionError(cause)
}

/** A provider that issues a native identity token. */
enum class NativeProvider(
    val wireName: String,
) {
    APPLE("apple"),
    GOOGLE("google"),
}

/** A browser authorization URL bound to the host that issued it. */
class AuthorizationRequest internal constructor(
    val url: String,
    internal val host: String,
)

/** The public parameters that identify an OAuth client. */
data class OAuthClient(
    val id: String,
    val redirectUri: String,
    val scope: String,
)
