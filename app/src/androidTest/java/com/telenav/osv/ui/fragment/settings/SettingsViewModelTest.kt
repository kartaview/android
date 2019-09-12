package com.telenav.osv.ui.fragment.settings

import android.Manifest
import android.content.Context
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreferenceCompat
import androidx.test.InstrumentationRegistry
import androidx.test.annotation.UiThreadTest
import androidx.test.rule.GrantPermissionRule
import com.telenav.osv.application.ApplicationPreferences
import com.telenav.osv.application.OSVApplication
import com.telenav.osv.application.PreferenceTypes
import com.telenav.osv.ui.fragment.HintsFragment
import com.telenav.osv.ui.fragment.IssueReportFragment
import com.telenav.osv.ui.fragment.settings.model.SettingsGroup
import com.telenav.osv.ui.fragment.settings.presenter.group.FooterPresenter
import com.telenav.osv.ui.fragment.settings.viewmodel.SettingsViewModel
import com.telenav.osv.utils.Utils
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.MockitoAnnotations

class SettingsViewModelTest {

    private lateinit var settingsViewModel: SettingsViewModel

    private lateinit var context: Context

    private lateinit var appPrefs: ApplicationPreferences

    @Before
    fun setUp() {
        GrantPermissionRule.grant(Manifest.permission.CAMERA)
        context = InstrumentationRegistry.getTargetContext()
        val app = InstrumentationRegistry.getTargetContext().applicationContext as OSVApplication
        MockitoAnnotations.initMocks(this)
        appPrefs = ApplicationPreferences(app)
        settingsViewModel = SettingsViewModel(app, appPrefs)
    }

    @Test
    @UiThreadTest
    fun testUploadOnCellular() {
        val uploadOnCellularPreference = getSettingsItem(0, 0) as SwitchPreferenceCompat
        Assert.assertTrue(uploadOnCellularPreference.key == PreferenceTypes.K_UPLOAD_DATA_ENABLED)
        Assert.assertNotNull(uploadOnCellularPreference.onPreferenceClickListener)
        Assert.assertNull(uploadOnCellularPreference.onPreferenceChangeListener)
        checkSwitchPreferenceChanged(uploadOnCellularPreference, PreferenceTypes.K_UPLOAD_DATA_ENABLED)
        checkPreferenceClickedListener(uploadOnCellularPreference)
    }

    @Test
    @UiThreadTest
    fun testAutoUpload() {
        val autoUploadPreference = getSettingsItem(0, 1) as SwitchPreferenceCompat
        Assert.assertTrue(autoUploadPreference.key == PreferenceTypes.K_UPLOAD_AUTO)
        Assert.assertNotNull(autoUploadPreference.onPreferenceClickListener)
        Assert.assertNull(autoUploadPreference.onPreferenceChangeListener)
        checkSwitchPreferenceChanged(autoUploadPreference, PreferenceTypes.K_UPLOAD_AUTO)
        checkPreferenceClickedListener(autoUploadPreference)
    }

    @Test
    @UiThreadTest
    fun testRecordingResolution() {
        val resolutionPreference = getSettingsItem(1, 0)
        Assert.assertNotNull(resolutionPreference.onPreferenceClickListener)
        Assert.assertNull(resolutionPreference.onPreferenceChangeListener)
        checkPreferenceOpenSubmenu(resolutionPreference, SettingsViewModel.SUBMENU_TAG_RESOLUTION)
    }

    @Test
    @UiThreadTest
    fun testRecordingVideo() {
        val videoRecordingPreference = getSettingsItem(1, 1) as SwitchPreferenceCompat
        Assert.assertTrue(videoRecordingPreference.key == PreferenceTypes.K_VIDEO_MODE_ENABLED)
        Assert.assertNull(videoRecordingPreference.onPreferenceClickListener)
        Assert.assertNull(videoRecordingPreference.onPreferenceChangeListener)
        checkSwitchPreferenceChanged(videoRecordingPreference, PreferenceTypes.K_VIDEO_MODE_ENABLED)
    }

