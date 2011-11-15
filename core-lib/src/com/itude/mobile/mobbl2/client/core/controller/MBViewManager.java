package com.itude.mobile.mobbl2.client.core.controller;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import android.app.Activity;
import android.app.ActivityGroup;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.MenuCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewParent;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;

import com.itude.mobile.mobbl2.client.core.android.compatibility.ActivityCompatHoneycomb;
import com.itude.mobile.mobbl2.client.core.configuration.mvc.MBConfigurationDefinition;
import com.itude.mobile.mobbl2.client.core.configuration.mvc.MBDialogDefinition;
import com.itude.mobile.mobbl2.client.core.configuration.mvc.MBPageDefinition;
import com.itude.mobile.mobbl2.client.core.controller.helpers.MBActivityHelper;
import com.itude.mobile.mobbl2.client.core.controller.util.MBBasicViewController;
import com.itude.mobile.mobbl2.client.core.controller.util.indicator.MBIndicatorI;
import com.itude.mobile.mobbl2.client.core.services.MBLocalizationService;
import com.itude.mobile.mobbl2.client.core.services.MBMetadataService;
import com.itude.mobile.mobbl2.client.core.services.MBResourceService;
import com.itude.mobile.mobbl2.client.core.services.MBWindowChangeType.WindowChangeType;
import com.itude.mobile.mobbl2.client.core.util.Constants;
import com.itude.mobile.mobbl2.client.core.util.MBDevice;
import com.itude.mobile.mobbl2.client.core.util.helper.MBSecurityHelper;
import com.itude.mobile.mobbl2.client.core.util.threads.MBThreadHandler;
import com.itude.mobile.mobbl2.client.core.view.MBPage;
import com.itude.mobile.mobbl2.client.core.view.components.MBTabBar;

public class MBViewManager extends ActivityGroup
{
  public enum MBViewState {
    MBViewStateFullScreen, MBViewStatePlain, MBViewStateTabbed, MBViewStateModal
  };

  protected static MBViewManager _instance;

  private ArrayList<String>      _dialogControllers;
  private ArrayList<String>      _sortedDialogNames;
  private Dialog                 _currentAlert;
  private boolean                _singlePageMode;
  private MBIndicatorI           _indeterminateIndicator;
  private MBIndicatorI           _activityIndicator;

  ///////////////////// Android lifecycle methods

  protected void onPreCreate()
  {
    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
  }

  @Override
  protected void onCreate(android.os.Bundle savedInstanceState)
  {
    onPreCreate();

    super.onCreate(savedInstanceState);

    _dialogControllers = new ArrayList<String>();
    _sortedDialogNames = new ArrayList<String>();
    _instance = this;

    MBApplicationController.getInstance().startController();
  }

  @Override
  protected void onRestart()
  {
    super.onRestart();

    MBApplicationController.getInstance().startOutcomeHandler();
  }

  @Override
  protected void onStop()
  {
    MBThreadHandler.getInstance().stopAllRunningThreads();

    MBApplicationController.getInstance().stopOutcomeHandler();

    super.onStop();
  }

  @Override
  protected void onPause()
  {
    if (MBActivityHelper.isApplicationBroughtToBackground(this))
    {
      stopAlert();
      MBSecurityHelper.getInstance().logOutIfCheckNotSelected();
    }
    super.onPause();
  }

  private void stopAlert()
  {
    Dialog currentAlert = getCurrentAlert();
    if (currentAlert != null) currentAlert.dismiss();
  }

  ///////////////////// 

  ///////////////////// Android methods

