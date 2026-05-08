package ngo.xnet.zeeksworld

object BuildConfig {
    val LLM_API_KEY: String = System.getenv("LLM_API_KEY") ?: ""
    val GEOAPIFY_KEY: String = System.getenv("GEOAPIFY_KEY") ?: "dff28ab1ea75480a8aadef9fb13c773f"
}
