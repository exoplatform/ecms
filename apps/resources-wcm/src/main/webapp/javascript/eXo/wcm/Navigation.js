function getCurrentNodes(navigations, selectedNodeUri) {
	var currentNodes = new Array();
	var currentNodeUris = new Array();	
	currentNodeUris = selectedNodeUri.split("/");	
	for (var i in navigations) {
		for (var j in navigations[i].nodes) {
			if(navigations[i].nodes[j].name == currentNodeUris[0]) {
				currentNodes[0] = navigations[i].nodes[j];
				break;
			}
		}
	}		
	var parent = currentNodes[0];	
	for(var k = 1; k<currentNodeUris.length; k++) {		
		if(parent.children == 'null')	{		
			break;
		}		
		for(var n in parent.children) {	
			var node = parent.children[n];			
			if(currentNodeUris[k] == node.name) {
				currentNodes[k]=node;
				parent = node;			
				break;
			}
		}
	}
	return currentNodes;	
}

function getBreadcrumbArr(navigations, previousURI, wcmContentTitle) {
	var breadcrumbNodes = new Array();
	var breadcrumbForNavigations = new Array();
	var previousNodeUris = previousURI.split("/");
	var JsonObj = {};
	
	for (var i in navigations) {
		for (var j in navigations[i].nodes) {
			if(navigations[i].nodes[j].name == previousNodeUris[0]) {
				JsonObj = {
					resolvedLabel: navigations[i].nodes[j].resolvedLabel,
					uri: navigations[i].nodes[j].uri
				}
				breadcrumbForNavigations[0] = navigations[i].nodes[j];
				breadcrumbNodes[0] = JsonObj;
				break;
			}
		}
	}
	
	function getChild(previousNodeUris, children, index) {
		var breadcrumbForNavigations = new Array();
		for (var i in children) {
			if(previousNodeUris[index] == children[i].name) {
				JsonObj = {
					resolvedLabel: children[i].resolvedLabel,
					uri: children[i].uri
				}
				breadcrumbNodes[index] = JsonObj;
				breadcrumbForNavigations[index] = children[i];
				break;
			}
		}
		if (breadcrumbForNavigations[index].length > 0) {
			getChild(previousNodeUris, breadcrumbForNavigations[index].children, ++index); 
		}
	}
	
	if (previousNodeUris.length > 1)
		getChild(previousNodeUris, breadcrumbForNavigations[0].children, 1);
	
	JsonObj = 	{
					resolvedLabel: wcmContentTitle,
					uri: "#"
				}
	breadcrumbNodes[breadcrumbNodes.length] = JsonObj;
		
	return breadcrumbNodes;
}

function getBreadcrumbs() {
	var navigations = eXo.env.portal.navigations;
	var selectedNodeUri = eXo.env.portal.selectedNodeUri;
	var wcmContentTitle = eXo.env.portal.wcmContentTitle;
	var previousURI = eXo.env.portal.previousURI;
	var breadcumbs = new Array();
		
	if (wcmContentTitle != 'null') {
		breadcumbs = getBreadcrumbArr(navigations, previousURI, wcmContentTitle);
	} else {
		breadcumbs = getCurrentNodes(navigations, selectedNodeUri);
	}
	
	return breadcumbs;
}


ScrollManager.prototype.checkAvailableSpace = function(maxSpace) { // in pixels
	if (!maxSpace) maxSpace = this.getElementSpace(this.mainContainer) - this.getElementSpace(this.arrowsContainer) - 50;
	var elementsSpace = 0;
	var margin = 0;
	var length =  this.elements.length;
	for (var i = 0; i < length; i++) {
		elementsSpace += this.getElementSpace(this.elements[i]);
		//dynamic margin;
		if (i+1 < length) margin = this.getElementSpace(this.elements[i+1]) / 3;
		else margin = this.margin;
		if (elementsSpace + margin < maxSpace) { // If the tab fits in the available space
			this.elements[i].isVisible = true;
			this.lastVisibleIndex = i;
		} else { // If the available space is full
			this.elements[i].isVisible = false;
		}
	}
};

