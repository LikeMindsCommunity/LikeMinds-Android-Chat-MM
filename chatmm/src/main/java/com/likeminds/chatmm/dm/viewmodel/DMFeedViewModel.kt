package com.likeminds.chatmm.dm.viewmodel

import android.util.Log
import androidx.lifecycle.*
import com.likeminds.chatmm.dm.model.DMFeedEmptyViewData
import com.likeminds.chatmm.dm.model.DMRequestFrom
import com.likeminds.chatmm.homefeed.model.HomeFeedItemViewData
import com.likeminds.chatmm.homefeed.util.HomeFeedUtil
import com.likeminds.chatmm.member.util.UserPreferences
import com.likeminds.chatmm.utils.coroutine.launchIO
import com.likeminds.chatmm.utils.model.BaseViewType
import com.likeminds.likemindschat.LMChatClient
import com.likeminds.likemindschat.chatroom.model.Chatroom
import com.likeminds.likemindschat.dm.model.CheckDMStatusRequest
import com.likeminds.likemindschat.dm.model.CheckDMStatusResponse
import com.likeminds.likemindschat.homefeed.util.HomeChatroomListener
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class DMFeedViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    companion object {
        private const val TAG = "DMFeed VM"
    }

    private val lmChatClient = LMChatClient.getInstance()

    private val compositeDisposable = CompositeDisposable()

    private val allChatRoomsData = mutableListOf<HomeFeedItemViewData>()

    sealed class DMFeedEvent {
        object ShowDMFeedData : DMFeedEvent()
    }

    private val dmFeedChannel = Channel<DMFeedEvent>(Channel.BUFFERED)
    val dmFeedFlow = dmFeedChannel.receiveAsFlow()

    private val errorMessageChannel = Channel<ErrorMessageEvent>(Channel.BUFFERED)
    val errorMessageEventFlow = errorMessageChannel.receiveAsFlow()

    private val _checkDMResponse by lazy { MutableLiveData<CheckDMStatusResponse>() }
    val checkDMResponse: LiveData<CheckDMStatusResponse> by lazy { _checkDMResponse }

    sealed class ErrorMessageEvent {
        data class GetDMChatroom(val errorMessage: String?) : ErrorMessageEvent()
        data class CheckDMStatus(val errorMessage: String?) : ErrorMessageEvent()
    }

    private val chatroomListener = object : HomeChatroomListener() {
        override fun initial(chatrooms: List<Chatroom>) {
            showInitialChatrooms(chatrooms)
        }

        override fun onChanged(
            removedIndex: List<Int>,
            inserted: List<Pair<Int, Chatroom>>,
            changed: List<Pair<Int, Chatroom>>
        ) {
            updateChatroomChanges(removedIndex, inserted, changed)
        }

        override fun onError(throwable: Throwable) {
            viewModelScope.launchIO {
                errorMessageChannel.send(ErrorMessageEvent.GetDMChatroom(throwable.message))
            }
            Log.e(TAG, "chatroomListener", throwable)
        }
    }

    //returns initial list of chatrooms
    private fun showInitialChatrooms(chatrooms: List<Chatroom>) {
        viewModelScope.launch {
            val chatViewDataList = chatrooms.map { chatroom ->
                HomeFeedUtil.getChatRoomViewData(chatroom, userPreferences)
            }
            allChatRoomsData.clear()
            allChatRoomsData.addAll(chatViewDataList)
            dmFeedChannel.send(DMFeedEvent.ShowDMFeedData)
        }
    }

    //returns the updates to the intial list
    private fun updateChatroomChanges(
        removedId: List<Int>,
        inserted: List<Pair<Int, Chatroom>>,
        changed: List<Pair<Int, Chatroom>>
    ) = viewModelScope.launch {
        removedId.forEach { index ->
            allChatRoomsData.removeAt(index)
        }
        val insertedChatViewDataList = inserted.map { pair ->
            Pair(pair.first, HomeFeedUtil.getChatRoomViewData(pair.second, userPreferences))
        }
        val changedChatViewDataList = changed.map { pair ->
            Pair(pair.first, HomeFeedUtil.getChatRoomViewData(pair.second, userPreferences))
        }

        insertedChatViewDataList.forEach { pair ->
            val index = pair.first
            val chatViewData = pair.second
            allChatRoomsData.add(index, chatViewData)
        }

        changedChatViewDataList.forEach { pair ->
            val index = pair.first
            val chatViewData = pair.second
            allChatRoomsData[index] = chatViewData
        }
        dmFeedChannel.send(DMFeedEvent.ShowDMFeedData)
    }

    //returns the list of dm chatrooms based on conditions
    fun fetchDMChatrooms(): List<BaseViewType> {
        val dataList = mutableListOf<BaseViewType>()
        when {
            allChatRoomsData.isNotEmpty() -> {
                dataList.addAll(allChatRoomsData)
            }

            else -> {
                dataList.add(getEmptyChatView())
            }
        }
        return dataList
    }

    private fun getEmptyChatView(): DMFeedEmptyViewData {
        return DMFeedEmptyViewData.Builder().build()
    }

    //observes all dm chatrooms from local and return in [chatroomListener]
    fun observeDMChatrooms() {
        compositeDisposable.clear()
        val disposable =
            lmChatClient.observeDMChatrooms(chatroomListener)
                ?.subscribeOn(Schedulers.computation())
                ?.observeOn(Schedulers.newThread())
                ?.subscribe() ?: return
        compositeDisposable.add(disposable)
    }

    //check whether user can intiate DM or not
    fun checkDMStatus() {
        viewModelScope.launchIO {

            val request = CheckDMStatusRequest.Builder()
                .requestFrom(DMRequestFrom.DM_FEED.value)
                .build()

            val response = lmChatClient.checkDMStatus(request)

            if (response.success) {
                val data = response.data
                data?.let {
                    _checkDMResponse.postValue(it)
                }
            } else {
                errorMessageChannel.send(ErrorMessageEvent.CheckDMStatus(response.errorMessage))
            }
        }
    }

    override fun onCleared() {
        compositeDisposable.dispose()
        super.onCleared()
    }
}