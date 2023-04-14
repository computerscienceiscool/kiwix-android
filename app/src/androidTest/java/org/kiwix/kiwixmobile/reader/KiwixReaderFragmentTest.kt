/*
 * Kiwix Android
 * Copyright (c) 2023 Kiwix <android.kiwix.org>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.kiwix.kiwixmobile.reader

import android.os.Build
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.internal.runner.junit4.statement.UiThreadStatement
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import leakcanary.LeakAssertions
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.nav.destination.library.LocalLibraryFragmentDirections
import org.kiwix.kiwixmobile.testutils.RetryRule
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class KiwixReaderFragmentTest : BaseActivityTest() {
  @Rule
  @JvmField
  var retryRule = RetryRule()

  private lateinit var kiwixMainActivity: KiwixMainActivity

  @Before
  override fun waitForIdle() {
    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).waitForIdle()
    PreferenceManager.getDefaultSharedPreferences(context).edit {
      putBoolean(SharedPreferenceUtil.PREF_SHOW_INTRO, false)
      putBoolean(SharedPreferenceUtil.PREF_WIFI_ONLY, false)
      putBoolean(SharedPreferenceUtil.PREF_IS_TEST, true)
    }
  }

  @Test
  fun testTabClosedDialog() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
      ActivityScenario.launch(KiwixMainActivity::class.java).onActivity {
        kiwixMainActivity = it
        kiwixMainActivity.navigate(R.id.libraryFragment)
      }
      val loadFileStream =
        KiwixReaderFragmentTest::class.java.classLoader.getResourceAsStream("testzim.zim")
      val zimFile = File(context.cacheDir, "testzim.zim")
      if (zimFile.exists()) zimFile.delete()
      zimFile.createNewFile()
      loadFileStream.use { inputStream ->
        val outputStream: OutputStream = FileOutputStream(zimFile)
        outputStream.use { it ->
          val buffer = ByteArray(inputStream.available())
          var length: Int
          while (inputStream.read(buffer).also { length = it } > 0) {
            it.write(buffer, 0, length)
          }
        }
      }
      UiThreadStatement.runOnUiThread {
        kiwixMainActivity.navigate(
          LocalLibraryFragmentDirections.actionNavigationLibraryToNavigationReader()
            .apply { zimFileUri = zimFile.toUri().toString() }
        )
      }
      reader {
        checkZimFileLoadedSuccessful(R.id.readerFragment)
        clickOnTabIcon()
        clickOnClosedAllTabsButton()
        clickOnUndoButton()
        assertTabRestored()
      }
      LeakAssertions.assertNoLeaks()
    }
  }

  @After
  fun setIsTestPreference() {
    PreferenceManager.getDefaultSharedPreferences(context).edit {
      putBoolean(SharedPreferenceUtil.PREF_IS_TEST, false)
    }
  }
}