ScrollManager.prototype.getVisibleElements = function() {
	var availableSpace = this.getElementSpace(this.mainContainer) - this.getElementSpace(this.arrowsContainer) - 50;
	var refereceIndex = 0;
	var margin = 0;
	var elementsSpace = 0;
	
	if (this.currDirection) {
		var length = this.elements.length;
		for (var i = this.firstVisibleIndex; i < length ; i++) {
			elementsSpace += this.getElementSpace(this.elements[i]);
			//dynamic margin;
			if (i+1 < length) margin = this.getElementSpace(this.elements[i+1]) / 3;
			else margin = this.margin;
			if (elementsSpace + margin < availableSpace) {
				this.elements[i].isVisible = true;
				refereceIndex = i;
			} else this.elements[i].isVisible = false;
		}
		if (this.lastVisibleIndex == refereceIndex) this.scrollRight();
		else this.lastVisibleIndex = refereceIndex;
	} else {
		for (var i = this.lastVisibleIndex; i >= 0 ; i--) {
			elementsSpace += this.getElementSpace(this.elements[i]);
			//dynamic margin;
			margin = this.getElementSpace(this.elements[this.lastVisibleIndex]) / 3;
			if (elementsSpace + margin < availableSpace) {
				this.elements[i].isVisible = true;
				refereceIndex = i;
			} else this.elements[i].isVisible = false;
		}
		if (this.firstVisibleIndex == refereceIndex) this.scrollLeft();
		else this.firstVisibleIndex = refereceIndex;
	}
};

/**
 * Manages the main navigation menu on the portal
 */
function UIWCMNavigation() {
  this.currentOpenedMenu = null;
  this.scrollMgr = null;
  this.scrollManagerLoaded = false;
};
/**
 * Sets some parameters :
 *  . the superClass to eXo.webui.UIPopupMenu
 *  . the css style classes
 * and calls the buildMenu function
 */
UIWCMNavigation.prototype.init = function(popupMenu, container, x, y) {
  this.superClass = eXo.webui.UIPopupMenu;
  this.superClass.init(popupMenu, container, x, y) ;
  
  this.tabStyleClass = "MenuItem";
  this.itemStyleClass = "NormalItem";
  this.selectedItemStyleClass = "SelectedItem";
  this.itemOverStyleClass = "OverItem";
  this.containerStyleClass = "MenuItemContainer";
  
  this.buildMenu(popupMenu);
  this.backupNavigationTabStyle = null;
};
/**
 * Calls the init function when the page loads
 */
UIWCMNavigation.prototype.onLoad = function(navigationId) {
  var uiWorkingWorkspace = document.getElementById("UIWorkingWorkspace");
  var uiNavPortlets = gj(uiWorkingWorkspace).find("div." + navigationId);
  if (uiNavPortlets.length) {
  		var mainContainer = gj(uiNavPortlets[0]).find("div.TabsContainer:first")[0];
	 		eXo.portal.UIWCMNavigation.init(uiNavPortlets[0], mainContainer, 0, 0);
  		for (var i = 1; i < uiNavPortlets.length; ++i) {
					uiNavPortlets[i].style.display = "none";
  		}
  }
};
/**
 * Builds the menu and the submenus
 * Configures each menu item :
 *  . sets onmouseover and onmouseout to call setTabStyle
 *  . sets the width of the item
 * Checks if a submenu exists, if yes, set some parameters :
 *  . sets onclick on the item to call toggleSubMenu
 *  . sets the width and min-width of the sub menu container
 * For each sub menu item :
 *  . set onmouseover to onMenuItemOver and onmouseout to onMenuItemOut
 *  . adds onclick event if the item contains a link, so a click on this item will call the link
 */
UIWCMNavigation.prototype.buildMenu = function(popupMenu) {
  var topContainer = gj(popupMenu).find("div.TabsContainer:first")[0];
  topContainer.id = "PortalNavigationTopContainer";
  // Top menu items
  var topItems = gj(topContainer).find("div.UITab");
  for (var i = 0; i < topItems.length; i++) {
    var item = topItems[i];
    item.onmouseover = eXo.portal.UIWCMNavigation.setTabStyleOnMouseOver ;
    item.onmouseout = eXo.portal.UIWCMNavigation.setTabStyleOnMouseOut ;
    item.onfocus = eXo.portal.UIWCMNavigation.setTabStyleOnMouseOver ;
    item.onblur = eXo.portal.UIWCMNavigation.setTabStyleOnMouseOut ;    
    if (!item.getAttribute('hidesubmenu')) {
      item.onmousemove = eXo.portal.UIWCMNavigation.tabOnMouseMove ;
    }
    item.style.width = item.offsetWidth + 3 +"px";
    /**
     * TODO: fix IE7;
     */
    var container = gj(item).find("div." + this.containerStyleClass + ":first")[0];
    if (container) {
      if (eXo.core.Browser.browserType == "mozilla" || eXo.core.Browser.isIE7() || eXo.core.Browser.browserType == "safari") {
        container.style.minWidth = item.offsetWidth + "px";
      } else {
        container.style.width = item.offsetWidth + "px";
      }
    }
  }
  
  // Sub menus items
  var menuItems = gj(topContainer).find("div." + this.tabStyleClass);
  for(var i = 0; i < menuItems.length; i++) {
    var menuItem = menuItems[i];
    menuItem.onmouseover = eXo.portal.UIWCMNavigation.onMenuItemOver;
    menuItem.onmouseout = eXo.portal.UIWCMNavigation.onMenuItemOut;
    menuItem.onfocus = eXo.portal.UIWCMNavigation.onMenuItemOver;
    menuItem.onblur = eXo.portal.UIWCMNavigation.onMenuItemOut;    

    // Set an id to each container for future reference
    var cont = gj(menuItem).parents("." + this.containerStyleClass + ":first")[0];
    if (!cont.id) cont.id = "PortalNavigationContainer-" + i + Math.random();
    cont.resized = false;
  }
};