    @Test
    @UiThreadTest
    fun testRecordingRemovableStorage() {
        if (!Utils.checkSDCard(context)) {
            return
        }
        val removableStoragePreference = getSettingsItem(1, 2) as SwitchPreferenceCompat
        Assert.assertTrue(removableStoragePreference.key == PreferenceTypes.K_EXTERNAL_STORAGE)
        Assert.assertNull(removableStoragePreference.onPreferenceClickListener)
        Assert.assertNotNull(removableStoragePreference.onPreferenceChangeListener)
        checkSwitchPreferenceChanged(removableStoragePreference, PreferenceTypes.K_EXTERNAL_STORAGE)
        checkPreferenceChangedListener(removableStoragePreference)
    }

    @Test
    @UiThreadTest
    fun testRecordingDistanceUnitMetric() {
        val distanceUnitPreference = if (Utils.checkSDCard(context)) {
            getSettingsItem(1, 3)
        } else {
            getSettingsItem(1, 2)
        }
        Assert.assertNotNull(distanceUnitPreference.onPreferenceClickListener)
        Assert.assertNull(distanceUnitPreference.onPreferenceChangeListener)
        checkPreferenceOpenSubmenu(distanceUnitPreference, SettingsViewModel.SUBMENU_TAG_DISTANCE_UNIT)
    }

    @Test
    @UiThreadTest
    fun testRecordingPoints() {
        val pointsPreference = if (Utils.checkSDCard(context)) {
            getSettingsItem(1, 4) as SwitchPreferenceCompat
        } else {
            getSettingsItem(1, 3) as SwitchPreferenceCompat
        }
        Assert.assertTrue(pointsPreference.key == PreferenceTypes.K_GAMIFICATION)
        Assert.assertNull(pointsPreference.onPreferenceClickListener)
        Assert.assertNull(pointsPreference.onPreferenceChangeListener)
        checkSwitchPreferenceChanged(pointsPreference, PreferenceTypes.K_GAMIFICATION)
    }

    @Test
    @UiThreadTest
    fun testSwitchMap() {
        val switchMapPreference = getSettingsItem(2, 0)
        Assert.assertTrue(switchMapPreference.key == PreferenceTypes.K_MAP_ENABLED)
        Assert.assertNull(switchMapPreference.onPreferenceClickListener)
        Assert.assertNotNull(switchMapPreference.onPreferenceChangeListener)
        settingsViewModel.openScreenObservable
        Assert.assertNull(settingsViewModel.openScreenObservable.value)
        val preferenceValue = appPrefs.getBooleanPreference(PreferenceTypes.K_MAP_ENABLED)
        checkPreferenceChangedListener(switchMapPreference)
        val newScreen = settingsViewModel.openScreenObservable.value
        Assert.assertNotNull(newScreen)
        Assert.assertTrue(newScreen!!.first == RestartAppDialogFragment.TAG)
        Assert.assertEquals(!preferenceValue, appPrefs.getBooleanPreference(PreferenceTypes.K_MAP_ENABLED))
    }

    @Test
    @UiThreadTest
    fun testImproveReportAProblem() {
        val reportAProblemPreference = getSettingsItem(3, 0)
        Assert.assertNotNull(reportAProblemPreference.onPreferenceClickListener)
        Assert.assertNull(reportAProblemPreference.onPreferenceChangeListener)
        checkPreferenceOpenSubmenu(reportAProblemPreference, IssueReportFragment.TAG)
    }

    @Test
    @UiThreadTest
    fun testImproveTips() {
        val tipsPreference = getSettingsItem(3, 1)
        Assert.assertNotNull(tipsPreference.onPreferenceClickListener)
        Assert.assertNull(tipsPreference.onPreferenceChangeListener)
        checkPreferenceOpenSubmenu(tipsPreference, HintsFragment.TAG)
    }

