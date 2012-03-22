function CategoryNavigation(){
}

CategoryNavigation.prototype.toggleSubMenu = function(e) {
  var event = e || window.event;
  var clickedElement = event.target || event.srcElement;
  var isExpand = false;

  //the click is to expand or collapse ?
  if (clickedElement.className.indexOf("IconExplain") > 0) {
    isExpand = true;
  } 

  //reset all sub-menu  
  var rootElement = eXo.core.DOMUtil.findAncestorByClass(clickedElement, "UIMicroSite");
  this.closeAllSubMenu(rootElement);

  //expand or collapse menu  
  var subMenu = eXo.core.DOMUtil.findFirstDescendantByClass(clickedElement.parentNode, "ul", "SubMenu");  
  if (subMenu) {
    if (isExpand) {
      subMenu.style.display = "block";
      clickedElement.className = clickedElement.className.replace("IconExplain", "IconClose");
    } else {
      subMenu.style.display = "none";
      clickedElement.className = clickedElement.className.replace("IconClose", "IconExplain");
    }
  } else {
    if (isExpand) {
      clickedElement.className = clickedElement.className.replace("IconExplain", "IconClose");
    } else {
      clickedElement.className = clickedElement.className.replace("IconClose", "IconExplain");
    }
  }
};

CategoryNavigation.prototype.closeAllSubMenu = function(rootElement) {  
  //close all sub menu
  var subMenus = eXo.core.DOMUtil.findDescendantsByClass(rootElement, "ul", "SubMenu");
  if (subMenus) {
    for(var i = 0; i < subMenus.length; i++) {
      subMenus[i].style.display = "none";
    }
  }

  //switch all to close button
  var categoryMenus = eXo.core.DOMUtil.findDescendantsByClass(rootElement, "a", "TabLeft");
  if (categoryMenus) {
    for(var i = 0; i < categoryMenus.length; i++) {
      if (categoryMenus[i].className.indexOf("IconClose") > 0) {
        categoryMenus[i].className = categoryMenus[i].className.replace("IconClose", "IconExplain");
      }
    }
  }  
};

CategoryNavigation.prototype.loadSubMenu = function() {
	//UIMicroSite
  var rootElement = eXo.core.DOMUtil.findDescendantsByClass(document, "div", "UIMicroSite")[0];
  if (!rootElement) {
  	return;
  }
  
  eXo.ecm.CategoryNavigation.closeAllSubMenu(rootElement);
  
  var expandedElement = eXo.core.DOMUtil.findDescendantsByClass(rootElement, "a", "SelectedCategory")[0];	
  if (expandedElement) {
	expandedElement.className = expandedElement.className.replace("IconExplain", "IconClose");
	var subMenu = eXo.core.DOMUtil.findFirstDescendantByClass(expandedElement.parentNode, "ul", "SubMenu");
	if (subMenu) {
	  subMenu.style.display = "block";
	}
  }
};

CategoryNavigation.prototype.addLoadEvent = function(func) { 
  var oldonload = window.onload; 
  if (typeof window.onload != 'function') { 
    window.onload = func; 
  } else { 
    window.onload = function() { 
      if (oldonload) { 
        oldonload(); 
      } 
      func(); 
    } 
  } 
} ;

eXo.ecm.CategoryNavigation = new CategoryNavigation();
eXo.ecm.CategoryNavigation.addLoadEvent(eXo.ecm.CategoryNavigation.loadSubMenu);
