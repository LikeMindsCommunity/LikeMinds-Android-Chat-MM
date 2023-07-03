package com.likeminds.chatmm.utils.mediauploader.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkInfo
import androidx.work.WorkerParameters
import com.likeminds.chatmm.SDKApplication
import com.likeminds.chatmm.utils.mediauploader.model.GenericFileRequest
import com.likeminds.chatmm.utils.mediauploader.model.WORKER_FAILURE
import com.likeminds.chatmm.utils.mediauploader.model.WORKER_RETRY
import com.likeminds.chatmm.utils.mediauploader.model.WORKER_SUCCESS
import com.likeminds.chatmm.utils.mediauploader.utils.WorkerUtil.getIntOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

abstract class MediaUploadWorker(
    appContext: Context,
    private val params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    protected val transferUtility by lazy { SDKApplication.getInstance().transferUtility }

    private var uploadedCount = 0
    protected val failedIndex by lazy { ArrayList<Int>() }
    protected lateinit var uploadList: ArrayList<GenericFileRequest>
    protected val thumbnailMediaMap by lazy { HashMap<Int, Pair<String?, String?>>() }
    private val progressMap by lazy { HashMap<Int, Pair<Long, Long>>() }

    abstract fun checkArgs()
    abstract fun init()
    abstract fun uploadFiles(continuation: Continuation<Int>)

    companion object {
        const val ARG_MEDIA_INDEX_LIST = "ARG_MEDIA_INDEX_LIST"
        const val ARG_PROGRESS = "ARG_PROGRESS"

        fun getProgress(workInfo: WorkInfo): Pair<Long, Long>? {
            val progress = workInfo.progress.getLongArray(ARG_PROGRESS)
            if (progress == null || progress.size != 2) {
                return null
            }
            return Pair(progress[0], progress[1])
        }
    }

    fun require(key: String) {
        if (!containsParam(key)) {
            throw Error("$key is required")
        }
    }

    override suspend fun doWork(): Result {
        try {
            checkArgs()
            init()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure()
        }
        return withContext(Dispatchers.IO) {
            val result = suspendCoroutine<Int> { continuation ->
                uploadFiles(continuation)
            }
            return@withContext when (result) {
                WORKER_SUCCESS -> {
                    Result.success()
                }
                WORKER_RETRY -> {
                    Result.retry()
                }
                else -> {
                    getFailureResult(failedIndex.toIntArray())
                }
            }
        }
    }

    private fun getFailureResult(failedArrayIndex: IntArray = IntArray(0)): Result {
        return Result.failure(
            Data.Builder()
                .putIntArray(ARG_MEDIA_INDEX_LIST, failedArrayIndex)
                .build()
        )
    }

    protected fun setProgress(id: Int, bytesCurrent: Long, bytesTotal: Long) {
        progressMap[id] = Pair(bytesCurrent, bytesTotal)
        var averageBytesCurrent = 0L
        var averageBytesTotal = 0L
        progressMap.values.forEach {
            averageBytesCurrent += it.first
            averageBytesTotal += it.second
        }
        if (averageBytesCurrent > 0L && averageBytesTotal > 0L) {
            setProgressAsync(
                Data.Builder()
                    .putLongArray(ARG_PROGRESS, longArrayOf(averageBytesCurrent, averageBytesTotal))
                    .build()
            )
        }
    }

    protected fun getStringParam(key: String): String {
        return params.inputData.getString(key)
            ?: throw Error("$key is required")
    }

    protected fun getIntParam(key: String): Int {
        return params.inputData.getIntOrNull(key)
            ?: throw Error("$key is required")
    }

    protected fun containsParam(key: String): Boolean {
        return params.inputData.keyValueMap.containsKey(key)
    }

//    protected fun createAWSRequestList(
//        thumbnailsToUpload: List<Attachment>,
//        attachmentsToUpload: List<Attachment>
//    ): ArrayList<GenericFileRequest> {
//        val awsFileRequestList = ArrayList<GenericFileRequest>()
//        thumbnailsToUpload.forEach { attachment ->
//            val request = GenericFileRequest.Builder()
//                .fileType(attachment.type)
//                .awsFolderPath(attachment.thumbnailAWSFolderPath ?: "")
//                .localFilePath(attachment.thumbnailLocalFilePath)
//                .index(attachment.index ?: 0)
//                .isThumbnail(true)
//                .build()
//            awsFileRequestList.add(request)
//        }
//        attachmentsToUpload.forEach { attachment ->
//            val request = GenericFileRequest.Builder()
//                .name(attachment.name)
//                .fileType(attachment.type)
//                .awsFolderPath(attachment.awsFolderPath)
//                .localFilePath(attachment.localFilePath)
//                .index(attachment.index)
//                .width(attachment.width)
//                .height(attachment.height)
//                .hasThumbnail(attachment.thumbnailAWSFolderPath != null)
//                .meta(attachment.meta)
//                .build()
//            awsFileRequestList.add(request)
//        }
//        return awsFileRequestList
//    }

//    protected fun uploadUrl(
//        downloadUri: Pair<String?, String?>?,
//        totalMediaCount: Int,
//        awsFileResponse: AWSFileResponse,
//        totalFilesToUpload: Int,
//        collabcardAnswerRO: CollabcardAnswerRO,
//        continuation: Continuation<Int>
//    ) {
//        val fileRequest = UploadConversationFileRequest.Builder()
//            .name(awsFileResponse.name())
//            .conversationId(collabcardAnswerRO.id)
//            .filesCount(totalMediaCount)
//            .url(downloadUri?.first)
//            .thumbnailUrl(downloadUri?.second)
//            .type(awsFileResponse.fileType)
//            .index(awsFileResponse.index)
//            .width(awsFileResponse.width)
//            .height(awsFileResponse.height)
//            .meta(awsFileResponse.meta)
//            .build()
//        runBlocking {
//            when (val uploadResponse = fileApi.uploadFileForConversation(fileRequest)) {
//                is NetworkResponse.Success -> {
//                    uploadUrlCompletes(
//                        uploadResponse.body.data, totalFilesToUpload, collabcardAnswerRO,
//                        awsFileResponse, continuation
//                    )
//                }
//                is NetworkResponse.Error -> {
//                    failedIndex.add(awsFileResponse.index())
//                    checkWorkerComplete(totalFilesToUpload, continuation)
//                }
//            }
//        }
//    }

//    private fun uploadUrlCompletes(
//        response: UploadResponse?,
//        totalFilesToUpload: Int,
//        collabcardAnswerRO: CollabcardAnswerRO,
//        awsFileResponse: AWSFileResponse,
//        continuation: Continuation<Int>
//    ) {
//        var conversation = response?.conversation
//        if (conversation != null) {
//            uploadedCount += 1
//            if (totalFilesToUpload != uploadedCount) {
//                conversation = conversation.toBuilder()
//                    .uploadWorkerUUID(collabcardAnswerRO.uploadWorkerUUID)
//                    .build()!!
//            }
//            chatRoomDb.updateConversation(conversation)
//        } else {
//            failedIndex.add(awsFileResponse.index)
//        }
//        checkWorkerComplete(totalFilesToUpload, continuation)
//    }

    protected fun checkWorkerComplete(
        totalFilesToUpload: Int,
        continuation: Continuation<Int>
    ) {
        if (totalFilesToUpload == uploadedCount + failedIndex.size) {
            if (totalFilesToUpload == uploadedCount) {
                continuation.resume(WORKER_SUCCESS)
            } else {
                continuation.resume(WORKER_FAILURE)
            }
        }
    }
}