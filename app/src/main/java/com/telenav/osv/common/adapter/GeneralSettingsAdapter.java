package com.telenav.osv.common.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.telenav.osv.R;
import com.telenav.osv.common.adapter.model.GeneralItemBase;
import com.telenav.osv.obd.model.HeaderSettingItem;
import com.telenav.osv.obd.model.IconTitleBtDeviceItem;
import com.telenav.osv.obd.model.LeftIconTitleSettingItem;
import com.telenav.osv.obd.model.LeftIconTitleSubtitleItem;
import com.telenav.osv.recorder.tagging.RecordingTaggingItem;
import com.telenav.osv.utils.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * General adapter for all settings item. This will create the item types available in {@code GeneralItemTypes} interface.
 * @author horatiuf
 * @see com.telenav.osv.common.adapter.model.GeneralItemBase.GeneralItemTypes
 */

public class GeneralSettingsAdapter extends RecyclerView.Adapter<GeneralSettingsAdapter.GeneralSettingsViewHolder> {

    /**
     * The {@code String} representing the TAG of the current class.
     */
    public static final String TAG = GeneralSettingsAdapter.class.getSimpleName();

    /**
     * The collection of {@code String} items which will be display by the current adapter.
     */
    private List<GeneralItemBase> items;

    /**
     * The tagging height used for all tagging items.
     */
    private int taggingWidth = 0;

    /**
     * Default constructor for the current class.
     * @param items the collection of {@code String} to be used for view binding/creation.
     */
    public GeneralSettingsAdapter(List<GeneralItemBase> items) {
        this.items = items;
    }

    /**
     * Default constructor for the current class for setting the list of items on going.
     */
    public GeneralSettingsAdapter() {
        items = new ArrayList<>();
    }

