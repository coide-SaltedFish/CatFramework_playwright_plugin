package org.sereinfish.catcat.framework.playwright

import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Playwright
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.sereinfish.cat.frame.utils.creatContextScope
import org.slf4j.LoggerFactory
import java.util.concurrent.LinkedBlockingQueue


object PlaywrightManager {

    private val scope = creatContextScope() // 协程域
    private val logger = LoggerFactory.getLogger(this::class.java)
    // 任务队列
    private val taskQueue = LinkedBlockingQueue<PlaywrightTask>()

    private val playwright = runCatching {
        Playwright.create()
    }.getOrElse {
        logger.error("Playwright 创建失败：{}", it.message)
        throw it
    }

    // 打开一个浏览器
    private val browser = playwright.chromium().launch(BrowserType.LaunchOptions().apply {
        headless = false
    })

    init {
        scope.launch {
            while (true) {
                try {
                    taskHandler(withContext(Dispatchers.IO) {
                        taskQueue.take()
                    })
                }catch (e: Exception){
                    logger.error("Playwright任务处理异常", e)
                }
            }
        }
        logger.info("Playwright 初始化完成")
    }

    fun add(task: PlaywrightTask) = taskQueue.add(task)

    /**
     * 任务处理
     */
    private fun taskHandler(task: PlaywrightTask) {
        val page = browser.newPage()
        try {
            task.pageBuilder(page)
            task.navigate?.let {
                if (it.isNotEmpty())
                    page.navigate(it)
            } ?: task.content?.let {
                if (it.isNotEmpty())
                    page.setContent(it)
            }
            task.callBack(page)
        } finally {
            if (page.isClosed.not())
                page.close()
        }
    }

    fun close() {
        browser.close()
        playwright.close()
    }
}