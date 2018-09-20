package com.flutter.example.locationbackgroundplugin;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry.PluginRegistrantCallback;
import io.flutter.view.FlutterCallbackInformation;
import io.flutter.view.FlutterMain;
import io.flutter.view.FlutterNativeView;
import io.flutter.view.FlutterRunArguments;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class LocationBackgroundService extends Service {
  public static final String TAG = "LocationBackground";
  private static AtomicBoolean sStarted = new AtomicBoolean(false);
  private static FlutterNativeView sBackgroundFlutterView;
  private static MethodChannel sBackgroundChannel;
  private static PluginRegistrantCallback sPluginRegistrantCallback;
  private static long sLocationCallbackHandle = -1;

  private static LocationListener sLocationListener = new LocationListener() {
    public void onLocationChanged(Location location) {
      // Called when a new location is found by the network location provider.
      Log.i(TAG, String.format("%f, %f", location.getLatitude(), location.getLongitude()));
      if(sBackgroundChannel != null && sLocationCallbackHandle != -1) {
        ArrayList arguments = new ArrayList();
        arguments.add(sLocationCallbackHandle);
        arguments.add(location.getTime()/1000.0); // iOS returns seconds since epoch so that's what we'll do
        arguments.add(location.getLatitude());
        arguments.add(location.getLongitude());
        arguments.add(location.getAltitude());
        arguments.add(location.getSpeed());
        arguments.add(location.getAccuracy());

        sBackgroundChannel.invokeMethod("onLocationEvent", arguments);
      }
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {}

    public void onProviderEnabled(String provider) {}

    public void onProviderDisabled(String provider) {}
  };

  private String mAppBundlePath;

  public static void onInitialized() {
    sStarted.set(true);
  }

  public static void startHeadlessService(Context context, long callbackHandle) {
    FlutterMain.ensureInitializationComplete(context, null);
    String mAppBundlePath = FlutterMain.findAppBundlePath(context);
    FlutterCallbackInformation cb = FlutterCallbackInformation.lookupCallbackInformation(callbackHandle);
    if (cb == null) {
      Log.e(TAG, "Fatal: failed to find callback");
      return;
    }

    // Note that we're passing `true` as the second argument to our
    // FlutterNativeView constructor. This specifies the FlutterNativeView
    // as a background view and does not create a drawing surface.
    sBackgroundFlutterView = new FlutterNativeView(context, true);
    if (mAppBundlePath != null && !sStarted.get()) {
      Log.i(TAG, "Starting LocationBackgroundService...");
      FlutterRunArguments args = new FlutterRunArguments();
      args.bundlePath = mAppBundlePath;
      args.entrypoint = cb.callbackName;
      args.libraryPath = cb.callbackLibraryPath;
      sBackgroundFlutterView.runFromBundle(args);
      sPluginRegistrantCallback.registerWith(sBackgroundFlutterView.getPluginRegistry());

    }
  }

  public static void monitorLocationChanges(Context context, long callbackHandle) {
    LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

    Log.i(TAG, "Starting location updates");
    // These arguments match the iOS settings
    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, sLocationListener);
    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 10, sLocationListener);
    locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 5000, 10, sLocationListener);
    sLocationCallbackHandle = callbackHandle;
  }

  public static void cancelLocationUpdates(Context context) {
    Log.i(TAG, "Stopping location updates");
    LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    locationManager.removeUpdates(sLocationListener);
  }

  public static void setBackgroundChannel(MethodChannel channel) {
    sBackgroundChannel = channel;
  }

  public static boolean setBackgroundFlutterView(FlutterNativeView view) {
    if (sBackgroundFlutterView != null && sBackgroundFlutterView != view) {
      Log.i(TAG, "setBackgroundFlutterView tried to overwrite an existing FlutterNativeView");
      return false;
    }
    sBackgroundFlutterView = view;
    return true;
  }

  public static void setPluginRegistrant(PluginRegistrantCallback callback) {
    sPluginRegistrantCallback = callback;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    Context context = getApplicationContext();
    FlutterMain.ensureInitializationComplete(context, null);
    mAppBundlePath = FlutterMain.findAppBundlePath(context);
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
}
