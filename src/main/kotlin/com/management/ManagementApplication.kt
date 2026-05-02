package com.management

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
@EnableScheduling
@SpringBootApplication
class ManagementApplication

fun main(args: Array<String>) {
	runApplication<ManagementApplication>(*args)
}
