package com.example.primo2

import java.time.LocalDate
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

data class PostInfo(
    val postID: String? = null,
    val title: String? = null,
    val Contents: ArrayList<String?> = arrayListOf(),
    val Format: ArrayList<String?> = arrayListOf(),
    val Comments: String? = null,
    val Writer: String? = null,
    val WriterID: String? = null,
    val PostDate: String? = null,
    val Like: HashMap<String,Boolean> = HashMap()
)