/**
 * Changes the style of a tab, depending on the over value (true or false)
 * Gives the defaut css style class names . UITab when tab is NOT highlighted .
 * HighlightNavigationTab when tab is highlighted
 */
changeTabNavigationStyle = function(clickedEle, over) {
    if (clickedEle == null)
      return;
    if (!gj(clickedEle).hasClass("UITab"))
      clickedEle = gj(clickedEle).parents(".UITab:first")[0]; 
    if (over) {
      this.backupNavigationTabStyle = clickedEle.className;
      clickedEle.className = "UITab HighlightNavigationTab";
    } else if (this.backupNavigationTabStyle) {
      clickedEle.className = this.backupNavigationTabStyle;
    }
  };
  
/**
 * Sets the tab style on mouse over and mouse out
 * If the mouse goes out of the item but stays on its sub menu, the item remains highlighted
 */
UIWCMNavigation.prototype.setTabStyle = function() {
  var tab = this;
//  var tabChildren = eXo.core.DOMUtil.getChildrenByTagName(tab, "div") ;
  var tabChildren = gj(tab).children("div");
  if (tabChildren[0].className != "HighlightNavigationTab") {
    // highlights the tab
	  changeTabNavigationStyle(tab, true);
  } else {
    if(tabChildren.length <= 1 || tabChildren[1].id != eXo.portal.UIWCMNavigation.currentOpenedMenu) {
      // de-highlights the tab if it doesn't have a submenu (cond 1) or its submenu isn't visible (cond 2)
    	changeTabNavigationStyle(tab, false);
    }
  }
}

UIWCMNavigation.prototype.setTabStyleOnMouseOver = function(e) {
  var tab = this ;
  if (eXo.portal.UIWCMNavigation.previousMenuItem != tab) {
    eXo.portal.UIWCMNavigation.hideMenu() ;
  }
	eXo.portal.UIWCMNavigation.setTabStyleOnMouseOut(e, tab) ;
  eXo.portal.UIWCMNavigation.previousMenuItem = tab ;
  if (!eXo.portal.UIWCMNavigation.menuVisible) {
    var menuItemContainer = gj(tab).find("div." + eXo.portal.UIWCMNavigation.containerStyleClass + ":first")[0];
    var hideSubmenu = tab.getAttribute('hideSubmenu') ;
    if (menuItemContainer && !hideSubmenu) {
      eXo.portal.UIWCMNavigation.toggleSubMenu(e, tab, menuItemContainer) ;
    }
  }
  eXo.portal.UIWCMNavigation.menuVisible = true ;  
} ;

UIWCMNavigation.prototype.setTabStyleOnMouseOut = function(e, src) {
  var tab = src || this;
//  var tabChildren = eXo.core.DOMUtil.getChildrenByTagName(tab, "div") ;
  var tabChildren = gj(tab).children("div") ;
  if (tabChildren.length <= 0) {
    return ;
  }
  if (tabChildren[0].className != "HighlightNavigationTab") {
    // highlights the tab
	  changeTabNavigationStyle(tab, true);
  } else {
    if(tabChildren.length <= 1 || tabChildren[1].id != eXo.portal.UIWCMNavigation.currentOpenedMenu) {
      // de-highlights the tab if it doesn't have a submenu (cond 1) or its submenu isn't visible (cond 2)
    	changeTabNavigationStyle(tab, false);
    }
  }
  eXo.portal.UIWCMNavigation.hideMenuTimeout(500) ;
}

UIWCMNavigation.prototype.tabOnMouseMove = function() {
  eXo.portal.UIWCMNavigation.cancelHideMenuContainer() ;
} ;

