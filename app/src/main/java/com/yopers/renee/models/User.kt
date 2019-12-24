package com.yopers.renee.models

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
data class User(
    @Id var id: Long = 0,
    var endPoint: String? = null,
    var accessKey: String? = null,
    var secretKey: String? = null,
    var niceName: String? = null,
    var defaultDownloadLocation: String? = null,
    var favouriteBucket: String? = null,
    val isActive: Boolean? = false
)