package com.ml.shubham0204.clipandroid

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.ml.shubham0204.clipandroid.ui.components.AppAlertDialog
import com.ml.shubham0204.clipandroid.ui.components.AppProgressDialog
import com.ml.shubham0204.clipandroid.ui.components.AppTooltip
import com.ml.shubham0204.clipandroid.ui.components.createAlertDialog
import com.ml.shubham0204.clipandroid.ui.components.hideProgressDialog
import com.ml.shubham0204.clipandroid.ui.components.setProgressDialogText
import com.ml.shubham0204.clipandroid.ui.components.setProgressDialogTitle
import com.ml.shubham0204.clipandroid.ui.components.showProgressDialog
import com.ml.shubham0204.clipandroid.ui.theme.CLIPAndroidTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.core.app.ActivityCompat

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val permissions = if (Build.VERSION.SDK_INT >= 33) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        ActivityCompat.requestPermissions(
            this,
            permissions,
            0
        )

        setContent {
            CLIPAndroidTheme {
                val viewModel: MainActivityViewModel = viewModel()
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    // Changed from bottomBar to topBar to display at the top
                    topBar = { CustomBottomBar(viewModel) }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        PhotosList(viewModel)
                        if (viewModel.isShowingResultsState.value) {
                            VectorSearchInfo(viewModel)
                        } else {
                            QueryInput(viewModel)
                        }
                    }
                    // Progress and dialog components remain as before
                    LoadModelProgressDialog(viewModel)
                    InsertImagesProgressDialog(viewModel)
                    ModelInfoDialog(viewModel)
                    RunningInferenceProgressDialog(viewModel)
                    BackHandler(viewModel.isShowingResultsState.value) { viewModel.closeResults() }
                }
            }
        }
    }

    @Composable
    private fun CustomBottomBar(viewModel: MainActivityViewModel) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (viewModel.isShowingResultsState.value) {
                    AppTooltip(tooltip = "Close Results") {
                        IconButton(onClick = { viewModel.closeResults() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Results",
                                tint = Color.White
                            )
                        }
                    }
                    Text(
                        text = "Search Results",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(48.dp))
                } else {
                    Row {
                        AddPhotos(viewModel)
                        Spacer(modifier = Modifier.width(4.dp))
                        LazyLoadAllImages(viewModel)
                        Spacer(modifier = Modifier.width(4.dp))
                        RemoveAllPhotos(viewModel)
                        Spacer(modifier = Modifier.width(4.dp))
                        AppTooltip(tooltip = "Model Info") {
                            IconButton(onClick = { viewModel.showModelInfo() }) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Model Info",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                    Text(
                        text = "Photos",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(48.dp))
                }
            }
        }
    }

    @Composable
    private fun LazyLoadAllImages(viewModel: MainActivityViewModel) {
        val context = LocalContext.current
        AppTooltip(tooltip = "Load All Images") {
            IconButton(onClick = {
                viewModel.loadAllImagesFromMediaStore(context)
            }) {
                // Uncomment and use the appropriate icon if needed.
                // Icon(
                //     imageVector = Icons.Default.PhotoLibrary,
                //     contentDescription = "Load All Images",
                //     tint = MaterialTheme.colorScheme.onPrimary
                // )
            }
        }
    }

    @Composable
    private fun AddPhotos(viewModel: MainActivityViewModel) {
        var selectedImagesUris by remember { viewModel.selectedImagesUriListState }
        val context = LocalContext.current
        val pickVisualMediaLauncher =
            rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickMultipleVisualMedia()
            ) { uris ->
                selectedImagesUris = uris
                selectedImagesUris.forEach { uri ->
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                viewModel.addImagesToDB(context)
            }
        AppTooltip(tooltip = "Add Photos") {
            IconButton(
                onClick = {
                    pickVisualMediaLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Photos",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }

    @Composable
    private fun RemoveAllPhotos(viewModel: MainActivityViewModel) {
        AppTooltip(tooltip = "Remove All Photos") {
            IconButton(
                onClick = {
                    createAlertDialog(
                        dialogTitle = "Remove All Photos",
                        dialogText = "Are you sure you want to remove all photos?",
                        dialogPositiveButtonText = "Yes",
                        dialogNegativeButtonText = "No",
                        onPositiveButtonClick = { viewModel.removeAllImages() },
                        onNegativeButtonClick = {}
                    )
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove All Photos",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        AppAlertDialog()
    }

    @Composable
    private fun ColumnScope.PhotosList(viewModel: MainActivityViewModel) {
        val selectedImagesUris by remember { viewModel.selectedImagesUriListState }
        LaunchedEffect(0) { viewModel.loadImages() }
        LazyVerticalStaggeredGrid(
            modifier = Modifier
                .background(Color.Transparent)
                .fillMaxWidth()
                .weight(1f),
            columns = StaggeredGridCells.Fixed(2),
            contentPadding = PaddingValues(4.dp)
        ) {
            items(selectedImagesUris) { uri ->
                Card(
                    modifier = Modifier.padding(4.dp),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    AsyncImage(
                        modifier = Modifier.fillMaxWidth(),
                        model = uri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }

    @Composable
    fun QueryInput(viewModel: MainActivityViewModel) {
        var queryText by remember { viewModel.queryTextState }
        val keyboardController = LocalSoftwareKeyboardController.current
        val context = LocalContext.current // Lấy context từ LocalContext

        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                modifier = Modifier.fillMaxWidth().weight(1f),
                value = queryText,
                onValueChange = { queryText = it },
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    disabledTextColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                placeholder = { Text(text = "Enter query...") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                enabled = queryText.isNotEmpty(),
                modifier = Modifier.background(Color.Blue, CircleShape),
                onClick = {
                    keyboardController?.hide()
                    viewModel.processQuery(context) // Truyền context vào đây
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Send query",
                    tint = Color.White
                )
            }
        }
    }

    @Composable
    private fun VectorSearchInfo(viewModel: MainActivityViewModel) {
        val vectorSearchResults by remember { viewModel.vectorSearchResultsState }
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary)
                .fillMaxWidth()
        ) {
            vectorSearchResults?.let { results ->
                Log.d("PhotosListScreen", "Scores: ${results.scores}")
                Text(
                    text = "Vector Search took ${results.timeTakenMillis} ms for ${results.numVectorsSearched} vectors",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp),
                    color = Color.White
                )
            }
        }
    }

    @Composable
    private fun InsertImagesProgressDialog(viewModel: MainActivityViewModel) {
        val isInsertingImages by remember { viewModel.isInsertingImagesState }
        val insertedImagesCount by remember { viewModel.insertedImagesCountState }
        if (isInsertingImages) {
            showProgressDialog()
            setProgressDialogTitle("Inserting images...")
            setProgressDialogText("Inserted $insertedImagesCount images")
        } else {
            hideProgressDialog()
        }
        AppProgressDialog()
    }

    @Composable
    private fun LoadModelProgressDialog(viewModel: MainActivityViewModel) {
        val isLoadingModel by remember { viewModel.isLoadingModelState }
        if (isLoadingModel) {
            showProgressDialog()
            setProgressDialogText("Loading model...")
        } else {
            hideProgressDialog()
        }
        AppProgressDialog()
    }

    @Composable
    private fun ModelInfoDialog(viewModel: MainActivityViewModel) {
        var showDialog by remember { viewModel.isShowingModelInfoDialogState }
        if (
            showDialog &&
            viewModel.visionHyperParameters != null &&
            viewModel.textHyperParameters != null
        ) {
            Dialog(onDismissRequest = { showDialog = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Model Info",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.headlineLarge
                        )
                        IconButton(onClick = { showDialog = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Model Info Dialog"
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Vision Hyper-parameters",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "imageSize = ${viewModel.visionHyperParameters?.imageSize}",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "hiddenSize = ${viewModel.visionHyperParameters?.hiddenSize}",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "patchSize = ${viewModel.visionHyperParameters?.patchSize}",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "projectionDim = ${viewModel.visionHyperParameters?.projectionDim}",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "num layers = ${viewModel.visionHyperParameters?.nLayer}",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "num intermediate = ${viewModel.visionHyperParameters?.nIntermediate}",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "num heads = ${viewModel.visionHyperParameters?.nHead}",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Text Hyper-parameters",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "num positions = ${viewModel.textHyperParameters?.numPositions}",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "hiddenSize = ${viewModel.textHyperParameters?.hiddenSize}",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "num vocab = ${viewModel.textHyperParameters?.nVocab}",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "projectionDim = ${viewModel.textHyperParameters?.projectionDim}",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "num layers = ${viewModel.textHyperParameters?.nLayer}",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "num intermediate = ${viewModel.textHyperParameters?.nIntermediate}",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "num heads = ${viewModel.textHyperParameters?.nHead}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }

    @Composable
    private fun RunningInferenceProgressDialog(viewModel: MainActivityViewModel) {
        val isInferenceRunning by remember { viewModel.isInferenceRunningState }
        if (isInferenceRunning) {
            showProgressDialog()
            setProgressDialogText("Running inference...")
        } else {
            hideProgressDialog()
        }
        AppProgressDialog()
    }
}

