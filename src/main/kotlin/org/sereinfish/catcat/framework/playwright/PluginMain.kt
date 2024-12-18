package org.sereinfish.catcat.framework.playwright

import org.sereinfish.cat.frame.plugin.Plugin

object PluginMain: Plugin {
    override fun start() {
        val classloader = Thread.currentThread().contextClassLoader
        Thread.currentThread().contextClassLoader = this::class.java.classLoader

        PlaywrightManager
        Thread.currentThread().contextClassLoader = classloader


        logger.info("已引入猫猫Playwright扩展插件")
    }

    override fun close() {
        PlaywrightManager.close()
    }
}