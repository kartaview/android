package com.telenav.osv.common.ui.divider;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Concrete divider implementation which will not put on the last position a divider for the item.
 */
public class RightDividerItemDecoration extends RecyclerView.ItemDecoration {

    /**
     * Reference to the divider drawable which will be applied by the current item decoration.
     */
    private Drawable dividerDrawable;

    /**
     * Default constructor for the current class.
     */
    public RightDividerItemDecoration(Drawable dividerDrawable) {
        this.dividerDrawable = dividerDrawable;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);

        int position = parent.getChildAdapterPosition(view);
        RecyclerView.Adapter adapter = parent.getAdapter();
        if (adapter == null) {
            return;
        }
        // hide the divider for the last child
        if (position == adapter.getItemCount() - 1) {
            outRect.setEmpty();
        } else {
            super.getItemOffsets(outRect, view, parent, state);
        }
    }
}
