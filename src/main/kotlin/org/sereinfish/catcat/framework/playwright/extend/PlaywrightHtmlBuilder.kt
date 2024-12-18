package org.sereinfish.catcat.framework.playwright.extend

import com.microsoft.playwright.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlinx.html.HTML
import kotlinx.html.html
import kotlinx.html.stream.createHTML
import org.sereinfish.catcat.framework.playwright.PlaywrightManager
import org.sereinfish.catcat.framework.playwright.PlaywrightTask
import org.sereinfish.catcat.framework.playwright.PluginMain
import org.sereinfish.catcat.framework.playwright.utils.CatStringBuilder
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.coroutines.resume

private val logger = PluginMain.logger

class PlaywrightImageBuilder {
    private var content: String = ""

    private var pageOptional: Page.() -> Unit = {}

    fun applyPageOptional(page: Page) {
        page.pageOptional()
    }

    fun html(
        prettyPrint: Boolean = true,
        xhtmlCompatible: Boolean = false,
        block: HTML.() -> Unit
    ) {
        content = PlaywrightHtmlBuilder().apply {
            html(prettyPrint, xhtmlCompatible, block)
        }.build()
    }

    fun htmlBuilder(block: PlaywrightHtmlBuilder.() -> Unit) {
        content = PlaywrightHtmlBuilder().apply(block).build()
    }

    fun page(block: PlaywrightPageBuilder.() -> Unit) {
        pageOptional = PlaywrightPageBuilder().apply(block).build()
    }

    fun content() = content
}

class PlaywrightHtmlBuilder {
    private var content: String = ""

    fun html(
        prettyPrint: Boolean = true,
        xhtmlCompatible: Boolean = false,
        block: HTML.() -> Unit
    ) {
        content = createHTML(prettyPrint, xhtmlCompatible).html(block = block)
    }

    fun file(path: Path) {
        content = Files.readString(path)
    }

    fun inputStream(inputStream: InputStream, charset: Charset = Charset.defaultCharset()) {
        content = inputStream.readBytes().toString(charset)
    }

    fun string(block: CatStringBuilder.() -> Unit) {
        content = CatStringBuilder().apply(block).build()
    }

    fun build(): String = content
}

class PlaywrightPageBuilder {
    private val optionalChain = ArrayList<Page.() -> Unit>()

    fun page(block: Page.() -> Unit) {
        optionalChain.add(block)
    }

    fun build(): Page.() -> Unit = {
        optionalChain.forEach {
            it()
        }
    }
}

/**
 * 构建图片
 */
suspend fun buildPlaywrightImage(
    timeout: Long = 15000L,
    pageBuilder: Page.() -> Unit = {},
    block: PlaywrightImageBuilder.() -> Unit
) = withTimeout(timeout) {
    suspendCancellableCoroutine { cancellableContinuation ->
        val builder = PlaywrightImageBuilder().apply(block)
        // 构建任务
        val task = PlaywrightTask(null, builder.content(), pageBuilder) {
            builder.applyPageOptional(it)
            cancellableContinuation.resume(Result.success(it.screenshot()))
        }

        PlaywrightManager.add(task)

        cancellableContinuation.invokeOnCancellation {
            task.cancel()
            cancellableContinuation.resume(Result.failure<ByteArray>(RuntimeException(it)))
        }
    }
}.getOrThrow()

/**
 * 查找指定元素渲染构建图片
 */
suspend fun buildPlaywrightElementImage(
    timeout: Long = 15000L,
    select: Page.() -> ElementHandle,
    pageBuilder: Page.() -> Unit = {},
    block: PlaywrightImageBuilder.() -> Unit
) = withTimeout(timeout) {
    suspendCancellableCoroutine { cancellableContinuation ->
        val builder = PlaywrightImageBuilder().apply(block)
        // 构建任务
        val task = PlaywrightTask(null, builder.content(), pageBuilder) {
            builder.applyPageOptional(it)
            val element = select(it)
            cancellableContinuation.resume(Result.success(element.screenshot()))
        }

        PlaywrightManager.add(task)

        cancellableContinuation.invokeOnCancellation {
            task.cancel()
            cancellableContinuation.resume(Result.failure<ByteArray>(RuntimeException(it)))
        }
    }
}.getOrThrow()


suspend fun buildPaywrightWebElementImage(
    timeout: Long = 15000L,
    select: Page.() -> ElementHandle,
    classLoader: ClassLoader,
    htmlPath: String,
    host: String = "127.0.0.114:11451",
    urlToPathHandler: (String) -> String = { url ->
        url.substring("http://${host}/".length)
    },
    pageBuilder: Page.() -> Unit = {},
) = withTimeout(timeout) {
    suspendCancellableCoroutine { cancellableContinuation ->
        // 构建任务
        val task = PlaywrightTask("http://${host}/index.html", null, {

            // 拦截资源
            route("http://${host}/**") { route ->
                logger.info("拦截请求 ${route.request().url()}")
                val resourcePath = urlToPathHandler(route.request().url())
                logger.info("返回资源 $resourcePath")
                classLoader.getResourceAsStream(resourcePath)?.use {
                    route.fulfill(
                        Route.FulfillOptions()
                            .setStatus(200)
                            .setContentType(when(resourcePath.substringAfter(".")){
                                "js" -> "application/javascript"
                                "css" -> "text/css"
                                "png" -> "image/png"
                                "jpg", "jpeg" -> "image/jpeg"
                                else -> "text/html"
                            })
                            .setBodyBytes(it.readBytes())
                    )
                }
            }

            // 拦截请求，返回html
            route("http://${host}/index.html") {
                logger.info("返回页面文件 $htmlPath")
                val htmlContent = classLoader.getResourceAsStream(htmlPath)?.readBytes()
                it.fulfill(
                    Route.FulfillOptions()
                        .setStatus(200)
                        .setContentType("text/html")
                        .setBodyBytes(htmlContent)
                )
            }

            pageBuilder()
        }) {
            val element = select(it)

            cancellableContinuation.resume(Result.success(element.screenshot()))
        }

        PlaywrightManager.add(task)

        cancellableContinuation.invokeOnCancellation {
            task.cancel()
            cancellableContinuation.resume(Result.failure<ByteArray>(RuntimeException(it)))
        }
    }
}.getOrThrow()

fun main() {
    val playwright = runCatching {
        Playwright.create()
    }.getOrElse {
        println("Playwright 创建失败：${it.message}")
        throw it
    }

    // 打开一个浏览器
    val browser = playwright.chromium().launch(BrowserType.LaunchOptions().apply {
        headless = false
    })

    val page = browser.newPage()

    page.route("**/**") {
        println("router 3 -> ${it.request().url()}")
        if (it.request().url().contains("image")) {
            it.fulfill(Route.FulfillOptions()
                .setStatus(200)
                .setContentType("image/png")
                .setBodyBytes(Files.readAllBytes(Paths.get("C:\\Users\\MiaoTiao\\Pictures\\115613049_p0.png")))
            )
        }

        if (it.request().url().contains(".html")) {
            val html = Files.readString(Paths.get("C:\\Users\\MiaoTiao\\Documents\\project\\idea\\CatFramework_playwright_plugin\\src\\main\\resources\\test.html"))
            println(html)
            it.fulfill(Route.FulfillOptions()
                .setStatus(200)
                .setContentType("text/html")
                .setBody(html)
            )
        }
    }
    page.navigate("http://127.0.0.1:80/index.html")
}