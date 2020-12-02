package com.telenav.osv.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

/**
 * Represents an utility class for fragments transactions such as:
 * <ul>
 * <li>addChild - {@link #addFragmentToContainer(FragmentManager, Fragment, int, boolean, String)}</li>
 * <li>remove - {@link #removeFragmentFromContainer(FragmentManager, Fragment, boolean, String)}</li>
 * <li>addRemove - {@link #addRemoveFragmentFromContainer(FragmentManager, Fragment, Fragment, int, boolean, String)}</li>
 * <li>replace - {@link #replaceFragment(FragmentManager, Fragment, int, boolean, String)}</li>
 * </ul>
 */
public class ActivityUtils {

    /**
     * The {@code fragment} is added to the container view with id {@code frameId}. The operation is performed by the {@code fragmentManager}.
     * @param fragmentManager the {@link FragmentManager} which will be used for the {@code fragmentTransaction}.
     * @param fragment the {@link Fragment} to be added to the {@code container}.
     * @param frameId the id for the container.
     * @param saveToBackStack if {@code true} the transaction will be added to the backstack.
     * @param tag the {@code String} identifier for the fragment. This is also used as identifier for the transaction backstack save if {@code saveToBackStack} is {@code true}.
     */
    public static void addFragmentToContainer(@NonNull FragmentManager fragmentManager,
                                              @NonNull Fragment fragment,
                                              int frameId,
                                              boolean saveToBackStack,
                                              @Nullable String tag) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(frameId, fragment, tag);
        if (saveToBackStack) {
            transaction.addToBackStack(tag);
        }
        transaction.commit();
    }

    /**
     * Remove an existing fragment.  If it was added to a container, its view is also removed from that container. The operation is performed by the {@code fragmentManager}.
     * @param fragmentManager the {@link FragmentManager} which will be used for the {@code fragmentTransaction}.
     * @param fragment the {@link Fragment} to be removed.
     * @param saveToBackStack if {@code true} the transaction will be added to the backstack.
     * @param tag the {@code String} identifier for the fragment. This is also used as identifier for the transaction backstack save if {@code saveToBackStack} is {@code true}.
     */
    public static void removeFragmentFromContainer(@NonNull FragmentManager fragmentManager,
                                                   @NonNull Fragment fragment,
                                                   boolean saveToBackStack,
                                                   @Nullable String tag) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.remove(fragment);
        if (saveToBackStack) {
            transaction.addToBackStack(tag);
        }
        transaction.commit();
    }

    /**
     * The {@code fragment} is replacing the current fragment from the container view with id {@code frameId}. The operation is
     * performed by the {@code fragmentManager}.
     * <p>The transaction does not use {@link FragmentTransaction#replace(int, Fragment)} due to issues known in special case with it, instead it simulates the replace
     * operations by removing the {@code oldFragment} and adding the {@code newFragment} in the same transaction. This preserves the backstack if the transaction is being added
     * and it safely removes the current fragment while adding the old one at {@link FragmentManager#popBackStack()}.</p>
     * @param fragmentManager the {@link FragmentManager} which will be used for the {@code fragmentTransaction}.
     * @param oldFragment the {@link Fragment} to be removed from the {@code container}.
     * @param newFragment the {@link Fragment} to be added to the {@code container}.
     * @param frameId the id for the container.
     * @param saveToBackStack if {@code true} the transaction will be added to the backstack.
     * @param tag the {@code String} identifier for the fragment. This is also used as identifier for the transaction backstack save if {@code saveToBackStack} is {@code true}.
     */
    public static void addRemoveFragmentFromContainer(@NonNull FragmentManager fragmentManager,
                                                      @NonNull Fragment oldFragment,
                                                      @NonNull Fragment newFragment,
                                                      int frameId,
                                                      boolean saveToBackStack,
                                                      @Nullable String tag) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.remove(oldFragment);
        transaction.add(frameId, newFragment, tag);
        if (saveToBackStack) {
            transaction.addToBackStack(tag);
        }
        transaction.commit();
    }

    /**
     * The {@code fragment} is removed from the container view with id {@code frameId}. The operation is performed by the {@code fragmentManager}.
     * @param fragmentManager the {@link FragmentManager} which will be used for the {@code fragmentTransaction}.
     * @param fragment the {@link Fragment} to be replaced to the {@code container}.
     * @param frameId the id for the container.
     * @param saveToBackStack if {@code true} the transaction will be added to the backstack.
     * @param tag the {@code String} identifier for the fragment. This is also used as identifier for the transaction backstack save if {@code saveToBackStack} is {@code true}.
     */
    public static void replaceFragment(@NonNull FragmentManager fragmentManager,
                                       @NonNull Fragment fragment,
                                       int frameId,
                                       boolean saveToBackStack,
                                       @Nullable String tag) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(frameId, fragment, tag);
        if (saveToBackStack) {
            transaction.addToBackStack(tag);
        }
        transaction.commit();
    }

    /**
     * Clear the back stack from the given fragment manager.
     * @param fragmentManager the manager that holds a reference to each back stack entry.
     */
    public static void clearBackStack(@NonNull FragmentManager fragmentManager) {
        for (int i = 0; i < fragmentManager.getBackStackEntryCount(); ++i) {
            fragmentManager.popBackStack();
        }
    }
}
