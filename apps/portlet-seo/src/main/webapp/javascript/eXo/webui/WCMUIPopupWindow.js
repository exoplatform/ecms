/**
 * Copyright (C) 2009 eXo Platform SAS.
 * 
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

/**
 * A class that manages a popup window
 */
function WCMUIPopupWindow() {} ;
/**
 * Inits a popup window, with these parameters :
 *  . sets the superClass as eXo.webui.UIPopup
 *  . sets the popup hidden
 *  . inits the drag and drop
 *  . inits the resize area if the window is resizable
 */
  
WCMUIPopupWindow.prototype.init = function(popupId, isShow, isResizable, showCloseButton, isShowMask, top, left) {
	this.superClass = eXo.webui.UIPopup ;
	this.topDelta = 10;
	this.leftDelta = 10;
	var popup = document.getElementById(popupId) ;
	if(popup == null) return;
	popup.style.visibility = "hidden" ;
	
	//TODO Lambkin: this statement create a bug in select box component in Firefox
	//this.superClass.init(popup) ;
	if (isShow) {
		popup.style.display = "block";
	}
	var contentBlock = gj(popup).find('div.PopupContent:first')[0];
	if(contentBlock && (gj(window).height() - 100 < contentBlock.offsetHeight)) {
		contentBlock.style.height = (gj(window).height() - 100) + "px";
	}
	var popupBar = gj(popup).find('span.PopupTitle:first')[0];

	eXo.webui.WCMUIPopupWindow.initDND(popupBar, popup);
	
	if(isShow == false) {
		this.superClass.hide(popup) ;
		if(isShowMask) eXo.webui.WCMUIPopupWindow.showMask(popup, false) ;
	} 
	
	if(isResizable) {
		var resizeBtn = gj(popup).find("span.ResizeButton:first")[0];
		resizeBtn.style.display = 'block';
		resizeBtn.onmousedown = this.startResizeEvt;
		resizeBtn.onkeydown = this.startResizeEvt;
	}
	
	popup.style.visibility = "hidden" ;
	if(isShow == true) {
		var iframes = gj(popup).find("iframe");
		if(iframes.length > 0) {
			setTimeout("eXo.webui.WCMUIPopupWindow.show('" + popupId + "'," + isShowMask + ")", 500, top, left) ;
		} else {
			this.show(popup, isShowMask, 500, top, left) ;
		}
	}
} ;

WCMUIPopupWindow.prototype.showMask = function(popup, isShowPopup) {
	var mask = popup.previousSibling;
  //Make sure mask is not TextNode because of previousSibling property
	if (mask && mask.className != "MaskLayer") {
		mask = null;
	}
	if(isShowPopup) {
		if(!mask) eXo.core.UIMaskLayer.createMask(popup.parentNode, popup, 1) ;		
	} else {
		if(mask) eXo.core.UIMaskLayer.removeMask(mask) ;
	}
} ;

//TODO: manage zIndex properties
/**
 * Shows the popup window passed in parameter
 * gets the highest z-index property of the elements in the page :
 *  . gets the z-index of the maskLayer
 *  . gets all the other popup windows
 *  . gets the highest z-index from these, if it's still at 0, set an arbitrary value of 2000
 * sets the position of the popup on the page (top and left properties)
 */
