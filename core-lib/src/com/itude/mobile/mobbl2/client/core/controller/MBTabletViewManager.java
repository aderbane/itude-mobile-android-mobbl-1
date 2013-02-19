package com.itude.mobile.mobbl2.client.core.controller;

import java.lang.reflect.Field;
import java.util.List;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.SearchManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;

import com.itude.mobile.android.util.ScreenUtil;
import com.itude.mobile.android.util.StringUtil;
import com.itude.mobile.mobbl2.client.core.configuration.mvc.MBConfigurationDefinition;
import com.itude.mobile.mobbl2.client.core.configuration.mvc.MBDialogDefinition;
import com.itude.mobile.mobbl2.client.core.configuration.mvc.MBDomainDefinition;
import com.itude.mobile.mobbl2.client.core.configuration.mvc.MBDomainValidatorDefinition;
import com.itude.mobile.mobbl2.client.core.configuration.mvc.MBToolDefinition;
import com.itude.mobile.mobbl2.client.core.controller.exceptions.MBExpressionNotBooleanException;
import com.itude.mobile.mobbl2.client.core.model.MBDocument;
import com.itude.mobile.mobbl2.client.core.services.MBDataManagerService;
import com.itude.mobile.mobbl2.client.core.services.MBLocalizationService;
import com.itude.mobile.mobbl2.client.core.services.MBMetadataService;
import com.itude.mobile.mobbl2.client.core.services.MBResourceService;
import com.itude.mobile.mobbl2.client.core.util.Constants;
import com.itude.mobile.mobbl2.client.core.util.MBParseUtil;
import com.itude.mobile.mobbl2.client.core.util.ScreenConstants;
import com.itude.mobile.mobbl2.client.core.util.threads.MBThread;
import com.itude.mobile.mobbl2.client.core.view.builders.MBStyleHandler;
import com.itude.mobile.mobbl2.client.core.view.builders.MBViewBuilderFactory;
import com.itude.mobile.mobbl2.client.core.view.components.tabbar.MBTab;
import com.itude.mobile.mobbl2.client.core.view.components.tabbar.MBTabBar;
import com.itude.mobile.mobbl2.client.core.view.components.tabbar.MBTabListener;
import com.itude.mobile.mobbl2.client.core.view.components.tabbar.MBTabSpinnerAdapter;

/**
 * @author Coen Houtman
 *
 *  This ViewManager can be used to perform actions that cannot be done on pre-Honeycomb devices.
 *  For example the use of the ActionBar.
 */
@TargetApi(11)
public class MBTabletViewManager extends MBViewManager
{
  private Menu             _menu           = null;
  private MBToolDefinition _refreshToolDef = null;

  @Override
  protected void onPreCreate()
  {
    // empty to override request window feature to hide Android's standard indeterminate progress indicator
  }

  // Android hooks
  @Override
  public boolean onCreateOptionsMenu(Menu menu)
  {
    _menu = menu;

    List<MBToolDefinition> tools = MBMetadataService.getInstance().getTools();

    for (MBToolDefinition def : tools)
    {
      if (isPreConditionValid(def))
      {
        String localizedTitle = MBLocalizationService.getInstance().getTextForKey(def.getTitle());
        MenuItem menuItem = menu.add(Menu.NONE, def.getName().hashCode(), tools.indexOf(def), localizedTitle);

        Drawable image = null;
        if (def.getIcon() != null)
        {
          image = MBResourceService.getInstance().getImageByID(def.getIcon());
          menuItem.setIcon(image);
        }

        menuItem.setShowAsAction(getMenuItemActionFlags(def));

        if ("REFRESH".equals(def.getType()))
        {
          _refreshToolDef = def;
        }
        else if ("SEARCH".equals(def.getType()))
        {
          final SearchView searchView = new SearchView(MBViewManager.getInstance().getApplicationContext());
          searchView.setTag(def);
          searchView.setOnQueryTextFocusChangeListener(new OnFocusChangeListener()
          {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
              if (hasFocus)
              {
                Object tag = v.getTag();
                if (tag instanceof MBToolDefinition)
                {
                  MBToolDefinition toolDef = (MBToolDefinition) tag;
                  if (toolDef.getOutcomeName() != null)
                  {
                    handleOutcome(toolDef);
                  }
                }
              }
              else
              {
                searchView.setIconified(true);
              }
            }
          });

          if (image != null)
          {
            try
            {
              // change the iconified icon
              Field searchButtonField = searchView.getClass().getDeclaredField("mSearchButton");
              searchButtonField.setAccessible(true);
              ImageView searchButton = (ImageView) searchButtonField.get(searchView);
              searchButton.setImageDrawable(image);

              // change the searchview
              Field searchEditField = searchView.getClass().getDeclaredField("mSearchEditFrame");
              searchEditField.setAccessible(true);
              LinearLayout searchLayout = (LinearLayout) searchEditField.get(searchView);

              LinearLayout searchPlate = (LinearLayout) searchLayout.getChildAt(0);
              MBViewBuilderFactory.getInstance().getStyleHandler().styleSearchPlate(searchPlate);

              // find first image view, assuming this is the icon we need
              setSearchImage(image, searchPlate);

              LinearLayout submitArea = (LinearLayout) searchLayout.getChildAt(1);
              MBViewBuilderFactory.getInstance().getStyleHandler().styleSearchSubmitArea(submitArea);
            }
            catch (Exception e)
            {
              Log.e(Constants.APPLICATION_NAME, "error changing searchbutton icon", e);
            }
          }

          SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
          searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

          menuItem.setActionView(searchView);
        }
      }
    }

