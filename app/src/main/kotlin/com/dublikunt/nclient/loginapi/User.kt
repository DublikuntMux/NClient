package com.dublikunt.nclient.loginapi

import com.dublikunt.nclient.settings.Global
import com.dublikunt.nclient.settings.Login
import com.dublikunt.nclient.utility.Utility
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import java.io.IOException

class User private constructor(val username: String, id: String, codename: String) {
    val codename: String
    val id: Int

    init {
        this.id = id.toInt()
        this.codename = codename
    }

    override fun toString(): String {
        return "$username($id/$codename)"
    }

    interface CreateUser {
        fun onCreateUser(user: User?)
    }

    companion object {

        fun createUser(createUser: CreateUser?) {
            Global.client
                ?.newCall(Request.Builder().url(Login.BASE_HTTP_URL).build())
                ?.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {}

                    @Throws(IOException::class)
                    override fun onResponse(call: Call, response: Response) {
                        var user: User? = null
                        val doc =
                            Jsoup.parse(response.body.byteStream(), null, Utility.getBaseUrl())
                        val elements = doc.getElementsByClass("fa-tachometer-alt")
                        if (elements.size > 0) {
                            val x = elements.first()!!.parent()
                            val username = x!!.text().trim { it <= ' ' }
                            val y =
                                x.attr("href").split("/".toRegex()).dropLastWhile { it.isEmpty() }
                                    .toTypedArray()
                            user = User(username, y[2], y[3])
                        }
                        Login.updateUser(user)
                        createUser?.onCreateUser(Login.user)
                    }
                })
        }
    }
}
