package com.flutter.example.locationbackgroundplugin;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.plugin.common.PluginRegistry.ViewDestroyListener;
import io.flutter.view.FlutterNativeView;

public class LocationBackgroundPlugin implements MethodCallHandler, ViewDestroyListener {
  public static final String TAG = "LocationBackground";
  public static final int LOCATION_REQUEST_ID = 143421069;
  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "plugins.flutter.io/background_location");

    final MethodChannel backgroundChannel = new MethodChannel(registrar.messenger(), "plugins.flutter.io/background_location_callback");
    LocationBackgroundPlugin plugin = new LocationBackgroundPlugin(registrar);
    channel.setMethodCallHandler(plugin);
    backgroundChannel.setMethodCallHandler(plugin);
    registrar.addViewDestroyListener(plugin);
    LocationBackgroundService.setBackgroundChannel(backgroundChannel);
  }

  private Context mContext;
  private Registrar mRegistrar;
  private ArrayList mMonitorLocationArguments;

  private LocationBackgroundPlugin(Registrar registrar) {
    this.mContext = registrar.context();
    this.mRegistrar = registrar;
    registrar.addRequestPermissionsResultListener(new BackgroundLocationRequestPermissionsListener());
  }

  @Override
  public void onMethodCall(MethodCall call, Result result) {
    String method = call.method;
    ArrayList arguments = (ArrayList) call.arguments;

    Log.i(TAG, method);

    if (method.equals("startHeadlessService")) {
      startHeadlessService(arguments);
      result.success(true);
    } else if (method.equals("monitorLocationChanges")) {
      monitorLocationChanges(arguments);
      result.success(true);
    } else if (method.equals("cancelLocationUpdates")) {
      cancelLocationUpdates(arguments);
      result.success(true);
    } else {
      result.notImplemented();
    }

  }

  private void startHeadlessService(ArrayList arguments) {
    long handle = Long.valueOf((int)arguments.get(0));
    Log.i(TAG, "startHeadlessService " + Long.toString(handle));
    LocationBackgroundService.startHeadlessService(mContext, handle);
  }

  private void monitorLocationChanges(ArrayList arguments) {
    if(hasLocationPermission()) {
      Log.i(TAG, "monitorLocationChanges " + Integer.toString(arguments.size()));
      LocationBackgroundService.monitorLocationChanges(mContext);
    } else {
      // Store these so we can resume after we get permission
      mMonitorLocationArguments = arguments;
      requestLocationPermission();
    }
  }

  private void cancelLocationUpdates(ArrayList arguments) {
    Log.i(TAG, "cancelLocationUpdates");
  }

  private boolean hasLocationPermission() {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            mContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
  }

  private void requestLocationPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//      requestingPermission = true;
      mRegistrar.activity().requestPermissions(
              new String[] {Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_ID);
    }
  }

  @Override
  public boolean onViewDestroy(FlutterNativeView nativeView) {
    return LocationBackgroundService.setBackgroundFlutterView(nativeView);
  }

  private class BackgroundLocationRequestPermissionsListener
          implements PluginRegistry.RequestPermissionsResultListener {
    @Override
    public boolean onRequestPermissionsResult(int id, String[] permissions, int[] grantResults) {
      if (id == LOCATION_REQUEST_ID) {
        // Retry to start tracking location. Doesn't matter if the request was approved or denied.
        // If approved then tracking will start, if denied then we will just ask again haha
        monitorLocationChanges(mMonitorLocationArguments);
        return true;
      }

      return false;
    }
  }
}
