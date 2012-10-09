package com.itude.mobile.mobbl2.client.core;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;

import com.itude.mobile.mobbl2.client.core.controller.MBViewManagerFactory;
import com.itude.mobile.mobbl2.client.core.services.MBResourceService;
import com.itude.mobile.mobbl2.client.core.util.Constants;

public class MBSplashScreen extends Activity
{
  private Handler _handler = null;
  private Thread  _thread  = null;

  @Override
  protected void onResume()
  {
    super.onResume();

    _handler = new Handler();

    _thread = new Thread()
    {
      @Override
      public void run()
      {
        finishSplashScreen();
      }
    };
    // Keep splashscreen visible for at least 1 second
    _handler.postDelayed(_thread, 1000);

  }

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);

    setView();
  }

  @Override
  protected void onPause()
  {
    _handler.removeCallbacks(_thread);
    super.onPause();
  }

  protected void finishSplashScreen()
  {
    Intent viewManager = new Intent(getBaseContext(), MBViewManagerFactory.getViewManagerClass());

    // Let's make sure that all extras we've set while starting the splashscreen will be tunneled to the viewmanager
    final Bundle extras = getIntent().getExtras();
    if (extras != null)
    {
      viewManager.putExtras(extras);
    }

    startActivity(viewManager);
  }

  protected void setView()
  {
    ImageView imageView = new ImageView(getApplicationContext());
    imageView.setImageDrawable(MBResourceService.getInstance().getImageByID(Constants.C_SPLASHSCREEN));

    setContentView(imageView);

  }
}
