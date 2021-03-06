/*
 * (C) Copyright Itude Mobile B.V., The Netherlands
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.itude.mobile.mobbl.core.controller;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import android.os.Handler;
import android.os.Message;

import com.itude.mobile.android.util.ComparisonUtil;
import com.itude.mobile.android.util.log.MBLog;
import com.itude.mobile.mobbl.core.configuration.mvc.MBDialogDefinition;
import com.itude.mobile.mobbl.core.configuration.mvc.MBPageStackDefinition;
import com.itude.mobile.mobbl.core.services.MBMetadataService;
import com.itude.mobile.mobbl.core.util.Constants;
import com.itude.mobile.mobbl.core.view.MBOutcomeListenerProtocol;

/**
 * {@link Handler} class responsable to handle an outcome
 */
public class MBOutcomeHandler extends Handler
{
  private final LinkedList<WeakReference<MBOutcomeListenerProtocol>> _outcomeListeners = new LinkedList<WeakReference<MBOutcomeListenerProtocol>>();

  @Override
  public void handleMessage(Message msg)
  {
    MBLog.d(Constants.APPLICATION_NAME, "MBOutcomeHandler.handleMessage(): " + msg.what);
    if (msg.what == Constants.C_MESSAGE_INITIAL_OUTCOMES_FINISHED)
    {
      MBApplicationController.getInstance().finishedInitialOutcomes();
      return;
    }
    MBOutcome outcome = msg.getData().getParcelable("outcome");
    boolean throwException = msg.getData().getBoolean("throwException", true);

    new MBOutcomeRunner(outcome, throwException).handle();
  }

  public void handleOutcomeSynchronously(MBOutcome outcome, boolean throwException)
  {
    outcome.setNoBackgroundProcessing(true);
    new MBOutcomeRunner(outcome, throwException).handle();

  }

  /***
   * In case of:
   * <ol>
   *  <li>a split dialog;</li> 
   *  <li>2. a page must always put either left or right; and</li>
   *  <li>3. that page can be used in multiple dialogs</li>
   *  </ol>
   *  
   *  Instead of defining the specific dialog name, either LEFT or RIGHT can be defined as the target dialog.
   *  The page will be displayed in either the left or right part of the active dialog.
   *  
   *  Example outcome definition:
   *  <Outcome origin="*" name="OUTCOME-page_winnerslosers_overview" action="PAGE-page_winnerslosers_overview" transferDocument="TRUE" dialog="LEFT"/>
   *  
   * @param dialogName
   * @return the dialog name to place the page in
   */
  static String resolvePageStackName(String pageStackName)
  {
    // find out if the page stack to resolve is actually a dialog..
    MBDialogDefinition dialogDef = MBMetadataService.getInstance().getDefinitionForDialogName(pageStackName, false);
    if (dialogDef != null)
    {
      return dialogDef.getChildren().get(0).getName();
    }

    String activeDialogName = MBViewManager.getInstance().getActiveDialogName();
    if (activeDialogName == null) return pageStackName;
    MBDialogDefinition activeDialogDef = MBMetadataService.getInstance().getDefinitionForDialogName(activeDialogName);

    MBPageStackDefinition pageStackDef = null;
    for (MBPageStackDefinition pageStack : activeDialogDef.getChildren())
      if (ComparisonUtil.safeEquals(pageStackName, pageStack.getLocalName()))
      {
        pageStackDef = pageStack;
        break;
      }

    if (pageStackDef != null)
    {

      MBLog.d(Constants.APPLICATION_NAME, "Dialog name '" + pageStackName + "' resolved to '" + pageStackDef.getName() + "'");
      return pageStackDef.getName();
    }

    return pageStackName;
  }

  public void registerOutcomeListener(MBOutcomeListenerProtocol listener)
  {
    synchronized (_outcomeListeners)
    {
      for (Iterator<WeakReference<MBOutcomeListenerProtocol>> it = _outcomeListeners.iterator(); it.hasNext();)
      {
        WeakReference<MBOutcomeListenerProtocol> ref = it.next();
        MBOutcomeListenerProtocol prot = ref.get();
        if (prot == null) it.remove();
        else if (prot.equals(listener)) return;
      }
      _outcomeListeners.add(new WeakReference<MBOutcomeListenerProtocol>(listener));
    }
  }

  public void unregisterOutcomeListener(MBOutcomeListenerProtocol listener)
  {
    synchronized (_outcomeListeners)
    {
      for (Iterator<WeakReference<MBOutcomeListenerProtocol>> it = _outcomeListeners.iterator(); it.hasNext();)
      {
        WeakReference<MBOutcomeListenerProtocol> ref = it.next();
        MBOutcomeListenerProtocol prot = ref.get();
        if (prot == null) it.remove();
        else if (prot.equals(listener)) it.remove();
      }
    }
  }

  public List<MBOutcomeListenerProtocol> getOutcomeListeners()
  {
    synchronized (_outcomeListeners)
    {
      List<MBOutcomeListenerProtocol> list = new ArrayList<MBOutcomeListenerProtocol>(_outcomeListeners.size());
      for (Iterator<WeakReference<MBOutcomeListenerProtocol>> it = _outcomeListeners.iterator(); it.hasNext();)
      {
        WeakReference<MBOutcomeListenerProtocol> ref = it.next();
        MBOutcomeListenerProtocol prot = ref.get();
        if (prot == null) it.remove();
        else list.add(prot);
      }
      return list;
    }
  }
}
