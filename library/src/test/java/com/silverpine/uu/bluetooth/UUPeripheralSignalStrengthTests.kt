package com.silverpine.uu.bluetooth

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UUPeripheralSignalStrengthTests
{
    private lateinit var originalThresholds: UUPeripheralSignalStrength.Thresholds

    @BeforeEach
    fun setup()
    {
        // Save original thresholds to restore after each test
        originalThresholds = UUPeripheralSignalStrength.thresholds.copy()
    }

    @AfterEach
    fun tearDown()
    {
        // Restore original thresholds after each test
        UUPeripheralSignalStrength.thresholds = originalThresholds
    }

    // from() tests with default thresholds

    @Test
    fun from_veryGoodSignal_returnsVeryGood()
    {
        // Test values >= -30 (default veryGood threshold)
        assertEquals(UUPeripheralSignalStrength.VERY_GOOD, UUPeripheralSignalStrength.from(-30))
        assertEquals(UUPeripheralSignalStrength.VERY_GOOD, UUPeripheralSignalStrength.from(-25))
        assertEquals(UUPeripheralSignalStrength.VERY_GOOD, UUPeripheralSignalStrength.from(-10))
        assertEquals(UUPeripheralSignalStrength.VERY_GOOD, UUPeripheralSignalStrength.from(0))
        assertEquals(UUPeripheralSignalStrength.VERY_GOOD, UUPeripheralSignalStrength.from(10))
    }

    @Test
    fun from_goodSignal_returnsGood()
    {
        // Test values >= -50 and < -30 (default good threshold)
        assertEquals(UUPeripheralSignalStrength.GOOD, UUPeripheralSignalStrength.from(-50))
        assertEquals(UUPeripheralSignalStrength.GOOD, UUPeripheralSignalStrength.from(-40))
        assertEquals(UUPeripheralSignalStrength.GOOD, UUPeripheralSignalStrength.from(-31))
        // Boundary: -30 should be VERY_GOOD, not GOOD
        assertEquals(UUPeripheralSignalStrength.VERY_GOOD, UUPeripheralSignalStrength.from(-30))
    }

    @Test
    fun from_moderateSignal_returnsModerate()
    {
        // Test values >= -70 and < -50 (default moderate threshold)
        assertEquals(UUPeripheralSignalStrength.MODERATE, UUPeripheralSignalStrength.from(-70))
        assertEquals(UUPeripheralSignalStrength.MODERATE, UUPeripheralSignalStrength.from(-60))
        assertEquals(UUPeripheralSignalStrength.MODERATE, UUPeripheralSignalStrength.from(-51))
        // Boundary: -50 should be GOOD, not MODERATE
        assertEquals(UUPeripheralSignalStrength.GOOD, UUPeripheralSignalStrength.from(-50))
    }

    @Test
    fun from_poorSignal_returnsPoor()
    {
        // Test values >= -90 and < -70 (default poor threshold)
        assertEquals(UUPeripheralSignalStrength.POOR, UUPeripheralSignalStrength.from(-90))
        assertEquals(UUPeripheralSignalStrength.POOR, UUPeripheralSignalStrength.from(-80))
        assertEquals(UUPeripheralSignalStrength.POOR, UUPeripheralSignalStrength.from(-71))
        // Boundary: -70 should be MODERATE, not POOR
        assertEquals(UUPeripheralSignalStrength.MODERATE, UUPeripheralSignalStrength.from(-70))
    }

    @Test
    fun from_veryPoorSignal_returnsVeryPoor()
    {
        // Test values < -90 (default poor threshold)
        assertEquals(UUPeripheralSignalStrength.VERY_POOR, UUPeripheralSignalStrength.from(-91))
        assertEquals(UUPeripheralSignalStrength.VERY_POOR, UUPeripheralSignalStrength.from(-100))
        assertEquals(UUPeripheralSignalStrength.VERY_POOR, UUPeripheralSignalStrength.from(-120))
        // Boundary: -90 should be POOR, not VERY_POOR
        assertEquals(UUPeripheralSignalStrength.POOR, UUPeripheralSignalStrength.from(-90))
    }

    @Test
    fun from_allThresholdBoundaries_returnsCorrectState()
    {
        // Test exact threshold boundaries with default values
        val default = UUPeripheralSignalStrength.thresholds
        assertEquals(UUPeripheralSignalStrength.VERY_GOOD, UUPeripheralSignalStrength.from(default.veryGood))
        assertEquals(UUPeripheralSignalStrength.GOOD, UUPeripheralSignalStrength.from(default.good))
        assertEquals(UUPeripheralSignalStrength.MODERATE, UUPeripheralSignalStrength.from(default.moderate))
        assertEquals(UUPeripheralSignalStrength.POOR, UUPeripheralSignalStrength.from(default.poor))
    }

    // Custom thresholds tests

    @Test
    fun from_customThresholds_returnsCorrectState()
    {
        // Set custom thresholds
        UUPeripheralSignalStrength.thresholds = UUPeripheralSignalStrength.Thresholds(
            veryGood = -40,
            good = -60,
            moderate = -80,
            poor = -100
        )

        // Test with custom thresholds
        assertEquals(UUPeripheralSignalStrength.VERY_GOOD, UUPeripheralSignalStrength.from(-40))
        assertEquals(UUPeripheralSignalStrength.VERY_GOOD, UUPeripheralSignalStrength.from(-30))
        assertEquals(UUPeripheralSignalStrength.GOOD, UUPeripheralSignalStrength.from(-60))
        assertEquals(UUPeripheralSignalStrength.GOOD, UUPeripheralSignalStrength.from(-50))
        assertEquals(UUPeripheralSignalStrength.MODERATE, UUPeripheralSignalStrength.from(-80))
        assertEquals(UUPeripheralSignalStrength.MODERATE, UUPeripheralSignalStrength.from(-70))
        assertEquals(UUPeripheralSignalStrength.POOR, UUPeripheralSignalStrength.from(-100))
        assertEquals(UUPeripheralSignalStrength.POOR, UUPeripheralSignalStrength.from(-90))
        assertEquals(UUPeripheralSignalStrength.VERY_POOR, UUPeripheralSignalStrength.from(-101))
        assertEquals(UUPeripheralSignalStrength.VERY_POOR, UUPeripheralSignalStrength.from(-110))
    }

    @Test
    fun from_modifiedIndividualThreshold_returnsCorrectState()
    {
        // Modify individual threshold property
        UUPeripheralSignalStrength.thresholds.veryGood = -35
        UUPeripheralSignalStrength.thresholds.good = -55

        // With veryGood = -35, signals >= -35 are VERY_GOOD
        // Note: -30 >= -35 is true (better signal), so -30 returns VERY_GOOD
        assertEquals(UUPeripheralSignalStrength.VERY_GOOD, UUPeripheralSignalStrength.from(-35))
        assertEquals(UUPeripheralSignalStrength.VERY_GOOD, UUPeripheralSignalStrength.from(-34))
        assertEquals(UUPeripheralSignalStrength.VERY_GOOD, UUPeripheralSignalStrength.from(-30))
        assertEquals(UUPeripheralSignalStrength.VERY_GOOD, UUPeripheralSignalStrength.from(-10))
        
        // With veryGood = -35, signals < -35 but >= -55 are GOOD
        // Note: -36 < -35, so -36 returns GOOD (not VERY_GOOD)
        assertEquals(UUPeripheralSignalStrength.GOOD, UUPeripheralSignalStrength.from(-36))
        assertEquals(UUPeripheralSignalStrength.GOOD, UUPeripheralSignalStrength.from(-55))
        assertEquals(UUPeripheralSignalStrength.GOOD, UUPeripheralSignalStrength.from(-50))
    }

    // Thresholds validation tests

    @Test
    fun Thresholds_invalidOrder_throwsException()
    {
        // Test that invalid threshold order throws IllegalArgumentException
        assertThrows(IllegalArgumentException::class.java) {
            UUPeripheralSignalStrength.Thresholds(
                veryGood = -50,  // Should be >= good
                good = -40,      // This violates the constraint
                moderate = -70,
                poor = -90
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            UUPeripheralSignalStrength.Thresholds(
                veryGood = -30,
                good = -70,      // Should be >= moderate
                moderate = -50,  // This violates the constraint
                poor = -90
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            UUPeripheralSignalStrength.Thresholds(
                veryGood = -30,
                good = -50,
                moderate = -90,  // Should be >= poor
                poor = -70       // This violates the constraint
            )
        }
    }

    @Test
    fun Thresholds_validOrder_createsSuccessfully()
    {
        // Test that valid thresholds can be created
        val thresholds = UUPeripheralSignalStrength.Thresholds(
            veryGood = -30,
            good = -50,
            moderate = -70,
            poor = -90
        )

        assertEquals(-30, thresholds.veryGood)
        assertEquals(-50, thresholds.good)
        assertEquals(-70, thresholds.moderate)
        assertEquals(-90, thresholds.poor)
    }

    @Test
    fun Thresholds_equalValues_createsSuccessfully()
    {
        // Test that equal values are allowed (descending order includes equality)
        val thresholds = UUPeripheralSignalStrength.Thresholds(
            veryGood = -50,
            good = -50,    // Equal to veryGood
            moderate = -50, // Equal to good
            poor = -50      // Equal to moderate
        )

        assertEquals(-50, thresholds.veryGood)
        assertEquals(-50, thresholds.good)
    }

    @Test
    fun Thresholds_defaultValues_areCorrect()
    {
        val default = UUPeripheralSignalStrength.Thresholds()
        assertEquals(-30, default.veryGood)
        assertEquals(-50, default.good)
        assertEquals(-70, default.moderate)
        assertEquals(-90, default.poor)
    }

    @Test
    fun Thresholds_copy_createsNewInstance()
    {
        val original = UUPeripheralSignalStrength.thresholds
        val copied = original.copy(veryGood = -35)

        assertEquals(-30, original.veryGood) // Original unchanged
        assertEquals(-35, copied.veryGood)   // Copy has new value
        assertEquals(original.good, copied.good)
        assertEquals(original.moderate, copied.moderate)
        assertEquals(original.poor, copied.poor)
    }

    // Edge cases

    @Test
    fun from_veryHighRssi_returnsVeryGood()
    {
        assertEquals(UUPeripheralSignalStrength.VERY_GOOD, UUPeripheralSignalStrength.from(100))
        assertEquals(UUPeripheralSignalStrength.VERY_GOOD, UUPeripheralSignalStrength.from(127))
        assertEquals(UUPeripheralSignalStrength.VERY_GOOD, UUPeripheralSignalStrength.from(Int.MAX_VALUE))
    }

    @Test
    fun from_veryLowRssi_returnsVeryPoor()
    {
        assertEquals(UUPeripheralSignalStrength.VERY_POOR, UUPeripheralSignalStrength.from(-200))
        assertEquals(UUPeripheralSignalStrength.VERY_POOR, UUPeripheralSignalStrength.from(Int.MIN_VALUE))
    }

    @Test
    fun from_allEnumValues_areCovered()
    {
        // Verify all enum values can be produced
        val results = setOf(
            UUPeripheralSignalStrength.from(0),
            UUPeripheralSignalStrength.from(-40),
            UUPeripheralSignalStrength.from(-60),
            UUPeripheralSignalStrength.from(-80),
            UUPeripheralSignalStrength.from(-100)
        )

        assertEquals(5, results.size, "All 5 enum values should be producible")
        assertEquals(
            setOf(
                UUPeripheralSignalStrength.VERY_GOOD,
                UUPeripheralSignalStrength.GOOD,
                UUPeripheralSignalStrength.MODERATE,
                UUPeripheralSignalStrength.POOR,
                UUPeripheralSignalStrength.VERY_POOR
            ),
            results
        )
    }
}

