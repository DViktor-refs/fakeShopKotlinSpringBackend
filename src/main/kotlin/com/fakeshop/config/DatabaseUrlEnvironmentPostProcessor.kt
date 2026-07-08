package com.fakeshop.config

import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource

/**
 * A Railway (és Heroku) által adott DATABASE_URL formátuma:
 *   postgresql://user:password@host:port/dbname
 * Ez NEM JDBC URL, ezért a Spring datasource nem tudja közvetlenül használni.
 *
 * Ez az osztály az indulás legelején lefut, és ha talál DATABASE_URL-t,
 * átalakítja a megfelelő spring.datasource.* property-kké.
 *
 * Ha a felhasználó kézzel megadja a SPRING_DATASOURCE_URL-t, az élvez elsőbbséget
 * (akkor ez a feldolgozó nem csinál semmit).
 */
class DatabaseUrlEnvironmentPostProcessor : EnvironmentPostProcessor {

    private val log = LoggerFactory.getLogger(DatabaseUrlEnvironmentPostProcessor::class.java)

    override fun postProcessEnvironment(environment: ConfigurableEnvironment, application: SpringApplication) {
        // Ha van kézzel megadott JDBC URL, ne írjuk felül.
        if (!environment.getProperty("SPRING_DATASOURCE_URL").isNullOrBlank()) return

        val raw = environment.getProperty("DATABASE_URL") ?: return
        if (!(raw.startsWith("postgres://") || raw.startsWith("postgresql://"))) return

        try {
            val withoutScheme = raw.substringAfter("://")
            val lastAt = withoutScheme.lastIndexOf('@')
            if (lastAt < 0) return

            val creds = withoutScheme.substring(0, lastAt)
            val hostPart = withoutScheme.substring(lastAt + 1)

            val user = creds.substringBefore(':')
            val pass = creds.substringAfter(':', "")

            val query = if (hostPart.contains('?')) "?" + hostPart.substringAfter('?') else ""
            val hostPortDb = hostPart.substringBefore('?')
            val hostPort = hostPortDb.substringBefore('/')
            val db = hostPortDb.substringAfter('/', "")
            val host = hostPort.substringBefore(':')
            val port = hostPort.substringAfter(':', "5432")

            val jdbcUrl = "jdbc:postgresql://$host:$port/$db$query"

            val props = mapOf<String, Any>(
                "spring.datasource.url" to jdbcUrl,
                "spring.datasource.username" to user,
                "spring.datasource.password" to pass,
            )
            // addFirst: felülírja az application.yml localhost alapértékét.
            environment.propertySources.addFirst(MapPropertySource("railwayDatabaseUrl", props))
            log.info("DATABASE_URL feldolgozva, datasource beállítva: jdbc:postgresql://{}:{}/{}", host, port, db)
        } catch (ex: Exception) {
            log.warn("A DATABASE_URL feldolgozása nem sikerült, maradnak az alapértékek. Hiba: {}", ex.message)
        }
    }
}
