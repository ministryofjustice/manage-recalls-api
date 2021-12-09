package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LocalDeliveryUnitTest {

  @Test
  fun getLabelSamples() {
    assertThat(LocalDeliveryUnit.CENTRAL_AUDIT_TEAM.label, equalTo("Central Audit Team"))
    assertThat(LocalDeliveryUnit.YOT_SEE_COMMENTS.label, equalTo("YOT - See Comments"))
  }

  @Test
  fun isInWalesExamplesTrueAndFalse() {
    assertThat(LocalDeliveryUnit.PS_SWINDON_AND_WILTSHIRE.isInWales, equalTo(false))
    assertThat(LocalDeliveryUnit.PS_SWANSEA_NEATH_AND_PORT_TALBOT.isInWales, equalTo(true))
  }

  @ParameterizedTest(name = "All Welsh LDUs return inWales as true")
  @MethodSource("sampleOfLDUsIncludingAllWelshAsTrue")
  fun `all welsh LDUs return inWales as true whilst sample of others all return as false`(
    ldu: LocalDeliveryUnit,
    expected: Boolean
  ) {
    assertThat(ldu.isInWales, equalTo(expected))
  }

  private fun sampleOfLDUsIncludingAllWelshAsTrue(): Stream<Arguments>? {
    return Stream.of(
      Arguments.of(LocalDeliveryUnit.PS_HOUNSLOW, false),
      Arguments.of(LocalDeliveryUnit.PS_NORTH_WALES, true),
      Arguments.of(LocalDeliveryUnit.PS_DYFED_POWYS, true),
      Arguments.of(LocalDeliveryUnit.PS_SWANSEA_NEATH_AND_PORT_TALBOT, true),
      Arguments.of(LocalDeliveryUnit.PS_CWM_TAF_MORGANNWG, true),
      Arguments.of(LocalDeliveryUnit.PS_CARDIFF_AND_VALE, true),
      Arguments.of(LocalDeliveryUnit.PS_GWENT, true),
      Arguments.of(LocalDeliveryUnit.PS_NORTH_DURHAM, false),
    )
  }
}
