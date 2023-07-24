package com.likeminds.chatmm.conversation.view.adapter.databinder

import android.view.LayoutInflater
import android.view.ViewGroup
import com.likeminds.chatmm.R
import com.likeminds.chatmm.branding.model.LMBranding
import com.likeminds.chatmm.chatroom.detail.view.ChatroomDetailFragment
import com.likeminds.chatmm.chatroom.detail.view.adapter.ChatroomDetailAdapterListener
import com.likeminds.chatmm.databinding.ItemConversationFollowBinding
import com.likeminds.chatmm.utils.customview.ViewDataBinder
import com.likeminds.chatmm.utils.model.BaseViewType
import com.likeminds.chatmm.utils.model.ITEM_CONVERSATION_FOLLOW

internal class ConversationFollowItemViewDataBinder constructor(
    private val adapterListener: ChatroomDetailAdapterListener,
) : ViewDataBinder<ItemConversationFollowBinding, BaseViewType>() {

    override val viewType: Int
        get() = ITEM_CONVERSATION_FOLLOW

    override fun createBinder(parent: ViewGroup): ItemConversationFollowBinding {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemConversationFollowBinding.inflate(inflater, parent, false)
        binding.btnFollow.setOnClickListener {
            adapterListener.onScreenChanged()
            adapterListener.follow(true, ChatroomDetailFragment.SOURCE_CHAT_ROOM_TELESCOPE)
        }
        return binding
    }

    override fun bindData(
        binding: ItemConversationFollowBinding,
        data: BaseViewType,
        position: Int,
    ) {
        binding.apply {
            buttonColor = LMBranding.getButtonsColor()
            val context = root.context
            if (adapterListener.getChatRoom()?.isSecret == true) {
                tvFollowConst.text =
                    context.getString(R.string.join_chatroom_message)
                btnFollow.text = context.getString(R.string.join)
            } else {
                tvFollowConst.text =
                    context.getString(R.string.join_chatroom_message)
                btnFollow.text = context.getString(R.string.join_small)
            }
        }
    }
}