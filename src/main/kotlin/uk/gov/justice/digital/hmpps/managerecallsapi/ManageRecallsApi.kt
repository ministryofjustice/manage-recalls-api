package uk.gov.justice.digital.hmpps.managerecallsapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication
@EnableJpaRepositories("uk.gov.justice.digital.hmpps.managerecallsapi.db")
@EnableCaching
class ManageRecallsApi

fun main(args: Array<String>) {
  runApplication<ManageRecallsApi>(*args)
}
