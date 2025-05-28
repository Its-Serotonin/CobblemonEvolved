package com.serotonin.common.chat

import net.minecraft.text.Text
import net.minecraft.util.Formatting

fun parseMiniMessageLite(input: String): Text {
    val stack = ArrayDeque<Formatting>()
    val output = mutableListOf<Text>()

    val tagPattern = Regex("<(/?)([a-zA-Z]+)>")
    var cursor = 0

    for (match in tagPattern.findAll(input)) {
        if (cursor < match.range.first) {
            val plainText = input.substring(cursor, match.range.first)
            val base = Text.literal(plainText)
            stack.forEach { base.style = base.style.withFormatting(it) }
            output += base
        }

        val isClosing = match.groupValues[1] == "/"
        val tag = match.groupValues[2].lowercase()

        val formatting = when (tag) {
            "red" -> Formatting.RED
            "gray" -> Formatting.GRAY
            "bold" -> Formatting.BOLD
            "italic" -> Formatting.ITALIC
            else -> null
        }

        if (formatting != null) {
            if (isClosing) stack.removeLastOrNull()
            else stack.addLast(formatting)
        }

        cursor = match.range.last + 1
    }

    if (cursor < input.length) {
        val plainText = input.substring(cursor)
        val base = Text.literal(plainText)
        stack.forEach { base.style = base.style.withFormatting(it) }
        output += base
    }

    val result = Text.literal("")
    output.forEach { result.append(it) }
    return output.reduce { acc, text -> acc.copy().append(text) }
}

