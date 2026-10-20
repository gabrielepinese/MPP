package com.mpp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MiniPaymentPlatformApplication

fun main(args: Array<String>) {
	runApplication<MiniPaymentPlatformApplication>(*args)
}
