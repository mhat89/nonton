package com.nonton.app.util

object Constants {
    // Content is loaded from user-configured JSON providers and scrapers.
    // Image URLs
    const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/"
    const val POSTER_SIZE_W154 = "w154"
    const val POSTER_SIZE_W185 = "w185"
    const val POSTER_SIZE_W342 = "w342"
    const val POSTER_SIZE_W500 = "w500"
    const val POSTER_SIZE_W780 = "w780"
    const val POSTER_SIZE_ORIGINAL = "original"
    const val BACKDROP_SIZE_W300 = "w300"
    const val BACKDROP_SIZE_W780 = "w780"
    const val BACKDROP_SIZE_W1280 = "w1280"
    const val BACKDROP_SIZE_ORIGINAL = "original"

    // Video Quality
    const val VIDEO_QUALITY_AUTO = "auto"
    const val VIDEO_QUALITY_4K = "2160p"
    const val VIDEO_QUALITY_1080P = "1080p"
    const val VIDEO_QUALITY_720P = "720p"
    const val VIDEO_QUALITY_480P = "480p"
    const val VIDEO_QUALITY_360P = "360p"

    // Download
    const val MAX_CONCURRENT_DOWNLOADS = 3
    const val DOWNLOAD_CHUNK_SIZE = 1024 * 1024 // 1MB
    const val DOWNLOAD_TIMEOUT = 30 * 60 * 1000 // 30 minutes

    // Cache
    const val MAX_CACHE_SIZE = 500 * 1024 * 1024 // 500MB
    const val CACHE_EXPIRY_DAYS = 7

    // Player
    const val BUFFER_DURATION_MS = 15000
    const val MAX_BUFFER_DURATION_MS = 50000
    const val PLAYBACK_START_BUFFER_MS = 2500
    const val REBUFFER_BUFFER_MS = 5000

    // Pagination
    const val DEFAULT_PAGE_SIZE = 20
    const val PREFETCH_DISTANCE = 5

    // Search
    const val SEARCH_DEBOUNCE_MS = 300
    const val MIN_SEARCH_LENGTH = 2

    // UI
    const val HERO_BANNER_HEIGHT_PHONE = 280
    const val HERO_BANNER_HEIGHT_TABLET = 350
    const val HERO_BANNER_HEIGHT_TV = 400
    const val MOVIE_CARD_ASPECT_RATIO = 2f / 3f
    const val TV_MOVIE_CARD_WIDTH = 280
    const val PHONE_MOVIE_CARD_WIDTH = 160

    // Animations
    const val ANIMATION_DURATION_SHORT = 150
    const val ANIMATION_DURATION_MEDIUM = 250
    const val ANIMATION_DURATION_LONG = 350

    // Deep Links
    const val DEEP_LINK_SCHEME = "nonton"
    const val DEEP_LINK_HOST_WATCH = "watch"
    const val DEEP_LINK_HOST_DETAIL = "detail"
    const val DEEP_LINK_HOST_SEARCH = "search"

    // Notification Channels
    const val CHANNEL_DOWNLOAD_ID = "download_channel"
    const val CHANNEL_PLAYBACK_ID = "playback_channel"
    const val CHANNEL_UPDATE_ID = "update_channel"

    // WorkManager
    const val WORK_TAG_DOWNLOAD = "download_work"
    const val WORK_TAG_SYNC = "sync_work"
    const val WORK_TAG_CLEANUP = "cleanup_work"

    // Preferences Keys
    const val PREF_KEY_THEME = "theme"
    const val PREF_KEY_VIDEO_QUALITY = "video_quality"
    const val PREF_KEY_DOWNLOAD_QUALITY = "download_quality"
    const val PREF_KEY_DOWNLOAD_WIFI_ONLY = "download_wifi_only"
    const val PREF_KEY_AUTO_PLAY_NEXT = "auto_play_next"
    const val PREF_KEY_SUBTITLE_LANGUAGE = "subtitle_language"
    const val PREF_KEY_AUDIO_LANGUAGE = "audio_language"
    const val PREF_KEY_PIP_ENABLED = "pip_enabled"
    const val PREF_KEY_DATA_SAVER = "data_saver"
    const val PREF_KEY_CONTINUE_WATCHING = "continue_watching"
    const val PREF_KEY_PARENTAL_CONTROL = "parental_control"
    const val PREF_KEY_PIN = "pin"

    // Theme
    enum class ThemeMode {
        SYSTEM, LIGHT, DARK
    }

    // Default Values
    const val DEFAULT_VIDEO_QUALITY = VIDEO_QUALITY_AUTO
    const val DEFAULT_DOWNLOAD_QUALITY = VIDEO_QUALITY_720P
    const val DEFAULT_DOWNLOAD_WIFI_ONLY = true
    const val DEFAULT_AUTO_PLAY_NEXT = true
    const val DEFAULT_SUBTITLE_LANGUAGE = "id"
    const val DEFAULT_AUDIO_LANGUAGE = "id"
    const val DEFAULT_PIP_ENABLED = true
    const val DEFAULT_DATA_SAVER = false
    const val DEFAULT_CONTINUE_WATCHING = true
    const val DEFAULT_PARENTAL_CONTROL = false
}