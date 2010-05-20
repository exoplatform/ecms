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

eXo.ecm.NewsletterManager = new NewsletterManager();