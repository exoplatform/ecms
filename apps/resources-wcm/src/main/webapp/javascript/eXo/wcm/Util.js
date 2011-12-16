window.wcm = function() {}
wcm.insertCSSFromTextArea2FCK = function(Instance, ContentCSS) {
	if (!Instance) return;
	var eContentCSS = document.getElementById(ContentCSS);
	var sContentCSSId = ContentCSS + "_Inline";
	var count = 1;
	eContentCSS.onblur = updateStyle;
	
	function updateStyle() {
		var sValue = eContentCSS.value;
		if(!sValue)	return;
		var iDoc = FCKeditorAPI.Instances[Instance].EditorWindow.document;
		var eHead = iDoc.getElementsByTagName("head")[0];
		var eStyle = iDoc.getElementById(sContentCSSId);
		if (eStyle) {
			eHead.removeChild(eStyle);
		}
		eStyle = iDoc.createElement("style");
		eStyle.setAttribute("type", "text/css");
		eStyle.setAttribute("id", sContentCSSId);
		if (eXo.core.Browser.isFF()) { //for FF			
			eStyle.innerHTML = sValue;
		} else {
			eStyle.styleSheet.cssText = sValue;
		}
		eHead.appendChild(eStyle);
	};
	
	(function checkFCKEditorAPI() {
		if (count <= 5) {
			try {
				updateStyle();
				if (updateStyle.time) {
					clearTimeout(updateStyle.time);
					updateStyle.time = null;
				}
			} catch(e) {
				count++;
				updateStyle.time = setTimeout(checkFCKEditorAPI, 500);
			}
		}
	})();
}

Utils = function(){
	Utils.prototype.removeQuickeditingBlock = function(portletID, quickEditingBlockId) {
		var presentation = document.getElementById(portletID);
		var pNode = presentation.parentNode;
		var quickEditingBlock = document.getElementById(quickEditingBlockId);
		if(quickEditingBlock != null) {
			pNode.removeChild(quickEditingBlock);
		}
	};
		
	Utils.prototype.insertQuickeditingBlock = function(portletID, quickEditingBlockId) {
		var presentation = document.getElementById(portletID);		
		var parentNode = presentation.parentNode;
		var fistChild = eXo.core.DOMUtil.getChildrenByTagName(parentNode, "div")[0];
		if (fistChild.id == quickEditingBlockId) {
			var quickEditingBlock = document.getElementById(quickEditingBlockId);
			quickEditingBlock.parentNode.removeChild(quickEditingBlock);
		}
		var quickEditingBlock = document.getElementById(quickEditingBlockId);		
		if(quickEditingBlock != null) {
			if(eXo.core.Browser.browserType == "ie") {
				var portalName = eXo.env.portal.portalName;
				if(portalName != "classic") {
					if(portletID == (portalName+"-signin")) quickEditingBlock.style.left = presentation.offsetWidth + quickEditingBlock.offsetWidth + 'px';
				} else {
					if(portletID == (portalName+"-logo") || portletID == (portalName+"-signin")) {
						quickEditingBlock.style.left = presentation.offsetWidth + quickEditingBlock.offsetWidth + 'px';
					}
				}
			}
			parentNode.insertBefore(quickEditingBlock, presentation);
		}
	};
}
eXo.wcm = new Utils();

function showObject(obj) {
	var element = eXo.core.DOMUtil.findNextElementByTagName(obj, "div");
	if (!element.style.display || element.style.display == 'none') {
		element.style.display = 'block';
	} else {
		element.style.display = 'none';
	}
}

function getHostName() {
	var parentLocation = window.parent.location;
	return parentLocation.href.substring(0, parentLocation.href.indexOf(parentLocation.pathname));
}

function getRuntimeContextPath() {
	return getHostName() + eXo.env.portal.context + '/' + eXo.env.portal.portalName + '/';
}
/*--------------------------------------SEARCH------------------------------------*/
function getKeynum(event) {
  var keynum = false ;
  if(window.event) { /* IE */
    keynum = window.event.keyCode;
    event = window.event ;
  } else if(event.which) { /* Netscape/Firefox/Opera */
    keynum = event.which ;
  }
  if(keynum == 0) {
    keynum = event.keyCode ;
  }
  return keynum ;
}

