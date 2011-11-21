package com.itude.mobile.mobbl2.client.core.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;

public final class DeviceUtil
{
  private static DeviceUtil _instance;
  private Context           _context;

  private DeviceUtil()
  {
  }

  public static DeviceUtil getInstance()
  {
    if (_instance == null)
    {
      _instance = new DeviceUtil();
    }

    return _instance;
  }

  public void setContext(Context context)
  {
    _context = context;
  }

  public String getUniqueID()
  {
    String androidID = Settings.Secure.getString(_context.getContentResolver(), Settings.Secure.ANDROID_ID);

    if (androidID == null)
    {
      return "";
    }

    return androidID;
  }

  public boolean isInternetConnectionAvailable()
  {
    ConnectivityManager cm = (ConnectivityManager) _context.getSystemService(Context.CONNECTIVITY_SERVICE);
    if (cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isAvailable() && cm.getActiveNetworkInfo().isConnected())
    {
      return true;
    }
    else
    {
      return false;
    }
  }

  public boolean isPhoneServiceAvailable()
  {
    TelephonyManager tm = (TelephonyManager) _context.getSystemService(Context.TELEPHONY_SERVICE);
    if (tm.getPhoneType() == TelephonyManager.PHONE_TYPE_NONE)
    {
      return false;
    }
    return true;

  }
}
