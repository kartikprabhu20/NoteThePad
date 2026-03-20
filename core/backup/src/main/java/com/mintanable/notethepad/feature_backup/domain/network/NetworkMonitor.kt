package com.mintanable.notethepad.feature_backup.domain.network

import kotlinx.coroutines.flow.Flow

interface NetworkMonitor {
    val isOnline: Flow<Boolean>
    val isUnmetered: Flow<Boolean>
}