eXo.portal.UIControlWorkspace.showWorkspace = function() {
	var cws = eXo.portal.UIControlWorkspace ;
	var uiWorkspace = document.getElementById(this.id) ;
	var uiWorkspaceContainer = document.getElementById("UIWorkspaceContainer") ;
	var uiWorkspacePanel = document.getElementById("UIWorkspacePanel") ;
	var slidebar = document.getElementById("ControlWorkspaceSlidebar") ;
	var uiControlWorkspace = document.getElementById("UIControlWorkspace") ;
	if(cws.showControlWorkspace) {
		// hides the workspace
		cws.showControlWorkspace = false ;
		uiWorkspaceContainer.style.display = "none" ;
		slidebar.style.display = "block" ;
		eXo.portal.UIControlWorkspace.width = eXo.portal.UIControlWorkspace.slidebar.offsetWidth ;
		uiWorkspace.style.width = slidebar.offsetWidth + "px";
		eXo.portal.UIWorkingWorkspace.onResize(null, null) ;
	} else {
		cws.showControlWorkspace = true ;
		slidebar.style.display = "none" ;
		eXo.portal.UIControlWorkspace.width = cws.defaultWidth;
		uiWorkspace.style.width = cws.defaultWidth + "px" ;
		eXo.portal.UIWorkingWorkspace.onResize(null, null) ;
		uiWorkspaceContainer.style.display = "block" ;
		uiWorkspaceContainer.style.width = cws.defaultWidth + "px" ;
		uiWorkspacePanel.style.height = (eXo.portal.UIControlWorkspace.height - 
																		 eXo.portal.UIControlWorkspace.uiWorkspaceControl.offsetHeight - 23) + "px" ;
		/*23 is height of User Workspace Title*/

		eXo.webui.UIVerticalScroller.init();
		eXo.portal.UIPortalControl.fixHeight();
	}
	
	/* Reorganize opened windows */
//	eXo.portal.UIWorkingWorkspace.reorganizeWindows(this.showControlWorkspace);
	/* Resize Dockbar */
	var uiPageDesktop = document.getElementById("UIPageDesktop") ;
	if(uiPageDesktop) eXo.desktop.UIDockbar.resizeDockBar() ;
	/* Resizes the scrollable containers */
	eXo.portal.UIPortalControl.initAllManagers();
	
	/* BEGIN - Check positon of widgets in order to avoid hide widgets when we expand/collapse workspace*/
	if(uiPageDesktop) {
		var DOMUtil = eXo.core.DOMUtil ;
		var uiWidget = DOMUtil.findChildrenByClass(uiPageDesktop, "div", "UIWidget") ;
		var uiControlWorkspace = document.getElementById("UIControlWorkspace") ;
		var size = uiWidget.length ;
		var limitX = 50 ;
		for(var i = 0 ; i < size ; i ++) {
			var dragObject = uiWidget[i] ;
			if (cws.showControlWorkspace) {
				dragObject.style.left = (dragObject.offsetLeft - uiControlWorkspace.offsetWidth) + "px";				
			}
			else {				
				dragObject.style.left = (dragObject.offsetLeft + uiControlWorkspace.offsetWidth + dragObject.offsetWidth) + "px";				
			}
			var offsetHeight = uiPageDesktop.offsetHeight - dragObject.offsetHeight  - limitX;
	  	var offsetTop = dragObject.offsetTop ;
	  	var offsetWidth = uiPageDesktop.offsetWidth - dragObject.offsetWidth - limitX ;
	  	var offsetLeft = dragObject.offsetLeft ;
	  	
	  	if (dragObject.offsetLeft < 0) dragObject.style.left = "0px" ;
	  	if (dragObject.offsetTop < 0) dragObject.style.top = "0px" ;
	  	if (offsetTop > offsetHeight) dragObject.style.top = (offsetHeight + limitX) + "px" ;
	  	if (offsetLeft > offsetWidth) dragObject.style.left = (offsetWidth + limitX) + "px" ;				
		}		
		
		//fix for UIGadget by Pham Dinh Tan
		var uiGadgets = DOMUtil.findChildrenByClass(uiPageDesktop, "div", "UIGadget") ;
		var limitXGadget = 80;
		for(var i = 0 ; i < uiGadgets.length; i++) {
			var dragObject = uiGadgets[i] ;
			if (cws.showControlWorkspace) {
				dragObject.style.left = (parseInt(dragObject.style.left) - uiControlWorkspace.offsetWidth) + "px";	
			}
			else {
				dragObject.style.left = (parseInt(dragObject.style.left) + uiControlWorkspace.offsetWidth + dragObject.offsetWidth - limitXGadget) + "px";			
			}
			
			var offsetHeight = uiPageDesktop.offsetHeight - dragObject.offsetHeight ;
			var offsetWidth = uiPageDesktop.offsetWidth - dragObject.offsetWidth ;
			var dragPosX = parseInt(dragObject.style.left);
			var dragPosY = parseInt(dragObject.style.top);
			if (dragPosX < 0) dragObject.style.left = "0px" ;
	  	if (dragPosY < 0) dragObject.style.top = "0px" ;
	  	if (dragPosY > offsetHeight) dragObject.style.top = offsetHeight + "px" ;
	  	if (dragPosX > offsetWidth) dragObject.style.left = offsetWidth + "px" ;			
		}		
	}
	
	// fix for DropDropList bug in IE by Pham Dinh Tan  
	var dropDownAnchors = eXo.core.DOMUtil.findDescendantsByClass(document, "div", "UIDropDownAnchor");
	for(var i = 0; i < dropDownAnchors.length; i++) {
		if(dropDownAnchors[i].style.display != "none") {
			dropDownAnchors[i].style.display = "none";
			dropDownAnchors[i].style.visibility = "hidden";
		}
	}
	//fix bug position of popup windows PORTAL-2690
	var popupWindows = eXo.core.DOMUtil.findDescendantsByClass(document, "div", "UIPopupWindow") ;
	for(var i = 0; i < popupWindows.length; i++) {
		if(popupWindows[i].style.display != "none") {
			eXo.webui.UIPopupWindow.show(popupWindows[i], popupWindows[i].isShowMask) ;
		}
	}
	eXo.ecm.ECMUtils.checkAvailableSpace();
	/* -- END -- */
	var params = [ {name: "objectId", value : cws.showControlWorkspace} ] ;
	ajaxAsyncGetRequest(eXo.env.server.createPortalURL(this.id, "SetVisible", true, params), false) ;
};