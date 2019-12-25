package com.yopers.renee.utils

import com.yopers.renee.ObjectBox
import com.yopers.renee.models.User
import com.yopers.renee.models.User_
import io.minio.MinioClient
import io.objectbox.Box
import io.objectbox.kotlin.boxFor
import java.util.concurrent.TimeUnit

class Builder {
    private val userBox: Box<User> = ObjectBox.boxStore.boxFor()

    fun minioClient(user: User): MinioClient {
        val minioClient = MinioClient(user.endPoint, user.accessKey, user.secretKey)
        minioClient.setTimeout(TimeUnit.SECONDS.toMillis(10),0,0)

        return minioClient
    }

    fun user(id: Long = 0): User {
        val user: User?

        if (id == 0L) {
            user = userBox.query().equal(User_.isActive, true).build().findFirst()
        } else {
            user = userBox.query().equal(User_.id, id).build().findFirst()
        }

        if (user != null ) {
            return user
        }

        return User()
    }
}