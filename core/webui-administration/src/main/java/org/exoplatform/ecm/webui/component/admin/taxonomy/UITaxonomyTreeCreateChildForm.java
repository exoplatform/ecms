/***************************************************************************
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
 *
 **************************************************************************/
package org.exoplatform.ecm.webui.component.admin.taxonomy;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.ecm.webui.form.validator.ECMNameValidator;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.cms.taxonomy.TaxonomyTreeData;
import org.exoplatform.services.cms.taxonomy.impl.TaxonomyNodeAlreadyExistsException;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.util.Text;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormInputInfo;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.validator.MandatoryValidator;

/**
 * Created by The eXo Platform SARL
 * Author : Hoang Van Hung
 *          hunghvit@gmail.com
 * Apr 15, 2009
 */

@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template =  "system:/groovy/webui/form/UIForm.gtmpl",
    events = {
      @EventConfig(listeners = UITaxonomyTreeCreateChildForm.SaveActionListener.class),
      @EventConfig(listeners = UITaxonomyTreeCreateChildForm.CancelActionListener.class, phase = Phase.DECODE)
    }
)

public class UITaxonomyTreeCreateChildForm extends UIForm {

  final static private String FIELD_PARENT = "parentPath";

  final static private String FIELD_NAME   = "taxonomyName";

  public UITaxonomyTreeCreateChildForm() throws Exception {
    addUIFormInput(new UIFormInputInfo(FIELD_PARENT, FIELD_PARENT, null));
    addUIFormInput(new UIFormStringInput(FIELD_NAME, FIELD_NAME, null).addValidator(MandatoryValidator.class).addValidator(ECMNameValidator.class));
  }

  public void setParent(String path) {
    getUIFormInputInfo(FIELD_PARENT).setValue(path);
    getUIStringInput(FIELD_NAME).setValue(null);
  }

  public static class SaveActionListener extends EventListener<UITaxonomyTreeCreateChildForm> {
    public void execute(Event<UITaxonomyTreeCreateChildForm> event) throws Exception {
      UITaxonomyTreeCreateChildForm uiForm = event.getSource();
      UITaxonomyTreeCreateChild uiCreateChild = uiForm.getAncestorOfType(UITaxonomyTreeCreateChild.class);
      UITaxonomyTreeContainer uiTaxonomyTreeContainer = uiForm.getAncestorOfType(UITaxonomyTreeContainer.class);
      UITaxonomyManagerTrees uiTaxonomyManageTrees = uiForm.getAncestorOfType(UITaxonomyManagerTrees.class);
      TaxonomyTreeData taxoTreeData = uiTaxonomyTreeContainer.getTaxonomyTreeData();
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class);
      String title = uiForm.getUIStringInput(FIELD_NAME).getValue();
      
      String name = Utils.cleanString(title);
      
      if (name == null || name.length() == 0) {
        uiApp.addMessage(new ApplicationMessage("UITaxonomyTreeCreateChildForm.msg.name-null",
            null, ApplicationMessage.WARNING));
        
        return;
      }

      try {
        TaxonomyService taxonomyService = uiForm.getApplicationComponent(TaxonomyService.class);
        if (title.length() > Integer.parseInt(taxonomyService.getCategoryNameLength())) {
          Object[] args = { taxonomyService.getCategoryNameLength() };
          uiApp.addMessage(new ApplicationMessage("UITaxonomyTreeCreateChildForm.msg.name-too-long",
                                                  args,
                                                  ApplicationMessage.WARNING));

          return;
        }
        String parentPath = uiForm.getUIFormInputInfo(FIELD_PARENT).getValue();
        taxonomyService.addTaxonomyNode(taxoTreeData
            .getTaxoTreeWorkspace(), parentPath, Text.escapeIllegalJcrChars(name), Util.getPortalRequestContext().getRemoteUser());
        
        SessionProvider p = WCMCoreUtils.getSystemSessionProvider();
        Session session = p.getSession(taxoTreeData.getTaxoTreeWorkspace(), WCMCoreUtils.getRepository());
        Node newNode = (Node)session.getItem(parentPath + "/" + name); 
        
        if (newNode.canAddMixin("exo:rss-enable")) {
          newNode.addMixin("exo:rss-enable");
          newNode.setProperty("exo:title", title);
        }
        newNode.save();
        
        uiCreateChild.update();
      } catch (TaxonomyNodeAlreadyExistsException e) {
        Object[] arg = { name };
        uiApp.addMessage(new ApplicationMessage("UITaxonomyTreeCreateChildForm.msg.exist", arg,
            ApplicationMessage.WARNING));
        
        return;
      } catch (RepositoryException e) {
        Object[] arg = { name };
        uiApp.addMessage(new ApplicationMessage("UITaxonomyTreeCreateChildForm.msg.error", arg,
            ApplicationMessage.WARNING));
        
        return;
      }
      uiForm.reset();
      UIPopupWindow uiPopup = uiForm.getParent();
      uiPopup.setRendered(false);
      uiPopup.setShow(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiTaxonomyManageTrees);
    }
  }

  public static class CancelActionListener extends EventListener<UITaxonomyTreeCreateChildForm> {
    public void execute(Event<UITaxonomyTreeCreateChildForm> event) throws Exception {
      UITaxonomyTreeCreateChildForm uiForm = event.getSource();
      UIPopupWindow uiPopup = uiForm.getParent();
      uiPopup.setRendered(false);
      uiPopup.setShow(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopup);
    }
  }
}
