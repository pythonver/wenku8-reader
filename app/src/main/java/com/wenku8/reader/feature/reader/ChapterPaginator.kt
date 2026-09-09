package com.wenku8.reader.feature.reader

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import com.wenku8.reader.core.data.model.NovelContentItem

/**
 * Stable position inside a chapter: which paragraph element + char offset.
 * This — NOT a page number — is what we persist, because a "page" is a
 * rendered artifact that changes with font size / rotation / settings.
 */
data class ReaderAnchor(val elementIndex: Int, val charOffset: Int) {
    override fun toString() = "$elementIndex:$charOffset"

    companion object {
        fun parse(raw: String?): ReaderAnchor? {
            if (raw == null) return null
            val parts = raw.split(":")
            if (parts.size != 2) return null
            val e = parts[0].toIntOrNull() ?: return null
            val c = parts[1].toIntOrNull() ?: return null
            return ReaderAnchor(e, c)
        }
    }
}

/**
 * A page covers whole elements [startElement, endElement] (inclusive). Elements
 * are never split across pages, so a page renders exactly the measured content
 * with no overflow. Each page starts at a paragraph → page-level progress.
 */
data class ReaderPage(
    val startElement: Int,
    val endElement: Int,
) {
    val anchor: ReaderAnchor get() = ReaderAnchor(startElement, 0)
}

/**
 * Whole-paragraph pagination: measure each TEXT paragraph's laid-out height
 * (+ paragraph gap) and pack paragraphs into pages of height [maxHeightPx].
 * Images occupy fixed [imageHeightPx] rows.
 */
class ChapterPaginator(
    private val content: List<NovelContentItem>,
    private val textMeasurer: TextMeasurer,
    private val style: TextStyle,
    private val paragraphGapPx: Float,
    private val imageHeightPx: Float,
) {

    fun paginate(maxWidthPx: Int, maxHeightPx: Int): List<ReaderPage> {
        if (content.isEmpty() || maxWidthPx <= 0 || maxHeightPx <= 0) return emptyList()

        val heights = FloatArray(content.size)
        for ((index, item) in content.withIndex()) {
            heights[index] = when (item.type) {
                NovelContentItem.ContentType.TEXT -> {
                    val layout = textMeasurer.measure(
                        AnnotatedString(item.content),
                        style,
                        constraints = Constraints(maxWidth = maxWidthPx),
                    )
                    layout.size.height.toFloat() + paragraphGapPx
                }
                NovelContentItem.ContentType.IMAGE -> imageHeightPx + paragraphGapPx
            }
        }

        val pages = mutableListOf<ReaderPage>()
        var start = 0
        var height = 0f
        for (i in content.indices) {
            if (i > start && height + heights[i] > maxHeightPx) {
                pages += ReaderPage(start, i - 1)
                start = i
                height = 0f
            }
            height += heights[i]
        }
        if (start < content.size) {
            pages += ReaderPage(start, content.size - 1)
        }
        return pages
    }

    companion object {
        /** Page index whose start element is closest to (<=) [anchor]. */
        fun pageIndexForAnchor(pages: List<ReaderPage>, anchor: ReaderAnchor?): Int {
            if (pages.isEmpty()) return 0
            val targetElement = anchor?.elementIndex ?: 0
            var best = 0
            for ((i, p) in pages.withIndex()) {
                if (p.startElement <= targetElement) best = i else break
            }
            return best
        }
    }
}