/**
 * Shows or hides a submenu
 * Calls hideMenuContainer to hide a submenu.
 * Hides any other visible sub menu before showing the new one
 * Sets the width of the submenu (the first time it is shown) to fix a bug in IE
 * Sets the currentOpenedMenu to the menu being opened
 */
UIWCMNavigation.prototype.toggleSubMenu = function(e, tab, menuItemContainer) {
  if (!e) e = window.event;
  e.cancelBubble = true;
  var src = eXo.core.Browser.getEventSource(e);
  if (src.tagName.toLowerCase() == "a" && !menuItemContainer) {
    if (src.href.substr(0, 7) == "http://") {
      if (!src.target) {
        window.location.href = src.href
      } else {
        return true ;
      }
    } else eval(src.href);
    return false;
  }
  var item = tab;
  if (menuItemContainer) {
    if (menuItemContainer.style.display == "none") {
      // shows the sub menu
      // hides a previously opened sub menu
      if (eXo.portal.UIWCMNavigation.currentOpenedMenu) eXo.portal.UIWCMNavigation.hideMenu();
      
      eXo.portal.UIWCMNavigation.superClass.pushVisibleContainer(menuItemContainer.id);
      var y = item.offsetHeight + item.offsetTop;
      var x = item.offsetLeft + 2;
      eXo.portal.UIWCMNavigation.superClass.setPosition(menuItemContainer, x, y);
      eXo.portal.UIWCMNavigation.superClass.show(menuItemContainer);
      
      menuItemContainer.style.width = menuItemContainer.offsetWidth - parseInt(gj(menuItemContainer).css('borderLeftWidth')) - parseInt(gj(menuItemContainer).css('borderRightWidth')) + "px";
      eXo.portal.UIWCMNavigation.currentOpenedMenu = menuItemContainer.id;
      
      /*Hide eXoStartMenu whenever click on the UIApplication*/
      var uiPortalApplication = document.getElementById("UIPortalApplication") ;
      uiPortalApplication.onclick = eXo.portal.UIWCMNavigation.hideMenu ;
    } else {
      // hides the sub menu
      eXo.portal.UIWCMNavigation.hideMenuContainer();
    }
  }
};

UIWCMNavigation.prototype.cancelHideMenuContainer = function() {
  if (this.hideMenuTimeoutId) {
    window.clearTimeout(this.hideMenuTimeoutId) ;
  }
} ;

UIWCMNavigation.prototype.closeMenuTimeout = function() {
  eXo.portal.UIWCMNavigation.hideMenuTimeout(200) ;
} ;

UIWCMNavigation.prototype.hideMenuTimeout = function(time) {
  this.cancelHideMenuContainer() ;
  if (!time || time <= 0) {
    time = 200 ;
  }
  //this.hideMenuTimeoutId = window.setTimeout(this.hideMenu, time) ;
  this.hideMenuTimeoutId = window.setTimeout('eXo.portal.UIWCMNavigation.hideMenu() ;', time) ;
} ;

/**
 * Adds the currentOpenedMenu to the list of containers to hide
 * and sets a time out to close them effectively
 * Sets currentOpenedMenu to null (no menu is opened)
 * Uses the methods from the superClass (eXo.webui.UIPopupMenu) to perform these operations
 */
UIWCMNavigation.prototype.hideMenuContainer = function() {
  var menuItemContainer = document.getElementById(eXo.portal.UIWCMNavigation.currentOpenedMenu);
  if (menuItemContainer) {
    eXo.portal.UIWCMNavigation.superClass.pushHiddenContainer(menuItemContainer.id);
    eXo.portal.UIWCMNavigation.superClass.popVisibleContainer();
    eXo.portal.UIWCMNavigation.superClass.setCloseTimeout();
    eXo.portal.UIWCMNavigation.superClass.hide(menuItemContainer);
    eXo.portal.UIWCMNavigation.currentOpenedMenu = null;
  }
  this.previousMenuItem = false ;
  eXo.portal.UIWCMNavigation.menuVisible = false ;
};
/**
 * Changes the style of the parent button when a submenu has to be hidden
 */
UIWCMNavigation.prototype.hideMenu = function() {
  if (eXo.portal.UIWCMNavigation.currentOpenedMenu) {
    var currentItemContainer = document.getElementById(eXo.portal.UIWCMNavigation.currentOpenedMenu);
    var tab = gj(currentItemContainer).parents(".UITab:first")[0];
    changeTabNavigationStyle(tab, false);
  }
  eXo.portal.UIWCMNavigation.hideMenuContainer();
};
/**
 * When the mouse goes over a menu item (in the main nav menu)
 * Check if this menu item has a sub menu, if yes, opens it
 * Changes the style of the button
 */
