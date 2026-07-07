package com.owlcoder.animeschedule.data.api.anilist

import android.content.Context
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.network.okHttpClient
import okhttp3.OkHttpClient

private const val ANILIST_BASE_URL = "https://graphql.anilist.co"

fun buildAniListApolloClient(context: Context, okHttpClient: OkHttpClient): ApolloClient =
    ApolloClient.Builder()
        .serverUrl(ANILIST_BASE_URL)
        .okHttpClient(okHttpClient)
        .build()
