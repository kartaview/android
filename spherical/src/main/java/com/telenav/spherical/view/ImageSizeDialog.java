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
//import com.telenav.spherical.model.ImageSize;
//
///**
// * Setting dialog fragment
// */
//public class ImageSizeDialog extends DialogFragment {
//
//    private ImageSize mImageSize;
//    private DialogBtnListener mListener = null;
//
//    /**
//     *
//     */
//    public ImageSizeDialog() {
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
//        final View layout = inflater.inflate(R.layout.dialog_image_size, null);
//        if (null != layout) {
//            Button btn = (Button)layout.findViewById(R.id.btn_commit);
//            btn.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    if (null != mListener) {
//                        mListener.onDialogCommitClick(mImageSize);
//                    }
//                    dismiss();
//                }
//            });
//        }
//
//        mImageSize = (ImageSize) getArguments().getSerializable("image_size");
//        if (null != mImageSize) {
//            RadioGroup rg = (RadioGroup) layout.findViewById(R.id.image_size);
//            if (null != rg) {
//                switch (mImageSize) {
//                    case IMAGE_SIZE_2048x1024:
//                        rg.check(R.id.image_size_2048x1024);
//                        break;
//                    default:
//                    case IMAGE_SIZE_5376x2688:
//                        rg.check(R.id.image_size_5376x2688);
//                        break;
//                }
//
//                rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
//                    @Override
//                    public void onCheckedChanged(RadioGroup group, int checkedId) {
//                        switch (checkedId) {
//                            case R.id.image_size_2048x1024:
//                                mImageSize = ImageSize.IMAGE_SIZE_2048x1024;
//                                break;
//                            default:
//                            case R.id.image_size_5376x2688:
//                                mImageSize = ImageSize.IMAGE_SIZE_5376x2688;
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
//     * @param imageSize Inertia settings for rotation process
//     */
//    public static void show(FragmentManager mgr, ImageSize imageSize) {
//        ImageSizeDialog dialog = new ImageSizeDialog();
//        Bundle bundle = new Bundle();
//        bundle.putSerializable("image_size", imageSize);
//
//        dialog.setArguments(bundle);
//        dialog.show(mgr, ImageSizeDialog.class.getSimpleName());
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
//        void onDialogCommitClick(ImageSize imageSize);
//    }
//}
