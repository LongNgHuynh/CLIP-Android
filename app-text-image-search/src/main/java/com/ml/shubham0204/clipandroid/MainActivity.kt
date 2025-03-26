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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.window.DialogProperties
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Slider
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.style.TextAlign


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
                var showLoadImagesDialog by remember { mutableStateOf(true) }
                val context = LocalContext.current

                // Load images dialog shown on start
                if (showLoadImagesDialog) {
                    Dialog(onDismissRequest = { showLoadImagesDialog = false }) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Load All Images",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Do you want to load all images from your device?",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = { showLoadImagesDialog = false },
                                        colors = ButtonDefaults.textButtonColors()
                                    ) {
                                        Text("No")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            showLoadImagesDialog = false
                                            viewModel.loadAllImagesFromMediaStore(context)
                                        },
                                        colors = ButtonDefaults.textButtonColors()
                                    ) {
                                        Text("Yes")
                                    }
                                }
                            }
                        }
                    }
                }

                // Rest of your UI
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = { CustomTopBar(viewModel) },
                    containerColor = Color.Black  // Set scaffold background to black
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .background(Color.Black) // Also ensuring the column background is black
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
    private fun CustomTopBar(viewModel: MainActivityViewModel) {
        // Set the background color to black instead of the primary color.
        Surface(
            color = Color.Black,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left group: AddPhotos and Model Info
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AddPhotos(viewModel)
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
                // Center: "Photos" title text
                Text(
                    text = "Photos",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                // Right group: RemoveAllPhotos button
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RemoveAllPhotos(viewModel)
                }
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

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun ViewMediaDialog(
        images: List<Uri>,
        initialIndex: Int,
        onDismiss: () -> Unit
    ) {
        val pagerState = rememberPagerState(
            initialPage = initialIndex
        ) { images.size }

        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                HorizontalPager(
                    state = pagerState
                ) { page ->
                    AsyncImage(
                        model = images[page],
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun ColumnScope.PhotosList(viewModel: MainActivityViewModel) {
        val selectedImagesUris by remember { viewModel.selectedImagesUriListState }
        var selectedImageIndex by remember { mutableStateOf<Int?>(null) }

        // Load images on first composition.
        LaunchedEffect(0) { viewModel.loadImages() }

        // State for the slider controlling image size.
        var sliderPosition by rememberSaveable { mutableStateOf(1f) } // Range: 0.5f (small) to 2.0f (large)

        // Place a slider above the grid.
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = "Image Size", color = Color.White)
            Slider(
                value = sliderPosition,
                onValueChange = { sliderPosition = it },
                valueRange = 0.5f..2.0f,
                steps = 3,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Use an adaptive grid that adjusts its cell size based on the slider.
        LazyVerticalStaggeredGrid(
            modifier = Modifier
                .background(Color.Transparent)
                .fillMaxWidth()
                .weight(1f),
            // The grid creates as many columns as possible with each cell having at least (100 * sliderPosition) dp.
            columns = StaggeredGridCells.Adaptive(minSize = (100 * sliderPosition).dp),
            contentPadding = PaddingValues(4.dp)
        ) {
            items(selectedImagesUris.toList()) { uri ->
                Card(
                    modifier = Modifier
                        .padding(4.dp)
                        // Adjust the height using sliderPosition so vertical size scales too.
                        .height((150 * sliderPosition).dp),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    onClick = {
                        selectedImageIndex = selectedImagesUris.indexOf(uri)
                    }
                ) {
                    // Make the image fill the card.
                    AsyncImage(
                        modifier = Modifier.fillMaxSize(),
                        model = uri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        // Show fullscreen viewer when an image is selected.
        selectedImageIndex?.let { index ->
            ViewMediaDialog(
                images = selectedImagesUris.toList(),
                initialIndex = index,
                onDismiss = { selectedImageIndex = null }
            )
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
                    viewModel.processQuery(context)
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
            Dialog(
                onDismissRequest = {
                    viewModel.isInsertingImagesState.value = false
                    viewModel.loadImages()
                },
                properties = DialogProperties(
                    dismissOnClickOutside = true
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Inserting images...",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Inserted $insertedImagesCount images",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            hideProgressDialog()
        }
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

