package com.kimi.community.ui.create

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kimi.community.data.model.CreateMomentImage
import com.kimi.community.data.model.CreateMomentRequest
import com.kimi.community.data.model.FileType
import com.kimi.community.data.repository.KimiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.InputStream
import java.util.UUID

sealed interface CreateMomentUiState {
    data object Idle : CreateMomentUiState
    data object Uploading : CreateMomentUiState
    data object Publishing : CreateMomentUiState
    data class Success(val momentId: String) : CreateMomentUiState
    data class Error(val message: String) : CreateMomentUiState
}

data class UploadedImage(
    val fileId: String,
    val originUrl: String?,
    val width: Int?,
    val height: Int?
)

class CreateMomentViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = KimiRepository()
    private val httpClient = OkHttpClient()

    private val _uiState = MutableStateFlow<CreateMomentUiState>(CreateMomentUiState.Idle)
    val uiState: StateFlow<CreateMomentUiState> = _uiState.asStateFlow()

    private val _uploadedImages = MutableStateFlow<List<UploadedImage>>(emptyList())
    val uploadedImages: StateFlow<List<UploadedImage>> = _uploadedImages.asStateFlow()

    private val _selectedImageUris = MutableStateFlow<List<Uri>>(emptyList())
    val selectedImageUris: StateFlow<List<Uri>> = _selectedImageUris.asStateFlow()

    fun addImage(uri: Uri) {
        if (_selectedImageUris.value.size >= 9) return
        _selectedImageUris.value = _selectedImageUris.value + uri
    }

    fun removeImage(uri: Uri) {
        _selectedImageUris.value = _selectedImageUris.value - uri
    }

    fun publishMoment(text: String, title: String? = null, hashtagIds: List<String>? = null) {
        if (_uiState.value is CreateMomentUiState.Uploading ||
            _uiState.value is CreateMomentUiState.Publishing) return

        viewModelScope.launch {
            try {
                // 1. 上传图片
                if (_selectedImageUris.value.isNotEmpty()) {
                    _uiState.value = CreateMomentUiState.Uploading
                    val uploaded = mutableListOf<UploadedImage>()
                    for (uri in _selectedImageUris.value) {
                        val result = uploadImage(uri)
                        if (result != null) {
                            uploaded.add(result)
                        }
                    }
                    _uploadedImages.value = uploaded
                }

                // 2. 发布动态
                _uiState.value = CreateMomentUiState.Publishing
                val images = _uploadedImages.value.map { img ->
                    CreateMomentImage(
                        fileId = img.fileId,
                        extra = com.kimi.community.data.model.CreateMomentImageExtra(
                            width = img.width,
                            height = img.height
                        )
                    )
                }

                val response = repository.createMoment(
                    CreateMomentRequest(
                        text = text.ifBlank { null },
                        title = title,
                        images = images.ifEmpty { null },
                        hashtagIds = hashtagIds
                    )
                )

                val momentId = response.moment?.id
                if (momentId != null) {
                    _uiState.value = CreateMomentUiState.Success(momentId)
                } else {
                    _uiState.value = CreateMomentUiState.Error("发布失败：未返回动态ID")
                }
            } catch (e: Exception) {
                _uiState.value = CreateMomentUiState.Error(e.message ?: "发布失败")
            }
        }
    }

    private suspend fun uploadImage(uri: Uri): UploadedImage? = withContext(Dispatchers.IO) {
        try {
            val context = getApplication<Application>()
            val contentResolver = context.contentResolver

            // 1. 获取预签名 URL
            val presigned = repository.generatePresignedURL(FileType.IMAGE)
            val uploadUrl = presigned.uploadUrl ?: return@withContext null
            val fileId = presigned.fileId ?: return@withContext null

            // 2. 读取图片数据并上传
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bytes = inputStream?.use { it.readBytes() } ?: return@withContext null
            inputStream.close()

            val requestBody = bytes.toRequestBody("image/*".toMediaTypeOrNull())
            val request = Request.Builder()
                .url(uploadUrl)
                .put(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext null
                }
            }

            // 3. 登记文件
            val fileName = "image_${UUID.randomUUID()}.jpg"
            val fileResponse = repository.createFile(fileId = fileId, fileName = fileName)

            UploadedImage(
                fileId = fileResponse.fileId ?: fileId,
                originUrl = fileResponse.originUrl,
                width = null,
                height = null
            )
        } catch (e: Exception) {
            null
        }
    }

    fun reset() {
        _uiState.value = CreateMomentUiState.Idle
        _uploadedImages.value = emptyList()
        _selectedImageUris.value = emptyList()
    }
}
