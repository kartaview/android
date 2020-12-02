package com.telenav.osv.report.fragment

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.telenav.osv.R
import com.telenav.osv.application.ApplicationPreferences
import com.telenav.osv.application.KVApplication
import com.telenav.osv.common.Injection
import com.telenav.osv.common.dialog.KVDialog
import com.telenav.osv.databinding.FragmentReportIssueBinding
import com.telenav.osv.jarvis.login.utils.LoginUtils
import com.telenav.osv.location.LocationService
import com.telenav.osv.report.viewmodel.ReportIssueViewModel

/**
 * This bottom sheet dialog fragment reports issue faced by a user while recording
 * Once user selects an issue user is navigated to next screen displaying edit text for optional notes and a submit button
 * If user successfully submits issue then a Toast is displayed and this fragment is dismissed
 * In case of failure a Toast message is displayed and Submit screen is not dismissed
 */

class ReportIssueBottomDialogFragment: BottomSheetDialogFragment() {

    private lateinit var fragmentReportIssueBinding: FragmentReportIssueBinding
    private lateinit var reportIssueViewModel: ReportIssueViewModel
    private lateinit var appPrefs: ApplicationPreferences
    private var sessionExpireDialog: KVDialog? = null
    private lateinit var locationService: LocationService

    init {
        TAG = ReportIssueBottomDialogFragment::class.java.simpleName
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity?.let {
            appPrefs = (it.application as KVApplication).appPrefs
            locationService = Injection.provideLocationService(it.applicationContext)
        }
        reportIssueViewModel = ViewModelProviders.of(this, Injection.provideReportIssueViewModelFactory(
                Injection.provideReportIssueUseCase(Injection.provideReportIssueApi(true, appPrefs)),
                Injection.provideGenericJarvisApiErrorHandler(context, appPrefs),
                locationService))
                .get(ReportIssueViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentReportIssueBinding = FragmentReportIssueBinding.inflate(inflater, container, false)
                .apply {
                    reportIssueVm = reportIssueViewModel
                    lifecycleOwner = this@ReportIssueBottomDialogFragment
                }
        return  fragmentReportIssueBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()
        reportIssueViewModel.isSubmitIssueSuccess.observe(viewLifecycleOwner, Observer { isSubmitIssueSuccess ->
            if (isSubmitIssueSuccess) {
                onSubmitIssueSuccess(context)
            } else {
                onSubmitIssueError(context)
            }
        })
        reportIssueViewModel.shouldReLogin.observe(viewLifecycleOwner, Observer { shouldReLogin ->
            if (shouldReLogin) {
                showSessionExpiredDialog(context)
            }
        })
        reportIssueViewModel.isGetLocationError.observe(viewLifecycleOwner, Observer { isGetLocationError ->
            if (isGetLocationError) {
                onGetLocationError(context)
            }
        })
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet: FrameLayout? =
                    bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        return dialog
    }

    /**
     * This method displays alert dialog for expired session
     */
    private fun showSessionExpiredDialog(context: Context) {
        if (sessionExpireDialog == null) {
            sessionExpireDialog = LoginUtils.getSessionExpiredDialog(context)
        }
        sessionExpireDialog?.show()
    }

    private fun onSubmitIssueSuccess(context: Context) {
        Toast.makeText(context, context.getString(R.string.report_issue_success_toast_message), Toast.LENGTH_SHORT).show()
        dismiss()
    }

    private fun onSubmitIssueError(context: Context) {
        Toast.makeText(context, context.getString(R.string.report_issue_failure_toast_message), Toast.LENGTH_SHORT).show()
    }

    private fun onGetLocationError(context: Context) {
        Toast.makeText(context, context.getString(R.string.location_failure_toast_message), Toast.LENGTH_SHORT).show()
    }

    companion object {
        lateinit var TAG: String
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         * @return A new instance of fragment ReportIssueBottomDialogFragment.
         */
        @JvmStatic
        fun newInstance() = ReportIssueBottomDialogFragment()
    }

}