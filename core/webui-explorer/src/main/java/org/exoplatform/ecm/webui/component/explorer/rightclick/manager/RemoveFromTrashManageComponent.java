package org.exoplatform.ecm.webui.component.explorer.rightclick.manager;

import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.VersionException;

import org.exoplatform.ecm.webui.component.explorer.UIConfirmMessage;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.control.filter.HasRemovePermissionFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsCheckedOutFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsInTrashFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotLockedFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotTrashHomeNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIWorkingAreaActionListener;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.actions.ActionServiceContainer;
import org.exoplatform.services.cms.folksonomy.NewFolksonomyService;
import org.exoplatform.services.cms.link.LinkUtils;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.cms.thumbnail.ThumbnailService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;
import org.exoplatform.webui.ext.manager.UIAbstractManager;
import org.exoplatform.webui.ext.manager.UIAbstractManagerComponent;

@ComponentConfig(
	events = {
	@EventConfig(listeners = RemoveFromTrashManageComponent.RemoveFromTrashActionListener.class)
   }
)

public class RemoveFromTrashManageComponent extends UIAbstractManagerComponent{
	
	private static final List<UIExtensionFilter> FILTERS
    = Arrays.asList(new UIExtensionFilter[] {  new IsInTrashFilter(),
                                               new IsNotLockedFilter(),
                                               new IsCheckedOutFilter(),
                                               new HasRemovePermissionFilter(),
                                               new IsNotTrashHomeNodeFilter() });

private final static Log                     LOG     = ExoLogger.getLogger(RestoreFromTrashManageComponent.class.getName());

@UIExtensionFilters
	public List<UIExtensionFilter> getFilters() {
	return FILTERS;
}
	
	public static void removeFromTrashManage(Event<? extends UIComponent> event) throws Exception {
		UIWorkingArea uiWorkingArea = event.getSource().getParent();
		UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
		String nodePath = event.getRequestContext().getRequestParameter(OBJECTID);
		UIPopupContainer UIPopupContainer = uiExplorer.getChild(UIPopupContainer.class);
		UIConfirmMessage uiConfirmMessage = uiWorkingArea.createUIComponent(UIConfirmMessage.class, null, null);
		UIApplication uiApp = uiExplorer.getAncestorOfType(UIApplication.class);
		
		//show message
		if(nodePath.indexOf(";") > -1) {   //delete multi
		 uiConfirmMessage.setMessageKey("UIWorkingArea.msg.confirm-delete-permanently-multi");
		 uiConfirmMessage.setArguments(new String[] {Integer.toString(nodePath.split(";").length)});
	   } else {    //delete one
		 uiConfirmMessage.setMessageKey("UIWorkingArea.msg.confirm-delete-permanently");
		 uiConfirmMessage.setArguments(new String[] {nodePath});
	   }
	uiConfirmMessage.setNodePath(nodePath);
	UIPopupContainer.activate(uiConfirmMessage, 500, 180);
	event.getRequestContext().addUIComponentToUpdateByAjax(UIPopupContainer);
	}
	
	 public void doRemoveFromTrash(String nodePath,Node node, String wsName, Event<?> event) throws Exception {
		UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
		DeleteManageComponent deleteManageComponent = new DeleteManageComponent();
		 if (nodePath.indexOf(";") > -1) {//Remove multi node
			 deleteManageComponent.processRemoveNode(nodePath, node, event, true);
		 }
		 else{
			 deleteManageComponent.processRemoveNode(nodePath, node, event, false);
		 }
		
		    uiExplorer.updateAjax(event);
		    uiExplorer.getSession().save();
	 }
	public static class RemoveFromTrashActionListener extends UIWorkingAreaActionListener<RemoveFromTrashManageComponent> {
	    public void processEvent(Event<RemoveFromTrashManageComponent> event) throws Exception {
	    	removeFromTrashManage(event);
	    }
	  }
	@Override
	public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
		// TODO Auto-generated method stub
		return null;
	}
	  

}
