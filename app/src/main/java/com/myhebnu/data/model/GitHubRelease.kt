package com.myhebnu.data.model

import com.google.gson.annotations.SerializedName

/**
 * Minimal model for GitHub Releases API response.
 * Only the fields needed for in-app update checking.
 */
data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("html_url") val htmlUrl: String,
    @SerializedName("name") val name: String?,
    @SerializedName("body") val body: String?
)
