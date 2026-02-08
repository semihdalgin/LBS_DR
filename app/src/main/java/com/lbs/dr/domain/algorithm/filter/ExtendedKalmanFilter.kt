package com.lbs.dr.domain.algorithm.filter

import org.apache.commons.math3.linear.Array2DRowRealMatrix
import org.apache.commons.math3.linear.ArrayRealVector
import org.apache.commons.math3.linear.LUDecomposition
import org.apache.commons.math3.linear.MatrixUtils
import org.apache.commons.math3.linear.RealMatrix
import org.apache.commons.math3.linear.RealVector

/**
 * Extended Kalman Filter for nonlinear state estimation.
 *
 * State vector: [x, y, z, vx, vy, vz, heading, floor]
 * - x, y: horizontal position (meters, local frame)
 * - z: altitude (meters)
 * - vx, vy, vz: velocity components
 * - heading: orientation (radians)
 * - floor: current floor number
 *
 * Improvements over original KalmanFilterSimple:
 * - Supports nonlinear measurement models via Jacobian linearization
 * - Adaptive process noise based on motion state
 * - Robust numerical handling with Joseph form covariance update
 */
class ExtendedKalmanFilter(
    private val stateDim: Int = STATE_DIM,
    private val initialUncertainty: Double = 100.0
) {
    companion object {
        const val STATE_DIM = 8
        const val IDX_X = 0
        const val IDX_Y = 1
        const val IDX_Z = 2
        const val IDX_VX = 3
        const val IDX_VY = 4
        const val IDX_VZ = 5
        const val IDX_HEADING = 6
        const val IDX_FLOOR = 7
    }

    /** State estimate */
    var x: RealVector = ArrayRealVector(stateDim)
        private set

    /** State covariance */
    var P: RealMatrix = MatrixUtils.createRealIdentityMatrix(stateDim)
        .scalarMultiply(initialUncertainty)
        private set

    /** Process noise covariance */
    var Q: RealMatrix = MatrixUtils.createRealIdentityMatrix(stateDim)
        .scalarMultiply(0.1)

    private var initialized = false

    /**
     * Initialize the filter with a known position.
     */
    fun initialize(
        x0: Double, y0: Double, z0: Double,
        heading: Double = 0.0, floor: Int = 0
    ) {
        x = ArrayRealVector(doubleArrayOf(x0, y0, z0, 0.0, 0.0, 0.0, heading, floor.toDouble()))
        P = MatrixUtils.createRealIdentityMatrix(stateDim).scalarMultiply(initialUncertainty)
        initialized = true
    }

    val isInitialized: Boolean get() = initialized

    /**
     * Predict step: propagate state forward using constant-velocity model.
     * x_k = F * x_{k-1}
     * P_k = F * P_{k-1} * F^T + Q
     *
     * @param dt time step in seconds
     * @param processNoiseScale adaptive scaling for motion type
     */
    fun predict(dt: Double, processNoiseScale: Double = 1.0) {
        if (!initialized) return

        // State transition matrix (constant velocity model)
        val F = MatrixUtils.createRealIdentityMatrix(stateDim)
        F.setEntry(IDX_X, IDX_VX, dt)
        F.setEntry(IDX_Y, IDX_VY, dt)
        F.setEntry(IDX_Z, IDX_VZ, dt)

        // Predict state
        x = F.operate(x)

        // Normalize heading to [0, 2π)
        x.setEntry(IDX_HEADING, normalizeAngle(x.getEntry(IDX_HEADING)))

        // Predict covariance: P = F * P * F^T + Q * scale
        val scaledQ = Q.scalarMultiply(processNoiseScale)
        P = F.multiply(P).multiply(F.transpose()).add(scaledQ)
    }

    /**
     * Update step with a linear measurement.
     * z = H * x + v,  v ~ N(0, R)
     *
     * Uses Joseph form for numerical stability: P = (I - KH)P(I - KH)^T + KRK^T
     *
     * @param z measurement vector
     * @param H measurement matrix (maps state to measurement space)
     * @param R measurement noise covariance
     */
    fun update(z: RealVector, H: RealMatrix, R: RealMatrix) {
        if (!initialized) return

        // Innovation: y = z - H * x
        val y = z.subtract(H.operate(x))

        // Innovation covariance: S = H * P * H^T + R
        val S = H.multiply(P).multiply(H.transpose()).add(R)

        // Kalman gain: K = P * H^T * S^{-1}
        val Sinv = LUDecomposition(S).solver.inverse
        val K = P.multiply(H.transpose()).multiply(Sinv)

        // State update: x = x + K * y
        x = x.add(K.operate(y))

        // Covariance update (Joseph form for numerical stability)
        val I = MatrixUtils.createRealIdentityMatrix(stateDim)
        val IKH = I.subtract(K.multiply(H))
        P = IKH.multiply(P).multiply(IKH.transpose())
            .add(K.multiply(R).multiply(K.transpose()))

        // Normalize heading
        x.setEntry(IDX_HEADING, normalizeAngle(x.getEntry(IDX_HEADING)))
    }

    /**
     * Update step with a nonlinear measurement model.
     * Uses Jacobian H (evaluated at current state) for linearization.
     *
     * @param z measurement vector
     * @param hx predicted measurement h(x) evaluated at current state
     * @param H Jacobian of measurement function at current state
     * @param R measurement noise covariance
     */
    fun updateNonlinear(z: RealVector, hx: RealVector, H: RealMatrix, R: RealMatrix) {
        if (!initialized) return

        // Innovation using nonlinear prediction
        val y = z.subtract(hx)
        val S = H.multiply(P).multiply(H.transpose()).add(R)
        val Sinv = LUDecomposition(S).solver.inverse
        val K = P.multiply(H.transpose()).multiply(Sinv)

        x = x.add(K.operate(y))

        val I = MatrixUtils.createRealIdentityMatrix(stateDim)
        val IKH = I.subtract(K.multiply(H))
        P = IKH.multiply(P).multiply(IKH.transpose())
            .add(K.multiply(R).multiply(K.transpose()))

        x.setEntry(IDX_HEADING, normalizeAngle(x.getEntry(IDX_HEADING)))
    }

    /**
     * Get current position estimate.
     */
    fun getPosition(): DoubleArray = doubleArrayOf(
        x.getEntry(IDX_X),
        x.getEntry(IDX_Y),
        x.getEntry(IDX_Z)
    )

    fun getHeading(): Double = x.getEntry(IDX_HEADING)

    fun getFloor(): Int = x.getEntry(IDX_FLOOR).toInt()

    fun getPositionUncertainty(): Double {
        val varX = P.getEntry(IDX_X, IDX_X)
        val varY = P.getEntry(IDX_Y, IDX_Y)
        return kotlin.math.sqrt(varX + varY)
    }

    private fun normalizeAngle(angle: Double): Double {
        var a = angle % (2 * Math.PI)
        if (a < 0) a += 2 * Math.PI
        return a
    }
}
