//package com.telenav.spherical.view;
//
//import android.app.Activity;
//import android.app.AlertDialog;
//import android.app.Dialog;
//import android.app.DialogFragment;
//import android.app.FragmentManager;
//import android.os.Bundle;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.widget.Button;
//import android.widget.RadioGroup;
//import com.telenav.osv.R;
//import com.telenav.spherical.model.RotateInertia;
//
///**
// * Setting dialog fragment
// */
//public class ConfigurationDialog extends DialogFragment {
//
//    private RotateInertia mRotateInertia;
//    private DialogBtnListener mListener = null;
//
//    /**
//     *
//     */
//    public ConfigurationDialog() {
//        super();
//    }
//
//    /**
//     * onCreateDialog Method
//     * @param savedInstanceState onCreateDialog Status value
//     * @return Dialog instance
//     */
//    @Override
//    public Dialog onCreateDialog(Bundle savedInstanceState) {
//
//        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//        LayoutInflater inflater = getActivity().getLayoutInflater();
//        final View layout = inflater.inflate(R.layout.dialog_glphotoview_config, null);
//        if (null != layout) {
//            Button btn = (Button)layout.findViewById(R.id.btn_commit);
//            btn.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    if (null != mListener) {
//                        mListener.onDialogCommitClick(mRotateInertia);
//                    }
//                    dismiss();
//                }
//            });
//        }
//
//        mRotateInertia = (RotateInertia) getArguments().getSerializable("rotate_inertia");
//        if (null != mRotateInertia) {
//            RadioGroup rg = (RadioGroup) layout.findViewById(R.id.rotation_inertia);
//            if (null != rg) {
//                switch (mRotateInertia) {
//                    case INERTIA_0:
//                        rg.check(R.id.inertia_0);
//                        break;
//                    case INERTIA_50:
//                        rg.check(R.id.inertia_50);
//                        break;
//                    case INERTIA_100:
//                        rg.check(R.id.inertia_100);
//                        break;
//                    default:
//                        break;
//                }
//
//                rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
//                    @Override
//                    public void onCheckedChanged(RadioGroup group, int checkedId) {
//                        switch (checkedId) {
//                            case R.id.inertia_0:
//                                mRotateInertia = RotateInertia.INERTIA_0;
//                                break;
//                            case R.id.inertia_50:
//                                mRotateInertia = RotateInertia.INERTIA_50;
//                                break;
//                            case R.id.inertia_100:
//                                mRotateInertia = RotateInertia.INERTIA_100;
//                                break;
//                            default:
//                                mRotateInertia = null;
//                                break;
//                        }
//                    }
//                });
//            }
//        }
//
//        builder.setView(layout);
//
//        return builder.create();
//    }
//
//    /**
//     * Dialog display method
//     * @param mgr Fragment manager object
//     * @param inertia Inertia settings for rotation process
//     */
//    public static void show(FragmentManager mgr, RotateInertia inertia) {
//        ConfigurationDialog dialog = new ConfigurationDialog();
//        Bundle bundle = new Bundle();
//        bundle.putSerializable("rotate_inertia", inertia);
//
//        dialog.setArguments(bundle);
//        dialog.show(mgr, ConfigurationDialog.class.getSimpleName());
//    }
//
//    /**
//     * onAttach Method
//     * @param activity Attached activity object
//     */
//    @Override
//    public void onAttach(Activity activity) {
//        super.onAttach(activity);
//        try {
//            mListener = (DialogBtnListener) activity;
//        }
//        catch (ClassCastException e) {
//            e.printStackTrace();
//            mListener = null;
//        }
//    }
//
//    /**
//     * Event listener interface for when a dialog is exited
//	 * If a selection value is required in this dialog, it is necessary to attach the activity that implemented this method
//     */
//    public interface DialogBtnListener {
//        void onDialogCommitClick(RotateInertia inertia);
//    }
//}
