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
package com.itude.mobile.mobbl.core.view.components.tabbar;

import java.lang.reflect.Field;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.SearchView;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.itude.mobile.android.util.log.MBLog;
import com.itude.mobile.mobbl.core.util.Constants;
import com.itude.mobile.mobbl.core.view.builders.MBViewBuilderFactory;

public class MBPhoneActionBarBuilder extends MBDefaultActionBarBuilder
{

  public MBPhoneActionBarBuilder(Context context)
  {
    super(context);
  }

  @Override
  protected void changeSearchImage(Drawable image, SearchView searchView)
  {
    if (image == null)
    {
      return;
    }

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

      setSearchImage(image, searchLayout);

      LinearLayout searchPlate = (LinearLayout) searchLayout.getChildAt(1);
      MBViewBuilderFactory.getInstance().getStyleHandler().styleSearchPlate(searchPlate);

      LinearLayout submitArea = (LinearLayout) searchLayout.getChildAt(2);
      MBViewBuilderFactory.getInstance().getStyleHandler().styleSearchSubmitArea(submitArea);
    }
    catch (Exception e)
    {
      MBLog.e(Constants.APPLICATION_NAME, "error changing searchbutton icon", e);
    }

  }

}
