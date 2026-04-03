package com.mintanable.notethepad.core.network.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfileDto(
    val id: String,
    val email: String,
    @SerialName("display_name")
    val displayName: String? = null,
    @SerialName("photo_url")
    val photoUrl: String? = null
)

@Serializable
data class NoteCollaboratorDto(
    val id: String? = null,
    @SerialName("note_id")
    val noteId: String,
    @SerialName("owner_user_id")
    val ownerUserId: String,
    @SerialName("collaborator_user_id")
    val collaboratorUserId: String,
    @SerialName("collaborator_email")
    val collaboratorEmail: String,
    @SerialName("collaborator_display_name")
    val collaboratorDisplayName: String? = null,
    @SerialName("collaborator_photo_url")
    val collaboratorPhotoUrl: String? = null
)

@Serializable
data class FindUserResult(
    @SerialName("user_id")
    val userId: String,
    @SerialName("display_name")
    val displayName: String? = null,
    @SerialName("photo_url")
    val photoUrl: String? = null
)
