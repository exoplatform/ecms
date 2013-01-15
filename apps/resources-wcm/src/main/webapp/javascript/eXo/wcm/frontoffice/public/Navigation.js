(function(gj, ecm_bootstrap, navigation) {
	/**
	 * Manages the main navigation menu on the portal
	 */
	function WCMNavigationPortlet() {
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
	WCMNavigationPortlet.prototype.init = function(popupMenu, container, x, y) {
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
	WCMNavigationPortlet.prototype.onLoad = function(navigationId) {
	  var uiWorkingWorkspace = document.getElementById("UIWorkingWorkspace");
	  var uiNavPortlets = gj(uiWorkingWorkspace).find("div." + navigationId);
	  if (uiNavPortlets.length) {
	  		var mainContainer = gj(uiNavPortlets[0]).find("div.TabsContainer:first")[0];
		 		eXo.ecm.WCMNavigationPortlet.init(uiNavPortlets[0], mainContainer, 0, 0);
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
	WCMNavigationPortlet.prototype.buildMenu = function(popupMenu) {
	  var topContainer = gj(popupMenu).find("div.TabsContainer:first")[0];
	  topContainer.id = "PortalNavigationTopContainer";
	  // Top menu items
	  var topItems = gj(topContainer).find("div.UITab");
	  for (var i = 0; i < topItems.length; i++) {
	    var item = topItems[i];
	    item.onmouseover = eXo.ecm.WCMNavigationPortlet.setTabStyleOnMouseOver ;
	    item.onmouseout = eXo.ecm.WCMNavigationPortlet.setTabStyleOnMouseOut ;
	    item.onfocus = eXo.ecm.WCMNavigationPortlet.setTabStyleOnMouseOver ;
	    item.onblur = eXo.ecm.WCMNavigationPortlet.setTabStyleOnMouseOut ;    
	    if (!item.getAttribute('hidesubmenu')) {
	      item.onmousemove = eXo.ecm.WCMNavigationPortlet.tabOnMouseMove ;
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
	    menuItem.onmouseover = eXo.ecm.WCMNavigationPortlet.onMenuItemOver;
	    menuItem.onmouseout = eXo.ecm.WCMNavigationPortlet.onMenuItemOut;
	    menuItem.onfocus = eXo.ecm.WCMNavigationPortlet.onMenuItemOver;
	    menuItem.onblur = eXo.ecm.WCMNavigationPortlet.onMenuItemOut;    
	
	    // Set an id to each container for future reference
	    var cont = gj(menuItem).parents("." + this.containerStyleClass + ":first")[0] ;
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
	WCMNavigationPortlet.prototype.setTabStyle = function() {
	  var tab = this;
	//  var tabChildren = eXo.core.DOMUtil.getChildrenByTagName(tab, "div") ;
	  var tabChildren = gj(tab).children("div") ;
	  if (tabChildren[0].className != "HighlightNavigationTab") {
	    // highlights the tab
		  changeTabNavigationStyle(tab, true);
	  } else {
	    if(tabChildren.length <= 1 || tabChildren[1].id != eXo.ecm.WCMNavigationPortlet.currentOpenedMenu) {
	      // de-highlights the tab if it doesn't have a submenu (cond 1) or its submenu isn't visible (cond 2)
	    	changeTabNavigationStyle(tab, false);
	    }
	  }
	};
	
	WCMNavigationPortlet.prototype.setTabStyleOnMouseOver = function(e) {
	  var tab = this ;
	  if (eXo.ecm.WCMNavigationPortlet.previousMenuItem != tab) {
	    eXo.ecm.WCMNavigationPortlet.hideMenu() ;
	  }
		eXo.ecm.WCMNavigationPortlet.setTabStyleOnMouseOut(e, tab) ;
	  eXo.ecm.WCMNavigationPortlet.previousMenuItem = tab ;
	  if (!eXo.ecm.WCMNavigationPortlet.menuVisible) {
	    var menuItemContainer = gj(tab).find("div." + eXo.ecm.WCMNavigationPortlet.containerStyleClass + ":first")[0];
	    var hideSubmenu = tab.getAttribute('hideSubmenu') ;
	    if (menuItemContainer && !hideSubmenu) {
	      eXo.ecm.WCMNavigationPortlet.toggleSubMenu(e, tab, menuItemContainer) ;
	    }
	  }
	  eXo.ecm.WCMNavigationPortlet.menuVisible = true ;  
	} ;
	
	WCMNavigationPortlet.prototype.setTabStyleOnMouseOut = function(e, src) {
	  var tab = src || this;
	//  var tabChildren = eXo.core.DOMUtil.getChildrenByTagName(tab, "div") ;
	  var tabChildren = gj(tab).children("div");
	  if (tabChildren.length <= 0) {
	    return ;
	  }
	  if (tabChildren[0].className != "HighlightNavigationTab") {
	    // highlights the tab
		  changeTabNavigationStyle(tab, true);
	  } else {
	    if(tabChildren.length <= 1 || tabChildren[1].id != eXo.ecm.WCMNavigationPortlet.currentOpenedMenu) {
	      // de-highlights the tab if it doesn't have a submenu (cond 1) or its submenu isn't visible (cond 2)
	    	changeTabNavigationStyle(tab, false);
	    }
	  }
	  eXo.ecm.WCMNavigationPortlet.hideMenuTimeout(500) ;
	};
	
	WCMNavigationPortlet.prototype.tabOnMouseMove = function() {
	  eXo.ecm.WCMNavigationPortlet.cancelHideMenuContainer() ;
	} ;
	
	/**
	 * Shows or hides a submenu
	 * Calls hideMenuContainer to hide a submenu.
	 * Hides any other visible sub menu before showing the new one
	 * Sets the width of the submenu (the first time it is shown) to fix a bug in IE
	 * Sets the currentOpenedMenu to the menu being opened
	 */
	WCMNavigationPortlet.prototype.toggleSubMenu = function(e, tab, menuItemContainer) {
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
	      if (eXo.ecm.WCMNavigationPortlet.currentOpenedMenu) eXo.ecm.WCMNavigationPortlet.hideMenu();
	      
	      eXo.ecm.WCMNavigationPortlet.superClass.pushVisibleContainer(menuItemContainer.id);
	      var y = item.offsetHeight + item.offsetTop;
	      var x = item.offsetLeft + 2;
	      eXo.ecm.WCMNavigationPortlet.superClass.setPosition(menuItemContainer, x, y);
	      eXo.ecm.WCMNavigationPortlet.superClass.show(menuItemContainer);
	      
	      menuItemContainer.style.width = menuItemContainer.offsetWidth - parseInt(gj(menuItemContainer).css('borderLeftWidth')) - parseInt(gj(menuItemContainer).css('borderRightWidth')) + "px";
	      eXo.ecm.WCMNavigationPortlet.currentOpenedMenu = menuItemContainer.id;
	      
	      /*Hide eXoStartMenu whenever click on the UIApplication*/
	      var uiPortalApplication = document.getElementById("UIPortalApplication") ;
	      uiPortalApplication.onclick = eXo.ecm.WCMNavigationPortlet.hideMenu ;
	    } else {
	      // hides the sub menu
	      eXo.ecm.WCMNavigationPortlet.hideMenuContainer();
	    }
	  }
	};
	
	WCMNavigationPortlet.prototype.cancelHideMenuContainer = function() {
	  if (this.hideMenuTimeoutId) {
	    window.clearTimeout(this.hideMenuTimeoutId) ;
	  }
	} ;
	
	WCMNavigationPortlet.prototype.closeMenuTimeout = function() {
	  eXo.ecm.WCMNavigationPortlet.hideMenuTimeout(200) ;
	} ;
	
	WCMNavigationPortlet.prototype.hideMenuTimeout = function(time) {
	  this.cancelHideMenuContainer() ;
	  if (!time || time <= 0) {
	    time = 200 ;
	  }
	  //this.hideMenuTimeoutId = window.setTimeout(this.hideMenu, time) ;
	  this.hideMenuTimeoutId = window.setTimeout('eXo.ecm.WCMNavigationPortlet.hideMenu() ;', time) ;
	} ;
	
	/**
	 * Adds the currentOpenedMenu to the list of containers to hide
	 * and sets a time out to close them effectively
	 * Sets currentOpenedMenu to null (no menu is opened)
	 * Uses the methods from the superClass (eXo.webui.UIPopupMenu) to perform these operations
	 */
	WCMNavigationPortlet.prototype.hideMenuContainer = function() {
	  var menuItemContainer = document.getElementById(eXo.ecm.WCMNavigationPortlet.currentOpenedMenu);
	  if (menuItemContainer) {
	    eXo.ecm.WCMNavigationPortlet.superClass.pushHiddenContainer(menuItemContainer.id);
	    eXo.ecm.WCMNavigationPortlet.superClass.popVisibleContainer();
	    eXo.ecm.WCMNavigationPortlet.superClass.setCloseTimeout();
	    eXo.ecm.WCMNavigationPortlet.superClass.hide(menuItemContainer);
	    eXo.ecm.WCMNavigationPortlet.currentOpenedMenu = null;
	  }
	  this.previousMenuItem = false ;
	  eXo.ecm.WCMNavigationPortlet.menuVisible = false ;
	};
	
	/**
	 * Changes the style of the parent button when a submenu has to be hidden
	 */
	WCMNavigationPortlet.prototype.hideMenu = function() {
	  if (eXo.ecm.WCMNavigationPortlet.currentOpenedMenu) {
	    var currentItemContainer = document.getElementById(eXo.portal.WCMNavigationPortlet.currentOpenedMenu);
	    var tab = gj(currentItemContainer).parents(".UITab:first")[0];
	    changeTabNavigationStyle(tab, false);
	  }
	  eXo.ecm.WCMNavigationPortlet.hideMenuContainer();
	};
	
	/**
	 * When the mouse goes over a menu item (in the main nav menu)
	 * Check if this menu item has a sub menu, if yes, opens it
	 * Changes the style of the button
	 */
	WCMNavigationPortlet.prototype.onMenuItemOver = function(e) {
	  var menuItem = this;
	  var subContainer = gj(menuItem).find("div." + eXo.ecm.WCMNavigationPortlet.containerStyleClass + ":first")[0];
	  if (subContainer) {
	    eXo.ecm.WCMNavigationPortlet.superClass.pushVisibleContainer(subContainer.id);
	    eXo.ecm.WCMNavigationPortlet.showMenuItemContainer(menuItem, subContainer) ;
	    if (!subContainer.firstTime) {
	        subContainer.style.width = subContainer.offsetWidth + 2 + "px";
	        subContainer.firstTime = true;
	    }
	  }
	  eXo.ecm.WCMNavigationPortlet.cancelHideMenuContainer() ;
	};
	
	/**
	 * Shows a sub menu, uses the methods from superClass (eXo.webui.UIPopupMenu)
	 */
	WCMNavigationPortlet.prototype.showMenuItemContainer = function(menuItem, menuItemContainer) {
	  var x = menuItem.offsetWidth;
	  var y = menuItem.offsetTop;
	  this.superClass.setPosition(menuItemContainer, x, y);
	  this.superClass.show(menuItemContainer);
	};
	
	/**
	 * When the mouse goes out a menu item from the main nav menu
	 * Checks if this item has a sub menu, if yes calls methods from superClass to hide it
	 */
	WCMNavigationPortlet.prototype.onMenuItemOut = function(e) {
	  var menuItem = this;
	  var subContainer = gj(menuItem).find("div." + eXo.ecm.WCMNavigationPortlet.containerStyleClass + ":first")[0];
	  if (subContainer) {
	    eXo.ecm.WCMNavigationPortlet.superClass.pushHiddenContainer(subContainer.id);
	    eXo.ecm.WCMNavigationPortlet.superClass.popVisibleContainer();
	    eXo.ecm.WCMNavigationPortlet.superClass.setCloseTimeout(300);
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
	WCMNavigationPortlet.prototype.loadScroll = function(e) {
	  var uiNav = eXo.ecm.WCMNavigationPortlet;
	  var portalNav = document.getElementById("navigation-generator");
	  if (portalNav) {
	    // Creates new ScrollManager and initializes it
	    uiNav.scrollMgr = new ScrollManager("navigation-generator");
	    // Adds the tab elements to the manager
	    uiNav.scrollMgr.loadElements("UITab"); 
	    uiNav.scrollMgr.init();
	    uiNav.scrollMgr.renderElements();    
	  }
	};
	
	/**
	 * Init function for the scroll manager
	 *  . Calls the init function of the scroll manager
	 *  . Calculates the available space to render the tabs
	 *  . Renders the tabs
	 */
	WCMNavigationPortlet.prototype.initScroll = function(e) {
	  if (!eXo.ecm.WCMNavigationPortlet.scrollManagerLoaded) eXo.ecm.WCMNavigationPortlet.loadScroll();
	  var scrollMgr = eXo.ecm.WCMNavigationPortlet.scrollMgr;
	  scrollMgr.init();
	  // Gets the maximum width available for the tabs
	  scrollMgr.checkAvailableSpace();
	  scrollMgr.renderElements();
	};
	
	/**
	 * A callback function to call after a scroll event occurs (and the elements are rendered)
	 * Is empty so far.
	 */
	WCMNavigationPortlet.prototype.scrollCallback = function() {
	};
	
	WCMNavigationPortlet.prototype.getCurrentNodes = function(navigations, selectedNodeUri) {
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
	};
	
	eXo.ecm.WCMNavigationPortlet = new WCMNavigationPortlet();
	return {
		WCMNavigationPortlet : eXo.ecm.WCMNavigationPortlet
	};
})(gj, ecm_bootstrap, navigation);
