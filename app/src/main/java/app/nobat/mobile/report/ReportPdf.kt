package app.nobat.mobile.report

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import androidx.core.content.FileProvider
import app.nobat.mobile.calendar.Jalali
import app.nobat.mobile.data.Appointment
import app.nobat.mobile.data.Personnel
import app.nobat.mobile.locale.AppLocale
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * On-device appointment PDF via [PdfDocument]. No cloud services.
 *
 * Layout (Art/Pen): orange title bar with brand + date range; columns
 * Time · Client · Staff · Note; B&W-friendly body; footer "Nobat Mobile".
 * FA → RTL page columns; times stay LTR.
 */
object ReportPdf {
    const val MIME_PDF = "application/pdf"
    private const val PAGE_W = 595 // A4 points @ 72dpi
    private const val PAGE_H = 842
    private const val MARGIN = 36f
    private const val TITLE_BAR_H = 44f
    private const val ROW_H = 18f
    private const val HEADER_ROW_H = 22f
    private const val BRAND_ORANGE = 0xFFE95420.toInt()
    private const val AUTHORITY_SUFFIX = ".fileprovider"

    data class Labels(
        val title: String,
        val colTime: String,
        val colClient: String,
        val colStaff: String,
        val colNote: String,
        val footer: String,
        val empty: String,
    )

    data class Range(
        val start: LocalDate,
        val end: LocalDate,
        /** Display label for the range (already localized). */
        val label: String,
    )

    fun dayRange(day: LocalDate, persian: Boolean): Range {
        val label = if (persian) Jalali.dayLabel(day) else day.format(ISO)
        return Range(day, day, label)
    }

    fun monthRange(anchor: LocalDate, persian: Boolean): Range {
        return if (persian) {
            val (start, end) = Jalali.monthBounds(anchor)
            Range(start, end, Jalali.monthLabel(anchor))
        } else {
            val ym = YearMonth.from(anchor)
            Range(ym.atDay(1), ym.atEndOfMonth(), ym.format(YM))
        }
    }

    fun labels(context: Context): Labels {
        val r = context.resources
        return Labels(
            title = r.getString(app.nobat.mobile.R.string.app_name),
            colTime = r.getString(app.nobat.mobile.R.string.report_col_time),
            colClient = r.getString(app.nobat.mobile.R.string.report_col_client),
            colStaff = r.getString(app.nobat.mobile.R.string.report_col_staff),
            colNote = r.getString(app.nobat.mobile.R.string.report_col_note),
            footer = r.getString(app.nobat.mobile.R.string.app_name),
            empty = r.getString(app.nobat.mobile.R.string.report_empty),
        )
    }

    fun suggestedFileName(range: Range): String {
        val tag = if (range.start == range.end) {
            range.start.format(ISO)
        } else {
            "${range.start.format(ISO)}_${range.end.format(ISO)}"
        }
        return "nobat-report-$tag.pdf"
    }

