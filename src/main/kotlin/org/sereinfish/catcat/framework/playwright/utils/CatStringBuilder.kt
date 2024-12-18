package org.sereinfish.catcat.framework.playwright.utils

class CatStringBuilder {
    private val stringBuilder = StringBuilder()

    operator fun String.unaryPlus(): CatStringBuilder {
        stringBuilder.append(this)
        return this@CatStringBuilder
    }

    operator fun plus(str: String): CatStringBuilder {
        stringBuilder.append(str)
        return this
    }

    fun append(value: Any): CatStringBuilder {
        stringBuilder.append(value)
        return this
    }

    fun build() = stringBuilder.toString()
}