    return true;
  }

  private void setSearchImage(Drawable image, LinearLayout linearLayout)
  {
    ImageView searchViewIcon = null;

    for (int i = 0; i < linearLayout.getChildCount(); i++)
    {
      View view = linearLayout.getChildAt(i);
      if (view instanceof ImageView)
      {
        searchViewIcon = (ImageView) view;
        break;
      }
    }
    searchViewIcon.setImageDrawable(image);
  }

  private int getMenuItemActionFlags(MBToolDefinition def)
  {
    String visibility = def.getVisibility();
    if (StringUtil.isBlank(visibility))
    {
      Log.w(Constants.APPLICATION_NAME, "No visibility specified for tool " + def.getName() + ": using default show as action if room");
      return MenuItem.SHOW_AS_ACTION_IF_ROOM;
    }

    int flags = 0;
    String[] split = visibility.split("\\|");
    for (String flagString : split)
    {
      int flag = getFlagForString(flagString);
      flags = flags | flag;
    }

    return flags;
  }

  private int getFlagForString(String flag)
  {
    int resultFlag = -1;
    if ("ALWAYS".equals(flag))
    {
      resultFlag = MenuItem.SHOW_AS_ACTION_ALWAYS;
    }
    else if ("IFROOM".equals(flag))
    {
      resultFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM;
    }
    else if ("OVERFLOW".equals(flag))
    {
      resultFlag = MenuItem.SHOW_AS_ACTION_NEVER;
    }
    else if ("SHOWTEXT".equals(flag))
    {
      resultFlag = MenuItem.SHOW_AS_ACTION_WITH_TEXT;
    }
    else
    {
      throw new IllegalArgumentException("Invalid flag: " + flag);
    }

    return resultFlag;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  {
    if (item.getItemId() == android.R.id.home)
    {
      onHomeSelected();
      return true;
    }

    for (MBToolDefinition def : MBMetadataService.getInstance().getTools())
    {
      if (item.getItemId() == def.getName().hashCode())
      {
        if (def.getOutcomeName() != null)
        {
          handleOutcome(def);
          return true;
        }
        return false;
      }
    }

    return super.onOptionsItemSelected(item);
  }

  private void handleOutcome(MBToolDefinition def)
  {
    MBOutcome outcome = new MBOutcome();
    outcome.setOriginName(def.getName());
    outcome.setOutcomeName(def.getOutcomeName());

    MBApplicationController.getInstance().handleOutcome(outcome);
  }

  // End of Android hooks

  private void onHomeSelected()
  {
    MBTabBar tabBar = getTabBar();
    if (tabBar != null)
    {
      resetViewPreservingCurrentDialog();
      int firstDialog = MBMetadataService.getInstance().getHomeDialogDefinition().getName().hashCode();
      MBTab selectedTab = tabBar.getSelectedTab();
      if (selectedTab == null || firstDialog != selectedTab.getTabId())
      {
        tabBar.selectTab(firstDialog, true);
      }
    }
  }

  @Override
  public boolean activateDialogWithName(String dialogName)
  {
    boolean activated = super.activateDialogWithName(dialogName);

    if (dialogName != null)
    {
      MBDialogDefinition dialogDefinition = MBMetadataService.getInstance().getDefinitionForDialogName(dialogName);
      if (dialogDefinition.getParent() != null)
      {
        dialogName = dialogDefinition.getParent();
        dialogDefinition = MBMetadataService.getInstance().getDefinitionForDialogName(dialogName);
      }

      if (StringUtil.isBlank(dialogDefinition.getShowAs()))
      {
        MBTabBar tabBar = getTabBar();
        if (tabBar != null)
        {
          tabBar.selectTab(null, true);
        }
      }
    }
    return activated;
  }

  @Override
  public void invalidateSlidingMenu()
  {
  }

  @Override
  public void selectTab(int id)
  {
    MBTabBar tabBar = getTabBar();
    if (tabBar != null)
    {
      tabBar.selectTab(id, true);
    }
  }

  @Override
  public MBTabBar getTabBar()
  {
    try
    {
      return (MBTabBar) getActionBar().getCustomView();
    }
    catch (Exception e)
    {
      Log.w(Constants.APPLICATION_NAME, "Unable to retrieve the tab bar, returning null", e);
      return null;
    }
  }

  private void populateActionBar()
  {
    runOnUiThread(new Runnable()
    {
      @Override
      public void run()
      {
        final ActionBar actionBar = getActionBar();

        MBStyleHandler styleHandler = MBViewBuilderFactory.getInstance().getStyleHandler();

        //fix the Home icon padding
        View homeIcon = findViewById(android.R.id.home);
        if (homeIcon != null)
        {
          styleHandler.styleHomeIcon(homeIcon);
        }

        styleHandler.styleActionBar(actionBar);

        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_USE_LOGO);

        MBTabBar tabBar = new MBTabBar(MBTabletViewManager.this);

        for (String dialogName : getSortedDialogNames())
        {
          MBDialogDefinition dialogDefinition = MBMetadataService.getInstance().getDefinitionForDialogName(dialogName);

          if (dialogDefinition.isShowAsTab())
          {
            if (dialogDefinition.getDomain() != null)
            {
              MBDomainDefinition domainDef = MBMetadataService.getInstance().getDefinitionForDomainName(dialogDefinition.getDomain());

              final MBTabSpinnerAdapter tabSpinnerAdapter = new MBTabSpinnerAdapter(MBTabletViewManager.this,
                  android.R.layout.simple_spinner_dropdown_item);

              for (MBDomainValidatorDefinition domDef : domainDef.getDomainValidators())
              {
                tabSpinnerAdapter.add(domDef.getTitle());
              }

              Drawable drawable = MBResourceService.getInstance().getImageByID("tab-spinner-leaf");

              MBTab tab = new MBTab(MBTabletViewManager.this);
              tab.setAdapter(tabSpinnerAdapter);
              tab.setSelectedBackground(drawable);
              tab.setIcon(MBResourceService.getInstance().getImageByID(dialogDefinition.getIcon()));

              setTabText(dialogDefinition, tab, tabBar);

              tab.setTabId(dialogName.hashCode());
              tab.setListener(new MBTabListener(dialogName.hashCode()));

              tabBar.addTab(tab);
            }
            else
            {
              MBTab tab = new MBTab(MBTabletViewManager.this);
              setTabText(dialogDefinition, tab, tabBar);

              tab.setListener(new MBTabListener(dialogName.hashCode()));
              tab.setTabId(dialogName.hashCode());

              if (dialogDefinition.getIcon() != null)
              {
                tab.setIcon(MBResourceService.getInstance().getImageByID(dialogDefinition.getIcon()));
              }
              tabBar.addTab(tab);
            }
          }
        }
        actionBar.setCustomView(tabBar, new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT,
            ActionBar.LayoutParams.MATCH_PARENT, Gravity.LEFT));

      }

      private void setTabText(MBDialogDefinition dialogDefinition, MBTab tab, MBTabBar tabBar)
      {
        String title;

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
        {
          title = dialogDefinition.getTitlePortrait();
        }
        else
        {
          title = dialogDefinition.getTitle();
        }

        if (StringUtil.isNotBlank(title))
        {
          tab.setText(title);
          tabBar.setTabPadding(0, 0, ScreenConstants.SIXTEEN, 0);
        }
        else
        {
          tabBar.setTabPadding(0, 0, 0, 0);
        }

      }
    });
  }

  private void refreshActionBar()
  {
    invalidateActionBar(false, false);
  }

  @Override
  public void invalidateActionBar(boolean selectFirstTab)
  {
    invalidateActionBar(selectFirstTab, true);
  }

  @Override
  public void invalidateActionBar(final boolean showFirst, final boolean notifyListener)
  {
    runOnUiThread(new MBThread()
    {
      @Override
      public void runMethod()
      {
        MBTabBar tabBar = getTabBar();
        int selectedTab = -1;
        if (tabBar != null)
        {
          selectedTab = tabBar.indexOfSelectedTab();

          if (tabBar.getSelectedTab() != null) tabBar.getSelectedTab().setSelected(false);
        }
        invalidateOptionsMenu();
        // throw away current MBActionBar and create a new one
        getActionBar().setCustomView(null);

        populateActionBar();

        tabBar = getTabBar();
        if (tabBar != null)
        {
          if (showFirst)
          {
            MBTab tab = tabBar.getTab(0);
            tabBar.selectTab(tab, notifyListener);
          }
          else if (selectedTab >= 0)
          {
            tabBar.selectTab(tabBar.getTab(selectedTab), notifyListener);
          }
        }
      }
    });
  }

  @Override
  public synchronized void showProgressIndicatorInTool()
  {
    if (_refreshToolDef != null && _menu != null)
    {
      final MenuItem item = _menu.findItem(_refreshToolDef.getName().hashCode());

      ImageView rotationImage = getRotationImage();

      float imageWidth = rotationImage.getDrawable().getIntrinsicWidth();
      int framePadding = (int) ((ScreenUtil.convertDimensionPixelsToPixels(getBaseContext(), 80) - imageWidth) / 2);

      final FrameLayout frameLayout = new FrameLayout(this);
      frameLayout.setLayoutParams(new FrameLayout.LayoutParams(ScreenUtil.convertDimensionPixelsToPixels(getBaseContext(), 80),
          LayoutParams.WRAP_CONTENT, Gravity.CENTER));
      frameLayout.setPadding(framePadding, 0, framePadding, 0);

      frameLayout.addView(rotationImage);

      runOnUiThread(new MBThread()
      {
        @Override
        public void runMethod()
        {
          //item.setIcon(null);
          item.setActionView(frameLayout);
          getRotationImage().getAnimation().startNow();
        }
      });
    }
  }

  private ImageView getRotationImage()
  {
    RotateAnimation rotateAnimation = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
    rotateAnimation.setDuration(1000L);
    rotateAnimation.setRepeatMode(Animation.INFINITE);
    rotateAnimation.setRepeatCount(Animation.INFINITE);
    rotateAnimation.setFillEnabled(false);
    rotateAnimation.setInterpolator(new LinearInterpolator());

    Drawable drawable = MBResourceService.getInstance().getImageByID(_refreshToolDef.getIcon());
    ImageView rotationImage = new ImageView(this);
    rotationImage.setImageDrawable(drawable);
    rotationImage.setAnimation(rotateAnimation);

    return rotationImage;
  }

  @Override
  public synchronized void hideProgressIndicatorInTool()
  {
    if (_refreshToolDef != null && _menu != null)
    {
      final MenuItem item = _menu.findItem(_refreshToolDef.getName().hashCode());

      runOnUiThread(new MBThread()
      {
        @Override
        public void runMethod()
        {
          item.setActionView(null);
        }
      });
    }
  }

  private boolean isPreConditionValid(MBToolDefinition def)
  {
    if (def.getPreCondition() == null)
    {
      return true;
    }

    MBDocument doc = MBDataManagerService.getInstance().loadDocument(MBConfigurationDefinition.DOC_SYSTEM_EMPTY);

    String result = doc.evaluateExpression(def.getPreCondition());
    Boolean bool = MBParseUtil.strictBooleanValue(result);
    if (bool != null) return bool;
    String msg = "Expression of tool with name=" + def.getName() + " precondition=" + def.getPreCondition() + " is not boolean (result="
                 + result + ")";
    throw new MBExpressionNotBooleanException(msg);
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig)
  {
    refreshActionBar();

    super.onConfigurationChanged(newConfig);

    // Android 3.2 (perhaps other versions, but not 3.0) destroys the styling of the home icon in the action bar
    // when an onConfigurationChanged is triggered. When the main thread is about here, the handling of
    // that onConfigurationChanged should be in the message queue. This places a new message at the end of
    // the queue that changes the home icon back, which is therefore handled after the onConfigurationChanged message
    // that ruins it.
    final View homeIcon = findViewById(android.R.id.home);
    if (homeIcon != null) new Handler().post(new Runnable()
    {

      @Override
      public void run()
      {
        MBViewBuilderFactory.getInstance().getStyleHandler().styleHomeIcon(homeIcon);
      }
    });

  }
}
