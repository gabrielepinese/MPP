package com.mpp.config

import org.jetbrains.exposed.sql.Database
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class ExposedConfig(dataSource: DataSource) {
    init {
        Database.connect(dataSource)
    }
}
