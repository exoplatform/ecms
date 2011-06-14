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
package org.exoplatform.ecm.webui.component.explorer.versions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormSelectBox;

/**
 * Created by The eXo Platform SARL
 * Design : nqhungvn
 *          nguyenkequanghung@yahoo.com
 * Implement: lxchiati
 *            lebienthuy@gmail.com
 * July 3, 2006
 * 10:07:15 AM
 */

@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template =  "system:/groovy/webui/form/UIFormWithTitle.gtmpl",
    events = {
      @EventConfig(listeners = UIRemoveLabelForm.RemoveActionListener.class),
      @EventConfig(listeners = UIRemoveLabelForm.CancelActionListener.class)
    }
)

public class UIRemoveLabelForm extends UIForm {

  private static  String FIELD_LABEL = "label" ;

  public UIRemoveLabelForm() throws Exception {
    addUIFormInput(new UIFormSelectBox(FIELD_LABEL, FIELD_LABEL, null) ).setRendered(false) ;
  }

  public void update() throws Exception {
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>()  ;
    UIVersionInfo uiVersionInfo = getParent();
    VersionHistory versionHistory = uiVersionInfo.getCurrentNode().getVersionHistory() ;
    Version version = versionHistory.getVersion(uiVersionInfo.getCurrentVersionNode().getName());
    String[] strOptions = versionHistory.getVersionLabels(version);
    if (strOptions.length <= 0) this.setRendered(false) ;
    else {
      for (String temp: strOptions) {
        options.add(new SelectItemOption<String>(temp, temp ));
      }
      getChild(UIFormSelectBox.class).setOptions(options) ;
      setRendered(true);
    }
  }


  static  public class RemoveActionListener extends EventListener<UIRemoveLabelForm> {
    @SuppressWarnings("unchecked")
    public void execute(Event<UIRemoveLabelForm> event) throws Exception {
      UIRemoveLabelForm uiRemoveLabelForm = event.getSource();
      String label= uiRemoveLabelForm.getUIFormSelectBox(FIELD_LABEL).getValue() ;
      UIVersionInfo uiVersionInfo = uiRemoveLabelForm.getParent();
      VersionHistory versionHistory = uiVersionInfo.getCurrentNode().getVersionHistory() ;
      versionHistory.removeVersionLabel(label);
      UIFormSelectBox box = uiRemoveLabelForm.getChild(UIFormSelectBox.class);
      List<SelectItemOption<String>> listValue = box.getOptions() ;
      Iterator iter = listValue.iterator() ;
      while (iter.hasNext()) {
        SelectItemOption<String> item = (SelectItemOption<String>) iter.next() ;
        if(item.getValue().equals(label)) iter.remove() ;
      }
      uiRemoveLabelForm.update() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiRemoveLabelForm.getParent()) ;
    }
  }

  static  public class CancelActionListener extends EventListener<UIRemoveLabelForm> {
    public void execute(Event<UIRemoveLabelForm> event) throws Exception {
      UIRemoveLabelForm uiRemoveLabelForm = event.getSource();
      uiRemoveLabelForm.setRendered(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiRemoveLabelForm.getParent()) ;
    }
  }
}

