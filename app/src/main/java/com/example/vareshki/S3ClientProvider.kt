package com.example.vareshki

import io.minio.MinioClient

object S3ClientProvider {
    val minioClient: MinioClient = MinioClient.builder()
        .endpoint("https://s3.regru.cloud")
        .credentials(
            "UUL4WF461HPIOAENQ0MS", // Access Key ID
            "xooLkVCAj9WG3wAdH1qZ6VB1esM8fIgEjm7AFxkD" // Secret Access Key
        )
        .region("ru-msk") // Регион для Reg.ru S3
        .build()
} 