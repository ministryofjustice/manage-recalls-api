package uk.gov.justice.digital.hmpps.managerecallsapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication()
class ManageRecallsApi

fun main(args: Array<String>) {
  runApplication<ManageRecallsApi>(*args)
}
