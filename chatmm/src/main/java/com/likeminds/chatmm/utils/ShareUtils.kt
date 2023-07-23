package com.likeminds.chatmm.utils

import android.content.Context
import android.content.Intent
import com.likeminds.chatmm.R

object ShareUtils {
    const val domain = "https://www.sampleapp.com"

    /**
     * Share post with url using default sharing in Android OS
     * @param context - context
     * @param chatroomId - id of the shared chatroom
     * @param domain - domain required to create share link
     */
    // todo: confirm share link
    fun shareChatroom(
        context: Context,
        chatroomId: String,
        domain: String
    ) {
        val shareLink = "$domain/chatroom?chatroom_id=$chatroomId"
        val shareTitle = context.getString(R.string.share_chatroom)
        shareLink(context, shareLink, shareTitle)
    }

    //create intent and open sharing options without link as text
    private fun shareLink(context: Context, shareLink: String, shareTitle: String) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareLink)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, shareTitle)
        }
        val shareIntent = Intent.createChooser(sendIntent, null)
        context.startActivity(shareIntent)
    }
}