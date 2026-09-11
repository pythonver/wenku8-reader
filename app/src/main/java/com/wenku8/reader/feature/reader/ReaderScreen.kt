package com.wenku8.reader.feature.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wenku8.reader.core.data.BookmarkEntity
import com.wenku8.reader.core.data.model.NovelContentItem
import com.wenku8.reader.core.designsystem.components.IllustrationErrorRegions
import com.wenku8.reader.core.designsystem.components.NovelIllustration
import com.wenku8.reader.core.designsystem.theme.BrandCyan
import com.wenku8.reader.core.designsystem.theme.ReaderColors
import com.wenku8.reader.core.designsystem.theme.ReaderThemeMode
import com.wenku8.reader.core.designsystem.theme.readerBodyStyle
import com.wenku8.reader.core.designsystem.theme.readerColorsFor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    aid: Int,
    targetCid: Int?,
    onExit: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val content by viewModel.content.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()
    val chapterIndex by viewModel.chapterIndex.collectAsStateWithLifecycle()
    val anchor by viewModel.anchor.collectAsStateWithLifecycle()
    val fontSize by viewModel.fontSize.collectAsStateWithLifecycle()
    val lineHeight by viewModel.lineHeight.collectAsStateWithLifecycle()
    val mode by viewModel.readerMode.collectAsStateWithLifecycle()
    val pageFlip by viewModel.pageFlip.collectAsStateWithLifecycle()
    val eink by viewModel.eink.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()

    val readerColors = readerColorsFor(mode)
    val bg by animateColorAsState(readerColors.background, spring(stiffness = Spring.StiffnessLow))
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    var showBars by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showChapters by remember { mutableStateOf(false) }
    var showBookmarks by remember { mutableStateOf(false) }
    var showBookmarkDialog by remember { mutableStateOf(false) }

    val currentChapter = chapters.getOrNull(chapterIndex)

    // Failed illustrations register their screen rects here so the tap overlay
    // (which sits above the pager) can forward taps as retries.
    val errorRegions = remember { IllustrationErrorRegions() }
    var illustrationRetryTick by remember { mutableIntStateOf(0) }
    var tapLayerCoords by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }

    // Text-area size is reported by the padded content box (insets included).
    var textAreaSize by remember { mutableStateOf(IntSize.Zero) }
    val gapDp = (fontSize * 0.5f).dp
    val imageHeightDp = 220.dp

    val pages = remember(content, fontSize, lineHeight, textAreaSize, textMeasurer) {
        if (textAreaSize.width <= 0 || textAreaSize.height <= 0) {
            emptyList()
        } else {
            ChapterPaginator(
                content = content,
                textMeasurer = textMeasurer,
                style = readerBodyStyle(fontSize).copy(lineHeight = (fontSize * lineHeight).sp),
                paragraphGapPx = with(density) { gapDp.toPx() },
                imageHeightPx = with(density) { imageHeightDp.toPx() },
            ).paginate(textAreaSize.width, textAreaSize.height)
        }
    }
    val pagerState = rememberPagerState(initialPage = 0) { pages.size }

    // Restore / re-anchor on content or anchor change.
    LaunchedEffect(pages, anchor) {
        val target = ChapterPaginator.pageIndexForAnchor(pages, anchor)
        if (pagerState.currentPage != target) pagerState.scrollToPage(target)
    }
    // Persist the stable anchor whenever a page settles.
    LaunchedEffect(pagerState.settledPage, pages) {
        if (pages.isNotEmpty()) viewModel.onPageSelected(pages, pagerState.settledPage)
    }

    Box(Modifier.fillMaxSize().background(bg)) {
        if (loading && content.isEmpty()) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = readerColors.text)
        } else if (content.isNotEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(vertical = 10.dp)
                    .onSizeChanged { textAreaSize = it },
            ) {
                val bodyStyle = readerBodyStyle(fontSize).copy(lineHeight = (fontSize * lineHeight).sp)
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { pageIndex ->
                    val page = pages.getOrNull(pageIndex) ?: return@HorizontalPager
                    ReaderPageContent(
                        page = page,
                        content = content,
                        bodyStyle = bodyStyle,
                        readerColors = readerColors,
                        paragraphGap = gapDp,
                        imageHeight = imageHeightDp,
                        errorRegions = errorRegions,
                        retryTick = illustrationRetryTick,
                    )
                }

                // Tap zones overlay the pager: left = prev page, right = next
                // page, centre = toggle bars. A tap landing on a failed
                // illustration is forwarded to that image as a retry.
                Box(
                    Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { tapLayerCoords = it }
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val rootPoint = tapLayerCoords?.localToRoot(offset) ?: offset
                                if (errorRegions.contains(rootPoint.x, rootPoint.y)) {
                                    illustrationRetryTick++
                                    return@detectTapGestures
                                }
                                val w = size.width
                                when {
                                    offset.x < w / 3f -> scope.launch {
                                        if (pagerState.currentPage > 0) {
                                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                        }
                                    }
                                    offset.x > w * 2f / 3f -> scope.launch {
                                        if (pagerState.currentPage < pages.size - 1) {
                                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                        }
                                    }
                                    else -> showBars = !showBars
                                }
                            }
                        },
                )
            }
        } else if (!loading) {
            Text("章节加载失败", color = readerColors.text, modifier = Modifier.align(Alignment.Center))
        }

        // ---- overlays (aligned to full screen) ----
        AnimatedVisibility(
            visible = showBars,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            ReaderTopBar(
                chapterName = currentChapter?.name ?: "",
                onBack = onExit,
                onBookmark = { showBookmarkDialog = true },
                onBookmarks = { showBookmarks = true },
                onSettings = { showSettings = true },
                readerColors = readerColors,
            )
        }

        AnimatedVisibility(
            visible = showBars,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            ReaderBottomBar(
                currentPage = pagerState.currentPage,
                totalPages = pages.size,
                chapterName = currentChapter?.name ?: "",
                onPrevChapter = viewModel::previousChapter,
                onNextChapter = viewModel::nextChapter,
                onChapters = { showChapters = true },
                readerColors = readerColors,
            )
        }
    }

    // ---- sheets & dialogs ----
    if (showSettings) {
        ReaderSettingsSheet(
            mode = mode,
            fontSize = fontSize,
            lineHeight = lineHeight,
            pageFlip = pageFlip,
            eink = eink,
            onMode = viewModel::setReaderMode,
            onFontSize = viewModel::setFontSize,
            onLineHeight = viewModel::setLineHeight,
            onPageFlip = viewModel::setPageFlip,
            onEink = viewModel::setEink,
            onDismiss = { showSettings = false },
        )
    }
    if (showChapters) {
        ReaderChaptersSheet(
            chapters = chapters,
            currentIndex = chapterIndex,
            onSelect = { index ->
                showChapters = false
                viewModel.openChapter(index)
            },
            onDismiss = { showChapters = false },
        )
    }
    if (showBookmarks) {
        ReaderBookmarksSheet(
            bookmarks = bookmarks,
            onAdd = { showBookmarks = false; showBookmarkDialog = true },
            onJump = { bookmark ->
                showBookmarks = false
                val index = chapters.indexOfFirst { it.cid == bookmark.cid }
                if (index >= 0) viewModel.openChapter(index, ReaderAnchor(bookmark.elementIndex, bookmark.charOffset))
            },
            onDelete = viewModel::removeBookmark,
            onDismiss = { showBookmarks = false },
        )
    }
    if (showBookmarkDialog) {
        BookmarkDialog(
            chapterName = currentChapter?.name ?: "",
            onConfirm = { name, note ->
                showBookmarkDialog = false
                viewModel.addBookmark(name, note)
            },
            onDismiss = { showBookmarkDialog = false },
        )
    }
}

