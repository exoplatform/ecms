function WCMQuickEdit() {
}

WCMQuickEdit.prototype.removeQuickeditingBlock = function(portletID, quickEditingBlockId) {
	var presentation = document.getElementById(portletID);
	var pNode = presentation.parentNode;
	var quickEditingBlock = document.getElementById(quickEditingBlockId);
	if(quickEditingBlock != null) {
		pNode.removeChild(quickEditingBlock);
	}
};

WCMQuickEdit.prototype.insertQuickeditingBlock = function(portletID, quickEditingBlockId) {
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

eXo.ecm.QuickEdit = new WCMQuickEdit();