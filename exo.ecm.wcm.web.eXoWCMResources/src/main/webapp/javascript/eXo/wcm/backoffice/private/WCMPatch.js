/*------------------Overrite method eXo.webui.UIPopup.init to show popup display center-------------------------------
UIPopupWindow.prototype.init = function(popupId, isShow, isResizable, showCloseButton, isShowMask) {
	window.scroll(0, 0);	
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

	popupBar.onmousedown = this.initDND ;
	
	if(isShow == false) {
		this.superClass.hide(popup) ;
		if(isShowMask) eXo.webui.UIPopupWindow.showMask(popup, false) ;
	} 
	
	if(isResizable) {
		var resizeBtn = DOMUtil.findFirstDescendantByClass(popup, "div", "ResizeButton");
		resizeBtn.style.display = 'block' ;
		resizeBtn.onmousedown = this.startResizeEvt ;
		portalApp.onmouseup = this.endResizeEvt ;
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