@Composable
private fun ReaderPageContent(
    page: ReaderPage,
    content: List<NovelContentItem>,
    bodyStyle: androidx.compose.ui.text.TextStyle,
    readerColors: ReaderColors,
    paragraphGap: androidx.compose.ui.unit.Dp,
    imageHeight: androidx.compose.ui.unit.Dp,
    errorRegions: IllustrationErrorRegions,
    retryTick: Int,
) {
    Column(
        Modifier
            .fillMaxSize()
            .clipToBounds()
            .background(readerColors.background),
        verticalArrangement = Arrangement.spacedBy(paragraphGap),
    ) {
        for (element in page.startElement..page.endElement) {
            val item = content.getOrNull(element) ?: continue
            when (item.type) {
                NovelContentItem.ContentType.TEXT -> {
                    if (item.content.isNotBlank()) {
                        Text(
                            text = item.content,
                            style = bodyStyle.copy(color = readerColors.text),
                        )
                    }
                }
                NovelContentItem.ContentType.IMAGE -> {
                    NovelIllustration(
                        url = item.content,
                        errorRegions = errorRegions,
                        retryTick = retryTick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(imageHeight),
                    )
                }
            }
        }
    }
}

@Composable
private fun ReaderTopBar(
    chapterName: String,
    onBack: () -> Unit,
    onBookmark: () -> Unit,
    onBookmarks: () -> Unit,
    onSettings: () -> Unit,
    readerColors: ReaderColors,
) {
    Surface(color = readerColors.background.copy(alpha = 0.92f), modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 4.dp, vertical = 4.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回", tint = readerColors.text)
            }
            Text(
                chapterName,
                style = MaterialTheme.typography.titleMedium,
                color = readerColors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onBookmark) {
                Icon(Icons.Outlined.BookmarkAdd, contentDescription = "添加书签", tint = readerColors.text)
            }
            IconButton(onClick = onBookmarks) {
                Icon(Icons.Outlined.Bookmarks, contentDescription = "书签列表", tint = readerColors.text)
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Outlined.Menu, contentDescription = "设置", tint = readerColors.text)
            }
        }
    }
}

