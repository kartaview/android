package com.telenav.osv.ui.fragment.settings.custom;

import java.util.List;
import android.content.Context;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import com.telenav.osv.R;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceViewHolder;

/**
 * Defines a {@code PreferenceGroup} for a radio group which can contain multiple radio buttons.
 */
public class RadioGroupPreference extends PreferenceGroup {

    /**
     * The list of radio buttons which are part of the group.
     */
    private List<RadioButton> radioButtonList;

    /**
     * Click listener called when a radio button was checked.
     */
    private RadioGroup.OnCheckedChangeListener listener;

    public RadioGroupPreference(@NonNull Context context, @NonNull List<RadioButton> buttonList) {
        super(context, null);
        setLayoutResource(R.layout.settings_item_radio_group);
        radioButtonList = buttonList;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        RadioGroup radioGroup = (RadioGroup) holder.findViewById(R.id.radio_group);
        radioGroup.setOnCheckedChangeListener((radioGroup1, i) -> listener.onCheckedChanged(radioGroup1, i));
        //addChild each radio button to the group
        for (RadioButton radioButton : radioButtonList) {
            radioGroup.addView(radioButton);
        }
    }

    /**
     * @return the group check listener, called when a new item is checked.
     */
    public RadioGroup.OnCheckedChangeListener getOnCheckedChangeListener() {
        return listener;
    }

    /**
     * Sets the radio group listener which is called when a radio button was checked.
     * @param listener
     */
    public void setOnCheckedChangeListener(RadioGroup.OnCheckedChangeListener listener) {
        this.listener = listener;
    }

    /**
     * @return the radio group items representing the options.
     */
    public List<RadioButton> getRadioButtonList() {
        return radioButtonList;
    }
}