package com.owlcoder.animeschedule

import androidx.test.platform.app.InstrumentationRegistry

/** Visual runs capture PNGs by default. Functional reruns can use -e qaScreenshots false. */
internal object QaCapture {
    val enabled: Boolean by lazy {
        !InstrumentationRegistry.getArguments().getString("qaScreenshots").equals("false", ignoreCase = true)
    }
}