function quickSearch(resultPageURI) {
	var searchBox = document.getElementById("siteSearchBox");
	var keyWordInput = eXo.core.DOMUtil.findFirstDescendantByClass(searchBox, "input", "keyword");
	var keyword = encodeURI(keyWordInput.value);
	var resultPageURIDefault = "searchResult";
	var params = "portal=" + eXo.env.portal.portalName + "&keyword=" + keyword;
	var baseURI = getHostName() + eXo.env.portal.context + "/" + eXo.env.portal.portalName; 
	if (resultPageURI != undefined) {
		baseURI = baseURI + "/" + resultPageURI; 
	} else {
		baseURI = baseURI + "/" + resultPageURIDefault;  
	}
	window.location = baseURI + "?" + params;
}

function quickSearchOnEnter(event, resultPageURI) {
  var keyNum = getKeynum(event);
  if (keyNum == 13) {
    quickSearch(resultPageURI);
  }
}

function search(comId) {
	var searchForm = document.getElementById(comId);
	var inputKey = eXo.core.DOMUtil.findDescendantById(searchForm, "keywordInput");
	searchForm.onsubmit = function() {return false;};
	inputKey.onkeypress = function(event) {
		var keyNum = getKeynum(event);
		if (keyNum == 13) {
			var searchButton = eXo.core.DOMUtil.findFirstDescendantByClass(this.form, "div", "SearchButton");
			searchButton.onclick();
  	 }		
	}
}	

function keepKeywordOnBoxSearch() {
	var queryRegex = /^portal=[\w%]+&keyword=[\w%]+/;
	var searchBox = document.getElementById("siteSearchBox");
	var keyWordInput = eXo.core.DOMUtil.findFirstDescendantByClass(searchBox, "input", "keyword");
	var queryString = location.search.substring(1);
	if (!queryString.match(queryRegex)) {return;}
	var portalParam = queryString.split('&')[0];
	var keyword = decodeURI(queryString.substring((portalParam + "keyword=").length +1));
	if (keyword != undefined && keyword.length != 0) {
		keyWordInput.value = unescape(keyword); 
	}
}

eXo.core.Browser.addOnLoadCallback("keepKeywordOnBoxSearch", keepKeywordOnBoxSearch);

/*------------------Overrite method eXo.webui.UIPopup.init to show popup display center-------------------------------*/
UIPopupWindow.prototype.init = function(popupId, isShow, isResizable, showCloseButton, isShowMask) {XoWork
	var DOMUtil = eXo.core.DOMUtil ;
	this.superClass = eXo.webui.UIPopup ;
	var popup = document.getElementById(popupId) ;
	var portalApp = document.getElementById("UIPortalApplication") ;
	if(popup == null) return;
	popup.style.visibility = "hidden" ;
	if(!isShowMask) isShowMask = false; 
	popup.isShowMask = isShowMask ;
	
	//TODO Lambkin: this statement create a bug in select box component in Firefox
	//this.superClass.init(popup) ;
	var contentBlock = DOMUtil.findFirstDescendantByClass(popup, 'div' ,'PopupContent');
	if((eXo.core.Browser.getBrowserHeight() - 100 ) < contentBlock.offsetHeight) {
		contentBlock.style.height = (eXo.core.Browser.getBrowserHeight() - 100) + "px";
	}
	var popupBar = DOMUtil.findFirstDescendantByClass(popup, 'div' ,'PopupTitle') ;

	popupBar.onmousedown = this.initDND;
	popupBar.onkeydown = this.initDND;
	
	if(isShow == false) {
		this.superClass.hide(popup) ;
		if(isShowMask) eXo.webui.UIPopupWindow.showMask(popup, false) ;
	} 
	
	if(isResizable) {
		var resizeBtn = DOMUtil.findFirstDescendantByClass(popup, "div", "ResizeButton");
		resizeBtn.style.display = 'block';
		resizeBtn.onmousedown = this.startResizeEvt;
		resizeBtn.onkeydown = this.startResizeEvt;
		portalApp.onmouseup = this.endResizeEvt;
	}
	
	popup.style.visibility = "hidden" ;
	if(isShow == true) {
		var iframes = DOMUtil.findDescendantsByTagName(popup, "iframe") ;
		if(iframes.length > 0) {
			setTimeout("eXo.webui.UIPopupWindow.show('" + popupId + "'," + isShowMask + ")", 500) ;
		} else {
		if(popup.offsetHeight == 0){
			setTimeout("eXo.webui.UIPopupWindow.show('" + popupId + "'," + isShowMask + ")", 500) ;
			return ;
		}
			this.show(popup, isShowMask) ;
		}
	}
} ;
/*----------------------------------------------End of overrite-------------------------------------------------------*/
/*----------------------------------------------Begin overite UIWorkSpace---------------------------------------------*/
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
		}
	}
	
	var popupWindows = eXo.core.DOMUtil.findDescendantsByClass(document, "div", "UIPopupWindow") ;
	for(var i = 0; i < popupWindows.length; i++) {
		if(popupWindows[i].style.display != "none") {
			eXo.webui.UIPopupWindow.show(popupWindows[i], popupWindows[i].isShowMask) ;
		}
	}
	
	/* -- END -- */
	var params = [ {name: "objectId", value : cws.showControlWorkspace} ] ;
	ajaxAsyncGetRequest(eXo.env.server.createPortalURL(this.id, "SetVisible", true, params), false) ;
};
/*----------------------------------------------End  overite UIWorkSpace---------------------------------------------*/
function initCheckedRadio(id) {
	eXo.core.Browser.chkRadioId = id;
};

