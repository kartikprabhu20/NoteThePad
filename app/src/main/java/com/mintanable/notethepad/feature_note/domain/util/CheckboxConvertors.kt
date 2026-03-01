package com.mintanable.notethepad.feature_note.domain.util

import com.mintanable.notethepad.feature_note.domain.model.CheckboxItem

object CheckboxConvertors {

    fun stringToCheckboxes(content: String): List<CheckboxItem> {
        if(content.isNotEmpty()) {
            return content.split("\n").filter { it.isNotBlank() }.map { line ->
                when {
                    line.startsWith("[x] ") -> CheckboxItem(
                        text = line.removePrefix("[x] "),
                        isChecked = true
                    )

                    line.startsWith("[ ] ") -> CheckboxItem(
                        text = line.removePrefix("[ ] "),
                        isChecked = false
                    )

                    else -> CheckboxItem(text = line, isChecked = false)
                }
            }
        }else {
            return listOf(CheckboxItem(text = "", isChecked = false))
        }
    }

    fun checkboxesToString(items: List<CheckboxItem>): String {
        return items.joinToString("\n") { item ->
                val prefix = if (item.isChecked) "[x] " else "[ ] "
                "$prefix${item.text}"
            }
    }

    fun checkboxesToContentString(items: List<CheckboxItem>): String {
        return items.joinToString("\n") { item ->
            "${item.text}"
        }
    }

    fun isContentCheckboxList(content: String): Boolean {
        return content.startsWith("[x]") || content.startsWith("[ ]")
    }

}