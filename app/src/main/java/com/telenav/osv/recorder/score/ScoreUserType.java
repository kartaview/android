package com.telenav.osv.recorder.score;

import androidx.annotation.IntDef;

/**
 * Interface that holds all the types of the users which are used in {@code Score} component.
 * Created by cameliao on 2/7/18.
 */

@IntDef({ScoreUserType.USER_BYOD_DRIVER, ScoreUserType.USER_NORMAL})
public @interface ScoreUserType {
    int USER_BYOD_DRIVER = 0;

    int USER_NORMAL = 1;
}