function initCondition(formid){
	var formElement = document.getElementById(formid);
	var radioboxes = [];
	for(var i=0; i < formElement.elements.length;i++){
		if(formElement.elements[i].type=="radio") radioboxes.push(formElement.elements[i]);
	}
	var i = radioboxes.length;
	while(i--){
		radioboxes[i].onclick = chooseCondition;
	}
	if(eXo.core.Browser.chkRadioId && eXo.core.Browser.chkRadioId != "null"){
		var selectedRadio = document.getElementById(eXo.core.Browser.chkRadioId);
	} else{		
		var selectedRadio = radioboxes[0];
	}
	var itemSelectedContainer = eXo.core.DOMUtil.findAncestorByClass(selectedRadio,"ContentSearchForm");
	var itemContainers = eXo.core.DOMUtil.findDescendantsByClass(selectedRadio.form,"div","ContentSearchForm");
	for(var i=1;i<itemContainers.length;i++){
		setCondition(itemContainers[i],true);
	}
	enableCondition(itemSelectedContainer);
}

function chooseCondition() {
	var me = this;
	var hiddenField = eXo.core.DOMUtil.findFirstDescendantByClass(me.form,"input","hidden");
	hiddenField.value = me.id;
	var itemSelectedContainer = eXo.core.DOMUtil.findAncestorByClass(me,"ContentSearchForm");
	var itemContainers = eXo.core.DOMUtil.findDescendantsByClass(me.form,"div","ContentSearchForm");
	for(var i=1;i<itemContainers.length;i++){
		setCondition(itemContainers[i],true);
	}
	enableCondition(itemSelectedContainer);
	window.wcm.lastCondition = itemSelectedContainer; 
};

function enableCondition(itemContainer) {
	if(window.wcm.lastCondition) setCondition(window.wcm.lastCondition,true);
	setCondition(itemContainer,false);
};

function setCondition(itemContainer,state) {
	var domUtil = eXo.core.DOMUtil;
	var action = domUtil.findDescendantsByTagName(itemContainer,"img");
	if(action && (action.length > 0)){
		for(var i = 0; i < action.length; i++){
			if(state) {
				action[i].style.visibility = "hidden";
			}	else {
				action[i].style.visibility = "";	
			}	
		}
	}
	var action = domUtil.findDescendantsByTagName(itemContainer,"input");
	if(action && (action.length > 0)){
		for(i = 0; i < action.length; i++){
			if(action[i].type != "radio") action[i].disabled = state;
		}
	}
	var action = domUtil.findDescendantsByTagName(itemContainer,"select");
	if(action && (action.length > 0)){
		for(i = 0; i < action.length; i++){
			action[i].disabled = state;
		}
	}
};
function removeCondition() {
	
};

