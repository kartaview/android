package com.telenav.osv.obd.pair.adapter;

import java.util.List;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.telenav.osv.R;
import androidx.recyclerview.widget.RecyclerView;

/**
 * @author cameliao
 */

public class ObdPairGuideAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<CharSequence> guideInstructions;

    public ObdPairGuideAdapter(List<CharSequence> guideInstructions) {
        this.guideInstructions = guideInstructions;
    }

    @Override
    public ObdPairGuideViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_obd_pair_guide, parent, false);
        return new ObdPairGuideViewHolder(v);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ObdPairGuideViewHolder obdPairGuideViewHolder = (ObdPairGuideViewHolder) holder;
        String displayableIndex = position + 1 + ".";
        obdPairGuideViewHolder.itemPairGuideIndex.setText(displayableIndex);
        obdPairGuideViewHolder.itemPairGuideInstruction.setMovementMethod(LinkMovementMethod.getInstance());
        obdPairGuideViewHolder.itemPairGuideInstruction.setText(guideInstructions.get(position));
    }

    @Override
    public int getItemCount() {
        return guideInstructions.size();
    }

    private class ObdPairGuideViewHolder extends RecyclerView.ViewHolder {

        private TextView itemPairGuideIndex;

        private TextView itemPairGuideInstruction;

        ObdPairGuideViewHolder(View view) {
            super(view);
            itemPairGuideIndex = view.findViewById(R.id.text_view_item_obd_pair_guide_current_index);
            itemPairGuideInstruction = view.findViewById(R.id.text_view_item_obd_pair_guide_instruction);
        }
    }
}