UIWCMNavigation.prototype.onMenuItemOver = function(e) {
  var menuItem = this;
  var subContainer = gj(menuItem).find("div." + eXo.portal.UIWCMNavigation.containerStyleClass + ":first")[0];
  if (subContainer) {
    eXo.portal.UIWCMNavigation.superClass.pushVisibleContainer(subContainer.id);
    eXo.portal.UIWCMNavigation.showMenuItemContainer(menuItem, subContainer) ;
    if (!subContainer.firstTime) {
        subContainer.style.width = subContainer.offsetWidth + 2 + "px";
        subContainer.firstTime = true;
    }
  }
  eXo.portal.UIWCMNavigation.cancelHideMenuContainer() ;
};
/**
 * Shows a sub menu, uses the methods from superClass (eXo.webui.UIPopupMenu)
 */
UIWCMNavigation.prototype.showMenuItemContainer = function(menuItem, menuItemContainer) {
  var x = menuItem.offsetWidth;
  var y = menuItem.offsetTop;
  this.superClass.setPosition(menuItemContainer, x, y);
  this.superClass.show(menuItemContainer);
};
/**
 * When the mouse goes out a menu item from the main nav menu
 * Checks if this item has a sub menu, if yes calls methods from superClass to hide it
 */
UIWCMNavigation.prototype.onMenuItemOut = function(e) {
  var menuItem = this;
  var subContainer = gj(menuItem).find("div." + eXo.portal.UIWCMNavigation.containerStyleClass + ":first")[0];
  if (subContainer) {
    eXo.portal.UIWCMNavigation.superClass.pushHiddenContainer(subContainer.id);
    eXo.portal.UIWCMNavigation.superClass.popVisibleContainer();
    eXo.portal.UIWCMNavigation.superClass.setCloseTimeout(300);
  }
};

/***** Scroll Management *****/
/**
 * Function called to load the scroll manager that will manage the tabs in the main nav menu
 *  . Creates the scroll manager with id PortalNavigationTopContainer
 *  . Adds the tabs to the scroll manager
 *  . Configures the arrows
 *  . Calls the initScroll function
 */
UIWCMNavigation.prototype.loadScroll = function(e) {
  var uiNav = eXo.portal.UIWCMNavigation;
  var portalNav = document.getElementById("PortalNavigationTopContainer");
  if (portalNav) {
    // Creates new ScrollManager and initializes it
    uiNav.scrollMgr = new ScrollManager("PortalNavigationTopContainer");
    uiNav.scrollMgr.initFunction = uiNav.initScroll;
    // Adds the tab elements to the manager
    var tabs = gj(portalNav).parents(".UIHorizontalTabs:first")[0];
    uiNav.scrollMgr.mainContainer = tabs;
    uiNav.scrollMgr.arrowsContainer = gj(tabs).find("div.ScrollButtons:first")[0];
    uiNav.scrollMgr.loadElements("UITab");
    // Configures the arrow buttons
    var arrowButtons = gj(uiNav.scrollMgr.arrowsContainer).find("div");
    if (arrowButtons.length == 2) {
      uiNav.scrollMgr.initArrowButton(arrowButtons[0], "left", "ScrollLeftButton", "HighlightScrollLeftButton", "DisableScrollLeftButton");
      uiNav.scrollMgr.initArrowButton(arrowButtons[1], "right", "ScrollRightButton", "HighlightScrollRightButton", "DisableScrollRightButton");
    }
    // Finish initialization
    uiNav.scrollMgr.callback = uiNav.scrollCallback;
    uiNav.scrollManagerLoaded = true;
    uiNav.initScroll();
  }
};
/**
 * Init function for the scroll manager
 *  . Calls the init function of the scroll manager
 *  . Calculates the available space to render the tabs
 *  . Renders the tabs
 */
UIWCMNavigation.prototype.initScroll = function(e) {
  if (!eXo.portal.UIWCMNavigation.scrollManagerLoaded) eXo.portal.UIWCMNavigation.loadScroll();
  var scrollMgr = eXo.portal.UIWCMNavigation.scrollMgr;
  scrollMgr.init();
  // Gets the maximum width available for the tabs
  scrollMgr.checkAvailableSpace();
  scrollMgr.renderElements();
};
/**
 * A callback function to call after a scroll event occurs (and the elements are rendered)
 * Is empty so far.
 */
UIWCMNavigation.prototype.scrollCallback = function() {
};
/***** Scroll Management *****/
eXo.portal.UIWCMNavigation = new UIWCMNavigation() ;
_module.UIWCMNavigation = eXo.portal.UIWCMNavigation;
