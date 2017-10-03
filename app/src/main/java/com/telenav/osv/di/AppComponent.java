package com.telenav.osv.di;

import android.app.Application;
import android.content.Context;
import com.telenav.osv.application.OSVApplication;
import dagger.BindsInstance;
import dagger.Component;
import dagger.android.AndroidInjectionModule;
import javax.inject.Singleton;

/**
 * Created by kalmanb on 9/21/17.
 */
@Singleton
@Component(modules = {
    AndroidInjectionModule.class,
    AppModule.class,
    ActivityBindingModule.class,
    ServiceBindingModule.class,
    LocationModule.class,
    NetworkModule.class,
    RecordModule.class,
    PlaybackModule.class,
    ProfileDataModule.class,
})

public interface AppComponent {

  void inject(OSVApplication app);

  @Component.Builder
  interface Builder {

    @BindsInstance
    Builder application(Application application);

    @BindsInstance
    Builder context(Context application);

    AppComponent build();
  }
}
