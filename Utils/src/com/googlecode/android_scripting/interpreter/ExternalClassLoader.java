package com.googlecode.android_scripting.interpreter;

import dalvik.system.DexClassLoader;

import java.util.Collection;
import java.util.Iterator;

public class ExternalClassLoader {

  public Object load(Collection<String> dexPaths, Collection<String> nativePaths, String className)
      throws Exception {
    String dexOutputDir = "/sdcard/dexoutput";
    String joinedDexPaths = join(dexPaths, ":");
    String joinedNativeLibPaths = nativePaths != null ? join(nativePaths, ":") : null;
    DexClassLoader loader =
        new DexClassLoader(joinedDexPaths, dexOutputDir, joinedNativeLibPaths, this.getClass()
            .getClassLoader());
    Class<?> classToLoad = Class.forName(className, true, loader);
    return classToLoad.newInstance();
  }

    private static String join(Collection<String> collection, String delimiter) {
        StringBuffer buffer = new StringBuffer();
        Iterator<String> iter = collection.iterator();
        while (iter.hasNext()) {
            buffer.append(iter.next());
            if (iter.hasNext()) {
                buffer.append(delimiter);
            }
        }
        return buffer.toString();
    }
}