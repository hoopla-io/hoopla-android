package uz.i_tv.domain.utils

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

fun String.formatToPrice(): String {
    val value = replace(" ", "")
    val reverseValue = StringBuilder(value).reverse()
        .toString()
    val finalValue = StringBuilder()
    for (i in 1..reverseValue.length) {
        val `val` = reverseValue[i - 1]
        finalValue.append(`val`)
        if (i % 3 == 0 && i != reverseValue.length && i > 0) {
            finalValue.append(" ")
        }
    }
    return finalValue.reverse().toString()
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