//
//  settings_factory_singleton.h
//  orbb
//
//  Created by Andrei Strugaru on 3/16/16.
//
//

#ifndef settings_factory_singleton_h
#define settings_factory_singleton_h

#include "SLSettings.h"

#include <map>

namespace SL
{

class SettingsFactorySingleton
{
  public:
    static SettingsFactorySingleton& get_instance();

    SLSettings& get_settings(enSettingsType new_settings = eLastDefault);

    enSettingsType get_default_settings_type() const { return current_setting_; }

    void set_default_settings_type(const enSettingsType new_setting) { current_setting_ = new_setting; }

  protected:
    // Make it noncopyable explicitly, in place, instead of inheriting
    // from a standard noncopyable object, to reduce header dependencies.
    SettingsFactorySingleton();
    ~SettingsFactorySingleton();

  private:
    // Noncopyable stuff.
    SettingsFactorySingleton(const SettingsFactorySingleton&);
    SettingsFactorySingleton& operator=(const SettingsFactorySingleton&);

    void init_all_settings();

    std::map<enSettingsType, SLSettings> settings_map_;
    enSettingsType current_setting_;
};

inline SLSettings& GlobalSettings(enSettingsType new_settings = eLastDefault)
{
    return SettingsFactorySingleton::get_instance().get_settings(new_settings);
}
}

#endif /* settings_factory_singleton_h */
