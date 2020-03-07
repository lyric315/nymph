package com.nymph.module;

import android.app.Application;

public interface IServiceFactory<T> {
    T newService(Application application);
}
