package com.yopers.renee.models

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
data class Task(
    @Id var id: Long = 0,
    var taskId: String? = null,
    var source: String? = null,
    var destination: String? = null
)