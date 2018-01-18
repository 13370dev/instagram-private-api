package de.lindenvalley.instagram

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import de.lindenvalley.exception.CheckpointRequiredException
import de.lindenvalley.khttp.responses.Response
import org.json.JSONObject


object Instagram {
    val CHECKPOINT_CHALLENGE_ERROR: String = "checkpoint_challenge_required"

    var username: String = "uname"
    var password: String = "pass"
    var deviceId: String = "xxxx"
    var uuid: String = "xxxx"
    var isLogin = false
    var ds_user_id = ""
    var token: String = "-"
    var rankToken: String = "-"
    var Req = Request()
    var cookiePersistor = CookiePersistor("")


    /**
     * Prepare Instagram API
     */
    fun prepare() {
        deviceId = Crypto.generateDeviceId(username)
        uuid = Crypto.randomUUID(true)
        cookiePersistor = CookiePersistor(username)
        if (cookiePersistor.exist()) {
            val cookieDisk = cookiePersistor.load()
            val account = JSONObject(cookieDisk.account)
            if (account.getString("status").toLowerCase().equals("ok")) {
                println("Already login to Instagram")
                val jar = cookieDisk.cookieJar
                Req.persistedCookies = jar
                isLogin = true
                ds_user_id = jar.getCookie("ds_user_id")?.value.toString()
                token = jar.getCookie("csrftoken")?.value.toString()
                rankToken = "${ds_user_id}_$uuid"
            }
        }
    }

    /**
     * Function for login to instagram, if force=true API will not use saved cookies
     */
    @Throws(CheckpointRequiredException::class)
    fun login(force: Boolean = false) {
        if (!isLogin || force) {
            var payload = """{"_csrftoken":"missing","device_id":"$deviceId","_uuid":"$uuid","username":"$username","password":"$password","login_attempt_count":0}"""
            var response = Req.prepareAsApi(Routes.login(), payload).send()
            cookiePersistor.save(response.text, response.cookies)
            var text = response.text
            if (response.statusCode == 400) {
                try {
                    val gson = Gson()
                    val checkpoint = gson.fromJson(text, InstagramCheckpointRequired::class.java)
                    if (CHECKPOINT_CHALLENGE_ERROR.equals(checkpoint.errorType)) {
                        throw CheckpointRequiredException(text)
                    }
                } catch (e: JsonSyntaxException) {
                    // text may be not InstagramCheckpointRequired json
                    println("Instagram.kt response status is 400, response is ${text}")
                }
            }

            println("Instagram.kt ${text}")
            println("Instagram.kt ${response.cookies.entries}")
            val account = response.jsonObject
            if (account.getString("status").toLowerCase().equals("ok")) {
                println("Already login to Instagram")
                val jar = response.cookies
                isLogin = true
                ds_user_id = jar.getCookie("ds_user_id")?.value.toString()
                token = jar.getCookie("csrftoken")?.value.toString()
                rankToken = "${ds_user_id}_$uuid"
                syncFeature()
                getAutoCompleteUserList()
                getTimelineFeed()
                getv2Inbox()
                getRecentActivity()
                println("Instagram login success")
            }
        }
    }

    /**
     * Function to logout from instagram
     */
    fun logout(): Response {
        val response = Req.prepareAsApi(Routes.logout()).send()
        cookiePersistor.destroy()
        return response
    }

    /**
     * Do SyncFeature
     */
    fun syncFeature(): Response {
        return Req.prepareAsApi(Routes.qeSync()).send()
    }

    /**
     * Get autcomplete user list
     */
    fun getAutoCompleteUserList(): Response {
        return Req.prepareAsApi(Routes.autocompleteUserList()).send()
    }

    fun getUserById(id: String): Response {
        return Req.prepareAsApi(Routes.userInfo(id)).send();
    }

    fun getUserByUserName(name: String): Response {
        return Req.prepare(Routes.userInfoByName(name)).send();
    }

    fun getMediaInfoByShortCode(shortCode: String): Response {
        return Req.prepare(Routes.mediaInfoByShortCode(shortCode)).send();
    }

    fun getUserPosts(name: String, maxId: String): Response {
        return Req.prepare(Routes.userPosts(name, maxId)).send();
    }

    fun getUserFeed(id: String, max_id: String, rankToken: String): Response {
        return Req.prepareAsApi(Routes.userFeed(id, max_id, rankToken)).send();
    }

    fun getPostsByLocation(locationId: String, slug: String): Response {
        return Req.prepare(Routes.postsByLocation(locationId, slug)).send();
    }

    fun mediaComments(mediaId: String, max_id: String): Response {
        return Req.prepareAsApi(Routes.mediaComments(mediaId, max_id)).send();
    }

    fun mediaInfo(mediaId: String): Response {
        return Req.prepareAsApi(Routes.mediaInfo(mediaId)).send();
    }

    /**
     * Get timeline feed
     */
    fun getTimelineFeed(): Response {
        return Req.prepareAsApi(Routes.timelineFeed()).send()
    }

    fun getv2Inbox(): Response {
        return Req.prepareAsApi(Routes.v2Inbox()).send()
    }

    fun getRecentActivity(): Response {
        return Req.prepareAsApi(Routes.recetActivity()).send()
    }

    fun findFollowersById(id: String): Response {
        return Req.prepare(Routes.followersById(id)).send()
    }
}