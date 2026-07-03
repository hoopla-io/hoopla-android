package uz.alphazet.hoopla.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Shader
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import uz.alphazet.hoopla.R

/**
 * Builds Google Maps marker icons that show a café's circular logo inside a
 * pin. Logos are loaded with Coil; when a shop has no picture (or the load
 * fails) the marker falls back to a branded coffee-cup placeholder.
 */
class ShopMarkerIconFactory(private val context: Context) {

    private val imageLoader = context.imageLoader
    private val logoSizePx = (LOGO_SIZE_DP * context.resources.displayMetrics.density).toInt()

    /** Cached placeholder icon shown immediately while a logo is loading. */
    val placeholder: BitmapDescriptor by lazy { render(logo = null, paused = false) }

    /** Dimmed/desaturated placeholder for shops that are not accepting orders. */
    private val pausedPlaceholder: BitmapDescriptor by lazy { render(logo = null, paused = true) }

    /** Placeholder variant matching the shop's accepting-orders state. */
    fun placeholderFor(paused: Boolean): BitmapDescriptor =
        if (paused) pausedPlaceholder else placeholder

    /**
     * Loads [logoUrl] and returns a marker icon with that logo, or the
     * matching [placeholderFor] when the logo is missing or cannot be loaded.
     * When [paused] is true the marker is rendered desaturated and dimmed to
     * signal the shop is not accepting orders.
     */
    suspend fun create(logoUrl: String?, paused: Boolean): BitmapDescriptor {
        val logo = loadLogo(logoUrl) ?: return placeholderFor(paused)
        return render(logo, paused)
    }

    private suspend fun loadLogo(url: String?): Bitmap? {
        if (url.isNullOrBlank()) return null
        val request = ImageRequest.Builder(context)
            .data(url)
            .size(logoSizePx, logoSizePx)
            .allowHardware(false) // the view is drawn onto a software canvas
            .build()
        return (imageLoader.execute(request) as? SuccessResult)
            ?.image
            ?.toBitmap(logoSizePx, logoSizePx)
            ?.cropToCircle()
    }

    /**
     * Crops the (square) logo into a circle so the marker shows a round logo
     * regardless of the off-screen view clip.
     */
    private fun Bitmap.cropToCircle(): Bitmap {
        val size = minOf(width, height)
        if (size == 0) return this
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(this@cropToCircle, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        val radius = size / 2f
        Canvas(output).drawCircle(radius, radius, radius, paint)
        return output
    }

    private fun render(logo: Bitmap?, paused: Boolean): BitmapDescriptor {
        val view = LayoutInflater.from(context).inflate(R.layout.view_map_marker, null)
        if (logo != null) {
            view.findViewById<ImageView>(R.id.marker_logo).setImageBitmap(logo)
        }
        val bitmap = view.toBitmap()
        return BitmapDescriptorFactory.fromBitmap(if (paused) bitmap.toPaused() else bitmap)
    }

    /**
     * Returns a desaturated, dimmed copy of the marker so a paused shop reads
     * as inactive on the map — mirroring the greyed-out scrim on the list.
     */
    private fun Bitmap.toPaused(): Bitmap {
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
            alpha = PAUSED_ALPHA
        }
        Canvas(output).drawBitmap(this, 0f, 0f, paint)
        return output
    }

    private fun View.toBitmap(): Bitmap {
        val spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        measure(spec, spec)
        layout(0, 0, measuredWidth, measuredHeight)
        val bitmap = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888)
        draw(Canvas(bitmap))
        return bitmap
    }

    private companion object {
        const val LOGO_SIZE_DP = 44
        const val PAUSED_ALPHA = 140 // ~55% opacity for the dimmed paused marker
    }
}