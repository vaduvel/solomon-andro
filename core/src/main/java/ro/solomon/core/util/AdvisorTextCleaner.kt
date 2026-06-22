package ro.solomon.core.util

object AdvisorTextCleaner {
    fun clean(text: String): String {
        return text
            .replace("**", "")
            .replace("__", "")
            .replace("30 de ani de experien\u021B\u0103", "experien\u021B\u0103 financiar\u0103 solid\u0103")
            .replace("30 ani experien\u021B\u0103", "principii financiare solide")
            .replace("30 ani", "principii financiare solide")
            .trim()
    }
}
