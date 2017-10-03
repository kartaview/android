package com.telenav.osv.ui.list;

import android.support.annotation.IntDef;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A collection of factories to create RecyclerView LayoutManagers so that you can easily set them
 * in your layout.
 */
public class LayoutManagers {

  protected LayoutManagers() {
  }

  /**
   * A {@link LinearLayoutManager}.
   */
  public static me.tatarka.bindingcollectionadapter2.LayoutManagers.LayoutManagerFactory linear() {
    return new me.tatarka.bindingcollectionadapter2.LayoutManagers.LayoutManagerFactory() {

      @Override
      public RecyclerView.LayoutManager create(RecyclerView recyclerView) {
        return new LinearLayoutManager(recyclerView.getContext());
      }
    };
  }

  /**
   * A {@link LinearLayoutManager} with the given orientation and reverseLayout.
   */
  public static me.tatarka.bindingcollectionadapter2.LayoutManagers.LayoutManagerFactory linear(
      @me.tatarka.bindingcollectionadapter2.LayoutManagers.Orientation final int orientation, final boolean reverseLayout) {
    return new me.tatarka.bindingcollectionadapter2.LayoutManagers.LayoutManagerFactory() {

      @Override
      public RecyclerView.LayoutManager create(RecyclerView recyclerView) {
        return new LinearLayoutManager(recyclerView.getContext(), orientation, reverseLayout);
      }
    };
  }

  /**
   * A {@link GridLayoutManager} with the given spanCount.
   */
  public static me.tatarka.bindingcollectionadapter2.LayoutManagers.LayoutManagerFactory grid(final int spanCount) {
    return new me.tatarka.bindingcollectionadapter2.LayoutManagers.LayoutManagerFactory() {

      @Override
      public RecyclerView.LayoutManager create(RecyclerView recyclerView) {
        return new GridLayoutManager(recyclerView.getContext(), spanCount);
      }
    };
  }

  /**
   * A {@link GridLayoutManager} with the given spanCount, orientation and reverseLayout.
   **/
  public static me.tatarka.bindingcollectionadapter2.LayoutManagers.LayoutManagerFactory grid(final int spanCount,
                                                                                              @me.tatarka
                                                                                                  .bindingcollectionadapter2.LayoutManagers.Orientation final int orientation,
                                                                                              final boolean reverseLayout) {
    return new me.tatarka.bindingcollectionadapter2.LayoutManagers.LayoutManagerFactory() {

      @Override
      public RecyclerView.LayoutManager create(RecyclerView recyclerView) {
        return new GridLayoutManager(recyclerView.getContext(), spanCount, orientation, reverseLayout);
      }
    };
  }

  /**
   * A {@link StaggeredGridLayoutManager} with the given spanCount and orientation.
   */
  public static me.tatarka.bindingcollectionadapter2.LayoutManagers.LayoutManagerFactory staggeredGrid(final int spanCount,
                                                                                                       @me.tatarka
                                                                                                           .bindingcollectionadapter2.LayoutManagers.Orientation final int orientation) {
    return new me.tatarka.bindingcollectionadapter2.LayoutManagers.LayoutManagerFactory() {

      @Override
      public RecyclerView.LayoutManager create(RecyclerView recyclerView) {
        return new StaggeredGridLayoutManager(spanCount, orientation);
      }
    };
  }

  public interface LayoutManagerFactory {

    RecyclerView.LayoutManager create(RecyclerView recyclerView);
  }

  @IntDef({LinearLayoutManager.HORIZONTAL, LinearLayoutManager.VERTICAL})
  @Retention(RetentionPolicy.SOURCE)
  public @interface Orientation {

  }
}