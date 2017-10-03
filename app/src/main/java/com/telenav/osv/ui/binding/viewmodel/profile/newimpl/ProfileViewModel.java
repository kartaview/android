package com.telenav.osv.ui.binding.viewmodel.profile.newimpl;

import android.app.Application;
import android.databinding.ObservableBoolean;
import com.telenav.osv.BR;
import com.telenav.osv.R;
import com.telenav.osv.application.ValueFormatter;
import com.telenav.osv.data.AccountPreferences;
import com.telenav.osv.ui.binding.viewmodel.AbstractViewModel;
import com.telenav.osv.ui.custom.ViewHolder;
import com.telenav.osv.ui.list.BindingRecyclerAdapter;
import com.telenav.osv.ui.list.ObservableMergeList;
import com.telenav.osv.ui.list.ObservableOrderedSet;
import me.tatarka.bindingcollectionadapter2.BindingRecyclerViewAdapter;
import me.tatarka.bindingcollectionadapter2.itembindings.OnItemBindClass;

/**
 * todo only used in new profile fragment impl.
 * Created by kalmanb on 9/21/17.
 */
public abstract class ProfileViewModel extends AbstractViewModel {

    /**
     * Custom adapter that logs calls.
     */
    public final BindingRecyclerAdapter<ListItemViewModel> adapter = new BindingRecyclerAdapter<>();

    public final OnItemBindClass<ListItemViewModel> multipleItems = new OnItemBindClass<ListItemViewModel>()
            .map(ByodTrackListHeaderViewModel.class, BR.item, R.layout.partial_track_list_header_byod)
            .map(TrackListItemViewModel.class, BR.item, R.layout.item_sequence_card);

    /**
     * Custom view holders for RecyclerView
     */
    public final BindingRecyclerViewAdapter.ViewHolderFactory viewHolder = binding -> new ViewHolder(binding.getRoot());

    protected final ValueFormatter formatter;

    final ObservableOrderedSet<ByodTrackListHeaderViewModel> headerList = new ObservableOrderedSet<>();

    final ObservableOrderedSet<TrackListItemViewModel> trackList = new ObservableOrderedSet<>();

    /**
     * Items merged with a header on top and footer on bottom.
     */
    public final ObservableMergeList<ListItemViewModel> headerFooterItems = new ObservableMergeList<ListItemViewModel>()
            .insertList(headerList)
            .insertList(trackList);

    private final AccountPreferences prefs;

    public ObservableBoolean refreshing = new ObservableBoolean();

    public ProfileViewModel(Application application, AccountPreferences prefs, ValueFormatter formatter) {
        super(application);
        this.formatter = formatter;
        this.prefs = prefs;
    }
}
