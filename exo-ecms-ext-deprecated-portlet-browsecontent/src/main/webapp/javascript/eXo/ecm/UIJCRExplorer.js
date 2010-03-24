
function UIJCRExplorer() {
	this.vnScrollMgr = null; // View FilePlan Node
	this.ntScrollMgr = null; // Node Type Popup
};

UIJCRExplorer.prototype.loadViewNodeScroll = function(e) {

	var jcr = eXo.ecm.UIJCRExplorer;
	var uiFilePlanView = document.getElementById("UIFilePlanView");
	if (uiFilePlanView) {
		jcr.vnScrollMgr = eXo.portal.UIPortalControl.newScrollManager("UIFilePlanView");
		jcr.vnScrollMgr.margin = 8;
		jcr.vnScrollMgr.initFunction = jcr.initViewNodeScroll;
		var mainCont = eXo.core.DOMUtil.findFirstDescendantByClass(uiFilePlanView, "div", "UIHorizontalTabs");
		var tabs = eXo.core.DOMUtil.findFirstDescendantByClass(mainCont, "div", "TabsContainer");
		var arrows = eXo.core.DOMUtil.findFirstDescendantByClass(mainCont, "div", "NavigationButtonContainer");
		jcr.vnScrollMgr.mainContainer = mainCont;
		jcr.vnScrollMgr.arrowsContainer = arrows;
		jcr.vnScrollMgr.loadElements("UITab");
		//var arrowButtons = eXo.core.DOMUtil.findDescendantsByTagName(arrows, "div");
		var arrowButtons = eXo.core.DOMUtil.findDescendantsByClass(arrows, "div", "NavigationIcon");
		if (arrowButtons.length == 2) {
			jcr.vnScrollMgr.initArrowButton(arrowButtons[0], "left", "NavigationIcon ScrollBackArrow16x16Icon", "NavigationIcon DisableBackArrow16x16Icon", "NavigationIcon DisableBackArrow16x16Icon");
			jcr.vnScrollMgr.initArrowButton(arrowButtons[1], "right", "NavigationIcon ScrollNextArrow16x16Icon", "NavigationIcon DisableNextArrow16x16Icon", "NavigationIcon DisableNextArrow16x16Icon");
		}
		jcr.vnScrollMgr.callback = jcr.viewNodeScrollCallback;
		jcr.initViewNodeScroll();
	}
};

UIJCRExplorer.prototype.initViewNodeScroll = function(e) {
	var scrollMgr = eXo.ecm.UIJCRExplorer.vnScrollMgr;
	scrollMgr.init();
	// Gets the maximum width available for the tabs
	scrollMgr.checkAvailableSpace();
	scrollMgr.renderElements();
};

UIJCRExplorer.prototype.viewNodeScrollCallback = function() {
	var scrollMgr = eXo.ecm.UIJCRExplorer.vnScrollMgr;
	var selTab = eXo.core.DOMUtil.findFirstDescendantByClass(scrollMgr.mainContainer, "div", "SelectedTab");
	if (selTab) {
		scrollMgr.cleanElements();
		scrollMgr.getElementsSpace();
	}
};

