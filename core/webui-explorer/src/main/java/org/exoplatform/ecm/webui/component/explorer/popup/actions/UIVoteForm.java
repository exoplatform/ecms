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
package org.exoplatform.ecm.webui.component.explorer.popup.actions;

import javax.jcr.Node;

import org.exoplatform.ecm.webui.component.explorer.UIDocumentInfo;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.voting.VotingService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Tran The Trong
 *          trongtt@gmail.com
 * Jan 30, 2006
 * 10:45:01 AM
 */
@ComponentConfig(
    template = "app:/groovy/webui/component/UIVoteForm.gtmpl",
    events = {
        @EventConfig(listeners = UIVoteForm.VoteActionListener.class),
        @EventConfig(listeners = UIVoteForm.CancelActionListener.class)
    }
)
public class UIVoteForm extends UIComponent implements UIPopupComponent {
  public UIVoteForm() throws Exception {}

  public void activate() throws Exception {}
  public void deActivate() throws Exception {}

  public double getRating() throws Exception {
    VotingService votingService = WCMCoreUtils.getService(VotingService.class);
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
    UIDocumentInfo uiDocInfo = uiExplorer.findFirstComponentOfType(UIDocumentInfo.class);
    String currentUser = ConversationState.getCurrent().getIdentity().getUserId();
    return votingService.getVoteValueOfUser(uiExplorer.getCurrentNode(),
                                            currentUser,
                                            uiDocInfo.getLanguage());
  }

  static  public class VoteActionListener extends EventListener<UIVoteForm> {
    public void execute(Event<UIVoteForm> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class) ;
      String userName = Util.getPortalRequestContext().getRemoteUser() ;
      UIDocumentInfo uiDocumentInfo = uiExplorer.findFirstComponentOfType(UIDocumentInfo.class) ;
      Node currentNode = uiExplorer.getCurrentNode();
      uiExplorer.addLockToken(currentNode);
      String language = uiDocumentInfo.getLanguage() ;
      double objId = Double.parseDouble(event.getRequestContext().getRequestParameter(OBJECTID)) ;

      VotingService votingService = uiExplorer.getApplicationComponent(VotingService.class) ;
      votingService.vote(uiExplorer.getCurrentNode(), objId, userName, language) ;
      event.getSource().getAncestorOfType(UIPopupContainer.class).cancelPopupAction() ;
      uiExplorer.updateAjax(event) ;
    }
  }

  static  public class CancelActionListener extends EventListener<UIVoteForm> {
    public void execute(Event<UIVoteForm> event) throws Exception {
      event.getSource().getAncestorOfType(UIPopupContainer.class).cancelPopupAction() ;
    }
  }
}
