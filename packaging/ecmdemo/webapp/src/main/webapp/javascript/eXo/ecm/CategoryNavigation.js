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
  var microSite = eXo.core.DOMUtil.findAncestorByClass(clickedElement, "UIMicroSite");
  this.closeAllSubMenu(microSite);

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

eXo.ecm.CategoryNavigation = new CategoryNavigation();