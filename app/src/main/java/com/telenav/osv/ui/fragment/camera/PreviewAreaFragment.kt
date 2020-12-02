package com.telenav.osv.ui.fragment.camera

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.telenav.osv.R
import com.telenav.osv.common.model.base.KVBaseFragment
import com.telenav.osv.common.toolbar.KVToolbar
import com.telenav.osv.common.toolbar.ToolbarSettings
import com.telenav.osv.utils.AnimationUtils

/**
 * A simple [Fragment] subclass.
 * Use the [PreviewAreaFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PreviewAreaFragment : KVBaseFragment() {

    /**
     * Flag which show in which status the current preview is.
     */
    private var isMapPreview = true

    private val previewAreaOnClickListener = View.OnClickListener { _: View? ->
        activity?.let {
            if (isMapPreview) {
                AnimationUtils.resizeCameraUI(it, R.id.layout_activity_obd_fragment_camera_preview_container, R.id.layout_activity_obd_fragment_camera_preview_map_container)
            } else {
                AnimationUtils.resizeCameraUI(it, R.id.layout_activity_obd_fragment_camera_preview_map_container, R.id.layout_activity_obd_fragment_camera_preview_container)
            }
            isMapPreview = !isMapPreview
        }
    }

    private lateinit var clickPreviewArea: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun getToolbarSettings(kvToolbar: KVToolbar?): ToolbarSettings? {
        return null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_preview_area, container, false)
        clickPreviewArea = root.findViewById(R.id.view_fragment_preview_area_click_area)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        clickPreviewArea.setOnClickListener(previewAreaOnClickListener)
    }

    override fun handleBackPressed(): Boolean {
        return false
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment PreviewAreaFragment.
         */
        @JvmStatic
        fun newInstance() = PreviewAreaFragment()

        const val TAG = "PreviewAreaFragment"
    }
}
