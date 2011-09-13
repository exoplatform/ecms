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
package org.exoplatform.ecm.webui.component.browsecontent;

import javax.jcr.Node;
import javax.jcr.lock.LockException;
import javax.jcr.version.VersionException;

import org.exoplatform.ecm.webui.utils.LockUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.voting.VotingService;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : pham tuan
 *          phamtuanchip@gmail.com
 * Jan 31, 2007
 */
@ComponentConfig(
    template = "app:/groovy/webui/component/UIVoteForm.gtmpl",
    events = {
        @EventConfig(listeners = UICBVoteForm.VoteActionListener.class),
        @EventConfig(listeners = UICBVoteForm.CancelActionListener.class)
    }
)

public class UICBVoteForm extends UIComponent implements UIPopupComponent {
  public UICBVoteForm() {}

  public Node getDocument() throws Exception {
    UIBrowseContentPortlet portlet = getAncestorOfType(UIBrowseContentPortlet.class) ;
    UIBrowseContainer uiBCContainer =  portlet.findFirstComponentOfType(UIBrowseContainer.class) ;
    UIDocumentDetail uiDocumentDetail = uiBCContainer.findFirstComponentOfType(UIDocumentDetail.class) ;
    return uiDocumentDetail.node_;
  }

  public double getRating() throws Exception {
    return  getDocument().getProperty("exo:votingRate").getDouble();
  }

  public void activate() throws Exception { }

  public void deActivate() throws Exception { }

  static  public class VoteActionListener extends EventListener<UICBVoteForm> {
    public void execute(Event<UICBVoteForm> event) throws Exception {
      UICBVoteForm uiForm = event.getSource() ;
      UIBrowseContentPortlet uiPortlet = uiForm.getAncestorOfType(UIBrowseContentPortlet.class) ;
      UIBrowseContainer uiBCContainer = uiPortlet.findFirstComponentOfType(UIBrowseContainer.class) ;
      UIDocumentDetail uiDocumentDetail = uiBCContainer.getChild(UIDocumentDetail.class) ;
      String userName = Util.getPortalRequestContext().getRemoteUser() ;
      long objId = Long.parseLong(event.getRequestContext().getRequestParameter(OBJECTID)) ;
      VotingService votingService = uiForm.getApplicationComponent(VotingService.class) ;
      String language = uiDocumentDetail.getLanguage() ;
      Node currentDoc = uiForm.getDocument() ;
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class) ;
      if(language == null && currentDoc.hasProperty(Utils.EXO_LANGUAGE)) {
        language = currentDoc.getProperty(Utils.EXO_LANGUAGE).getValue().getString() ;
      }
      String lockToken = LockUtil.getLockToken(currentDoc);
      if(lockToken != null) currentDoc.getSession().addLockToken(lockToken);
      try {
        votingService.vote(currentDoc, objId, userName, language) ;
      } catch (LockException le) {
        uiApp.addMessage(new ApplicationMessage("UICBVoteForm.msg.locked-doc", null,
            ApplicationMessage.WARNING)) ;
        
        return ;
      } catch (VersionException ve) {
        uiApp.addMessage(new ApplicationMessage("UICBVoteForm.msg.versioning-doc", null,
            ApplicationMessage.WARNING)) ;
        
        return ;
      } catch (Exception e) {
        uiApp.addMessage(new ApplicationMessage("UICBVoteForm.msg.error-vote", null,
            ApplicationMessage.WARNING)) ;
        
        return ;
      }
      UIPopupContainer uiPopupAction = uiForm.getAncestorOfType(UIPopupContainer.class) ;
      uiPopupAction.deActivate() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupAction) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiBCContainer) ;
    }
  }

  static  public class CancelActionListener extends EventListener<UICBVoteForm> {
    public void execute(Event<UICBVoteForm> event) throws Exception {
      event.getSource().getAncestorOfType(UIPopupContainer.class).cancelPopupAction() ;
    }
  }
}
