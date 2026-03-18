package com.mintanable.notethepad

import com.mintanable.notethepad.core.common.AppVersionProvider
import javax.inject.Inject

class AndroidAppVersionProvider @Inject constructor() : AppVersionProvider {
    override val versionCode: Int = BuildConfig.VERSION_CODE
    override val versionName: String = BuildConfig.VERSION_NAME
}