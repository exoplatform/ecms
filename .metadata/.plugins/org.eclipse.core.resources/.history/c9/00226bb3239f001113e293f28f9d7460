function NewsletterManager() {
}

NewsletterManager.prototype.checkAllSelected = function(checkBox) {
	var elements = checkBox.form.elements;
	var checked = checkBox.checked;
	for (i = 0; i < elements.length; i++) {
		if (elements[i].type == "checkbox") 
			elements[i].checked = checked;
	}
};

NewsletterManager.prototype.checkBeforeDelete = function(uiSubscriptionsForm, checkMess, confirmMess) {
	var isChecked = false;
	var elements = document.getElementById(uiSubscriptionsForm).elements;
	for(var i = 0; i < elements.length; i ++){
		if(elements[i].type == "checkbox" && elements[i].checked == true) isChecked = true;
	}
	if(isChecked == false){
		alert(checkMess);
		return false;
	}
	return confirm(confirmMess);
}


NewsletterManager.prototype.hide = function() {
	var ln = eXo.core.DOMUtil.hideElementList.length ;
	if (ln > 0) {
		for (var i = 0 ; i < ln ; i++) {
			eXo.core.DOMUtil.hideElementList[i].style.display = "none" ;
		}
	}
} ;

NewsletterManager.prototype.show = function(obj, evt) {
		if(!evt) evt = window.event ;
		evt.cancelBubble = true ;
		var DOMUtil = eXo.core.DOMUtil ;
		var uiPopupCategory = DOMUtil.findFirstDescendantByClass(obj, 'div', 'UIPopupCategory') ;	
		if (!uiPopupCategory) return ;	
		if(uiPopupCategory.style.display == "none") {
				eXo.ecm.NewsletterManager.hide();
				if (eXo.core.Browser.isIE7()) {	
						uiPopupCategory.style.left = "-176px";
			  	uiPopupCategory.style.top = "16px";
						uiPopupCategory.style.display = "block";			
		  }
		  else {
		  		uiPopupCategory.style.display = "block";
		  }
				eXo.core.DOMUtil.listHideElements(uiPopupCategory);
		}	
		else uiPopupCategory.style.display = "none" ;
}

eXo.ecm.NewsletterManager = new NewsletterManager();
