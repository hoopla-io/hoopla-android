package uz.alphazet.domain.repositories

import androidx.collection.ArrayMap
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import uz.alphazet.data.services.ProfileService
import uz.alphazet.domain.cache.AppCache
import uz.alphazet.domain.network.BaseRepo

class ProfileRepo(
    private val api: ProfileService,
    private val appCache: AppCache
) : BaseRepo() {

    suspend fun getMe() = handle { api.getMe() }
    suspend fun editMe() = handle { api.editMe() }

    suspend fun updateMe(name: String?, gender: String?, dateOfBirth: String?) = handleFlow {
        val jsonParams: MutableMap<String?, Any?> = ArrayMap()

        if (!name.isNullOrEmpty()) jsonParams["name"] = name
        if (!gender.isNullOrEmpty()) jsonParams["gender"] = gender
        if (!dateOfBirth.isNullOrEmpty()) jsonParams["dateOfBirth"] = dateOfBirth

        val body: RequestBody = (JSONObject(jsonParams)).toString()
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        api.updateMe(body)
    }

    // Multi-device aware: revokes only this device's session, leaving the
    // user's other devices signed in.
    suspend fun logout() = handleFlow {
        api.logout(appCache.refreshToken)
    }

    suspend fun deactivate() = handleFlow {
        api.deactivate()
    }

}