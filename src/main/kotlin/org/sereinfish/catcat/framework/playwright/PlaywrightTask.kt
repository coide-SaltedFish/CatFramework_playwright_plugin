package org.sereinfish.catcat.framework.playwright

import com.microsoft.playwright.Page

/**
 * Playwright 任务
 */
data class PlaywrightTask(
    val navigate: String? = null,
    val content: String? = null,
    val pageBuilder: Page.() -> Unit = {},
    val callBack: (Page) -> Unit
){

    var cancel: Boolean = false

    fun cancel() {
        cancel = true
    }
}