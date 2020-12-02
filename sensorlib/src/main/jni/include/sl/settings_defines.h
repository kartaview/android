//
//  settings_defines.h
//  orbb
//
//  Created by Andrei Strugaru on 5/10/16.
//
//

#ifndef settings_defines_h
#define settings_defines_h

#include <map>
#include <opencv2/core/types.hpp>

namespace SL
{
//! Pair of threshold values <min, max>.
typedef std::pair<cv::Scalar, cv::Scalar> ThresholdValues;

typedef std::map<int, ThresholdValues> ThresholdValuesMap;

typedef std::map<enDetectionFilterType, bool> FilterEnabledMap;

typedef std::map<enSignType, std::vector<enSignType> > SignTypesMap;

//! struct for holding ThresholdIndex constructed from the colorspace and filter type enum's
typedef struct ThresholdIndex
{

    ThresholdIndex(int e1, int e2) { index_ = e1 | e2; }

    int get_index() const { return index_; }

  private:
    ThresholdIndex();
    int index_;

} ThresholdIndex;
}

#endif /* settings_defines_h */
