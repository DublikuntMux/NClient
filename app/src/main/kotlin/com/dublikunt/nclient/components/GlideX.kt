package com.dublikunt.nclient.components

import android.content.Context
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager

object GlideX {
    operator fun get(context: Context): Glide? {
        return try {
            Glide.get(context)
        } catch (ignore: VerifyError) {
            null
        } catch (ignore: IllegalStateException) {
            null
        }
    }


    fun with(view: View): RequestManager? {
        return try {
            Glide.with(view!!)
        } catch (ignore: VerifyError) {
            null
        } catch (ignore: IllegalStateException) {
            null
        }
    }

    fun with(context: Context): RequestManager? {
        return try {
            Glide.with(context!!)
        } catch (ignore: VerifyError) {
            null
        } catch (ignore: IllegalStateException) {
            null
        }
    }

    fun with(fragment: Fragment): RequestManager? {
        return try {
            Glide.with(fragment!!)
        } catch (ignore: VerifyError) {
            null
        } catch (ignore: IllegalStateException) {
            null
        }
    }

    fun with(fragmentActivity: FragmentActivity): RequestManager? {
        return try {
            Glide.with(fragmentActivity!!)
        } catch (ignore: VerifyError) {
            null
        } catch (ignore: IllegalStateException) {
            null
        }
    }

    fun with(activity: AppCompatActivity): RequestManager? {
        return try {
            Glide.with(activity!!)
        } catch (ignore: VerifyError) {
            null
        } catch (ignore: IllegalStateException) {
            null
        }
    }
}
