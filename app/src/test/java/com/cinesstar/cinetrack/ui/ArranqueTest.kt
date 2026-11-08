package com.cinesstar.cinetrack.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cinesstar.cinetrack.MainActivity
import com.cinesstar.cinetrack.navigation.DestinoBarra
import com.cinesstar.cinetrack.ui.components.TagsBarra
import com.cinesstar.cinetrack.ui.components.tag
import com.cinesstar.cinetrack.ui.components.tagPantalla
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * T0.03 (RES-01, RNF-10, CU-23 FP paso 3): la app arranca en minSdk (26) y targetSdk (36) y abre Biblioteca.
 * Se ejecuta con Robolectric en las dos versiones; la comprobación en emulador real queda en la lista manual.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [26, 36])
class ArranqueTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun t0_03_arrancaYMuestraBiblioteca() {
        compose.onNodeWithTag(DestinoBarra.BIBLIOTECA.tagPantalla()).assertIsDisplayed()
        compose.onNodeWithTag(DestinoBarra.BIBLIOTECA.tag()).assertIsSelected()
        compose.onNode(hasTestTag(TagsBarra.SUPERIOR)).assertIsDisplayed()
        compose.onNode(hasText("CineTrack"), useUnmergedTree = true).assertIsDisplayed()
        compose.onNode(hasText("100% Local"), useUnmergedTree = true).assertIsDisplayed()
    }
}
