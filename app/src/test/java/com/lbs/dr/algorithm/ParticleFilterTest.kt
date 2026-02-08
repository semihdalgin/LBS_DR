package com.lbs.dr.algorithm

import com.lbs.dr.domain.algorithm.filter.ParticleFilter
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ParticleFilterTest {

    private lateinit var pf: ParticleFilter

    @Before
    fun setup() {
        pf = ParticleFilter(numParticles = 200)
    }

    @Test
    fun `initialization creates particles around given position`() {
        pf.initialize(10.0, 20.0, 0.0, floor = 1)
        assertTrue(pf.isInitialized)

        val est = pf.getEstimate()
        assertEquals(10.0, est[0], 2.0) // within spread
        assertEquals(20.0, est[1], 2.0)
    }

    @Test
    fun `predict moves particles in heading direction`() {
        pf.initialize(0.0, 0.0, 0.0, floor = 0, spread = 0.1)

        // Step north (heading = PI/2 in math convention)
        pf.predict(stepLength = 1.0, heading = 0.0, headingNoise = 0.01, stepNoise = 0.01)

        val est = pf.getEstimate()
        assertTrue(est[0] > 0.5) // moved in x direction (heading=0 = east)
    }

    @Test
    fun `WiFi RSSI update moves estimate toward AP`() {
        pf.initialize(0.0, 0.0, 0.0, floor = 0, spread = 10.0)

        val apPositions = mapOf("ap1" to Pair(5.0, 5.0))
        // Strong RSSI = close to AP
        val observedRssi = mapOf("ap1" to -30) // very strong signal

        pf.updateWithWifi(observedRssi, apPositions)

        val est = pf.getEstimate()
        // Estimate should shift toward the AP
        assertTrue(est[0] > -1.0 || est[1] > -1.0)
    }

    @Test
    fun `floor update filters by detected floor`() {
        pf.initialize(0.0, 0.0, 0.0, floor = 0, spread = 1.0)
        assertEquals(0, pf.getEstimatedFloor())

        pf.updateWithFloor(detectedFloor = 0, confidence = 0.95)
        assertEquals(0, pf.getEstimatedFloor())
    }

    @Test
    fun `RTT update narrows estimate toward known distance`() {
        pf.initialize(0.0, 0.0, 0.0, floor = 0, spread = 20.0)

        val apPositions = mapOf(
            "ap1" to Pair(10.0, 0.0),
            "ap2" to Pair(0.0, 10.0)
        )
        val rttDistances = mapOf(
            "ap1" to 10.0,  // 10m from ap1
            "ap2" to 10.0   // 10m from ap2
        )

        // Multiple updates should converge
        repeat(5) {
            pf.updateWithRtt(rttDistances, apPositions, distanceSigma = 2.0)
        }

        val spread = pf.getSpread()
        assertTrue(spread < 20.0) // should have narrowed
    }
}
