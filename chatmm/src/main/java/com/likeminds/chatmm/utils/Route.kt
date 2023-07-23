package com.likeminds.chatmm.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.likeminds.chatmm.LMAnalytics
import com.likeminds.chatmm.chatroom.detail.model.ChatroomDetailExtras
import com.likeminds.chatmm.chatroom.detail.view.ChatroomDetailActivity
import com.likeminds.chatmm.member.model.MemberViewData

object Route {
    // todo:
    const val ROUTE_CHATROOM = "collabcard"
    const val ROUTE_CHATROOM_DETAIL = "chatroom_detail"
    const val ROUTE_BROWSER = "browser"
    const val ROUTE_MAIL = "mail"
    const val ROUTE_MEMBER = "member"
    const val ROUTE_MEMBER_PROFILE = "member_profile"

    const val PARAM_CHATROOM_ID = "chatroom_id"
    const val PARAM_COMMUNITY_ID = "community_id"
    private const val PARAM_COHORT_ID = "cohort_id"

    fun handleDeepLink(context: Context, url: String?): Intent? {
        val data = Uri.parse(url) ?: return null
        if (data.pathSegments.isNullOrEmpty()) {
            return null
        }
        val firstPath = getRouteFromDeepLink(data) ?: return null
        return getRouteIntent(
            context,
            firstPath,
            0
        )
    }

    //create route string as per uri with check for the host (likeminds)
    private fun getRouteFromDeepLink(data: Uri?): String? {
        val host = data?.host ?: return null
        val firstPathSegment = data.pathSegments.firstOrNull()
//        if (host == getLmWebHost() && firstPathSegment == DEEP_LINK_CHATROOM) {
//            return createCollabcardRoute(data)
//        }
//        if (!isLMHost(host)) {
//            return createWebsiteRoute(data)
//        }
        return null
//        return when {
//            data.scheme == DEEP_LINK_SCHEME -> {
//                when (firstPathSegment) {
//                    DEEP_LINK_COMMUNITY_FEED -> {
//                        createCommunityFeedRoute(data)
//                    }
//                    DEEP_LINK_CREATE_COMMUNITY -> {
//                        createCommunityCreateRoute()
//                    }
//                    else -> null
//                }
//            }
//            firstPathSegment == DEEP_LINK_CHATROOM -> {
//                createCollabcardRoute(data)
//            }
//            else -> {
//                createWebsiteRoute(data)
//            }
//        }
    }

    // todo: removed profle routes
    fun getRouteIntent(
        context: Context,
        routeString: String,
        flags: Int = 0,
        source: String? = null,
        map: HashMap<String, String>? = null,
        deepLinkUrl: String? = null
    ): Intent? {
        val route = Uri.parse(routeString)
        var intent: Intent? = null
        when {
            route.host == ROUTE_CHATROOM -> {
                intent = getRouteToChatroom(
                    context,
                    getChatroomRouteWithExtraParameters(route, map),
                    source,
                    deepLinkUrl
                )
            }
            route.host == ROUTE_BROWSER -> {
                intent = getRouteToBrowser(route)
            }
            route.host == ROUTE_CHATROOM_DETAIL -> {
                intent = getRouteToChatRoomDetail(context, route, source, deepLinkUrl)
            }
            route.host == ROUTE_MAIL -> {
                intent = getRouteToMail(route)
            }
        }
        if (intent != null) {
            intent.flags = flags
        }
        return intent
    }

    //route://collabcard?collabcard_id=
    private fun getRouteToChatroom(
        context: Context,
        route: Uri,
        source: String?,
        deepLinkUrl: String?
    ): Intent {
        val chatroomId = route.getQueryParameter("collabcard_id")
        val sourceChatroomId = route.getQueryParameter(PARAM_CHATROOM_ID)
        val sourceCommunityId = route.getQueryParameter(PARAM_COMMUNITY_ID)
        val cohortId = route.getQueryParameter(PARAM_COHORT_ID)

        val builder = ChatroomDetailExtras.Builder()
            .chatroomId(chatroomId.toString())
            .source(source)
            .sourceChatroomId(sourceChatroomId)
            .sourceCommunityId(sourceCommunityId)
            .cohortId(cohortId)

        when (source) {
            LMAnalytics.Source.NOTIFICATION -> {
                builder.fromNotification(true).sourceLinkOrRoute(route.toString())
            }

            LMAnalytics.Source.DEEP_LINK -> {
                builder.openedFromLink(true).sourceLinkOrRoute(deepLinkUrl)
            }
        }

        return ChatroomDetailActivity.getIntent(
            context,
            builder.build()
        )
    }

    /**
     * * This function is used to add extra parameters in route
     */
    private fun getChatroomRouteWithExtraParameters(
        route: Uri,
        extraParameters: HashMap<String, String>?,
    ): Uri {
        return if (extraParameters != null) {
            appendQueryParametersToRoute(route, extraParameters)
        } else {
            route
        }
    }

    private fun appendQueryParametersToRoute(uri: Uri, map: HashMap<String, String>): Uri {
        val query = uri.queryParameterNames
        val newUri = Uri.Builder()
            .scheme(uri.scheme)
            .authority(uri.authority)
            .path(uri.path)
        for (item in map) {
            newUri.appendQueryParameter(item.key, item.value)
        }
        for (item in query) {
            newUri.appendQueryParameter(item, uri.getQueryParameter(item))
        }
        return newUri.build()
    }

    private fun getRouteToBrowser(route: Uri): Intent {
        return Intent(Intent.ACTION_VIEW, Uri.parse(route.getQueryParameter("link")))
    }

    //route://chatroom_detail?chatroom_id=
    private fun getRouteToChatRoomDetail(
        context: Context,
        route: Uri,
        source: String?,
        deepLinkUrl: String?
    ): Intent {
        val chatroomId = route.getQueryParameter("chatroom_id")
        val conversationId = route.getQueryParameter("conversation_id")

        val builder = ChatroomDetailExtras.Builder()
            .chatroomId(chatroomId.toString())
            .conversationId(conversationId)
            .source(source)

        when (source) {
            LMAnalytics.Source.NOTIFICATION -> {
                builder.fromNotification(true).sourceLinkOrRoute(route.toString())
            }

            LMAnalytics.Source.DEEP_LINK -> {
                builder.openedFromLink(true).sourceLinkOrRoute(deepLinkUrl)
            }
        }

        return ChatroomDetailActivity.getIntent(
            context,
            builder.build()
        )
    }

    //route://mail?to=<email>
    private fun getRouteToMail(route: Uri): Intent? {
        val sendTo = route.getQueryParameter("to")
        if (AuthValidator.isValidEmail(sendTo)) {
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("mailto:$sendTo")
            return Intent.createChooser(intent, "Select an email client")
        }
        return null
    }

    fun createRouteForMemberProfile(member: MemberViewData?, communityId: String?): String {
        return "<<${member?.name}|route://member/${member?.id}?community_id=${communityId}>>"
    }

    fun Uri.getNullableQueryParameter(key: String): String? {
        val value = this.getQueryParameter(key)
        return if (value == "null") {
            null
        } else {
            value
        }
    }
}