@Composable
private fun ReaderBottomBar(
    currentPage: Int,
    totalPages: Int,
    chapterName: String,
    onPrevChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onChapters: () -> Unit,
    readerColors: ReaderColors,
) {
    Surface(color = readerColors.background.copy(alpha = 0.92f), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                "第 ${currentPage + 1} / ${totalPages} 页 · $chapterName",
                style = MaterialTheme.typography.bodySmall,
                color = readerColors.secondaryText,
            )
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { if (totalPages > 0) (currentPage + 1f) / totalPages else 0f },
                color = BrandCyan,
                trackColor = readerColors.divider,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth(),
            ) {
                IconButton(onClick = onChapters) {
                    Icon(Icons.Outlined.FormatListBulleted, contentDescription = "目录", tint = readerColors.text)
                }
                IconButton(onClick = onPrevChapter) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "上一章", tint = readerColors.text)
                }
                IconButton(onClick = onNextChapter) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "下一章", tint = readerColors.text)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderSettingsSheet(
    mode: ReaderThemeMode,
    fontSize: Int,
    lineHeight: Float,
    pageFlip: Boolean,
    eink: Boolean,
    onMode: (ReaderThemeMode) -> Unit,
    onFontSize: (Int) -> Unit,
    onLineHeight: (Float) -> Unit,
    onPageFlip: (Boolean) -> Unit,
    onEink: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text("阅读设置", style = MaterialTheme.typography.titleMedium)

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReaderThemeMode.entries.forEach { m ->
                    val selected = m == mode
                    Surface(
                        onClick = { onMode(m) },
                        shape = MaterialTheme.shapes.small,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            when (m) {
                                ReaderThemeMode.DAY -> "白天"
                                ReaderThemeMode.NIGHT -> "夜间"
                                ReaderThemeMode.SEPIA -> "羊皮纸"
                                ReaderThemeMode.GREEN -> "护眼"
                            },
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp).fillMaxWidth(),
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("字号 $fontSize", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(88.dp))
                Slider(
                    value = fontSize.toFloat(),
                    onValueChange = { onFontSize(it.toInt()) },
                    valueRange = 12f..28f,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("行距", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(88.dp))
                Slider(
                    value = lineHeight,
                    onValueChange = onLineHeight,
                    valueRange = 1.4f..2.2f,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("翻页动画", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Switch(checked = pageFlip, onCheckedChange = onPageFlip)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("电纸书模式（关动画）", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Switch(checked = eink, onCheckedChange = onEink)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderChaptersSheet(
    chapters: List<ChapterRef>,
    currentIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Text("目录", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 20.dp))
        LazyColumn(Modifier.height(500.dp)) {
            items(chapters.size) { index ->
                val chapter = chapters[index]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                        .clickable { onSelect(index) }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    Text(
                        chapter.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (index == currentIndex) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (index == currentIndex) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderBookmarksSheet(
    bookmarks: List<BookmarkEntity>,
    onAdd: () -> Unit,
    onJump: (BookmarkEntity) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("书签", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onAdd) {
                    Icon(Icons.Outlined.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("添加当前页")
                }
            }
            if (bookmarks.isEmpty()) {
                Text(
                    "暂无书签",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            } else {
                LazyColumn(Modifier.height(420.dp)) {
                    items(bookmarks, key = { it.id }) { bookmark ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { onJump(bookmark) }.padding(vertical = 8.dp),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(bookmark.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (bookmark.note.isNotBlank()) {
                                    Text(bookmark.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                } else if (bookmark.excerpt.isNotBlank()) {
                                    Text(bookmark.excerpt, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            IconButton(onClick = { onDelete(bookmark.id) }) {
                                Icon(Icons.Outlined.Close, contentDescription = "删除", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookmarkDialog(
    chapterName: String,
    onConfirm: (name: String, note: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加书签") },
        text = {
            Column {
                Text(chapterName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("书签名（默认章节名）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("备注（可选）") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim(), note.trim()) }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