WCMUIPopupWindow.prototype.show = function(popup, isShowMask, middleBrowser, top, left) {
	if(typeof(popup) == "string") popup = document.getElementById(popup) ;
	var portalApp = document.getElementById("UIPortalApplication") ;
	var maskLayer = gj(portalApp).find("div.UIMaskWorkspace:first")[0];
	var zIndex = 0 ;
	var currZIndex = 0 ;
	if (maskLayer != null) {
		currZIndex = gj(maskLayer).css("zIndex");
		if (!isNaN(currZIndex) && currZIndex > zIndex) zIndex = currZIndex ;
	}
	var popupWindows = gj(portalApp).find("div.WCMUIPopupWindow") ;
	var len = popupWindows.length ;
	for (var i = 0 ; i < len ; i++) {
		currZIndex = gj(popupWindows[i]).css("zIndex") ;
		if (!isNaN(currZIndex) && currZIndex > zIndex) zIndex = currZIndex ;
	}
	if (zIndex == 0) zIndex = 2000 ;
	// We don't increment zIndex here because it is done in the superClass.show function
	if(isShowMask) eXo.webui.WCMUIPopupWindow.showMask(popup, true) ;
	popup.style.visibility = "hidden" ;
	this.superClass.show(popup) ;
  if(top > -1 && left > -1) {
  	var seoPopup = document.getElementById('UISEOPopupWindow');
  	var popupWidth = 390+90;
  	if(eXo.ecm.WCMUtils.showRightContent)
  		popupWidth = 630+90;
    var pageWidth = 0;
    var wsElement = document.getElementById('UIWorkingWorkspace');
    if(wsElement) pageWidth = wsElement.clientWidth;
    if(screen.width - pageWidth <= popupWidth)
    	left = left - (screen.width - pageWidth);  
    else
			 left = left - (screen.width - pageWidth)/2;
    popup.style.top = top + this.topDelta + "px";
    var deltaX = screen.width-left;
    if(deltaX < popupWidth) {
      left = left - (popupWidth - deltaX) - 70;
		}  
    popup.style.left = left + "px";
    popup.style.position = "fixed";
  } else {
	 	var offsetParent = popup.offsetParent ;
	 	var scrollY = 0;
		if (window.pageYOffset != undefined) scrollY = window.pageYOffset;
		else if (document.documentElement && document.documentElement.scrollTop) scrollY = document.documentElement.scrollTop;
		else	scrollY = document.body.scrollTop;
		//reference
		if(offsetParent) {
			var middleWindow = (gj(offsetParent).hasClass("WCMUIPopupWindow") || gj(offsetParent).hasClass("UIWindow"));
			if (middleWindow) {			
				popup.style.top = Math.ceil((offsetParent.offsetHeight - popup.offsetHeight) / 2) + "px" ;
			} 
			if (middleBrowser || !middleWindow) {
				popup.style.top = Math.ceil((gj(window).height() - popup.offsetHeight ) / 2) + scrollY + "px";
			}
			//Todo: set popup of UIPopup always display in the center browsers in case UIMaskWorkspace
			if(gj(offsetParent).hasClass("UIMaskWorkspace")) {
				//if(eXo.core.Browser.browserType=='ie') offsetParent.style.position = "relative";
				popup.style.top = Math.ceil((offsetParent.offsetHeight - popup.offsetHeight) / 2) + "px" ;
			}
		
			// hack for position popup alway top in IE6.
			var checkHeight = popup.offsetHeight > 300; 

			if (document.getElementById("UIDockBar") && checkHeight) {
				popup.style.top = "6px";
			}
			if(eXo.core.I18n.lt) popup.style.left = Math.ceil((offsetParent.offsetWidth - popup.offsetWidth) / 2) + "px" ;
			else popup.style.right = Math.ceil((offsetParent.offsetWidth - popup.offsetWidth) / 2) + "px" ;
		
		}
		if (gj(popup).offset().top < 0) popup.style.top = scrollY + "px" ;
  }
  popup.style.visibility = "visible" ;
} ;
/**
 * @param {Object} evt
 */
WCMUIPopupWindow.prototype.increasezIndex = function(popup) {
	if(typeof(popup) == "string") popup = document.getElementById(popup) ;
	var portalApp = document.getElementById("UIPortalApplication") ;
  var uiLogin = gj(portalApp).find("div.UILoginForm:first")[0]; 
  if(uiLogin) {
    var curMaskzIndex = parseInt(gj(document.getElementById('UIMaskWorkspace')).css("zIndex"));
    popup.style.zIndex = ++curMaskzIndex +"";
  }
}

/**
 * Hides (display: none) the popup window when the close button is clicked
 */
WCMUIPopupWindow.prototype.closePopupEvt = function(evt) {
	gj(this).parents(".UIDragObject:first")[0].style.display = "none" ;
}
/**
 * Called when the window starts being resized
 * sets the onmousemove and onmouseup events on the portal application (not the popup)
 * associates these events with WCMUIPopupWindow.resize and WCMUIPopupWindow.endResizeEvt respectively
 */
WCMUIPopupWindow.prototype.startResizeEvt = function(evt) {
  //disable select text
  eXo.webui.WCMUIPopupWindow.backupEvent = null;
  if (navigator.userAgent.indexOf("MSIE") >= 0) {
    //Need to check if we have remove resizedPopup after last mouseUp
    //IE bug: not call endResizeEvt when mouse moved out of page
    if (!eXo.webui.WCMUIPopupWindow.resizedPopup && document.onselectstart) {
      eXo.webui.WCMUIPopupWindow.backupEvent = document.onselectstart;
    }
    document.onselectstart = function() {return false};   
  } else {    
    if (document.onmousedown) {
      eXo.webui.WCMUIPopupWindow.backupEvent = document.onmousedown;
    }
    document.onmousedown = function() {return false};   
  } 


  var targetPopup = gj(this).parents(".UIPopupWindow:first")[0];
  eXo.webui.WCMUIPopupWindow.resizedPopup = targetPopup;
  eXo.webui.WCMUIPopupWindow.backupPointerY = eXo.core.Browser.findMouseRelativeY(targetPopup, evt) ;     
  
  document.onmousemove = eXo.webui.WCMUIPopupWindow.resize;
  document.onmouseup = eXo.webui.WCMUIPopupWindow.endResizeEvt ;
}

