package com.likeminds.chatmm.chatroom.explore.view.adapter

import com.likeminds.chatmm.chatroom.explore.view.adapter.databinder.ChatroomExploreViewDataBinder
import com.likeminds.chatmm.homefeed.view.adapter.databinder.HomeBlankSpaceItemViewDataBinder
import com.likeminds.chatmm.utils.customview.BaseRecyclerAdapter
import com.likeminds.chatmm.utils.customview.ViewDataBinder
import com.likeminds.chatmm.utils.model.BaseViewType

// todo: ask about hide secret
internal class ChatroomExploreAdapter(
    private val listener: ExploreClickListener,
    private val isHideSecretChatroomLockIcon: Boolean
) : BaseRecyclerAdapter<BaseViewType>() {

    init {
        initViewDataBinders()
    }

    override fun getSupportedViewDataBinder(): MutableList<ViewDataBinder<*, *>> {
        val viewDataBinders = ArrayList<ViewDataBinder<*, *>>(2)

        val chatroomExploreViewDataBinder =
            ChatroomExploreViewDataBinder(listener, isHideSecretChatroomLockIcon)
        viewDataBinders.add(chatroomExploreViewDataBinder)

        val homeBlankSpaceItemViewDataBinder = HomeBlankSpaceItemViewDataBinder()
        viewDataBinders.add(homeBlankSpaceItemViewDataBinder)
        return viewDataBinders
    }
}