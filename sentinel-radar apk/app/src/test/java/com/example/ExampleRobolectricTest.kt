package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.FresnelParams
import com.example.data.model.VolumetricState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Sentinel Radar", appName)
    }

    @Test
    fun `verify fresnel corridor calculation`() {
        // At 2.437 GHz, across 3.0m link distance, at midpoint (1.5m),
        // r1 is ~30.3 cm as defined in Section 6 Step 2 of the specification
        val params = FresnelParams(
            totalDistanceMeters = 3.0,
            targetDistanceMeters = 1.5,
            frequencyGhz = 2.437
        )
        val r1 = params.firstFresnelRadiusCm
        assertTrue("Fresnel radius should be ~30.3 cm, got: $r1", r1 in 29.5..31.5)
    }

    @Test
    fun `verify volumetric state mapping`() {
        assertEquals(VolumetricState.FALL, VolumetricState.fromRaw("fall"))
        assertEquals(VolumetricState.BREATHING, VolumetricState.fromRaw("breathing"))
        assertEquals(VolumetricState.EMPTY, VolumetricState.fromRaw("empty"))
    }
}
