package com.sourcepack.core

import com.sourcepack.BuildConfig

object Str {
    // Removed bilingual support functions get(cn, en) and all UI-related strings
    const val APP_VERSION = "v${BuildConfig.VERSION_NAME}"
}