UIJCRExplorer.prototype.loadNodeTypeScroll = function() {
	var jcr = eXo.ecm.UIJCRExplorer;
	var uiPopup = document.getElementById("UINodeTypeInfoPopup");
	if (uiPopup) {
		jcr.ntScrollMgr = eXo.portal.UIPortalControl.newScrollManager("UINodeTypeInfoPopup");
		jcr.ntScrollMgr.margin = 5;
		jcr.ntScrollMgr.initFunction = jcr.initNodeTypeScroll;
		var mainCont = eXo.core.DOMUtil.findFirstDescendantByClass(uiPopup, "div", "UIHorizontalTabs");
		var tabs = eXo.core.DOMUtil.findFirstDescendantByClass(mainCont, "div", "TabsContainer");
		var arrows = eXo.core.DOMUtil.findFirstDescendantByClass(mainCont, "div", "NavigationButtonContainer");
		jcr.ntScrollMgr.mainContainer = mainCont;
		jcr.ntScrollMgr.arrowsContainer = arrows;
		jcr.ntScrollMgr.loadElements("UITab");
		//var arrowButtons = eXo.core.DOMUtil.findDescendantsByTagName(arrows, "div");
		var arrowButtons = eXo.core.DOMUtil.findDescendantsByClass(arrows, "div", "NavigationIcon");
		if (arrowButtons.length == 2) {
			jcr.ntScrollMgr.initArrowButton(arrowButtons[0], "left", "NavigationIcon ScrollBackArrow16x16Icon", "NavigationIcon DisableBackArrow16x16Icon", "NavigationIcon DisableBackArrow16x16Icon");
			jcr.ntScrollMgr.initArrowButton(arrowButtons[1], "right", "NavigationIcon ScrollNextArrow16x16Icon", "NavigationIcon DisableNextArrow16x16Icon", "NavigationIcon DisableNextArrow16x16Icon");
		}
		//jcr.ntScrollMgr.callback = jcr.scrollCallback;
		jcr.initNodeTypeScroll();
	}
};

UIJCRExplorer.prototype.initNodeTypeScroll = function() {
	var scrollMgr = eXo.ecm.UIJCRExplorer.ntScrollMgr;
	scrollMgr.init();
	// Gets the maximum width available for the tabs
	scrollMgr.checkAvailableSpace();
	scrollMgr.renderElements();
};

UIJCRExplorer.prototype.initEvent = function(uniqueId) {
	var store =	eXo.portal.UIControlWorkspace.showWorkspace;
	eXo.portal.UIControlWorkspace.showWorkspace = function() {
		store.call(eXo.portal.UIControlWorkspace);
		eXo.ecm.UIJCRExplorer.dropDownIconList(uniqueId);
	}
	eXo.ecm.UIJCRExplorer.dropDownIconList(uniqueId);
	if (eXo.core.Browser.isFF()) {
		eXo.core.Browser.addOnResizeCallback(
			'ECMresize', 
			function(){eXo.ecm.UIJCRExplorer.dropDownIconList(uniqueId)}
		);
	}
};

UIJCRExplorer.prototype.dropDownIconList = function(uniqueId) {
 	var DOMUtil = eXo.core.DOMUtil;
	var actionBar = document.getElementById(uniqueId);
	if (!actionBar) return;
	var activeBoxContent = DOMUtil.findFirstDescendantByClass(actionBar, "div", "ActiveBoxContent");
	var actionBgs = DOMUtil.findChildrenByClass(activeBoxContent, "div", "ActionBg");
	var nSize = actionBgs.length;
	if (nSize) {
		var storeBoxContentContainer = DOMUtil.findFirstDescendantByClass(actionBar, "div", "StoreBoxContentContainer");
		storeBoxContentContainer.style.display = "block";
		var showHideBoxContainer = DOMUtil.findFirstDescendantByClass(actionBar, "div", "ShowHideBoxContainer");
		showHideBoxContainer.innerHTML = "";
		var posY = eXo.core.Browser.findPosY(activeBoxContent);
		for (var o = 0; o < nSize; ++ o) {
			actionBgs[o].style.display = "block";
			Y = eXo.core.Browser.findPosY(actionBgs[o]);
			if (Y - posY) {
				showHideBoxContainer.appendChild(actionBgs[o].cloneNode(true));
				actionBgs[o].style.display = "none";
			}
		}
		if (showHideBoxContainer.innerHTML != "") {
			var clearElement = document.createElement("div");
			clearElement.style.clear = "left";
			showHideBoxContainer.appendChild(clearElement);
		} else {
			storeBoxContentContainer.style.display = "none";
		}
	}
};

eXo.ecm.UIJCRExplorer = new UIJCRExplorer();