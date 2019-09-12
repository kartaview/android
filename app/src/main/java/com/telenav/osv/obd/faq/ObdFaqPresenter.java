package com.telenav.osv.obd.faq;

import java.util.ArrayList;
import java.util.List;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import com.telenav.osv.R;
import com.telenav.osv.common.adapter.model.GeneralItemBase;
import com.telenav.osv.obd.faq.details.adapters.ObdDetailsAdapterFragment;
import com.telenav.osv.obd.faq.details.reason.ObdDetailsReasonsFragment;
import com.telenav.osv.obd.faq.details.what.ObdDetailsWhatFragment;
import com.telenav.osv.obd.faq.details.why.ObdDetailsWhyFragment;
import com.telenav.osv.obd.model.LeftIconTitleSettingItem;
import com.telenav.osv.utils.ActivityUtils;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

/**
 * Concrete implementation for {@code ObdFaqPresenter}.
 * @author horatiuf
 * @see ObdFaqPresenter
 */

public class ObdFaqPresenter implements ObdFaqContract.ObdFaqPresenter {

    /**
     * Email address for sending an email to OSC support team.
     */
    private static final String OSC_SUPPORT_EMAIL = "osc-support@telenav.com";

    /**
     * The title for the email chooser screen.
     */
    private static final String OSC_SUPPORT_CHOOSER_TITLE = "Contact OSC support";

    /**
     * The uri used to pass the email to send mail intent.
     */
    private static final String OSC_SUPPORT_EMAIL_URI = "mailto: " + OSC_SUPPORT_EMAIL;

    /**
     * Collection of the settings items.
     */
    List<GeneralItemBase> obdFaqSettings;

    /**
     * Instance for the obd faq view.
     */
    ObdFaqContract.ObdFaqView obdFaqView;

    /**
     * Instance to the fragment manager for fragment initialisation.
     */
    FragmentManager fragmentManager;

    /**
     * Default constructor for the current class.
     * @param obdFaqView the view to adhere to the contract.
     */
    ObdFaqPresenter(@NonNull ObdFaqContract.ObdFaqView obdFaqView, @NonNull FragmentManager fragmentManager) {
        this.obdFaqView = obdFaqView;
        this.fragmentManager = fragmentManager;
    }

    @Override
    public void start() {

    }

    @Override
    public List<GeneralItemBase> getObdFaqSettings() {
        if (obdFaqSettings == null) {
            obdFaqSettings = new ArrayList<>();
            obdFaqSettings.add(new LeftIconTitleSettingItem(R.string.what_is_obd,
                    R.drawable.vector_faq,
                    clickConsumer -> ActivityUtils.replaceFragment(fragmentManager,
                            ObdDetailsWhatFragment.newInstance(),
                            R.id.layout_activity_obd_fragment_container,
                            true,
                            ObdDetailsWhatFragment.TAG)));
            obdFaqSettings.add(new LeftIconTitleSettingItem(R.string.why_obd_matter,
                    R.drawable.vector_faq,
                    clickConsumer -> ActivityUtils.replaceFragment(fragmentManager,
                            ObdDetailsWhyFragment.newInstance(),
                            R.id.layout_activity_obd_fragment_container,
                            true,
                            ObdDetailsWhyFragment.TAG)));
            obdFaqSettings.add(new LeftIconTitleSettingItem(R.string.obd_adapters,
                    R.drawable.vector_faq,
                    clickConsumer -> ActivityUtils.replaceFragment(fragmentManager,
                            ObdDetailsAdapterFragment.newInstance(),
                            R.id.layout_activity_obd_fragment_container,
                            true,
                            ObdDetailsAdapterFragment.TAG)));
            obdFaqSettings.add(new LeftIconTitleSettingItem(R.string.obd_reasons,
                    R.drawable.vector_faq,
                    clickConsumer -> ActivityUtils.replaceFragment(fragmentManager,
                            ObdDetailsReasonsFragment.newInstance(),
                            R.id.layout_activity_obd_fragment_container,
                            true,
                            ObdDetailsReasonsFragment.TAG)));
        }
        return obdFaqSettings;
    }

    @Override
    public void contactOscSupport(Context context) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse(OSC_SUPPORT_EMAIL_URI));
        context.startActivity(Intent.createChooser(intent, OSC_SUPPORT_CHOOSER_TITLE));
    }
}