function setHiddenValue() {
	var inputHidden = document.getElementById("checkedRadioId");
	if(eXo.core.Browser.chkRadioId == "null") {
		inputHidden.value = "name";
		document.getElementById("name").checked = true;
	} else {
		inputHidden.value = eXo.core.Browser.chkRadioId; 
		document.getElementById(eXo.core.Browser.chkRadioId).checked = true;
	}
}

function showHideOrderBy() {
	var formObj = document.getElementById('UIViewerManagementForm');
	var viewerModeObj = formObj['ViewerMode'];
	var orderXXX = eXo.core.DOMUtil.findDescendantsByClass(formObj, 'tr', 'OrderBlock');			
	viewerModeObj[0].onclick = function() {
		for (var i = 0; i < orderXXX.length; i++) {
			orderXXX[i].style.display = '';
		}
	}
	viewerModeObj[1].onclick = function() {
		for (var i = 0; i < orderXXX.length; i++) {
			orderXXX[i].style.display = 'none';
		}
	}
}  


function showPopupMenu(obj) {
	if(!obj) return;
	var uiNavi = document.getElementById('PortalNavigationTopContainer');
	
	// Todo fix bug show menu popup appears under Navigation
	// Will remove when add javascript for navagation ok
	var uiACMENavi = document.getElementById('navigation-generator');
	var uiWCMNavigationPortlet = eXo.core.DOMUtil.findFirstDescendantByClass(uiACMENavi, "div", "UIWCMNavigationPortlet");
	if(eXo.core.Browser.browserType == 'ie')  {
		if(uiNavi) uiNavi.style.position = "static";
		if(uiWCMNavigationPortlet) uiWCMNavigationPortlet.style.position = "static";
	}
	if(obj.Timeout) clearTimeout(obj.Timeout);
	var DOMUtil = eXo.core.DOMUtil;
	var mnuItemContainer = DOMUtil.findNextElementByTagName(obj, "div");
	var objParent = DOMUtil.findAncestorByClass(obj, "TBItem");
	if(mnuItemContainer && mnuItemContainer.style.display != "block") {
		mnuItemContainer.style.display = 'block';
		mnuItemContainer.style.width = mnuItemContainer.offsetWidth - parseInt(DOMUtil.getStyle(mnuItemContainer, "borderLeftWidth")) - parseInt(DOMUtil.getStyle(mnuItemContainer, "borderRightWidth")) + 'px';
		objParent.className = 'TBItemHover';
		mnuItemContainer.onmouseout = function(){
			if(eXo.core.Browser.browserType == 'ie')  {
			 if(uiNavi) uiNavi.style.position = "relative";
			 if(uiWCMNavigationPortlet) uiWCMNavigationPortlet.style.position = "relative";
			}
			obj.Timeout = setTimeout(function() {
				mnuItemContainer.style.display = 'none';
				objParent.className = 'TBItem';
				mnuItemContainer.onmouseover = null;
				mnuItemContainer.onmouseout = null;
        mnuItemContainer.onfocus = null;
        mnuItemContainer.onblur = null;				
			},1*10);
		}
		
    mnuItemContainer.onblur = function(){
      if(eXo.core.Browser.browserType == 'ie')  {
       if(uiNavi) uiNavi.style.position = "relative";
       if(uiWCMNavigationPortlet) uiWCMNavigationPortlet.style.position = "relative";
      }
      obj.Timeout = setTimeout(function() {
        mnuItemContainer.style.display = 'none';
        objParent.className = 'TBItem';
        mnuItemContainer.onmouseover = null;
        mnuItemContainer.onmouseout = null;
        mnuItemContainer.onfocus = null;
        mnuItemContainer.onblur = null;     
      },1*10);
    }		

		mnuItemContainer.onmouseover = function() {
			objParent.className = 'TBItemHover';
			if(eXo.core.Browser.browserType == 'ie')  {
				if(uiNavi) uiNavi.style.position = "static";
				if(uiWCMNavigationPortlet) uiWCMNavigationPortlet.style.position = "static";
			}
			if(obj.Timeout) clearTimeout(obj.Timeout);
			obj.Timeout = null;
		}
		
    mnuItemContainer.onfocus = function() {
      objParent.className = 'TBItemHover';
      if(eXo.core.Browser.browserType == 'ie')  {
        if(uiNavi) uiNavi.style.position = "static";
        if(uiWCMNavigationPortlet) uiWCMNavigationPortlet.style.position = "static";
      }
      if(obj.Timeout) clearTimeout(obj.Timeout);
      obj.Timeout = null;
    }
    		
		obj.onmouseout = mnuItemContainer.onmouseout;
		obj.onblur = mnuItemContainer.onblur;
	}
}		

