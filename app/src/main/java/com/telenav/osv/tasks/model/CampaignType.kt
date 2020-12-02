package com.telenav.osv.tasks.model

/**
 * Identifier for the campaign type used in the Jarvis API.
 */
enum class CampaignType(val value: Int) {

    /**
     * Identifier for the vendor type. Correlates with the back-end value.
     */
    CAMPAIGN_TYPE_VENDOR(1),

    /**
     * Identifier for the crowd sourced type. Correlates with the back-end value.
     */
    CAMPAIGN_TYPE_CROWD_SOURCED(2)
}