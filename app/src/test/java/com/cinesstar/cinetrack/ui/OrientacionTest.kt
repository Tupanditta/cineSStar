package com.cinesstar.cinetrack.ui

import android.content.ComponentName
import android.content.pm.ActivityInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cinesstar.cinetrack.CineTrackApplication
import com.cinesstar.cinetrack.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** T9.09 (RNF-11): la app está bloqueada en vertical, así que el modo apaisado no la rompe. */
@RunWith(AndroidJUnit4::class)
class OrientacionTest {
    @Test
    fun t9_09_laActividadEstaBloqueadaEnVertical() {
        val app = ApplicationProvider.getApplicationContext<CineTrackApplication>()
        val info = app.packageManager.getActivityInfo(ComponentName(app, MainActivity::class.java), 0)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, info.screenOrientation)
    }
}
