package com.example

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class JaBoardNavigationRobolectricTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun `top level navigation flows between Home, Explore, Studio, and Settings`() {
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("home_bento_grid").assertIsDisplayed()

    composeTestRule.onNodeWithTag("nav_explore").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("explore_search_input").assertIsDisplayed()
    composeTestRule.onNodeWithText("Battery Monitor").assertIsDisplayed()

    composeTestRule.onNodeWithTag("nav_studio").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Theme & Dynamic Color").assertIsDisplayed()
    composeTestRule.onNodeWithTag("studio_list").performScrollToNode(hasText("Layout Presets"))
    composeTestRule.onNodeWithText("Layout Presets").assertIsDisplayed()
    composeTestRule.onNodeWithTag("studio_list").performScrollToNode(hasText("Home Clock Format"))
    composeTestRule.onNodeWithText("Home Clock Format").assertIsDisplayed()
    composeTestRule.onNodeWithTag("studio_clock_12h").assertIsDisplayed()
    composeTestRule.onNodeWithTag("studio_clock_24h").assertIsDisplayed()
    composeTestRule.onNodeWithTag("studio_clock_seconds_switch").assertIsDisplayed()
    composeTestRule.onNodeWithTag("studio_clock_millis_switch").assertIsDisplayed()

    composeTestRule.onNodeWithTag("nav_settings").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Appearance").assertIsDisplayed()
    composeTestRule.onNodeWithTag("settings_list").performScrollToNode(hasText("About JaBoard"))
    composeTestRule.onNodeWithText("About JaBoard").assertIsDisplayed()
  }

  @Test
  fun `studio clock format toggles update home clock display`() {
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("nav_studio").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("studio_list").performScrollToNode(hasText("Home Clock Format"))

    composeTestRule.onNodeWithTag("studio_clock_12h").performClick()
    composeTestRule.onNodeWithTag("studio_clock_millis_switch").performClick()
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      runCatching {
        composeTestRule.onNodeWithTag("studio_clock_12h").assertIsSelected()
        composeTestRule.onNodeWithTag("studio_clock_millis_switch").assertIsOn()
      }.isSuccess
    }

    composeTestRule.onNodeWithTag("nav_home").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onAllNodesWithTag("home_bento_grid").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("home_bento_grid").performScrollToNode(hasTestTag("home_card_clock"))
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      runCatching {
        composeTestRule.onNodeWithTag("home_clock_time_text", useUnmergedTree = true)
          .assert(hasText(".", substring = true))
          .assert(hasText("AM", substring = true) or hasText("PM", substring = true))
      }.isSuccess
    }
    composeTestRule.onNodeWithTag("home_clock_time_text", useUnmergedTree = true)
      .assertIsDisplayed()
      .assert(hasText(".", substring = true))
      .assert(hasText("AM", substring = true) or hasText("PM", substring = true))
  }
}
