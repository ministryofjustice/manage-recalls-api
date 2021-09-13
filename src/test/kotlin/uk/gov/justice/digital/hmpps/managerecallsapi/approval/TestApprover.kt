package uk.gov.justice.digital.hmpps.managerecallsapi.approval

import org.http4k.testing.FileSystemApprovalSource
import org.http4k.testing.TestNamer
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import java.io.File

class TestApprover : BeforeEachCallback, ParameterResolver {

  private lateinit var _approver: ContentApprover

  override fun beforeEach(context: ExtensionContext) {
    _approver =
      NamedResourceContentApprover(
        TestNamer { testClass, testMethod ->
          testClass.packageName.replace('.', '/') + '/' + testClass.simpleName + "_" + testMethod.name
        }.nameFor(context.testClass.get(), context.testMethod.get()),
        FileSystemApprovalSource(File("src/test/resources"))
      )
  }

  override fun supportsParameter(parameterContext: ParameterContext, context: ExtensionContext) =
    parameterContext.parameter.type == ContentApprover::class.java

  override fun resolveParameter(parameterContext: ParameterContext, context: ExtensionContext) =
    if (supportsParameter(parameterContext, context)) approver else null

  val approver
    get() = _approver
}
