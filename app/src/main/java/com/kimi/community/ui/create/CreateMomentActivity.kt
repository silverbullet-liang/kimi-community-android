package com.kimi.community.ui.create

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.kimi.community.ui.theme.KimiCommunityTheme

class CreateMomentActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KimiCommunityTheme {
                CreateMomentScreen(
                    onBack = { finish() },
                    onPublished = { momentId ->
                        setResult(Activity.RESULT_OK, Intent().putExtra("momentId", momentId))
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMomentScreen(
    onBack: () -> Unit,
    onPublished: (String) -> Unit
) {
    val viewModel: CreateMomentViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val selectedImages by viewModel.selectedImageUris.collectAsState()

    var text by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { uri -> viewModel.addImage(uri) }
    }

    LaunchedEffect(uiState) {
        if (uiState is CreateMomentUiState.Success) {
            onPublished((uiState as CreateMomentUiState.Success).momentId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("发布动态", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.publishMoment(text = text, title = title.ifBlank { null })
                        },
                        enabled = text.isNotBlank() || selectedImages.isNotEmpty(),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (uiState is CreateMomentUiState.Publishing || uiState is CreateMomentUiState.Uploading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp))
                        } else {
                            Text("发布", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 标题输入
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("标题（可选）") },
                singleLine = true,
                maxLines = 1
            )

            // 正文输入
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = { Text("分享你的想法...") },
                maxLines = 10
            )

            // 图片选择网格
            Text("图片（最多9张）", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(selectedImages) { uri ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(uri),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { viewModel.removeImage(uri) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(24.dp)
                                .padding(2.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "删除",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // 添加图片按钮
                if (selectedImages.size < 9) {
                    item {
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { imagePickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "添加图片",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "${selectedImages.size}/9",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // 错误提示
    if (uiState is CreateMomentUiState.Error) {
        AlertDialog(
            onDismissRequest = { viewModel.reset() },
            title = { Text("发布失败") },
            text = { Text((uiState as CreateMomentUiState.Error).message) },
            confirmButton = {
                TextButton(onClick = { viewModel.reset() }) {
                    Text("确定")
                }
            }
        )
    }
}
