package com.lbs.dr.domain.algorithm.filter

import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Particle Filter (Sequential Monte Carlo) for indoor positioning.
 *
 * Each particle represents a hypothesis of the user's position.
 * Particles are weighted by how well they match sensor observations
 * (WiFi RSSI, BLE beacons, floor constraints).
 *
 * Advantages over EKF for indoor positioning:
 * - Handles multimodal distributions (e.g., symmetric building layouts)
 * - No linearity/Gaussian assumptions
 * - Can incorporate map constraints (walls, walkable areas)
 */
class ParticleFilter(
    private val numParticles: Int = DEFAULT_PARTICLES,
    private val resampleThreshold: Double = 0.5
) {
    companion object {
        const val DEFAULT_PARTICLES = 500
    }

    data class Particle(
        var x: Double,
        var y: Double,
        var z: Double,
        var heading: Double,
        var floor: Int,
        var weight: Double = 1.0 / DEFAULT_PARTICLES
    )

    private var particles = mutableListOf<Particle>()
    private var initialized = false

    val isInitialized: Boolean get() = initialized

    /**
     * Initialize particles around an initial position with Gaussian spread.
     */
    fun initialize(x0: Double, y0: Double, z0: Double, floor: Int, spread: Double = 5.0) {
        particles = MutableList(numParticles) {
            Particle(
                x = x0 + Random.nextGaussian() * spread,
                y = y0 + Random.nextGaussian() * spread,
                z = z0 + Random.nextGaussian() * 1.0,
                heading = Random.nextDouble() * 2 * Math.PI,
                floor = floor,
                weight = 1.0 / numParticles
            )
        }
        initialized = true
    }

    /**
     * Propagate particles forward using dead reckoning motion model.
     *
     * @param stepLength estimated step length in meters
     * @param heading estimated heading in radians
     * @param headingNoise standard deviation of heading noise (radians)
     * @param stepNoise standard deviation of step length noise (meters)
     */
    fun predict(
        stepLength: Double,
        heading: Double,
        headingNoise: Double = 0.15,
        stepNoise: Double = 0.1
    ) {
        if (!initialized) return

        for (p in particles) {
            val noisyStep = stepLength + Random.nextGaussian() * stepNoise
            val noisyHeading = heading + Random.nextGaussian() * headingNoise

            p.x += noisyStep * cos(noisyHeading)
            p.y += noisyStep * sin(noisyHeading)
            p.heading = noisyHeading
        }
    }

    /**
     * Update particle weights based on WiFi RSSI observation.
     * Uses log-distance path loss model: RSSI = A - 10*n*log10(d)
     *
     * @param observedRssi map of AP BSSID to observed RSSI
     * @param apPositions map of AP BSSID to known (x, y) position
     * @param pathLossExponent environment-specific path loss exponent (2.0-4.0)
     * @param rssiAtOneMeter RSSI at 1 meter reference distance
     */
    fun updateWithWifi(
        observedRssi: Map<String, Int>,
        apPositions: Map<String, Pair<Double, Double>>,
        pathLossExponent: Double = 2.5,
        rssiAtOneMeter: Double = -40.0
    ) {
        if (!initialized) return

        for (p in particles) {
            var logLikelihood = 0.0
            for ((bssid, rssi) in observedRssi) {
                val apPos = apPositions[bssid] ?: continue
                val dx = p.x - apPos.first
                val dy = p.y - apPos.second
                val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(0.1)

                // Expected RSSI from path loss model
                val expectedRssi = rssiAtOneMeter - 10 * pathLossExponent * Math.log10(dist)
                val diff = rssi - expectedRssi

                // Gaussian likelihood with sigma = 5 dBm
                logLikelihood += -0.5 * (diff * diff) / 25.0
            }
            p.weight *= exp(logLikelihood)
        }

        normalizeWeights()

        if (effectiveSampleSize() < resampleThreshold * numParticles) {
            resample()
        }
    }

    /**
     * Update particle weights based on BLE beacon RSSI observations.
     */
    fun updateWithBle(
        observedRssi: Map<String, Int>,
        beaconPositions: Map<String, Pair<Double, Double>>,
        pathLossExponent: Double = 2.0,
        rssiAtOneMeter: Double = -59.0
    ) {
        updateWithWifi(observedRssi, beaconPositions, pathLossExponent, rssiAtOneMeter)
    }

    /**
     * Update particle weights with Wi-Fi RTT distance measurements.
     * RTT gives more precise distances than RSSI.
     *
     * @param rttDistances map of AP BSSID to measured distance in meters
     * @param apPositions map of AP BSSID to known (x, y) position
     * @param distanceSigma expected RTT distance error std dev (meters)
     */
    fun updateWithRtt(
        rttDistances: Map<String, Double>,
        apPositions: Map<String, Pair<Double, Double>>,
        distanceSigma: Double = 1.0
    ) {
        if (!initialized) return

        for (p in particles) {
            var logLikelihood = 0.0
            for ((bssid, measuredDist) in rttDistances) {
                val apPos = apPositions[bssid] ?: continue
                val dx = p.x - apPos.first
                val dy = p.y - apPos.second
                val particleDist = sqrt(dx * dx + dy * dy)
                val diff = measuredDist - particleDist
                logLikelihood += -0.5 * (diff * diff) / (distanceSigma * distanceSigma)
            }
            p.weight *= exp(logLikelihood)
        }

        normalizeWeights()

        if (effectiveSampleSize() < resampleThreshold * numParticles) {
            resample()
        }
    }

    /**
     * Update weights based on floor constraint from barometric floor detection.
     */
    fun updateWithFloor(detectedFloor: Int, confidence: Double = 0.9) {
        if (!initialized) return

        for (p in particles) {
            p.weight *= if (p.floor == detectedFloor) confidence else (1.0 - confidence)
        }
        normalizeWeights()

        if (effectiveSampleSize() < resampleThreshold * numParticles) {
            resample()
        }
    }

    /**
     * Get weighted mean position estimate.
     */
    fun getEstimate(): DoubleArray {
        if (!initialized) return doubleArrayOf(0.0, 0.0, 0.0)

        var sx = 0.0; var sy = 0.0; var sz = 0.0
        for (p in particles) {
            sx += p.x * p.weight
            sy += p.y * p.weight
            sz += p.z * p.weight
        }
        return doubleArrayOf(sx, sy, sz)
    }

    fun getEstimatedFloor(): Int {
        if (!initialized) return 0
        // Majority vote weighted by particle weight
        val floorWeights = mutableMapOf<Int, Double>()
        for (p in particles) {
            floorWeights[p.floor] = (floorWeights[p.floor] ?: 0.0) + p.weight
        }
        return floorWeights.maxByOrNull { it.value }?.key ?: 0
    }

    fun getSpread(): Double {
        if (!initialized) return Double.MAX_VALUE
        val est = getEstimate()
        var variance = 0.0
        for (p in particles) {
            val dx = p.x - est[0]
            val dy = p.y - est[1]
            variance += (dx * dx + dy * dy) * p.weight
        }
        return sqrt(variance)
    }

    private fun normalizeWeights() {
        val sum = particles.sumOf { it.weight }
        if (sum > 0) {
            for (p in particles) p.weight /= sum
        } else {
            // Reset to uniform if all weights collapsed
            for (p in particles) p.weight = 1.0 / numParticles
        }
    }

    private fun effectiveSampleSize(): Double {
        val sumSq = particles.sumOf { it.weight * it.weight }
        return if (sumSq > 0) 1.0 / sumSq else 0.0
    }

    /**
     * Systematic resampling: more efficient than multinomial resampling.
     */
    private fun resample() {
        val cumWeights = DoubleArray(numParticles)
        cumWeights[0] = particles[0].weight
        for (i in 1 until numParticles) {
            cumWeights[i] = cumWeights[i - 1] + particles[i].weight
        }

        val newParticles = mutableListOf<Particle>()
        val step = 1.0 / numParticles
        var u = Random.nextDouble() * step

        var j = 0
        for (i in 0 until numParticles) {
            while (j < numParticles - 1 && u > cumWeights[j]) j++
            newParticles.add(particles[j].copy(weight = step))
            u += step
        }

        particles = newParticles
    }

    private fun Random.nextGaussian(): Double {
        // Box-Muller transform
        val u1 = nextDouble()
        val u2 = nextDouble()
        return sqrt(-2.0 * Math.log(u1)) * cos(2.0 * Math.PI * u2)
    }
}
