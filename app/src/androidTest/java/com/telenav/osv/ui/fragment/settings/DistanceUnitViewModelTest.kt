package com.telenav.osv.ui.fragment.settings

import android.Manifest
import android.content.Context
import android.view.View
import android.widget.RadioGroup
import androidx.preference.PreferenceViewHolder
import androidx.test.InstrumentationRegistry
import androidx.test.annotation.UiThreadTest
import androidx.test.rule.GrantPermissionRule
import com.telenav.osv.R
import com.telenav.osv.application.ApplicationPreferences
import com.telenav.osv.application.KVApplication
import com.telenav.osv.application.PreferenceTypes
import com.telenav.osv.ui.fragment.settings.custom.RadioGroupPreference
import com.telenav.osv.ui.fragment.settings.viewmodel.DistanceUnitViewModel
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.MockitoAnnotations

class DistanceUnitViewModelTest {
    private lateinit var viewModel: DistanceUnitViewModel

    private lateinit var context: Context

    private lateinit var appPrefs: ApplicationPreferences
    @Before
    fun setUp() {
        GrantPermissionRule.grant(Manifest.permission.CAMERA)
        context = InstrumentationRegistry.getTargetContext()
        val app = InstrumentationRegistry.getTargetContext().applicationContext as KVApplication
        MockitoAnnotations.initMocks(this)
        appPrefs = ApplicationPreferences(app)
        viewModel = DistanceUnitViewModel(app, appPrefs)
    }

    @Test
    @UiThreadTest
    fun testDistanceUnitClick() {
        val preference = getGroupPreference()
        val checkedChangeListener = preference.onCheckedChangeListener
        var preferenceChanged = false
        preference.onCheckedChangeListener = (RadioGroup.OnCheckedChangeListener { radioGroup, i ->
            preferenceChanged = true
            checkedChangeListener.onCheckedChanged(radioGroup, i)
        })
        preference.onBindViewHolder(PreferenceViewHolder.createInstanceForTests(View.inflate(context, R.layout.settings_item_radio_group, null)))
        preference.radioButtonList[0].isChecked = false
        preference.radioButtonList[0].isChecked = true
        Assert.assertTrue(preferenceChanged)
        val distanceUnitTag = preference.radioButtonList[0].tag as Int
        Assert.assertEquals(distanceUnitTag == DistanceUnitViewModel.TAG_METRIC, appPrefs.getBooleanPreference(PreferenceTypes.K_DISTANCE_UNIT_METRIC))
    }

    @Test
    @UiThreadTest
    fun testStoredDistanceUnitCheckedWhenDisplayingTheList() {
        val preference = getGroupPreference()
        for (radioButton in preference.radioButtonList) {
            if (radioButton.isChecked) {
                val distanceUnitTag = radioButton.tag as Int
                Assert.assertEquals(distanceUnitTag == DistanceUnitViewModel.TAG_METRIC, appPrefs.getBooleanPreference(PreferenceTypes.K_DISTANCE_UNIT_METRIC))
                return
            }
        }
        Assert.assertTrue(false)
    }

    private fun getGroupPreference(): RadioGroupPreference {
        viewModel.settingsDataObservable
        viewModel.start()
        val settingsGroups = viewModel.settingsDataObservable.value!!
        return settingsGroups[0].getPreference(context) as RadioGroupPreference
    }
}