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
package org.exoplatform.ecm.webui.component.explorer.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.Value;
import javax.jcr.query.QueryResult;

import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.validator.MandatoryValidator;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * May 6, 2007
 * 10:18:56 AM
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template =  "app:/groovy/webui/component/explorer/search/UICompareExactlyForm.gtmpl",
    events = {
      @EventConfig(listeners = UICompareExactlyForm.SelectActionListener.class),
      @EventConfig(phase=Phase.DECODE, listeners = UICompareExactlyForm.CancelActionListener.class)
    }
)
public class UICompareExactlyForm extends UIForm implements UIPopupComponent {
  private static final String FILTER = "filter" ;
  private static final String RESULT = "result";
  private static final String TEMP_RESULT = "tempSel";
  private List<String> listValue_ ;

  public UICompareExactlyForm() throws Exception {}

  public void activate() throws Exception {}
  public void deActivate() throws Exception {}

  public void init(String properties, QueryResult result) throws Exception {
    listValue_ = new ArrayList<String>() ;
    List<SelectItemOption<String>> opts = new ArrayList<SelectItemOption<String>>();
    addUIFormInput(new UIFormStringInput(FILTER, FILTER, null)) ;
    addUIFormInput(new UIFormSelectBox(RESULT, RESULT, opts).setSize(15).addValidator(MandatoryValidator.class)) ;
    addUIFormInput(new UIFormSelectBox(TEMP_RESULT, TEMP_RESULT, opts)) ;

    NodeIterator iter = result.getNodes() ;
    String[] props = {} ;
    if(properties.indexOf(",") > -1) props = properties.split(",") ;
    while(iter.hasNext()) {
      Node node = iter.nextNode() ;
      if(props.length > 0) {
        for(String pro : props) {
          if(node.hasProperty(pro)) {
            Property property = node.getProperty(pro) ;
            setPropertyResult(property) ;
          }
        }
      } else {
        if(node.hasProperty(properties)) {
          Property property = node.getProperty(properties) ;
          setPropertyResult(property) ;
        }
      }
    }
    Collections.sort(listValue_) ;
    for(String value : listValue_) {
      opts.add(new SelectItemOption<String>(value, value)) ;
    }
  }

  public void setPropertyResult(Property property) throws Exception {
    if(property.getDefinition().isMultiple()) {
      Value[] values = property.getValues() ;
      for(Value value : values) {
        if(!listValue_.contains(value.getString())) listValue_.add(value.getString()) ;
      }
    } else {
      Value value = property.getValue() ;
      if(!listValue_.contains(value.getString())) listValue_.add(value.getString()) ;
    }
  }

  static  public class CancelActionListener extends EventListener<UICompareExactlyForm> {
    public void execute(Event<UICompareExactlyForm> event) throws Exception {
      UISearchContainer uiSearchContainer = event.getSource().getAncestorOfType(UISearchContainer.class) ;
      UIPopupContainer uiPopup = uiSearchContainer.getChild(UIPopupContainer.class) ;
      uiPopup.deActivate() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopup) ;
    }
  }

  static  public class SelectActionListener extends EventListener<UICompareExactlyForm> {
    public void execute(Event<UICompareExactlyForm> event) throws Exception {
      UICompareExactlyForm uiForm = event.getSource() ;
      String value = uiForm.getUIFormSelectBox(RESULT).getValue();
      UIPopupContainer uiPopupAction = uiForm.getAncestorOfType(UIPopupContainer.class);
      UISearchContainer uiSearchContainer = uiPopupAction.getParent() ;
      UIConstraintsForm uiConstraintsForm =
        uiSearchContainer.findFirstComponentOfType(UIConstraintsForm.class) ;
      uiConstraintsForm.getUIStringInput(UIConstraintsForm.CONTAIN_EXACTLY).setValue(value) ;
      uiPopupAction.deActivate() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupAction) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiConstraintsForm) ;
    }
  }
}
