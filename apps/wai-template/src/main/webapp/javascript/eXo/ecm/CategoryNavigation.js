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
  var rootElement = gj(clickedElement).parents(".UIMicroSite:first")[0];
  this.closeAllSubMenu(rootElement);

  //expand or collapse menu  
  var subMenu = gj(clickedElement.parentNode).find("ul.SubMenu:first")[0];  
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
  var subMenus = gj(rootElement).find("ul.SubMenu");
  if (subMenus) {
    for(var i = 0; i < subMenus.length; i++) {
      subMenus[i].style.display = "none";
    }
  }

  //switch all to close button
  var categoryMenus = gj(rootElement).find("a.TabLeft");
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
  var rootElement = gj(document).find("div.UIMicroSite")[0];
  if (!rootElement) {
  	return;
  }
  
  eXo.ecm.CategoryNavigation.closeAllSubMenu(rootElement);
  
  var expandedElement = gj(rootElement).find("a.SelectedCategory")[0];	
  if (expandedElement) {
	expandedElement.className = expandedElement.className.replace("IconExplain", "IconClose");
	var subMenu = gj(expandedElement.parentNode).find("ul.SubMenu:first")[0];
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
_module.CategoryNavigation = eXo.ecm.CategoryNavigation;
eXo.ecm.CategoryNavigation.addLoadEvent(eXo.ecm.CategoryNavigation.loadSubMenu);
