package com.mintanable.notethepad.feature_ai.domain.model

object BuiltInTaskId {
    const val LLM_CHAT = "llm_chat"
    const val LLM_PROMPT_LAB = "llm_prompt_lab"
    const val LLM_ASK_IMAGE = "llm_ask_image"
    const val LLM_ASK_AUDIO = "llm_ask_audio"
}


enum class Accelerator(val label: String) {
    CPU(label = "CPU"),
    GPU(label = "GPU"),
    NPU(label = "NPU"),
}