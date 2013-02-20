/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.ecm.webui.component.admin.drives;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.MissingResourceException;

import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.views.ManageViewService;
import org.exoplatform.services.cms.views.ViewConfig;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.exception.MessageException;
import org.exoplatform.webui.form.UIFormInputSet;
import org.exoplatform.webui.form.input.UICheckBoxInput;

/**
 * Created by The eXo Platform SARL
 * Author : Tran The Trong
 *          trongtt@gmail.com
 * Jun 28, 2006
 */
public class UIViewsInputSet extends UIFormInputSet {

  public UIViewsInputSet(String name) throws Exception {
    super(name);
  }

  public String getViewsSelected() throws Exception {
    StringBuilder selectedView = new StringBuilder() ;
    List<ViewConfig> views_ = getApplicationComponent(ManageViewService.class).getAllViews();
    for(ViewConfig view : views_){
      String viewName= view.getName() ;
      boolean checked = getUICheckBoxInput(viewName).isChecked() ;
      if(checked){
        if(selectedView.length() > 0) selectedView.append(", ") ;
        selectedView.append(viewName) ;
      }
    }
    if(selectedView.length() < 1 ) {
      throw new MessageException(new ApplicationMessage("UIDriveForm.msg.drive-views-invalid",
                                                        null, ApplicationMessage.WARNING)) ;
    }
    return selectedView.toString() ;
  }

  private void clear() throws Exception {
    
    List<ViewConfig> views_ = getApplicationComponent(ManageViewService.class).getAllViews();
    Collections.sort(views_, new ViewComparator());
    for(ViewConfig view : views_){
      String viewName = view.getName() ;
      if(getUICheckBoxInput(viewName) != null) {
        getUICheckBoxInput(viewName).setChecked(false) ;
      }else{
        addUIFormInput(new UICheckBoxInput(viewName, viewName, null)) ;
      }

    }
  }
  
  static public class ViewComparator implements Comparator<ViewConfig> {
    public int compare(ViewConfig v1, ViewConfig v2) throws ClassCastException {
      WebuiRequestContext webReqContext = WebuiRequestContext.getCurrentInstance();
      String displayName1, displayName2;
      try {
        displayName1 = webReqContext.getApplicationResourceBundle().getString("UIDriveForm.label." + v1.getName());  
      } catch(MissingResourceException mre) {
        displayName1 = v1.getName();
      }
      try {
        displayName2 = webReqContext.getApplicationResourceBundle().getString("UIDriveForm.label." + v2.getName());
      } catch(MissingResourceException mre) {
        displayName2 = v2.getName();
      }
      return displayName1.compareToIgnoreCase(displayName2);
    }
  }

  public void update(DriveData drive) throws Exception {
    clear() ;
    if( drive == null) return ;
    String views = drive.getViews() ;
    String[] array = views.split(",") ;
    for(String view: array){
      getUICheckBoxInput(view.trim()).setChecked(true) ;
    }
  }
}
