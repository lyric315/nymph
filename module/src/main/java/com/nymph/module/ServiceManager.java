package com.nymph.module;

import android.app.Application;
import android.content.res.AssetManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;

/**
 *
 */
public final class ServiceManager {
    private static final String TAG = "ServiceManager";

    private static volatile Application sApplication;

    private static final HashMap<Class<?>, Object> sServiceApiImplMap = new HashMap<>();

    private static volatile HashMap<String, String> sInterfaceFactoryMap;

    private ServiceManager() {}


    public static void setsApplication(Application sApplication) {
        ServiceManager.sApplication = sApplication;
    }

    public static <T> T getService(Class<T> clz) {
        if (clz == null) {
            return null;
        }

        if (!clz.isInterface()) {
            return null;
        }

        synchronized (ServiceManager.class) {
            Object obj = sServiceApiImplMap.get(clz);
            if (obj != null) {
                //noinspection unchecked
                return (T) obj;
            }

            IServiceFactory<?> iServiceFactory = createFactory(clz.getName());
            if (iServiceFactory == null) {
                Log.e(TAG, "IServiceFactory null for " + clz.getName());
                return null;
            }

            obj = iServiceFactory.newService(sApplication);
            if (obj == null) {
                Log.e(TAG, "new ServiceFactory null for " + clz.getName() + " by " + iServiceFactory);
                return null;
            }

            if (!clz.isInstance(obj)) {
                Log.e(TAG, "service instance is not right for " + clz.getName());
                return null;
            }

            sServiceApiImplMap.put(clz, obj);
            return (T) obj;
        }
    }

    public static boolean registerService(Class<?> api, String implClassName) {
        if (api == null || TextUtils.isEmpty(implClassName)) {
            return false;
        }

        synchronized (ServiceManager.class) {
            if (getService(api) != null) {
                return false;
            }

            try {
                Class<?> clz = Class.forName(implClassName);
                Constructor<?> constructor = clz.getDeclaredConstructor((Class<?>[]) null);
                constructor.setAccessible(true);

                Object impl = constructor.newInstance((Object[]) null);
                if (!api.isInstance(impl)) {
                    return false;
                }

                sServiceApiImplMap.put(api, impl);
                return true;
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        return false;
    }

    public static boolean registerService(Class<?> api, Object impl) {
        if (api == null || impl == null) {
            return false;
        }

        if (!api.isInstance(impl)) {
            return false;
        }

        synchronized (ServiceManager.class) {
            Object service = getService(api);
            if (service != null && impl != service) {
                return false;
            }

            sServiceApiImplMap.put(api, impl);
        }
        return true;
    }

    public static void unregisterService(Class<?> api) {
        if (api == null) {
            return;
        }

        synchronized (ServiceManager.class) {
            sServiceApiImplMap.remove(api);
        }
    }

    private static IServiceFactory<?> createFactory(String name) {
        if (name == null) {
            return null;
        }

        String factoryName = null;
        // try load from apt
        try {
            Class<?> clz = Class.forName(name + "_sf_");
            Field f = clz.getDeclaredField("SERVICE_FACTORY_CLASS_NAME");
            f.setAccessible(true);

            factoryName = (String) f.get(null);
        } catch (Throwable t) {
            Log.w(TAG, Log.getStackTraceString(t));
        }

        if (sInterfaceFactoryMap == null) {
            sInterfaceFactoryMap = new HashMap<>();
        }

        // try load from assets config file
        if (factoryName == null) {
            synchronized (ServiceManager.class) {
                BufferedReader br = null;
                try {
                    AssetManager am = sApplication.getAssets();
                    br = new BufferedReader(new InputStreamReader(am.open("service_map_config.txt"), "UTF-8"));
                    String line;
                    while ((line = br.readLine()) != null) {
                        line = line.trim();
                        if (TextUtils.isEmpty(line) || line.startsWith("#")) {
                            continue;
                        }

                        String[] commonents = line.split("->");
                        if (commonents == null || commonents.length != 2) {
                            continue;
                        }

                        String iService = commonents[0].trim();
                        String fService = commonents[1].trim();

                        if (!TextUtils.isEmpty(iService) && !TextUtils.isEmpty(fService)) {
                            sInterfaceFactoryMap.put(iService, fService);
                        }
                    }
                } catch (Throwable t) {
                    if (br != null) {
                        try {
                            br.close();
                        } catch (Throwable ignore) {
                        }
                    }
                }
            }

            factoryName = sInterfaceFactoryMap.get(name);
        }

        if (factoryName != null) {
            try {
                Class<?> clazz = Class.forName(factoryName);
                Object obj = clazz.newInstance();
                return (IServiceFactory<?>) obj;
            } catch (Throwable t) {
                Log.w(TAG, Log.getStackTraceString(t));
            }
        }

        return null;
    }
}