    /**
     * Build PDF into app cache `reports/` and return the file.
     * [personnelById] maps staff id → display name (missing → blank).
     */
    fun write(
        context: Context,
        appointments: List<Appointment>,
        personnelById: Map<Long, Personnel>,
        range: Range,
        labels: Labels,
    ): File {
        val persian = AppLocale.isPersian(context)
        val dir = File(context.cacheDir, "reports").also { it.mkdirs() }
        // Clear prior report files so cache stays small.
        dir.listFiles()?.forEach { it.delete() }
        val outFile = File(dir, suggestedFileName(range))

        val doc = PdfDocument()
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val rangePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 11f
            typeface = Typeface.DEFAULT
        }
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 9.5f
            typeface = Typeface.DEFAULT
        }
        val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 9f
            typeface = Typeface.DEFAULT
        }
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 0.6f
        }
        val barPaint = Paint().apply { color = BRAND_ORANGE }
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.GRAY
            textSize = 9f
            typeface = Typeface.DEFAULT
        }

        val contentLeft = MARGIN
        val contentRight = PAGE_W - MARGIN
        val contentWidth = contentRight - contentLeft
        // Column fractions: Time 0.14 · Client 0.22 · Staff 0.28 · Note 0.36
        val colW = floatArrayOf(
            contentWidth * 0.14f,
            contentWidth * 0.22f,
            contentWidth * 0.28f,
            contentWidth * 0.36f,
        )

        fun colX(index: Int): Float {
            var x = contentLeft
            for (i in 0 until index) x += colW[i]
            return x
        }

        fun drawTitleBar(canvas: Canvas) {
            canvas.drawRect(0f, 0f, PAGE_W.toFloat(), TITLE_BAR_H, barPaint)
            val baseline = 28f
            if (persian) {
                titlePaint.textAlign = Paint.Align.RIGHT
                rangePaint.textAlign = Paint.Align.LEFT
                canvas.drawText(labels.title, contentRight, baseline, titlePaint)
                canvas.drawText(range.label, contentLeft, baseline, rangePaint)
            } else {
                titlePaint.textAlign = Paint.Align.LEFT
                rangePaint.textAlign = Paint.Align.RIGHT
                canvas.drawText(labels.title, contentLeft, baseline, titlePaint)
                canvas.drawText(range.label, contentRight, baseline, rangePaint)
            }
            titlePaint.textAlign = Paint.Align.LEFT
            rangePaint.textAlign = Paint.Align.LEFT
        }

        fun drawColHeaders(canvas: Canvas, y: Float) {
            val headers = listOf(labels.colTime, labels.colClient, labels.colStaff, labels.colNote)
            for (i in headers.indices) {
                val text = headers[i]
                if (persian) {
                    val right = colX(i) + colW[i] - 2f
                    headerPaint.textAlign = Paint.Align.RIGHT
                    canvas.drawText(text, right, y, headerPaint)
                } else {
                    headerPaint.textAlign = Paint.Align.LEFT
                    canvas.drawText(text, colX(i) + 2f, y, headerPaint)
                }
            }
            headerPaint.textAlign = Paint.Align.LEFT
            canvas.drawLine(contentLeft, y + 4f, contentRight, y + 4f, linePaint)
        }

        fun drawFooter(canvas: Canvas, pageIndex: Int, pageCount: Int) {
            val y = PAGE_H - 20f
            canvas.drawLine(contentLeft, y - 10f, contentRight, y - 10f, linePaint)
            footerPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(labels.footer, PAGE_W / 2f, y, footerPaint)
            if (pageCount > 1) {
                footerPaint.textAlign = if (persian) Paint.Align.LEFT else Paint.Align.RIGHT
                val pageLabel = "${pageIndex + 1}/$pageCount"
                val x = if (persian) contentLeft else contentRight
                canvas.drawText(pageLabel, x, y, footerPaint)
            }
            footerPaint.textAlign = Paint.Align.LEFT
        }

        fun ellipsize(text: String, maxWidth: Float, paint: Paint): String {
            if (paint.measureText(text) <= maxWidth) return text
            val ellipsis = "…"
            var end = text.length
            while (end > 0 && paint.measureText(text.substring(0, end) + ellipsis) > maxWidth) {
                end--
            }
            return if (end <= 0) ellipsis else text.substring(0, end) + ellipsis
        }

        fun drawCell(canvas: Canvas, text: String, col: Int, baseline: Float, paint: Paint, forceLtr: Boolean = false) {
            val pad = 2f
            val maxW = colW[col] - pad * 2
            val shown = ellipsize(text, maxW, paint)
            val rtl = persian && !forceLtr
            if (rtl) {
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText(shown, colX(col) + colW[col] - pad, baseline, paint)
            } else {
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText(shown, colX(col) + pad, baseline, paint)
            }
            paint.textAlign = Paint.Align.LEFT
        }

        data class Row(val time: String, val client: String, val staff: String, val note: String)

        val rows = appointments.map { a ->
            val staffName = when {
                a.personnelId > 0L -> personnelById[a.personnelId]?.name?.trim().orEmpty()
                else -> ""
            }
            Row(
                time = formatTime(a.startMinute),
                client = a.initials.trim(),
                staff = staffName,
                note = a.note.trim(),
            )
        }

        // Paginate: title bar + header + rows; leave room for footer.
        val firstContentY = TITLE_BAR_H + 28f
        val bottomLimit = PAGE_H - 36f
        val rowsPerFirst = ((bottomLimit - firstContentY - HEADER_ROW_H) / ROW_H).toInt().coerceAtLeast(1)
        val rowsPerNext = ((bottomLimit - (TITLE_BAR_H + 16f) - HEADER_ROW_H) / ROW_H).toInt().coerceAtLeast(1)

        val pages = mutableListOf<List<Row>>()
        if (rows.isEmpty()) {
            pages.add(emptyList())
        } else {
            var offset = 0
            var first = true
            while (offset < rows.size) {
                val n = if (first) rowsPerFirst else rowsPerNext
                first = false
                val end = (offset + n).coerceAtMost(rows.size)
                pages.add(rows.subList(offset, end))
                offset = end
            }
        }

        pages.forEachIndexed { pageIndex, pageRows ->
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageIndex + 1).create()
            val page = doc.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)
            drawTitleBar(canvas)

            var y = firstContentY
            if (pageIndex > 0) y = TITLE_BAR_H + 16f

            drawColHeaders(canvas, y)
            y += HEADER_ROW_H

            if (pageRows.isEmpty()) {
                mutedPaint.textAlign = Paint.Align.CENTER
                canvas.drawText(labels.empty, PAGE_W / 2f, y + 24f, mutedPaint)
                mutedPaint.textAlign = Paint.Align.LEFT
            } else {
                for (row in pageRows) {
                    // Time always LTR (forceLtr).
                    drawCell(canvas, row.time, 0, y, cellPaint, forceLtr = true)
                    drawCell(canvas, row.client, 1, y, cellPaint)
                    drawCell(canvas, row.staff, 2, y, cellPaint)
                    drawCell(canvas, row.note, 3, y, cellPaint)
                    canvas.drawLine(contentLeft, y + 4f, contentRight, y + 4f, linePaint)
                    y += ROW_H
                }
            }

            drawFooter(canvas, pageIndex, pages.size)
            doc.finishPage(page)
        }

        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
        return outFile
    }

    fun contentUri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(
            context,
            context.packageName + AUTHORITY_SUFFIX,
            file,
        )

    fun share(context: Context, file: File) {
        val uri = contentUri(context, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = MIME_PDF
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = android.content.ClipData.newUri(context.contentResolver, file.name, uri)
        }
        val chooser = Intent.createChooser(intent, context.getString(app.nobat.mobile.R.string.share))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /** System print dialog via PrintManager (also offers Save as PDF). */
    fun print(context: Context, file: File, jobName: String) {
        val uri = contentUri(context, file)
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val adapter = object : android.print.PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: android.os.CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: android.os.Bundle?,
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = android.print.PrintDocumentInfo.Builder(file.name)
                    .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(android.print.PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out android.print.PageRange>?,
                destination: android.os.ParcelFileDescriptor?,
                cancellationSignal: android.os.CancellationSignal?,
                callback: WriteResultCallback?,
            ) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        java.io.FileOutputStream(destination?.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                }
            }
        }
        printManager.print(jobName, adapter, null)
    }

    private fun formatTime(startMinute: Int): String {
        val h = startMinute / 60
        val m = startMinute % 60
        return "%02d:%02d".format(h, m)
    }

    private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val YM: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM", Locale.US)
}
