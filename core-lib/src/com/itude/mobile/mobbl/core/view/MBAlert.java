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
package com.itude.mobile.mobbl.core.view;

import android.app.AlertDialog;

import com.itude.mobile.mobbl.core.configuration.MBDefinition;
import com.itude.mobile.mobbl.core.configuration.mvc.MBAlertDefinition;
import com.itude.mobile.mobbl.core.model.MBDocument;
import com.itude.mobile.mobbl.core.services.MBLocalizationService;
import com.itude.mobile.mobbl.core.view.builders.MBViewBuilderFactory;

public class MBAlert extends MBComponentContainer
{

  private String _alertName;
  private String _rootPath;
  private String _title;

  public MBAlert(MBAlertDefinition definition, MBDocument document, String rootPath)
  {
    super(definition, document, null);
    setRootPath(rootPath);
    setAlertName(definition.getName());
    setTitle(definition.getTitle());

    // Ok, now we can build the children
    buildChildren(definition, document, getParent());
  }

  final private void buildChildren(MBAlertDefinition definition, MBDocument document, MBComponentContainer parent)
  {
    for (MBDefinition def : definition.getChildren())
    {
      String parentAbsoluteDataPath = null;
      if (parent != null)
      {
        parentAbsoluteDataPath = parent.getAbsoluteDataPath();
      }

      if (def.isPreConditionValid(document, parentAbsoluteDataPath))
      {
        addChild(MBComponentFactory.getComponentFromDefinition(def, document, this));
      }
    }
  }

  public AlertDialog buildAlertDialog()
  {
    return MBViewBuilderFactory.getInstance().getAlertViewBuilder().buildAlertDialog(this);
  }

  public String getAlertName()
  {
    return _alertName;
  }

  public void setAlertName(String _alertName)
  {
    this._alertName = _alertName;
  }

  public String getRootPath()
  {
    return _rootPath;
  }

  public void setRootPath(String _rootPath)
  {
    this._rootPath = _rootPath;
  }

  public String getTitle()
  {
    String result = _title;

    if (_title != null)
    {
      result = _title;
    }
    else
    {
      MBAlertDefinition definition = (MBAlertDefinition) getDefinition();
      if (definition.getTitle() != null)
      {
        result = definition.getTitle();
      }
      else if (definition.getTitlePath() != null)
      {
        String path = definition.getTitlePath();
        if (!path.startsWith("/"))
        {
          if (getRootPath() != null)
          {
            path = getRootPath() + "/" + path;
          }
          else
          {
            path = getAbsoluteDataPath() + "/" + path;
          }
        }

        result = (String) getDocument().getValueForPath(path);
      }
    }

    return MBLocalizationService.getInstance().getTextForKey(result);
  }

  public void setTitle(String _title)
  {
    this._title = _title;
  }

}
