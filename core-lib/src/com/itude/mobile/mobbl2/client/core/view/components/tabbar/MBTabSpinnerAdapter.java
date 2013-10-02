/*
 * (C) Copyright ItudeMobile.
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
package com.itude.mobile.mobbl2.client.core.view.components.tabbar;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.widget.ArrayAdapter;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class MBTabSpinnerAdapter extends ArrayAdapter<CharSequence>
{
  private int _selectedElement = 0;

  public MBTabSpinnerAdapter(Context context, int textViewResourceId)
  {
    super(context, textViewResourceId);
  }

  public void setSelectedElement(int selectedElement)
  {
    _selectedElement = selectedElement;
  }

  public int getSelectedElement()
  {
    return _selectedElement;
  }

}
