package org.dtvkit.inputsource;

import android.view.Surface;

public class Platform {
   private native boolean setNativeSurface(Surface surface);
   private native void unsetNativeSurface();

   public Boolean setSurface(Surface surface) {
      unsetNativeSurface();
      if (surface != null) {
         setNativeSurface(surface);
      }
      return true;
   }

   static {
      System.loadLibrary("platform");
   }
}
