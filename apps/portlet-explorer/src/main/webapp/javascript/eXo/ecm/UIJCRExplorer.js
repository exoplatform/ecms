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
	UIJCRExplorer.prototype.displayTabContent = function(clickedItem) {
        var uiNodeTypeInfo = gj("#UINodeTypeInfoPopup");
        var nav = gj(uiNodeTypeInfo).find("ul.nav-tabs:first")[0];
        var listHiddenTabsContainer = gj(nav).find("li.listHiddenTabsContainer:first")[0];
        var uiDropdownContainer   = gj(listHiddenTabsContainer).find("ul.dropdown-menu:first")[0];
        var navPaddingLeft = gj(nav).css("padding-left").replace("px","");
        var navPaddingRight = gj(nav).css("padding-right").replace("px","");
        var allowedSpace  = nav.offsetWidth - navPaddingLeft - navPaddingRight - gj(listHiddenTabsContainer).width();
        var totalNavsLength = 0;

        // Set active for clicked tab
        gj(nav).children("li").removeClass("active");
        gj(uiDropdownContainer).children("li").removeClass("active");
        gj(clickedItem).addClass("active");

        // Show tab content
        gj(uiNodeTypeInfo).find(".UITabContent").hide();
        gj(uiNodeTypeInfo).find("div[nodetypename='" + gj(clickedItem).text().trim().replace(':','').replace('-','') + "']").show();

        // Total length of navigation items
        gj(nav).children("li").not(".dropdown").each(function(i) {
            totalNavsLength += gj(this).width();
        });

        // If tab item in dropdown clicked, move it to tab bar
        if (gj(clickedItem).closest(".dropdown").length) {
            nav.appendChild(clickedItem);
            gj(clickedItem).removeClass("last").addClass("moved");
            totalNavsLength = totalNavsLength + gj(clickedItem).width();

            // Move last tab bar item to first of dropdown
            while (totalNavsLength > allowedSpace) {
                var tabBarLastNavItem = gj(nav).children("li").not(".dropdown").not(".moved").last()[0];
                totalNavsLength = totalNavsLength - gj(tabBarLastNavItem).width();
                gj(uiDropdownContainer).prepend(tabBarLastNavItem);
            }
            gj(clickedItem).removeClass("moved");

            // If allowedSpace still avaiable, move tab items from dropdown to bar
            while (totalNavsLength < allowedSpace) {
                var dropdownFirstNavItem = gj(uiDropdownContainer).children("li").first()[0];
                nav.appendChild(dropdownFirstNavItem);
                if (totalNavsLength + gj(dropdownFirstNavItem).width() > allowedSpace) {
                    gj(uiDropdownContainer).prepend(dropdownFirstNavItem);
                    break;
                }
                totalNavsLength += gj(dropdownFirstNavItem).width();
            }
            gj(uiDropdownContainer).children("li").last().addClass("last");
        }

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