    @Override
    public GeneralSettingsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == GeneralItemBase.GENERAL_TYPE_TAGGING) {
            View taggingView = LayoutInflater.from(parent.getContext()).inflate(R.layout.partial_tagging_subitem, parent, false);
            if (taggingWidth == 0) {
                Log.d(TAG, "getParentWidth " + parent.getMeasuredWidth());
                Log.d(TAG, "getParentWidth width " + parent.getWidth());
                taggingWidth = ((parent.getMeasuredWidth() - 70) / 3);
            }
            taggingView.setLayoutParams(new CardView.LayoutParams(taggingWidth, CardView.LayoutParams.WRAP_CONTENT));
            return new TaggingViewHolder(taggingView);
        }
        return new ItemViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.partial_kv_setting_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull GeneralSettingsViewHolder holder, int position) {
        final GeneralItemBase currentItem = items.get(holder.getAdapterPosition());
        if (holder instanceof ItemViewHolder) {
            bindItemViewHolder((ItemViewHolder) holder, currentItem);
        } else if (holder instanceof TaggingViewHolder) {
            bindTaggingViewHolder((TaggingViewHolder) holder, (RecordingTaggingItem) currentItem);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).getType();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * Updates a {@link HeaderSettingItem} and notifies the adapter about the change.
     * @param headerSettingItem the new item for display.
     */
    public void updateItem(HeaderSettingItem headerSettingItem) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getType() == GeneralItemBase.GENERAL_TYPE_OBD_HEADER) {
                if (items.get(i).equals(headerSettingItem)) {
                    HeaderSettingItem currentHeaderSettingItem = (HeaderSettingItem) items.get(i);
                    if (currentHeaderSettingItem.isProgressVisible() != headerSettingItem.isProgressVisible()) {
                        currentHeaderSettingItem.setProgressVisible(headerSettingItem.isProgressVisible());
                        notifyItemChanged(i);
                        notifyItemChanged(i);
                    }
                    return;
                }
            }
        }
    }

    /**
     * Adds a {@link GeneralItemBase} element to be displayed.
     * @param settingsItemBase the item that will be added and displayed.
     */
    public void addItem(GeneralItemBase settingsItemBase) {
        if (!items.contains(settingsItemBase)) {
            items.add(settingsItemBase);
            notifyItemInserted(items.size() - 1);
        }
    }

    /**
     * Clear the list of items from the given index  to the end of list.
     * @param index the index from where should start deleting the items.
     */
    public void clearFromIndex(int index) {
        if (index <= items.size()) {
            int numberOfItemsRemoved = items.size() - index;
            items = items.subList(0, index);
            notifyItemRangeRemoved(index, numberOfItemsRemoved);
        }
    }

    /**
     * Binds view of type {@code ItemViewHolder}.
     */
    @SuppressLint("SwitchIntDef")
    private void bindItemViewHolder(ItemViewHolder holder, GeneralItemBase currentItem) {
        resetViewHolderVisibilities(holder);
        resetViewHolderLayout(holder);
        holder.layout.setOnClickListener(currentItem.getClickListener());
        switch (currentItem.getType()) {
            case GeneralItemBase.GENERAL_TYPE_OBD_LEFT_ICON_TITLE:
                bindLeftIconTitle((LeftIconTitleSettingItem) currentItem, holder);
                break;
            case GeneralItemBase.GENERAL_TYPE_OBD_LEFT_ICON_TITLE_SUBTITLE:
                bindLeftIconTitleSubtitle((LeftIconTitleSubtitleItem) currentItem, holder);
                break;
            case GeneralItemBase.GENERAL_TYPE_OBD_BT_DEVICE_ITEM:
                bindBtDeviceItem((IconTitleBtDeviceItem) currentItem, holder);
                holder.layout.setOnClickListener(v -> ((IconTitleBtDeviceItem) currentItem).getOnDeviceClickListener().onDeviceSelected(((IconTitleBtDeviceItem)
                        currentItem).getAddress()));
                break;
            case GeneralItemBase.GENERAL_TYPE_OBD_HEADER:
                bindHeader((HeaderSettingItem) currentItem, holder);
                break;
            default:
                // empty since will not handle any types which are not bt devices
        }
    }

    /**
     * Binds view of type {@code ItemViewHolder}.
     */
    private void bindTaggingViewHolder(TaggingViewHolder holder, RecordingTaggingItem currentItem) {
        holder.subtitle.setText(currentItem.getSubtitle());
        holder.subtitle.setCompoundDrawablesRelativeWithIntrinsicBounds(currentItem.getIconResId(), 0, 0, 0);
        holder.layout.setOnClickListener(currentItem.getClickListener());
    }

    /**
     * Bind the item view holder for {@code IconTitleBtDeviceItem}.
     * @param currentItem the item to be displayed.
     * @param holder the current view holder to be bound
     */
    private void bindBtDeviceItem(IconTitleBtDeviceItem currentItem, ItemViewHolder holder) {
        holder.leftIcon.setVisibility(View.VISIBLE);
        holder.leftIcon.setImageResource(currentItem.getIconResId());
        holder.title.setVisibility(View.VISIBLE);
        holder.title.setText(currentItem.getName());
        if (currentItem.isItemDividerVisible()) {
            holder.divider.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Bind the header view holder for {@code HeaderSettingItem}.
     * @param currentItem the item to be displayed.
     * @param holder the current view holder to be bound.
     */
    private void bindHeader(HeaderSettingItem currentItem, ItemViewHolder holder) {
        ViewGroup.LayoutParams params = holder.layout.getLayoutParams();
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        holder.layout.setLayoutParams(params);
        holder.title.setVisibility(View.VISIBLE);
        holder.title.setText(currentItem.getTitleResId());
        holder.title.setTextAppearance(holder.layout.getContext(), R.style.obd_settings_header);
        holder.progressBar.setVisibility(currentItem.isProgressVisible() ? View.VISIBLE : View.GONE);
    }

    /**
     * Bind the view holder for {@code LeftIconTitleSettingItem} item type.
     * @param leftIconTitleSettingItem the {@code LeftIconTitleSettingItem}.
     * @param holder the current view holder to be bound.
     */
    private void bindLeftIconTitle(LeftIconTitleSettingItem leftIconTitleSettingItem, ItemViewHolder holder) {
        holder.leftIcon.setVisibility(View.VISIBLE);
        holder.leftIcon.setImageResource(leftIconTitleSettingItem.getIconResId());
        holder.title.setVisibility(View.VISIBLE);
        holder.title.setText(leftIconTitleSettingItem.getTitleResId());
    }

    /**
     * Bind the view holder for {@code LeftIconTitleSubtitleItem} item type.
     * @param leftIconTitleSubtitleItem the item to be displayed.
     * @param holder the current view holder to be bound.
     */
    private void bindLeftIconTitleSubtitle(LeftIconTitleSubtitleItem leftIconTitleSubtitleItem, ItemViewHolder holder) {
        holder.leftIcon.setVisibility(View.VISIBLE);
        holder.leftIcon.setImageResource(leftIconTitleSubtitleItem.getIconResId());
        holder.title.setVisibility(View.VISIBLE);
        holder.title.setText(leftIconTitleSubtitleItem.getTitleResId());
        holder.subtitle.setVisibility(View.VISIBLE);
        holder.subtitle.setText(leftIconTitleSubtitleItem.getSubtitleResId());
    }

    /**
     * @param holder the current view holder.
     * Sets the visibilities to the all the view holder view to gone.
     */
    private void resetViewHolderVisibilities(ItemViewHolder holder) {
        holder.title.setVisibility(View.GONE);
        holder.subtitle.setVisibility(View.GONE);
        holder.leftIcon.setVisibility(View.GONE);
        holder.rightIcon.setVisibility(View.GONE);
        holder.divider.setVisibility(View.GONE);
        holder.progressBar.setVisibility(View.GONE);
    }


    /**
     * Sets the default layout for the view holder item.
     * @param holder the current view holder.
     */
    private void resetViewHolderLayout(ItemViewHolder holder) {
        Context context = holder.layout.getContext();
        holder.title.setTextAppearance(context, R.style.obd_faq_text_style);
        ViewGroup.LayoutParams params = holder.layout.getLayoutParams();
        params.height = (int) context.getResources().getDimension(R.dimen.partial_kv_setting_item);
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        holder.layout.setLayoutParams(params);
    }

    /**
     * Default view holder for the recycler view.
     */
    class GeneralSettingsViewHolder extends RecyclerView.ViewHolder {

        /**
         * Default constructor for the current class.
         * @param itemView the view which will be passed.
         */
        public GeneralSettingsViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    /**
     * Item View holder class in order to hold all references to the view's elements.
     */
    class ItemViewHolder extends GeneralSettingsViewHolder {

        /**
         * {@code ImageView} representing the right icon.
         */
        private ImageView rightIcon;

        /**
         * {@code ImageView} representing the left icon.
         */
        private ImageView leftIcon;

        /**
         * {@code TextView} representing the title.
         */
        private TextView title;

        /**
         * {@code TextView} representing the subtitle.
         */
        private TextView subtitle;

        /**
         * {@code View} representing the parent layout for all the views.
         */
        private View layout;

        /**
         * {@code View} representing a divider decorator between the items.
         */
        private View divider;

        /**
         * {@code ProgressBar} representing the progress for the current header items.
         */
        private ProgressBar progressBar;

        /**
         * Default constructor for the current class.
         * @param itemView the item view to be set for the current holder.
         */
        ItemViewHolder(View itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.text_view_kv_setting_item_title);
            subtitle = itemView.findViewById(R.id.text_view_kv_setting_item_subtitle);
            leftIcon = itemView.findViewById(R.id.image_view_kv_setting_item_left_icon);
            rightIcon = itemView.findViewById(R.id.image_view_kv_setting_item_right_icon);
            layout = itemView.findViewById(R.id.layout_partial_kv_setting_item);
            divider = itemView.findViewById(R.id.view_kv_setting_item_divider);
            progressBar = itemView.findViewById(R.id.progress_bar_kv_setting_item_loader);
        }
    }

    /**
     * Tagging view holder class which will hold all references related to the view's elements.
     */
    class TaggingViewHolder extends GeneralSettingsViewHolder {

        /**
         * {@code TextView} representing the subtitle.
         */
        private TextView subtitle;

        /**
         * {@code View} representing the parent layout for all the views.
         */
        private View layout;

        /**
         * Default constructor for the current class.
         * @param itemView the view which will be passed.
         */
        public TaggingViewHolder(@NonNull View itemView) {
            super(itemView);
            subtitle = itemView.findViewById(R.id.text_tagging_subitem_subtitle);
            layout = itemView.findViewById(R.id.card_view_partial_tagging_subitem_parent);
        }
    }
}
