package com.telenav.osv.ui.fragment.settings.viewmodel

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.content.ContextCompat
import com.telenav.osv.application.ApplicationPreferences
import com.telenav.osv.application.OSVApplication
import com.telenav.osv.application.PreferenceTypes
import com.telenav.osv.recorder.RecorderManager
import com.telenav.osv.ui.fragment.settings.presenter.item.SwitchPresenter
import com.telenav.osv.utils.Utils
import org.junit.*
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.reset
import org.mockito.MockitoAnnotations
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
@PrepareForTest(ContextCompat::class, Utils::class, SwitchPresenter::class, SettingsViewModel::class)
class SettingsViewModelTest {

    private lateinit var settingsViewModel: SettingsViewModel

    @Rule
    val rule: TestRule = InstantTaskExecutorRule()

    @Mock
    lateinit var application: OSVApplication

    @Mock
    lateinit var appPrefs: ApplicationPreferences

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var resources: Resources

    @Mock
    lateinit var packageInfo: PackageInfo

    @Mock
    lateinit var packageManager: PackageManager

    @Mock
    lateinit var recorderManager: RecorderManager

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        settingsViewModel = SettingsViewModel(application, appPrefs)
    }

    @After
    fun tearDown() {
        reset(application, appPrefs, context)
        reset(resources, packageInfo, packageManager, recorderManager)
    }

    @Test
    fun testCameraPermissionDenied() {
        mockComponentsAtStarting(PackageManager.PERMISSION_DENIED, true)
        settingsViewModel.cameraPermissionsObservable
        Assert.assertNull(settingsViewModel.cameraPermissionsObservable.value)
        settingsViewModel.start()
        Assert.assertNotNull(settingsViewModel.cameraPermissionsObservable.value)
    }

    @Test
    fun testStart() {
        mockComponentsAtStarting(PackageManager.PERMISSION_GRANTED, true)
        settingsViewModel.getSettingsDataObservable()
        settingsViewModel.start()
        Assert.assertNotNull(settingsViewModel.settingsDataObservable.value)
        Assert.assertTrue(settingsViewModel.settingsDataObservable.value!!.size != 0)
    }

    @Test
    fun testSDCardAvailable() {
        mockComponentsAtStarting(PackageManager.PERMISSION_GRANTED, false)
        settingsViewModel.getSettingsDataObservable()
        settingsViewModel.start()
        Assert.assertNotNull(settingsViewModel.settingsDataObservable.value)
        var recordingCategory = settingsViewModel.settingsDataObservable.value!![1]
        val itemsWithoutSdCard = recordingCategory.items.size
        mockComponentsAtStarting(PackageManager.PERMISSION_GRANTED, true)
        settingsViewModel.start()
        recordingCategory = settingsViewModel.settingsDataObservable.value!![1]
        val itemsWithSdCard = recordingCategory.items.size
        Assert.assertEquals(itemsWithoutSdCard + 1, itemsWithSdCard)

    }

    @Test
    fun testStartWithDebugMode() {
        mockComponentsAtStarting(PackageManager.PERMISSION_GRANTED, true)
        settingsViewModel.getSettingsDataObservable()
        settingsViewModel.start()
        val withoutDebugCategorySize = settingsViewModel.settingsDataObservable.value!!.size
        PowerMockito.`when`(appPrefs.getBooleanPreference(PreferenceTypes.K_DEBUG_ENABLED, false))
                .thenReturn(true)
        settingsViewModel.start()
        val withDebugCategorySize = settingsViewModel.settingsDataObservable.value!!.size
        Assert.assertTrue(withoutDebugCategorySize + 1 == withDebugCategorySize)
    }

    @Test
    fun testResume() {
        settingsViewModel.getSummaryPreferenceObservable()
        settingsViewModel.onResume()
        Assert.assertNotNull(settingsViewModel.summaryPreferenceObservable.value)
    }

    @Test
    fun testPause() {
        settingsViewModel.getOpenScreenObservable()
        settingsViewModel.getOpenActivityScreenObservable()
        settingsViewModel.getSettingsDataObservable()
        settingsViewModel.onPause()
        Assert.assertNull(settingsViewModel.openScreenObservable.value)
        Assert.assertNull(settingsViewModel.openActivityScreenObservable.value)
        Assert.assertNull(settingsViewModel.settingsDataObservable.value)
    }

    private fun mockComponentsAtStarting(cameraPermissionStatus: Int, isSDCardAvailable: Boolean) {
        PowerMockito.mockStatic(ContextCompat::class.java)
        PowerMockito.`when`(ContextCompat.checkSelfPermission(application.applicationContext, android.Manifest.permission.CAMERA))
                .thenReturn(cameraPermissionStatus)
        PowerMockito.mockStatic(Utils::class.java)
        PowerMockito.`when`(Utils.checkSDCard(application.applicationContext))
                .thenReturn(isSDCardAvailable)
        `when`(application.resources)
                .thenReturn(resources)
        `when`(application.packageManager)
                .thenReturn(packageManager)
        `when`(application.packageManager.getPackageInfo(application.packageName, 0))
                .thenReturn(packageInfo)
        `when`(application.recorder)
                .thenReturn(recorderManager)
    }
}