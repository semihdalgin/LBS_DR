package com.lbs.dr.algorithm

import com.lbs.dr.domain.algorithm.filter.ExtendedKalmanFilter
import org.apache.commons.math3.linear.Array2DRowRealMatrix
import org.apache.commons.math3.linear.ArrayRealVector
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ExtendedKalmanFilterTest {

    private lateinit var ekf: ExtendedKalmanFilter

    @Before
    fun setup() {
        ekf = ExtendedKalmanFilter()
        ekf.initialize(0.0, 0.0, 0.0)
    }

    @Test
    fun `initialization sets correct state`() {
        assertTrue(ekf.isInitialized)
        val pos = ekf.getPosition()
        assertEquals(0.0, pos[0], 1e-6)
        assertEquals(0.0, pos[1], 1e-6)
        assertEquals(0.0, pos[2], 1e-6)
    }

    @Test
    fun `predict moves state forward`() {
        // Set some velocity in x
        ekf.x.setEntry(ExtendedKalmanFilter.IDX_VX, 1.0) // 1 m/s in x

        ekf.predict(1.0) // 1 second

        val pos = ekf.getPosition()
        assertEquals(1.0, pos[0], 0.1) // x should move ~1m
    }

    @Test
    fun `update corrects position toward measurement`() {
        // Position measurement at (5, 5)
        val z = ArrayRealVector(doubleArrayOf(5.0, 5.0))
        val H = Array2DRowRealMatrix(2, ExtendedKalmanFilter.STATE_DIM)
        H.setEntry(0, ExtendedKalmanFilter.IDX_X, 1.0)
        H.setEntry(1, ExtendedKalmanFilter.IDX_Y, 1.0)
        val R = Array2DRowRealMatrix(arrayOf(
            doubleArrayOf(1.0, 0.0),
            doubleArrayOf(0.0, 1.0)
        ))

        ekf.update(z, H, R)

        val pos = ekf.getPosition()
        // State should move toward measurement
        assertTrue(pos[0] > 0)
        assertTrue(pos[1] > 0)
    }

    @Test
    fun `uncertainty decreases after measurement update`() {
        val uncertaintyBefore = ekf.getPositionUncertainty()

        val z = ArrayRealVector(doubleArrayOf(0.0, 0.0))
        val H = Array2DRowRealMatrix(2, ExtendedKalmanFilter.STATE_DIM)
        H.setEntry(0, ExtendedKalmanFilter.IDX_X, 1.0)
        H.setEntry(1, ExtendedKalmanFilter.IDX_Y, 1.0)
        val R = Array2DRowRealMatrix(arrayOf(
            doubleArrayOf(1.0, 0.0),
            doubleArrayOf(0.0, 1.0)
        ))

        ekf.update(z, H, R)

        val uncertaintyAfter = ekf.getPositionUncertainty()
        assertTrue(uncertaintyAfter < uncertaintyBefore)
    }

    @Test
    fun `multiple updates converge to true position`() {
        val trueX = 10.0
        val trueY = 20.0

        val H = Array2DRowRealMatrix(2, ExtendedKalmanFilter.STATE_DIM)
        H.setEntry(0, ExtendedKalmanFilter.IDX_X, 1.0)
        H.setEntry(1, ExtendedKalmanFilter.IDX_Y, 1.0)
        val R = Array2DRowRealMatrix(arrayOf(
            doubleArrayOf(2.0, 0.0),
            doubleArrayOf(0.0, 2.0)
        ))

        // Simulate 20 noisy measurements
        repeat(20) {
            val z = ArrayRealVector(doubleArrayOf(trueX, trueY))
            ekf.predict(0.1)
            ekf.update(z, H, R)
        }

        val pos = ekf.getPosition()
        assertEquals(trueX, pos[0], 1.0)
        assertEquals(trueY, pos[1], 1.0)
    }
}
