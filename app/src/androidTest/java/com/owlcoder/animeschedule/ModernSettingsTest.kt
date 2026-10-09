package com.owlcoder.animeschedule

import android.graphics.Bitmap
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.owlcoder.animeschedule.data.local.datastore.*
import com.owlcoder.animeschedule.domain.model.*
import com.owlcoder.animeschedule.domain.repository.WatchSourceRepository
import com.owlcoder.animeschedule.presentation.screens.settings.*
import com.owlcoder.animeschedule.presentation.screens.onboarding.OnboardingScreen
import com.owlcoder.animeschedule.ui.theme.AnimeScheduleTheme
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ModernSettingsTest {
    @get:Rule val compose=createComposeRule()
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private val models=ViewModelStore()
    private val file=File(instrumentation.targetContext.cacheDir,"qa-modern-${System.nanoTime()}.preferences_pb")
    private val backing=PreferenceDataStoreFactory.create(scope=scope){file}
    private val prefs=UserPreferencesDataStore(backing)
    private val tools=WatchToolsStore(backing,prefs)
    private fun text(id:Int)=instrumentation.targetContext.getString(id)
    @After fun cleanup(){instrumentation.runOnMainSync{models.clear()};scope.cancel();file.delete()}

    @Test fun episodeDurationPresetsAndStepControlsUpdateTheValue() {
        var minutes by mutableIntStateOf(24)
        compose.setContent { AnimeScheduleTheme(themeMode=ThemeMode.LIGHT){ EpisodeLengthSheet(minutes,{ minutes=it },{}) } }
        compose.onNodeWithTag("duration-increase").performClick()
        compose.runOnIdle {assertEquals(25,minutes)}
        compose.onNodeWithText(instrumentation.targetContext.getString(R.string.episode_length_value,45)).performClick()
        compose.onNodeWithTag("duration-decrease").performClick()
        compose.runOnIdle {assertEquals(44,minutes)}
        screenshot("episode-duration")
    }

    private class Sources: WatchSourceRepository {
        val rows=MutableStateFlow(listOf(WatchSource(1,"Alpha source","https://example.com/?q={query}",null),WatchSource(2,"Beta source","https://example.org/?q={query}",null)))
        override fun getAll()=rows
        override suspend fun add(name:String,urlTemplate:String,faviconUrl:String?,openExternally:Boolean){rows.value=rows.value+WatchSource(3,name,urlTemplate,faviconUrl,openExternally=openExternally)}
        override suspend fun update(source:WatchSource){rows.value=rows.value.map{if(it.id==source.id)source else it}}
        override suspend fun delete(source:WatchSource){rows.value=rows.value.filterNot{it.id==source.id}}
    }
    @Test fun watchSourceEditingReusesOneDialogAndAllowsTheNextSource() {
        val sources=Sources();val vm=WatchSourcesViewModel(sources).also{models.put("sources",it)}
        compose.setContent {AnimeScheduleTheme(themeMode=ThemeMode.LIGHT){WatchSourcesBottomSheet({},vm)}}
        compose.onNodeWithText("Alpha source").performClick()
        compose.onAllNodes(isDialog()).assertCountEquals(1)
        compose.onAllNodes(hasSetTextAction())[0].performTextReplacement("Updated Alpha")
        compose.onNodeWithText(text(R.string.common_save)).performClick()
        compose.waitUntil{sources.rows.value.first().name=="Updated Alpha"}
        compose.onNodeWithText("Beta source").performClick()
        compose.onAllNodes(isDialog()).assertCountEquals(1)
        compose.onAllNodes(hasSetTextAction())[0].assertTextContains("Beta source")
        screenshot("watch-source-single-editor")
        compose.onNodeWithText(text(R.string.common_cancel)).performClick()
        compose.onNodeWithText("Updated Alpha").assertIsDisplayed()
    }

    @Test fun personalBackupExportsThroughPickerThenRestoresAfterExplicitConfirmation() {
        runBlocking{tools.setNote(101,"QA backup note");tools.setTags(101,"Akcija");tools.setEpisodeMinutes(45)}
        val store=PersonalBackupStore(backing,prefs,tools)
        compose.setContent {AnimeScheduleTheme(themeMode=ThemeMode.LIGHT){PersonalBackupSheet(store,"QA",{})}}
        compose.onNodeWithTag("backup-export").performClick()
        val automation=instrumentation.uiAutomation
        compose.waitUntil(10_000){automation.rootInActiveWindow?.packageName?.toString()?.contains("documentsui")==true}
        val name="qa54-${System.nanoTime() % 1_000_000}.json"
        fun descendants(root:AccessibilityNodeInfo):Sequence<AccessibilityNodeInfo> = sequence{yield(root);for(i in 0 until root.childCount)root.getChild(i)?.let{yieldAll(descendants(it))}}
        val editable=descendants(automation.rootInActiveWindow).first{it.isEditable}
        assertTrue(editable.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,Bundle().apply{putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,name)}))
        val save=automation.rootInActiveWindow.findAccessibilityNodeInfosByText("Save").first{it.text?.toString()?.equals("Save",true)==true}
        assertTrue(save.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        compose.waitUntil(10_000){automation.rootInActiveWindow?.packageName?.toString()==instrumentation.targetContext.packageName}
        compose.waitUntil(10_000){compose.onAllNodesWithText(text(R.string.backup_exported)).fetchSemanticsNodes().isNotEmpty()}
        runBlocking{tools.setNote(101,"Modified");tools.setEpisodeMinutes(12)}
        compose.onNodeWithTag("backup-select").performClick()
        compose.waitUntil(10_000){automation.rootInActiveWindow?.packageName?.toString()?.contains("documentsui")==true}
        automation.waitForIdle(500,5000)
        compose.waitUntil(10_000){automation.rootInActiveWindow.findAccessibilityNodeInfosByText(name).any{it.text?.toString()==name && !it.isEditable && it.isVisibleToUser}}
        val node=automation.rootInActiveWindow.findAccessibilityNodeInfosByText(name).first{it.text?.toString()==name && !it.isEditable && it.isVisibleToUser}
        val bounds=android.graphics.Rect();node.getBoundsInScreen(bounds)
        // Wait for the shell command to finish; closing its descriptor early cancels input.
        android.os.ParcelFileDescriptor.AutoCloseInputStream(
            automation.executeShellCommand("input tap ${bounds.centerX()} ${bounds.centerY()}")
        ).use { it.readBytes() }
        compose.waitUntil(10_000){automation.rootInActiveWindow?.packageName?.toString()==instrumentation.targetContext.packageName || automation.rootInActiveWindow.findAccessibilityNodeInfosByText("Open").any{it.text?.toString()?.equals("Open",true)==true}}
        if (automation.rootInActiveWindow?.packageName?.toString()?.contains("documentsui")==true) {
            val open=automation.rootInActiveWindow.findAccessibilityNodeInfosByText("Open").first{it.text?.toString()?.equals("Open",true)==true}
            assertTrue(open.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        }
        compose.waitUntil(10_000){automation.rootInActiveWindow?.packageName?.toString()==instrumentation.targetContext.packageName}
        compose.onNodeWithTag("backup-restore").performScrollTo().assertIsDisplayed()
        assertEquals("Modified",runBlocking{tools.data.first().notes[101]})
        screenshot("backup-preview")
        compose.onNodeWithTag("backup-restore").performClick()
        compose.waitUntil(10_000){runBlocking{tools.data.first().notes[101]}=="QA backup note"}
        assertEquals(45,runBlocking{tools.data.first().episodeMinutes})
        compose.onNodeWithText(text(R.string.backup_restored)).assertIsDisplayed()
    }
    @Test fun onboardingAllSixPagesKeepActionsVisible() {
        var completed=false
        compose.setContent {AnimeScheduleTheme(themeMode=ThemeMode.LIGHT){OnboardingScreen({completed=true},{},false,"",ThemeMode.LIGHT,ThemePalette.CLASSIC,AppLanguage.ENGLISH,{},{},{})}}
        repeat(6){page->
            screenshot("onboarding-$page")
            compose.onNodeWithText(if(page==5)"Get started" else "Continue").assertIsDisplayed().performClick()
            compose.waitForIdle()
        }
        compose.runOnIdle{assertTrue(completed)}
    }
    @Test fun roundedNotificationChoicesRemainSeparateAndSelectionUpdates() {
        var offset by mutableIntStateOf(0)
        compose.setContent {AnimeScheduleTheme(themeMode=ThemeMode.DARK,options=ThemeOptions(palette=ThemePalette.NEON)){NotificationSettingsSheet(true,offset,{}, {offset=it},{})}}
        compose.onNodeWithText(instrumentation.targetContext.getString(R.string.notif_offset_before,15)).performScrollTo().performClick()
        compose.runOnIdle{assertEquals(-15,offset)}
        screenshot("notifications-rounded-choices")
    }
    @Test fun roundedCacheChoicesUpdateTheRetention() {
        var days by mutableIntStateOf(10)
        compose.setContent {AnimeScheduleTheme(themeMode=ThemeMode.DARK,options=ThemeOptions(palette=ThemePalette.NEON)){CacheRetentionSheet(days,{days=it},{})}}
        compose.onNodeWithText(instrumentation.targetContext.getString(R.string.settings_cache_retention_days,14)).performClick()
        compose.runOnIdle{assertEquals(14,days)}
        screenshot("cache-rounded-choices")
    }
    private fun screenshot(name:String){
        compose.waitForIdle()
        if (!QaCapture.enabled) return
        instrumentation.uiAutomation.waitForIdle(400,5000);val bitmap=instrumentation.uiAutomation.takeScreenshot()
        File(instrumentation.targetContext.getExternalFilesDir(null),"qa-540-$name.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
    }
}
