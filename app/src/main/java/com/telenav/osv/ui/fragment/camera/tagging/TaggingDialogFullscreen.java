package com.telenav.osv.ui.fragment.camera.tagging;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import com.telenav.osv.R;
import com.telenav.osv.common.toolbar.OSCToolbar;
import com.telenav.osv.common.toolbar.ToolbarSettings;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.fragment.app.DialogFragment;

public class TaggingDialogFullscreen extends DialogFragment {

    public static final String TAG = TaggingDialogFullscreen.class.getSimpleName();

    public static final String TAGGING_DIALOG_IDENTIFIER = "tagging_dialog_identifier";

    public static final int ROAD_NARROW = 0;

    public static final int ROAD_CLOSED = 1;

    public static final int DROP_NOTE = 2;

    private OSCToolbar oscToolbar;

    private int identifier;

    private EditText editText;

    private Button button;

    private Consumer<String> onButtonPress;

    public TaggingDialogFullscreen(@TaggingDialogIdentifier int identifier, Consumer<String> onButtonPress) {
        this.identifier = identifier;
        this.onButtonPress = onButtonPress;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialogStyle);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.dialog_tagging, container, false);
        oscToolbar = new OSCToolbar(view.findViewById(R.id.toolbar_partial), v -> dismiss());
        editText = view.findViewById(R.id.text_dialog_tagging_note);
        button = view.findViewById(R.id.button_dialog_tagging_end);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        int titleResId;
        int buttonStringResId;
        switch (identifier) {
            case ROAD_NARROW:
                buttonStringResId = R.string.add_narrow_road;
                titleResId = R.string.narrow_road;
                break;
            case ROAD_CLOSED:
                buttonStringResId = R.string.add_road_closed;
                titleResId = R.string.road_closed;
                break;
            default:
                buttonStringResId = R.string.add_drop_note;
                titleResId = R.string.drop_note;
                break;
        }
        oscToolbar.updateToolbar(
                new ToolbarSettings.Builder()
                        .setNavigationIcon(R.drawable.vector_back_black)
                        .setTitle(titleResId)
                        .build());
        button.setText(buttonStringResId);
        button.setOnClickListener(click -> {
            if (onButtonPress != null) {
                onButtonPress.accept(editText.getText().toString());
            }
            dismiss();
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            dialog.getWindow().setLayout(width, height);
        }
    }

    @IntDef(value = {ROAD_CLOSED, ROAD_NARROW, DROP_NOTE})
    public @interface TaggingDialogIdentifier {
        //empty since we use the values for the dialog
    }
}
