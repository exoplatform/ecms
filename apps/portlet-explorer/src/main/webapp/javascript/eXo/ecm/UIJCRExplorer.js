(function(gj) {
	function UIJCRExplorer() {
		this.vnScrollMgr = null; // View FilePlan Node
		this.ntScrollMgr = null; // Node Type Popup
	};
	
	UIJCRExplorer.prototype.loadViewNodeScroll = function(e) {
	
		var jcr = eXo.ecm.UIJCRExplorer;
		var uiFilePlanView = document.getElementById("UIFilePlanView");
		if (uiFilePlanView) {
			jcr.vnScrollMgr = new ScrollManager("UIFilePlanView");
			jcr.vnScrollMgr.margin = 8;
			jcr.vnScrollMgr.initFunction = jcr.initViewNodeScroll;
			var mainCont = gj(uiFilePlanView).find("div.UIHorizontalTabs:first")[0];
			var tabs = gj(mainCont).find("div.TabsContainer:first")[0];
			var arrows = gj(mainCont).find("div.NavigationButtonContainer:first")[0];
			jcr.vnScrollMgr.mainContainer = mainCont;
			jcr.vnScrollMgr.arrowsContainer = arrows;
			jcr.vnScrollMgr.loadElements("UITab");
			//var arrowButtons = eXo.core.DOMUtil.findDescendantsByTagName(arrows, "div");
			var arrowButtons = gj(arrows).find("div.NavigationIcon");
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
		var selTab = gj(scrollMgr.mainContainer).find("div.SelectedTab:first")[0];
		if (selTab) {
			scrollMgr.cleanElements();
			scrollMgr.getElementsSpace();
		}
	};
	
	UIJCRExplorer.prototype.loadNodeTypeScroll = function() {
		var jcr = eXo.ecm.UIJCRExplorer;
		var uiPopup = document.getElementById("UINodeTypeInfoPopup");
		if (uiPopup) {
			jcr.ntScrollMgr = new ScrollManager("UINodeTypeInfoPopup");
			jcr.ntScrollMgr.margin = 5;
			jcr.ntScrollMgr.initFunction = jcr.initNodeTypeScroll;
			var mainCont = gj(uiPopup).find("div.UIHorizontalTabs:first")[0];
			var tabs = gj(mainCont).find("div.TabsContainer:first")[0];
			var arrows = gj(mainCont).find("div.NavigationButtonContainer:first")[0];
			jcr.ntScrollMgr.mainContainer = mainCont;
			jcr.ntScrollMgr.arrowsContainer = arrows;
			jcr.ntScrollMgr.loadElements("UITab");
			//var arrowButtons = eXo.core.DOMUtil.findDescendantsByTagName(arrows, "div");
			var arrowButtons = gj(arrows).find("div.NavigationIcon");
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
	
	UIJCRExplorer.prototype.dropDownIconList = function(uniqueId) {
		var actionBar = document.getElementById(uniqueId);
		if (!actionBar) return;
		var activeBoxContent = gj(actionBar).find("div.ActiveBoxContent:first")[0];
		var actionBgs = gj(activeBoxContent).children("div.ActionBg");
		var nSize = actionBgs.length;
		if (nSize) {
			var storeBoxContentContainer = gj(actionBar).find("div.StoreBoxContentContainer:first")[0];
			storeBoxContentContainer.style.display = "block";
			var showHideBoxContainer = gj(actionBar).find("div.ShowHideBoxContainer:first")[0];
			showHideBoxContainer.innerHTML = "";
			var posY = gj(activeBoxContent).offset().top;
			for (var o = 0; o < nSize; ++ o) {
				actionBgs[o].style.display = "block";
				Y = gj(actionBgs[o]).offset().top;
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
	
	/**
	 * Gets the tab element and the tab content associated and displays them
	 *  . changes the style of the tab
	 *  . displays the tab content of the selected tab (display: block)
	 * if tabId are provided, can get the tab content by Ajax
	 */
	UIJCRExplorer.prototype.displayTabContent = function(clickedEle) {
	  var uiSelectTab = gj(clickedEle).parents(".UITab:first")[0];
	
	  var uiHorizontalTabs = gj(clickedEle).parents(".UIHorizontalTabs:first")[0];
	  var uiTabs = gj(uiHorizontalTabs).find("li.UITab");
	  var parentdHorizontalTab = uiHorizontalTabs.parentNode ;
	  var contentTabContainer = gj(parentdHorizontalTab).find("div.UITabContentContainer:first")[0];
	  var uiTabContents = gj(contentTabContainer).children("div.UITabContent");
	//    var form = DOMUtil.getChildrenByTagName(contentTabContainer, "form") ;
	  var form = gj(contentTabContainer).children("form") ;
	    if(form.length > 0) {
	        var tmp = gj(form[0]).children("div.UITabContent");
	    for(var i = 0; i < tmp.length; i++) {
	        uiTabContents.push(tmp[i]) ;
	    }
	    }
	  var index = 0 ;
	  uiTabs.each(function(i, elem) {
	//	var styleTabDiv = DOMUtil.getChildrenByTagName(uiTabs[i], "div")[0] ;	  
	    var styleTabDiv = gj(uiTabs[i]).children("div")[0] ;
	    if (!(styleTabDiv.className == "DisabledTab")) {
		    var changeClassName = true;
		    if(uiSelectTab == uiTabs[i]) {
		      styleTabDiv.className = "SelectedTab" ;
		      index = i ;
		      changeClassName = false;
		    }
		    if (changeClassName) {
			    styleTabDiv.className = "NormalTab" ;
			    uiTabContents[i].style.display = "none" ;
		    }
	    }
	  });
	  uiTabContents[index].style.display = "block" ;
	    if (eXo.ecm.UIJCRExplorer) {
	        try {
	                eXo.ecm.UIJCRExplorer.initViewNodeScroll();
	        } catch(e) {void(0);}
	    }
	};
	
	eXo.ecm.UIJCRExplorer = new UIJCRExplorer();
	return {
		UIJCRExplorer : eXo.ecm.UIJCRExplorer
	};
})(gj);
