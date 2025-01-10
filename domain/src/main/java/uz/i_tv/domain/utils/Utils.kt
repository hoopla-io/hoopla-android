package uz.i_tv.domain.utils

object Utils {

    fun getMapImageUrl(long: String?, lat: String?): String {
        return "http://static-maps.yandex.ru/1.x/?lang=en-US&ll=$long,$lat&size=450,450&z=14&l=map&pt=$long,$lat,vkbkm"
    }

    fun String.formatPhoneNumber(): String {
        val regex = """(\d{3})(\d{2})(\d{3})(\d{2})(\d{2})""".toRegex()
        val output = regex.replace(this, "+$1 $2 $3-$4-$5")
        return output
    }

}