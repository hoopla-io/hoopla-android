package uz.alphazet.domain.utils

import android.content.Context
import uz.alphazet.domain.R
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

fun getMapImageUrl(long: String?, lat: String?): String {
    return "http://static-maps.yandex.ru/1.x/?lang=en-US&ll=$long,$lat&size=450,450&z=14&l=map&pt=$long,$lat,vkbkm"
}

fun String.formatPhoneNumber(): String {
    val regex = """(\d{3})(\d{2})(\d{3})(\d{2})(\d{2})""".toRegex()
    val output = regex.replace(this, "+$1 $2 $3-$4-$5")
    return output
}

fun Double.formatToPrice(): String {

    val intValue = this.toInt()

    if (this == intValue.toDouble()) return intValue.toString().moneyType()
    else {
        val string = this.toString()
        val indexOfFirst = string.indexOfFirst { it == '.' }
        val substring = string.substring(indexOfFirst)
        return intValue.toString().moneyType().plus(substring)
    }
}

fun String.moneyType(): String {
    return this
        .reversed()
        .chunked(3)
        .joinToString(" ")
        .reversed()
}

fun Context.formatDistance(distance: Double): String {
    val int = distance.toInt()
    val dec = DecimalFormat("#,###.##")
    return if (int == 0) {
        getString(
            R.string.label_distance_away,
            dec.format((distance * 1000)),
            getString(R.string.short_metr)
        )
    } else {
        getString(
            R.string.label_distance_away,
            dec.format(distance),
            getString(R.string.short_km)
        )
    }
}

fun Long.getDateDDMMMMYYYYHHmm(): String {
    val format = "dd MMMM, yyyy  HH:mm" // you can add the format you need
    val sdf = SimpleDateFormat(format, Locale.getDefault()) // default local
    sdf.timeZone = TimeZone.getDefault() // set anytime zone you need
    val s = sdf.format(java.sql.Date(this * 1000))
    val stringBuilder = StringBuilder(s)
    if (!s.isNullOrEmpty())
        stringBuilder[0] = stringBuilder[0].uppercaseChar()
    return stringBuilder.toString()
}

fun Long.getDateDMMMMYYYYHHmm(): String {
    val format = "d MMMM, yyyy  HH:mm" // you can add the format you need
    val sdf = SimpleDateFormat(format, Locale.getDefault()) // default local
    sdf.timeZone = TimeZone.getDefault() // set anytime zone you need
    val s = sdf.format(java.sql.Date(this * 1000))
    val stringBuilder = StringBuilder(s)
    if (!s.isNullOrEmpty())
        stringBuilder[0] = stringBuilder[0].uppercaseChar()
    return stringBuilder.toString()
}

fun Long.getDateDMMMMYYYY(): String {
    val format = "d MMMM, yyyy" // you can add the format you need
    val sdf = SimpleDateFormat(format, Locale.getDefault()) // default local
    sdf.timeZone = TimeZone.getDefault() // set anytime zone you need
    val s = sdf.format(java.sql.Date(this * 1000))
    val stringBuilder = StringBuilder(s)
    if (!s.isNullOrEmpty())
        stringBuilder[0] = stringBuilder[0].uppercaseChar()
    return stringBuilder.toString()
}
