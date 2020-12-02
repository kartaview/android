//
//  Singleton.h
//  SensorLib
//
//  Created by Andrei Strugaru on 11/21/14.
//  Copyright (c) 2014 Telenav EU. All rights reserved.
//

#ifndef SensorLib_Singleton
#define SensorLib_Singleton

namespace SL
{
template <class T> class Singleton
{
  public:
    static T& getInstance();

  private:
    static T& instanceWrapper();
};

template <class T> T& Singleton<T>::getInstance() { return instanceWrapper(); }

template <class T> T& Singleton<T>::instanceWrapper()
{
    static T theInstance;
    return theInstance;
}
}

#endif /* defined(SensorLib_Singleton) */