/**
 * 1. Popup window 's bottom 's height is required to set correctly 'Resize' button during resize process
 * 
 * 2. For unknow reasons, property 'offsetHeight' of the bottom div is not accessible during resize process
 * It's likely that the bottom 'div' is locked during that period of time.
 * 
 * 3. As bottom height is unchanged across popup window (as long as it has bottom), we store its height in a
 * global variable
 */
var POPUP_WINDOW_BOTTOM_HEIGHT=50;

/**
 * Function called when the window is being resized
 *  . gets the position of the mouse
 *  . calculates the height and the width of the window from this position
 *  . sets these values to the window
 */
WCMUIPopupWindow.prototype.resize = function(evt) {
  var targetPopup = eXo.webui.WCMUIPopupWindow.resizedPopup ;
  var content = gj(targetPopup).find("div.PopupContent:first")[0];
  var isRTL = eXo.core.I18n.isRT();
  var pointerX = eXo.core.Browser.findMouseRelativeX(targetPopup, evt, isRTL) ;
  var pointerY = eXo.core.Browser.findMouseRelativeY(targetPopup, evt) ;
  
  var delta = pointerY - eXo.webui.WCMUIPopupWindow.backupPointerY; 
  if ((content.offsetHeight + delta) > 0) {
    eXo.webui.WCMUIPopupWindow.backupPointerY = pointerY;   
    content.style.height = content.offsetHeight + delta +"px" ; 
  }
  targetPopup.style.height = "auto";
  
  if(isRTL){
   pointerX = (-1) * pointerX;
  } 
  if(pointerX > 200) targetPopup.style.width = (pointerX+10) + "px" ; 
} ;

/**
 * Called when the window stops being resized
 * cancels the mouse events on the portal app
 * inits the scroll managers active on this page (in case there is one in the popup)
 */
WCMUIPopupWindow.prototype.endResizeEvt = function(evt) {
  eXo.webui.WCMUIPopupWindow.popupId = null;
  this.onmousemove = null;
  this.onmouseup = null;
  
  //enable select text
  if (navigator.userAgent.indexOf("MSIE") >= 0) {
    document.onselectstart = eXo.webui.WCMUIPopupWindow.backupEvent;
  } else {    
    document.onmousedown = eXo.webui.WCMUIPopupWindow.backupEvent;
  }
  eXo.webui.WCMUIPopupWindow.backupEvent = null;  
  // Added by Philippe
  // inits all the scroll managers, in case there is one in the popup that needs to be recalculated
  //eXo.portal.UIPortalControl.initAllManagers();
  // other solutions :
  // - add a callback property that points to the init function of the concerned scroll manager. call it here
  // - add a boolean to each scroll manager that specifies if it's in a popup. re init only those that have this property true
}


/**
 * Inits the drag and drop
 * configures the DragDrop callback functions
 *  . initCallback : sets overflow: hidden to elements in the popup if browser is mozilla
 *  . dragCallback : empty
 *  . dropCallback : sets overflow: auto to elements in the popup if browser is mozilla
 */
WCMUIPopupWindow.prototype.initDND = function(popupBar, popup) {
  eXo.core.DragDrop.init(popupBar, popup);

  popup.onDragStart = function(x, y, last_x, last_y, e)
  {
    if (eXo.core.Browser.isFF() && popup.uiWindowContent)
    {
      popup.uiWindowContent.style.overflow = "auto";
      gj(popup.uiWindowContent).find("ul.PopupMessageBox").css("overflow", "auto");
    }
  };

  popup.onDrag = function(nx, ny, ex, ey, e)
  {
  };

  popup.onDragEnd = function(x, y, clientX, clientY)
  {
    if (eXo.core.Browser.isFF() && popup.uiWindowContent)
    {
      popup.uiWindowContent.style.overflow = "auto";
      gj(popup.uiWindowContent).find("ul.PopupMessageBox").css("overflow", "auto");
    }
    var offsetParent = popup.offsetParent;
    if(!offsetParent) offsetParent = document.getElementById('UIWorkingWorkspace');
    if (offsetParent)
    {
      if (clientY < 0)
      {
        popup.style.top = (0 - offsetParent.offsetTop) + "px";
      }
    }
    else
    {
      alert('aaa');
      popup.style.top = "0px";
    }
  };

  popup.onCancel = function(e)
  {
  };

} ;


eXo.webui.WCMUIPopupWindow = new WCMUIPopupWindow();
_module.WCMUIPopupWindow = eXo.webui.WCMUIPopupWindow;