    @Test
    @UiThreadTest
    fun testImproveAppGuide() {
        val appGuidePreference = getSettingsItem(3, 2)
        Assert.assertNotNull(appGuidePreference.onPreferenceClickListener)
        Assert.assertNull(appGuidePreference.onPreferenceChangeListener)
        checkPreferenceOpenScreen(appGuidePreference, null)
    }

    @Test
    @UiThreadTest
    fun testLegalTermsAndConditions() {
        val termsAndConditions = getSettingsItem(4, 0)
        Assert.assertNotNull(termsAndConditions.onPreferenceClickListener)
        Assert.assertNull(termsAndConditions.onPreferenceChangeListener)
        checkPreferenceOpenScreen(termsAndConditions, "URL_TERMS_AND_CONDITIONS")
    }

    @Test
    @UiThreadTest
    fun testLegalPrivacyPolicy() {
        val privacyPolicyPreference = getSettingsItem(4, 1)
        Assert.assertNotNull(privacyPolicyPreference.onPreferenceClickListener)
        Assert.assertNull(privacyPolicyPreference.onPreferenceChangeListener)
        checkPreferenceOpenScreen(privacyPolicyPreference, "URL_PRIVACY_POLICY")
    }


    @Test
    @UiThreadTest
    fun testDebug() {
        appPrefs.saveBooleanPreference(PreferenceTypes.K_DEBUG_ENABLED, true)
        val debugPreference = getSettingsItem(5, 0)
        Assert.assertNotNull(debugPreference.onPreferenceClickListener)
        Assert.assertNull(debugPreference.onPreferenceChangeListener)
        checkPreferenceOpenSubmenu(debugPreference, SettingsViewModel.SUBMENU_DEBUG)
    }