  @Override
  public boolean onCreateOptionsMenu(Menu menu)
  {
    if (MBDevice.getInstance().isTablet())
    {
      return false;
    }

    for (String dialogName : getSortedDialogNames())
    {
      MBDialogDefinition dialogDefinition = MBMetadataService.getInstance().getDefinitionForDialogName(dialogName);
      MenuItem menuItem = menu.add(Menu.NONE, dialogName.hashCode(), Menu.NONE, dialogDefinition.getTitle());
      menuItem.setIcon(MBResourceService.getInstance().getImageByID(dialogDefinition.getIcon()));
      MenuCompat.setShowAsAction(menuItem, MenuItem.SHOW_AS_ACTION_WITH_TEXT | MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    return true;
  }

  @Override
  public boolean onMenuItemSelected(int featureId, MenuItem item)
  {
    if (!getLocalActivityManager().getCurrentActivity().onMenuItemSelected(featureId, item)) activateDialogWithID(item.getItemId());
    return super.onMenuItemSelected(featureId, item);
  }

  @Override
  public void finishFromChild(Activity child)
  {
    if (child instanceof MBDialogController)
    {
      final MBDialogController childController = (MBDialogController) child;
      MBDialogDefinition firstDialogDefinition = MBMetadataService.getInstance().getFirstDialogDefinition();
      final String firstDialog = firstDialogDefinition.getName();
      if (!childController.getName().equals(firstDialog))
      {
        if (MBDevice.getInstance().isPhone())
        {
          activateDialogWithName(firstDialog);
        }
        else if (MBDevice.getInstance().isTablet())
        {
          runOnUiThread(new Runnable()
          {
            @Override
            public void run()
            {
              selectTab(firstDialog.hashCode());
            }
          });
        }
        setTitle(firstDialogDefinition.getTitle());
      }
      else
      {
        String message = MBLocalizationService.getInstance().getTextForKey("close app message");
        String positive = MBLocalizationService.getInstance().getTextForKey("close app positive button");
        String negative = MBLocalizationService.getInstance().getTextForKey("close app negative button");
        new AlertDialog.Builder(this).setMessage(message).setPositiveButton(positive, new OnClickListener()
        {

          @Override
          public void onClick(DialogInterface dialog, int which)
          {
            MBSecurityHelper.getInstance().logOutIfCheckNotSelected();
            finish();
          }
        }).setNegativeButton(negative, new OnClickListener()
        {

          @Override
          public void onClick(DialogInterface dialog, int which)
          {
            dialog.dismiss();
          }
        }).show();

      }
    }
    else super.finishFromChild(child);
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event)
  {
    if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0)
    {
      // Take care of calling this method on earlier versions of
      // the platform where it doesn't exist.
      ((FragmentActivity) getLocalActivityManager().getCurrentActivity()).onBackPressed();
      return true;
    }

    return super.onKeyDown(keyCode, event);
  }

  /////////////////////////////////////////////////////

  // Activate a dialog based on the hashed Name
  public void activateDialogWithID(int itemId)
  {
    for (MBDialogDefinition dialogDefinition : MBMetadataService.getInstance().getDialogs())
    {
      if (itemId == dialogDefinition.getName().hashCode())
      {
        if (!getActiveDialog().getName().equals(dialogDefinition.getName()))
        {
          activateDialogWithName(dialogDefinition.getName());
        }
        else
        {
          getActiveDialog().clearAllViews();
        }
      }
    }
  }

  public Dialog getCurrentAlert()
  {
    return _currentAlert;
  }

  public void setCurrentAlert(Dialog currentAlert)
  {
    _currentAlert = currentAlert;
  }

  public boolean getSinglePageMode()
  {
    return _singlePageMode;
  }

  public void setSinglePageMode(boolean singlePageMode)
  {
    _singlePageMode = singlePageMode;
  }

  public void setActivityIndicator(MBIndicatorI activityIndicator)
  {
    _activityIndicator = activityIndicator;
  }

  public void setIndeterminateIndicator(MBIndicatorI indeterminateIndicator)
  {
    _indeterminateIndicator = indeterminateIndicator;
  }

  public void showPage(MBPage page, String mode)
  {
    showPage(page, mode, true, true);
  }

  public void showPage(MBPage page, String displayMode, boolean shouldSelectDialog, boolean addToBackStack)
  {

    Log.d(Constants.APPLICATION_NAME,
          "MBViewManager: showPage name=" + page.getPageName() + " dialog=" + page.getDialogName() + " mode=" + displayMode + " type="
              + page.getPageType() + " orientation=" + ((MBPageDefinition) page.getDefinition()).getOrientationPermissions()
              + " backStack=" + addToBackStack);

    if (page.getPageType() == MBPageDefinition.MBPageType.MBPageTypesErrorPage || "POPUP".equals(displayMode))
    {
      showAlertView(page);
    }
    else
    {
      addPageToDialog(page, displayMode, shouldSelectDialog, addToBackStack);
    }
  }

  private void showAlertView(MBPage page)
  {

    if (getCurrentAlert() != null) getCurrentAlert().dismiss();

    String title = null;
    String message = null;

    if (page.getDocument().getName().equals(MBConfigurationDefinition.DOC_SYSTEM_EXCEPTION))
    {
      title = page.getDocument().getValueForPath(MBConfigurationDefinition.PATH_SYSTEM_EXCEPTION_NAME);
      message = page.getDocument().getValueForPath(MBConfigurationDefinition.PATH_SYSTEM_EXCEPTION_DESCRIPTION);
    }
    else
    {
      title = page.getTitle();
      message = MBLocalizationService.getInstance().getTextForKey((String) page.getDocument().getValueForPath("/message[0]/@text"));
      if (message == null) message = MBLocalizationService.getInstance().getTextForKey((String) page.getDocument()
                                                                                           .getValueForPath("/message[0]/@text()"));
    }

    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setMessage(message).setTitle(title).setCancelable(true).setNeutralButton("Ok", new DialogInterface.OnClickListener()
    {
      @Override
      public void onClick(DialogInterface dialog, int id)
      {
        dialog.cancel();
      }
    });

    runOnUiThread(new Runnable()
    {

      @Override
      public void run()
      {
        Dialog dialog = builder.create();
        dialog.show();
        setCurrentAlert(dialog);
      }
    });

  }

  private void addPageToDialog(MBPage page, String displayMode, boolean shouldSelectDialog, boolean addToBackStack)
  {
    MBDialogDefinition topDefinition = MBMetadataService.getInstance().getTopDialogDefinitionForDialogName(page.getDialogName());
    MBDialogController dialogController = getDialogWithName(topDefinition.getName());
    if (dialogController == null || dialogController.getTemporary())
    {
      activateDialogWithPage(page);
    }
    else
    {
      dialogController.showPage(page, displayMode, page.getDialogName() + page.getPageName(), page.getDialogName(), addToBackStack);
    }

    if (shouldSelectDialog) activateDialogWithName(topDefinition.getName());
  }

  public void activateDialogWithPage(MBPage page)
  {
    if (page != null)
    {
      String dialogName = MBMetadataService.getInstance().getTopDialogDefinitionForDialogName(page.getDialogName()).getName();
      Log.d(Constants.APPLICATION_NAME, "MBViewManager.activateDialogWithPage: dialogName=" + dialogName);

      _dialogControllers.add(dialogName);
      Intent intent = MBApplicationFactory.getInstance().createIntent(page.getDialogName());

      if (!CollectionUtils.isEqualCollection(getViewControllers(dialogName), getViewControllers(getActiveDialogName())))
      {
        MBDialogController dialogController = getDialogWithName(getActiveDialogName());
        // skip if the DialogController is already activated or not created yet.
        if (dialogController != null && dialogController != this.getLocalActivityManager().getCurrentActivity())
        {
          // Some Android smartphone devices don't onPause an Activity when expected. 
          // This is a workaround to make sure that all activities handle their stuff when leaving.
          dialogController.handleAllOnLeavingWindow();
        }
      }

      if (dialogName == null)
      {
        dialogName = getActiveDialogName();
      }

      intent.putExtra("dialogName", dialogName);
      //
      String id = page.getDialogName() + page.getPageName();
      MBApplicationController.getInstance().setPage(id, page);
      intent.putExtra("outcomeID", id);
      //
      Window window = getLocalActivityManager().startActivity(dialogName, intent);
      View view = window.getDecorView();
      MBDialogDefinition dialogDefinition = MBMetadataService.getInstance().getDefinitionForDialogName(dialogName);
      setTitle(dialogDefinition.getTitle());
      setContentView(view);

      MBBasicViewController vc = findViewController(dialogName, id);

      if (vc != null)
      {
        MBApplicationController.getInstance().changedWindow(vc, WindowChangeType.ACTIVATE);
      }

    }
  }

  private MBDialogController getDialogWithName(String dialogName)
  {
    return (MBDialogController) getLocalActivityManager().getActivity(dialogName);
  }

  public void activateDialogWithName(String dialogName)
  {
    Log.d(Constants.APPLICATION_NAME, "MBViewManager.activateDialogWithName: dialogName=" + dialogName);

    if (dialogName != null)
    {
      MBDialogDefinition dialogDefinition = MBMetadataService.getInstance().getDefinitionForDialogName(dialogName);
      if (dialogDefinition.getParent() != null)
      {
        dialogName = dialogDefinition.getParent();
        dialogDefinition = MBMetadataService.getInstance().getDefinitionForDialogName(dialogName);
      }

      if ("TRUE".equals(dialogDefinition.getAddToNavbar()) && !_sortedDialogNames.contains(dialogName))
      {
        _sortedDialogNames.add(dialogName);
      }

      MBDialogController dialogController = getDialogWithName(dialogName);
      // skip if the DialogController is already activated or not created yet.
      if (dialogController != null && dialogController != this.getLocalActivityManager().getCurrentActivity())
      {
        String previousDialogName = ((MBDialogController) getLocalActivityManager().getCurrentActivity()).getName();

        if (!CollectionUtils.isEqualCollection(getViewControllers(dialogName), getViewControllers(previousDialogName)))
        {
          MBDialogController previousDialogController = getDialogWithName(previousDialogName);
          if (previousDialogController != null)
          {
            // Some Android smartphone devices don't onPause an Activity when expected. 
            // This is a workaround to make sure that all activities handle their stuff when leaving.
            previousDialogController.handleAllOnLeavingWindow();
          }
        }

        Intent dialogIntent = MBApplicationFactory.getInstance().createIntent(dialogName);
        Window window = this.getLocalActivityManager().startActivity(dialogName, dialogIntent);
        final View view = window.getDecorView();
        runOnUiThread(new Runnable()
        {
          @Override
          public void run()
          {
            setContentView(view);
          }
        });

        if (getViewControllers(dialogName).size() > 0)
        {
          dialogController.handleAllOnWindowActivated();
        }

        if (MBDevice.getInstance().isTablet())
        {
          MBTabBar tabBar = getTabBar();
          if (tabBar != null)
          {
            tabBar.selectTab(dialogName.hashCode(), false);
          }
        }
      }
    }
  }

  public void endDialog(String dialogName, boolean keepPosition)
  {
  }

  public void popPage(String dialogName)
  {
    getDialogWithName(dialogName).popView();
  }

  public void showIndeterminateProgressIndicator()
  {
    if (_indeterminateIndicator != null)
    {
      _indeterminateIndicator.show(this);
    }
  }

  public void hideIndeterminateProgressIndicator()
  {
    if (_indeterminateIndicator != null)
    {
      _indeterminateIndicator.dismiss(this);
    }
  }

  public void showActivityIndicator()
  {
    if (_activityIndicator != null)
    {
      _activityIndicator.show(this);
    }
  }

  public synchronized void hideActivityIndicator()
  {
    if (_activityIndicator != null && _activityIndicator.isActive())
    {
      _activityIndicator.dismiss(this);
    }
  }

  public void makeKeyAndVisible()
  {
  }

  public String getActiveDialogName()
  {
    if (getCurrentActivity() == null)
    {
      return null;
    }

    return ((MBDialogController) getCurrentActivity()).getName();
  }

  public MBDialogController getActiveDialog()
  {
    if (getCurrentActivity() == null)
    {
      return null;
    }

    return (MBDialogController) getCurrentActivity();
  }

  public void resetView()
  {
  }

  public void resetViewPreservingCurrentDialog()
  {
    // Walk trough all dialogControllers
    for (int i = 0; i < _dialogControllers.size(); i++)
    {
      // Pop all controller apart from first one
      MBDialogController dc = (MBDialogController) getLocalActivityManager().getActivity(_dialogControllers.get(i));
      if (dc != null)
      {
        dc.clearAllViews();
      }
    }
  }

  public void endModalDialog(String modalPageID)
  {
    MBDialogController dc = (MBDialogController) getLocalActivityManager().getCurrentActivity();
    dc.endModalPage(modalPageID);
  }

  public void endModalDialog()
  {
    endModalDialog(MBApplicationController.getInstance().getModalPageID());
  }

  public MBViewState getCurrentViewState()
  {
    if (_dialogControllers.size() > 1)
    {
      return MBViewState.MBViewStateTabbed;
    }
    return MBViewState.MBViewStatePlain;
  }

  public static MBViewManager getInstance()
  {
    return _instance;
  }

  public void setSortedDialogNames(ArrayList<String> sortedDialogNames)
  {
    _sortedDialogNames = sortedDialogNames;
  }

  public ArrayList<String> getSortedDialogNames()
  {
    return _sortedDialogNames;
  }

  @Override
  public boolean onSearchRequested()
  {
    return getLocalActivityManager().getCurrentActivity().onSearchRequested();
  }

  /**
   * @param dialogName dialogName
   */
  public void removeDialog(String dialogName)
  {
    clearDialogFromStack(dialogName);
    MBDialogController activeDialog = getActiveDialog();
    if (activeDialog != null)
    {
      MBBasicViewController fragment = activeDialog.findFragment(dialogName);
      if (fragment != null)
      {
        View root = fragment.getView();
        if (root != null)
        {
          ViewParent parent = root.getParent();
          if (parent instanceof FrameLayout)
          {
            final FrameLayout fragmentContainer = (FrameLayout) parent;
            runOnUiThread(new Runnable()
            {
              @Override
              public void run()
              {
                fragmentContainer.removeAllViews();
              }
            });
          }
        }
      }
    }
  }

  /**
   * @param dialogName dialogName
   */
  public void clearDialogFromStack(String dialogName)
  {
    MBDialogDefinition dialogDefinition = MBMetadataService.getInstance().getDefinitionForDialogName(dialogName);
    if (dialogDefinition.getParent() != null)
    {
      dialogName = dialogDefinition.getParent();
    }
    MBDialogController controller = getDialogWithName(dialogName);
    if (controller != null) controller.clearAllViews();
  }

  public void hideSoftKeyBoard(View triggeringView)
  {
    InputMethodManager imm = (InputMethodManager) triggeringView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.hideSoftInputFromWindow(triggeringView.getWindowToken(), 0);
  }

  public List<MBBasicViewController> getViewControllers(String dialogName)
  {
    Log.d(Constants.APPLICATION_NAME, "MBViewManager.getViewControllers: dialogName=" + dialogName);

    List<MBBasicViewController> lijst = new ArrayList<MBBasicViewController>();

    if (dialogName != null)
    {
      MBDialogController dc = getDialogWithName(dialogName);
      if (dc != null)
      {
        List<MBBasicViewController> fragments = dc.getAllFragments();
        if (!fragments.isEmpty()) lijst.addAll(fragments);
      }
    }
    return lijst;

  }

  public MBBasicViewController findViewController(String dialogName, String viewID)
  {
    MBBasicViewController controller = null;
    Log.d(Constants.APPLICATION_NAME, "MBViewManager.findViewController: dialogName=" + dialogName + "' viewId=" + viewID);
    if (dialogName != null && viewID != null)
    {
      MBDialogController dc = getDialogWithName(dialogName);
      if (dc != null)
      {
        controller = dc.findFragment(viewID);
      }
    }
    return controller;
  }

  /**
   * Method can be used to manually request an orientation
   * @param orientation use {@link ActivityInfo} to set your requested orientation.
   */
  public void setOrientation(int orientation)
  {
    Log.d(Constants.APPLICATION_NAME, "MBViewManager.setOrientation: Changing to " + orientation);
    setRequestedOrientation(orientation);
  }

  public void setOrientation(MBPage page)
  {

    if (page.isAllowedAnyOrientation())
    {
      if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_SENSOR)
      {
        Log.d(Constants.APPLICATION_NAME, "MBViewManager.setOrientation: Changing to SENSOR");
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
      }
    }
    else if (page.isAllowedPortraitOrientation())
    {
      Log.d(Constants.APPLICATION_NAME, "MBViewManager.setOrientation: Changing to PORTRAIT");
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }
    else if (page.isAllowedLandscapeOrientation())
    {
      Log.d(Constants.APPLICATION_NAME, "MBViewManager.setOrientation: Changing to LANDSCAPE");
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig)
  {
    Log.d(Constants.APPLICATION_NAME, "MBViewManager.onConfigurationChanged");

    // Only handle orientationchanges when orientation changed, obviously
    // http://dev.itude.com/jira/browse/BINCKAPPS-1096
    for (MBBasicViewController controller : getViewControllers(getActiveDialogName()))
    {
      controller.handleOrientationChange(newConfig);
    }

    if (MBDevice.getInstance().isTablet())
    {
      // Also, tell all Dialogs
      for (String dialog : _dialogControllers)
      {
        MBDialogController dc = (MBDialogController) getLocalActivityManager().getActivity(dialog);
        dc.handleOrientationChange(newConfig);
      }
    }

    super.onConfigurationChanged(newConfig);
  }

  public List<MBBasicViewController> getAllFragments()
  {
    List<MBBasicViewController> list = new ArrayList<MBBasicViewController>();
    // Walk trough all dialogControllers
    for (int i = 0; i < _dialogControllers.size(); i++)
    {
      // Pop all controller apart from first one
      MBDialogController dc = (MBDialogController) getLocalActivityManager().getActivity(_dialogControllers.get(i));
      //TODO Duplicaten er nog eens uit halen.
      if (dc != null && !dc.getAllFragments().isEmpty()) list.addAll(dc.getAllFragments());
    }

    return list;
  }

  // Tablet specific methods. Some methods are implemented also to run on smartphone.
  // Others are for tablet only.

  /**
   * Copied from FragmentActivity.java in the Android Compatibility Package. Invoke this method
   * to invalidate the options menu, but avoiding linker errors due to SDK incompatibility.
   */
  public void supportInvalidateOptionsMenu()
  {
    if (MBDevice.getInstance().isTablet())
    {
      // If we are running on HC or greater, we can use the framework
      // API to invalidate the options menu.
      ActivityCompatHoneycomb.invalidateOptionsMenu(this);
    }
  }

  public void invalidateActionBar(boolean selectFirstTab)
  {
    throw new UnsupportedOperationException("This method is not supported on smartphone");
  }

  public void showProgressIndicatorInTool()
  {
    throw new UnsupportedOperationException("This method is not supported on smartphone");
  }

  public void hideProgressIndicatorInTool()
  {
    throw new UnsupportedOperationException("This method is not supported on smartphone");
  }

  public MBTabBar getTabBar()
  {
    throw new UnsupportedOperationException("This method is not supported on smartphone");
  }

  public void selectTab(int hashCode)
  {
    throw new UnsupportedOperationException("This method is not supported on smartphone");
  }

  public void hideSearchView()
  {
    throw new UnsupportedOperationException("This method is not supported on smartphone");
  }

}