function showPopupSubMenu(obj) {
	if(!obj) return;
	if(obj.Timeout) clearTimeout(obj.Timeout);	
	var DOMUtil = eXo.core.DOMUtil;
	var objParent = DOMUtil.findAncestorByClass(obj, "ArrowIcon");
	var subMenuItemContainer = false;
	if(objParent) subMenuItemContainer = DOMUtil.findNextElementByTagName(objParent, "div");
	if(subMenuItemContainer && subMenuItemContainer.style.display != "block") {
		subMenuItemContainer.style.display = 'block';
		objParent.className = 'MenuItemHover ArrowIcon';
		subMenuItemContainer.onmouseout = function() {
			objParent.Timeout = setTimeout(function() {
				subMenuItemContainer.style.display = 'none';
				objParent.className = 'MenuItem ArrowIcon';
				subMenuItemContainer.onmouseover = null;
				subMenuItemContainer.onmouseout = null;
        subMenuItemContainer.onfocus = null;
        subMenuItemContainer.onblur = null;
			}, 1*10);
		}
		
    subMenuItemContainer.onblur = function() {
      objParent.Timeout = setTimeout(function() {
        subMenuItemContainer.style.display = 'none';
        objParent.className = 'MenuItem ArrowIcon';
        subMenuItemContainer.onmouseover = null;
        subMenuItemContainer.onmouseout = null;
        subMenuItemContainer.onfocus = null;
        subMenuItemContainer.onblur = null;
      }, 1*10);
    }		
		
		subMenuItemContainer.onmouseover = function() {
			objParent.className = 'MenuItemHover ArrowIcon';
			if(objParent.Timeout) clearTimeout(objParent.Timeout);
			objParent.Timeout =  null;
		}
		
    subMenuItemContainer.onfocus = function() {
      objParent.className = 'MenuItemHover ArrowIcon';
      if(objParent.Timeout) clearTimeout(objParent.Timeout);
      objParent.Timeout =  null;
    }		

		obj.onmouseout = subMenuItemContainer.onmouseout;
		obj.onblur = subMenuItemContainer.onblur;
		subMenuItemContainer.style.width = subMenuItemContainer.offsetWidth - parseInt(DOMUtil.getStyle(subMenuItemContainer, "borderLeftWidth")) - parseInt(DOMUtil.getStyle(subMenuItemContainer, "borderRightWidth")) + 'px';
		subMenuItemContainer.style.left = objParent.offsetLeft + objParent.offsetWidth + 'px';
		subMenuItemContainer.style.top =  eXo.core.Browser.findPosYInContainer(objParent,subMenuItemContainer.offsetParent) + 'px';
	}
}

function requestAjax(url) {
	var xmlHttpRequest = false;
  if(window.XMLHttpRequest) {
		try {
			xmlHttpRequest = new XMLHttpRequest();
		} catch(e) {
			xmlHttpRequest = false;
		}
  } else if(window.ActiveXObject) {
     	try {
      	xmlHttpRequest = new ActiveXObject("Msxml2.XMLHTTP");
    	} catch(e) {
      	try {
        	xmlHttpRequest = new ActiveXObject("Microsoft.XMLHTTP");
      	} catch(e) {
        	xmlHttpRequest = false;
      	}
		}
  }
	if(xmlHttpRequest) {
		xmlHttpRequest.open("GET", url, false);
		xmlHttpRequest.send();
		return xmlHttpRequest.responseXML;
	}
}