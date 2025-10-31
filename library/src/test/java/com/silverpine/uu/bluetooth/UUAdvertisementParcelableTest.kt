package com.silverpine.uu.bluetooth

import com.silverpine.uu.test.UUParcelableBaseTest
import org.junit.jupiter.api.Test

class UUAdvertisementParcelableTest: UUParcelableBaseTest<UUAdvertisement>()
{
    @Test
    fun test_0000_default()
    {
        doTest(UUAdvertisement())
    }
}