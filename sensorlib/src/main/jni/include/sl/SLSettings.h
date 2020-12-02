//
//  SLSettings.h
//  SensorLib
//
//  Created by Andrei Strugaru on 11/21/14.
//  Copyright (c) 2014 Telenav EU. All rights reserved.
//

#ifndef SensorLib_SLSettingsSingleton
#define SensorLib_SLSettingsSingleton

#include "SensorLibExports.h"
#include "Singleton.h"
#include "Strategos.h"
#include <opencv2/imgproc.hpp>
#include "settings_defines.h"

namespace SL
{

class SLSettings
{
  public: // Attributes
    SLSettings(enSettingsType setting_type);

    bool are_settings_valid() const;
    bool are_recording_settings_valid() const;
    void set_detection_filter(const enDetectionFilterType filter_type, const bool filter_value);
    bool get_detection_filter(const enDetectionFilterType filter_type) const;
    const std::vector<enSignType>& get_types_for_sign(const enSignType sign_type) const;
    bool is_current_color_space(const enColorSpace color_space) const;

    void set_storage_path(const std::string& storage_path);
    const std::string& get_storage_path() const;

    void set_settings_type(enSettingsType settings_type);
    enSettingsType get_settings_type() const;

    void set_save_roi_for_detection(const bool save_roi_for_detections);
    bool get_save_roi_for_detection() const;

    ThresholdValues get_threshold_values_for_color(const enDetectionFilterType eFilterType);
    cv::ColorConversionCodes get_color_space_converion_4_input() const;
    void update_to_color_space(enColorSpace eColorSpace);

    FrameCallbackType frame_callback_;
    SignDetectedCallbackType sign_detected_callback_;

    // outside the lib stuff
    size_t frame_count_4_callback_;
    int max_frame_difference_for_tracking_;
    bool perform_color_space_transformation_;

    bool double_thresholding_enabled_;
    bool live_stream_;
    static bool display_binary_contour_frame_;
    static bool display_all_shapes_;
    Strategos strategos_;

  private:
    SLSettings();
    void read_speed_limits_types();
    void read_speed_limits_US_types();
    void read_regulatory_directions_types();
    void read_canada_regulatory_directions_types();
    void read_turn_restrictions_types();
    void read_threshold_values();
    void read_detection_filters();

    ThresholdValuesMap threshold_values_map_;
    FilterEnabledMap detection_filters_enabled_;
    SignTypesMap sign_types_map_;
    enColorSpace current_color_space_;
    enSettingsType settings_type_;
    std::string storage_path_;
    bool save_roi_for_detections_;
};

inline bool SLSettings::are_settings_valid() const { return false; }

inline bool SLSettings::is_current_color_space(const enColorSpace color_space) const
{
    if (color_space == current_color_space_)
    {
        return true;
    }
    if (color_space & current_color_space_)
    {
        return true;
    }

    return false;
}

inline enSettingsType SLSettings::get_settings_type() const { return settings_type_; }

inline void SLSettings::set_settings_type(enSettingsType settings_type) { settings_type_ = settings_type; }

inline void SLSettings::set_storage_path(const std::string& storage_path) { storage_path_ = storage_path; }

inline const std::string& SLSettings::get_storage_path() const { return storage_path_; }

inline void SLSettings::set_save_roi_for_detection(const bool save_roi_for_detections)
{
    save_roi_for_detections_ = save_roi_for_detections;
}

inline bool SLSettings::get_save_roi_for_detection() const { return save_roi_for_detections_; }
}

#endif /* defined(SensorLib_SLSettingsSingleton) */