    @Test
    @UiThreadTest
    fun testFooterWhenDebugIsDisable() {
        //disable debug mode
        appPrefs.saveBooleanPreference(PreferenceTypes.K_DEBUG_ENABLED, false)
        //get settings group for debug category
        settingsViewModel.settingsDataObservable
        settingsViewModel.start()
        val settingsGroups: List<SettingsGroup> = settingsViewModel.settingsDataObservable.value!!
        val settingsGroup = settingsGroups[5]
        //change accessibility for presenter in the settings group from private to public
        val fieldPresenter = settingsGroup.javaClass.getDeclaredField("presenter")
        fieldPresenter.isAccessible = true
        val groupPresenter: FooterPresenter = fieldPresenter.get(settingsGroup) as FooterPresenter
        //change accessibility for touch listener in presenter from private to public
        val fieldTouchListener = groupPresenter.javaClass.getDeclaredField("onTouchListener")
        fieldTouchListener.isAccessible = true
        //set a new touch listener in order to verify if this is called when a motion event is triggered
        val previousTouchListener: View.OnTouchListener = fieldTouchListener.get(groupPresenter) as View.OnTouchListener
        var touchListenerCalled = false
        fieldTouchListener.set(groupPresenter, View.OnTouchListener { view, motionEvent ->
            touchListenerCalled = true
            return@OnTouchListener previousTouchListener.onTouch(view, motionEvent)
        })
        //create tips preference
        val footerPreference = settingsGroup.getPreference(context)
        Assert.assertNull(footerPreference.onPreferenceClickListener)
        Assert.assertNull(footerPreference.onPreferenceChangeListener)
        //set a custom view on which the touch listener will be set in order to trigger a motion event
        val view = View(context)
        footerPreference.onBindViewHolder(PreferenceViewHolder.createInstanceForTests(view))
        view.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 0F, 0F, 0))
        Assert.assertTrue(touchListenerCalled)
    }

    @Test
    @UiThreadTest
    fun testFooterWhenDebugIsEnable() {
        //enable debug mode
        appPrefs.saveBooleanPreference(PreferenceTypes.K_DEBUG_ENABLED, true)
        //get settings group for debug category
        settingsViewModel.settingsDataObservable
        settingsViewModel.start()
        val settingsGroups: List<SettingsGroup> = settingsViewModel.settingsDataObservable.value!!
        val settingsGroup = settingsGroups[6]
        //change accessibility for presenter in the settings group from private to public
        val fieldPresenter = settingsGroup.javaClass.getDeclaredField("presenter")
        fieldPresenter.isAccessible = true
        val groupPresenter: FooterPresenter = fieldPresenter.get(settingsGroup) as FooterPresenter
        //change accessibility for touch listener in presenter from private to public
        val fieldTouchListener = groupPresenter.javaClass.getDeclaredField("onTouchListener")
        fieldTouchListener.isAccessible = true
        //check if the touch listener is null, debug menu is already enable
        val touchListener = fieldTouchListener.get(groupPresenter)
        Assert.assertNull(touchListener)
    }

    private fun getSettingsItem(groupIndex: Int, itemIndex: Int): Preference {
        settingsViewModel.settingsDataObservable
        settingsViewModel.start()
        val settingsGroup: List<SettingsGroup> = settingsViewModel.settingsDataObservable.value!!
        return settingsGroup[groupIndex].items[itemIndex].getPreference(context)
    }

    private fun checkPreferenceClickedListener(preference: Preference) {
        var preferenceClicked = false
        val onPreferenceClickListener = preference.onPreferenceClickListener
        preference.onPreferenceClickListener = Preference.OnPreferenceClickListener { pref ->
            preferenceClicked = true
            return@OnPreferenceClickListener onPreferenceClickListener.onPreferenceClick(pref)
        }
        preference.performClick()
        Assert.assertTrue(preferenceClicked)
    }

    private fun checkPreferenceChangedListener(preference: Preference) {
        var preferenceChanged = false
        val onPreferenceChangeListener = preference.onPreferenceChangeListener
        preference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { pref, newValue ->
            preferenceChanged = true
            return@OnPreferenceChangeListener onPreferenceChangeListener.onPreferenceChange(pref, newValue)
        }
        preference.callChangeListener(!appPrefs.getBooleanPreference(preference.key))
        Assert.assertTrue(preferenceChanged)
    }

    private fun checkPreferenceOpenSubmenu(preference: Preference, key: String) {
        settingsViewModel.openScreenObservable
        Assert.assertNull(settingsViewModel.openScreenObservable.value)
        checkPreferenceClickedListener(preference)
        val newScreen = settingsViewModel.openScreenObservable.value
        Assert.assertNotNull(newScreen)
        Assert.assertTrue(newScreen!!.first == key)
    }

    private fun checkPreferenceOpenScreen(preference: Preference, urlFieldName: String?) {
        settingsViewModel.openActivityScreenObservable
        Assert.assertNull(settingsViewModel.openActivityScreenObservable.value)
        checkPreferenceClickedListener(preference)
        val newScreen = settingsViewModel.openActivityScreenObservable.value
        Assert.assertNotNull(newScreen)
        //check if an URL intent is received
        if (urlFieldName != null) {
            val field = SettingsViewModel::class.java.getDeclaredField(urlFieldName)
            field.isAccessible = true
            Assert.assertTrue(newScreen!!.dataString == field.get(SettingsViewModel::class.java))
        }
    }

    private fun checkSwitchPreferenceChanged(switchPreference: SwitchPreferenceCompat, preferenceKey: String) {
        val field = switchPreference.javaClass.superclass!!.superclass!!.getDeclaredField("mPreferenceManager")
        field.isAccessible = true
        field.set(switchPreference, getPreferenceManager())
        val preferenceValue = appPrefs.getBooleanPreference(preferenceKey)
        switchPreference.isChecked = !preferenceValue
        Assert.assertEquals(!preferenceValue, appPrefs.getBooleanPreference(preferenceKey))
    }

    private fun getPreferenceManager(): PreferenceManager {
        val preferenceManager = PreferenceManager(context)
        preferenceManager.sharedPreferencesName = ApplicationPreferences.PREFS_NAME
        preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
        return preferenceManager
    }
}