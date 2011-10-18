 function ECMUtils() {
	var Self = this;

	//set private property;
	var DOM = eXo.core.DOMUtil;
	var Browser = eXo.core.Browser;
	var RightClick = eXo.webui.UIRightClickPopupMenu;
	if (eXo.require && !RightClick) {
		eXo.require('eXo.webui.UIRightClickPopupMenu');
	    RightClick = eXo.webui.UIRightClickPopupMenu;
	}
	ECMUtils.prototype.popupArray = [];
	ECMUtils.prototype.init = function(portletId) {
		var portlet = document.getElementById(portletId) ;
		if(!portlet) return ;
		RightClick.disableContextMenu(portletId) ;
		portlet.onmousedown = function(event) {
			eXo.ecm.ECMUtils.closeAllPopup() ;
		}
		if(document.getElementById("UIPageDesktop")) {
			Self.fixHeight(portletId) ;
			var uiPageDeskTop = document.getElementById("UIPageDesktop");
			var uiJCRExplorers = DOM.findDescendantsByClass(uiPageDeskTop, 'div', 'UIJCRExplorer') ;
			if (uiJCRExplorers.length) {
				for (var i = 0; i < uiJCRExplorers.length; i++) {
					var uiResizeBlock = DOM.findAncestorByClass(uiJCRExplorers[i], "UIResizableBlock");
					if (uiResizeBlock) uiResizeBlock.style.overflow = "hidden";
				}
			}
		} else {
			Self.controlLayout(portletId) ;
			eXo.core.Browser.addOnResizeCallback(
				'controlLayout',
				function(){
					eXo.ecm.ECMUtils.controlLayout(portletId);
				}
			);
		}
	};

	ECMUtils.prototype.fixHeight = function(portletId) {
		var portlet = document.getElementById(portletId);
 	 	var refElement = DOM.findAncestorByClass(portlet, "UIApplication");
 	 	if (!refElement) return;

 	 	// 30/06/2009
 	 	//Recalculate height of UIResizableBlock in the UISideBarContainer
 	 	//var delta = parseInt(refElement.style.height) - portlet.offsetHeight;

 	 	var uiControl = document.getElementById('UIControl');
 	 	var uiSideBar = document.getElementById('UISideBar');
 	 	if(!uiControl || !uiSideBar) return;
 	 	var uiSideBarControl = DOM.findFirstDescendantByClass(uiSideBar, 'div', 'UISideBarControl');
 	 	if(!uiSideBarControl) return;
 	 	var deltaH = refElement.offsetHeight - uiControl.offsetHeight - uiSideBarControl.offsetHeight;
 	 	var resizeObj = DOM.findDescendantsByClass(portlet, 'div', 'UIResizableBlock');
 	 	if(resizeObj.length) {
	 	 	for(var i = 0; i < resizeObj.length; i++) {
	 	 	  resizeObj[i].style.display = 'block';
	 	 	  resizeObj[i].style.height = (resizeObj[i].offsetHeight + deltaH) + "px";
	 	 	}
 	 	}
	};

	ECMUtils.prototype.controlLayout = function(portletId) {
		var portlet = document.getElementById(portletId) ;
		var uiWorkingArea = DOM.findFirstDescendantByClass(portlet, 'div', 'UIWorkingArea');
		if (!uiWorkingArea) return;
		var delta = document.body.scrollHeight - eXo.core.Browser.getBrowserHeight();
		var uiDocumentWorkspace = DOM.findFirstDescendantByClass(portlet, 'div', 'UIDocumentWorkspace');
		if (delta < 0) {
			var resizeObj = DOM.findDescendantsByClass(portlet, 'div', 'UIResizableBlock');
			if(resizeObj.length) {
				for(var i = 0; i < resizeObj.length; i++) {
					resizeObj[i].style.height = resizeObj[i].offsetHeight - delta + "px" ;
				}
			}
			if (uiDocumentWorkspace) uiDocumentWorkspace.style.height = uiDocumentWorkspace.offsetHeight - delta + "px";
		}
		if (uiDocumentWorkspace) uiDocumentWorkspace.style.height = uiWorkingArea.offsetHeight + "px";
		eXo.core.Browser.addOnResizeCallback('controlLayout', function(){eXo.ecm.ECMUtils.controlLayout(portletId)});
	};

	ECMUtils.prototype.clickLeftMouse = function(event, clickedElement, position, option) {
		var event = event || window.event;
		event.cancelBubble = true;
		popupSelector = DOM.findAncestorByClass(clickedElement, "UIPopupSelector");
		showBlock = DOM.findFirstDescendantByClass(popupSelector,"div", "UISelectContent");
		if(option == 1) {
			showBlock.style.width = (popupSelector.offsetWidth - 2) + "px";
		}
		if(showBlock.style.display == "block") {
			eXo.webui.UIPopup.hide(showBlock) ;
			return ;
		}
		eXo.webui.UIPopup.show(showBlock) ;
		showBlock.onmousedown = function(event) {
			var event = event || window.event ;
			event.cancelBubble = true ;
		}
		Self.popupArray.push(showBlock);
		showBlock.style.top = popupSelector.offsetHeight + "px";
	};

	ECMUtils.prototype.closeAllPopup = function() {
		for(var i = 0; i < Self.popupArray.length; i++) {
			Self.popupArray[i].style.display = "none" ;
		}
		Self.popupArray.clear() ;
	};

	ECMUtils.prototype.initVote = function(voteId, rate) {
		var vote = document.getElementById(voteId) ;
		vote.rate = rate = parseInt(rate) ;
		var optsContainer = DOM.findFirstDescendantByClass(vote, "div", "OptionsContainer") ;
		var options = DOM.getChildrenByTagName(optsContainer, "div") ;
		for(var i = 0; i < options.length; i++) {
			options[i].onmouseover = Self.overVote ;
			if(i < rate) options[i].className = "RatedVote" ;
		}

		vote.onmouseover = function() {
			var optsCon= DOM.findFirstDescendantByClass(this, "div", "OptionsContainer") ;
			var opts = DOM.getChildrenByTagName(optsCon, "div") ;
			for(var j = 0; j < opts.length; j++) {
				if(j < this.rate) opts[j].className = "RatedVote" ;
				else opts[j].className = "NormalVote" ;
			}
		}
		optsContainer.onmouseover = function(event) {
			var event = event || window.event ;
			event.cancelBubble = true ;
		}
	};

	ECMUtils.prototype.overVote = function(event) {
		var optionsContainer = DOM.findAncestorByClass(this, "OptionsContainer") ;
		var opts = DOM.getChildrenByTagName(optionsContainer, "div") ;
		var i = opts.length;
		for(--i; i >= 0; i--) {
			if(opts[i] == this) break ;
			opts[i].className = "NormalVote" ;
		}
		if(opts[i].className == "OverVote") return ;
		for(; i >= 0; i--) {
			opts[i].className = "OverVote" ;
		}
	};

	ECMUtils.prototype.showHideExtendedView = function(event) {
	  var elemt = document.getElementById("ListExtendedView");
	  event = event || window.event;
	  event.cancelBubble = true;
    var iconTree = document.getElementById("iconTreeExplorer");
	  if(elemt.style.display == 'none') {
	    elemt.style.display = 'block';
	    iconTree.style.position = 'static';
	  } else {
	    elemt.style.display = 'none' ;
	    iconTree.style.position = 'relative';
	  }
	  DOM.listHideElements(elemt);
	}

	ECMUtils.prototype.showHideComponent = function(elemtClicked) {

		var nodeReference = DOM.findAncestorByClass(elemtClicked,  "ShowHideContainer");
		var elemt = DOM.findFirstDescendantByClass(nodeReference, "div", "ShowHideComponent") ;

		if(elemt.style.display == 'none') {
			elemtClicked.childNodes[0].style.display = 'none' ;
			elemtClicked.childNodes[1].style.display = 'block' ;
			elemt.style.display = 'block' ;
			eXo.ecm.ECMUtils.setScrollBar();
		} else {
			elemtClicked.childNodes[0].style.display = 'block' ;
			elemtClicked.childNodes[1].style.display = 'none' ;
			elemt.style.display = 'none' ;
		}
	};

	ECMUtils.prototype.setScrollBar = function()  {
    try	{
      var elementWorkingArea = document.getElementById('UIWorkingArea');
      var parent = document.getElementById('TabContainerParent');
      if(parent!=null)	{
        var elements  = eXo.core.DOMUtil.findDescendantsByClass(parent,"div", "UITabContent");
        if(elements!=null)	{
					for(i=0;i<elements.length;i++)
					{
						var obj = elements[i];
						if(obj.style.display!="none")	{
							var height = obj.offsetHeight;
							if(height>430)	{
							  //obj.style.height="470px";
								obj.style.height=elementWorkingArea.offsetHeight-50+"px";
							  obj.style.overflow="auto";
							}
						}
					}
				}
      }
    }
    catch(err){}
  }

	ECMUtils.prototype.showHideContentOnRow = function(elemtClicked) {

		var nodeReference = DOM.findAncestorByClass(elemtClicked,  "Text");
		var elemt = DOM.findFirstDescendantByClass(nodeReference, "div", "ShowHideComponent") ;
		var shortContent = DOM.findFirstDescendantByClass(elemt, "div", "ShortContentPermission") ;
		var fullContent = DOM.findFirstDescendantByClass(elemt, "div", "FullContentPermission") ;

		if(shortContent.style.display == 'none') {
			fullContent.style.display = 'none';
			shortContent.style.display = 'block';
		} else {
			fullContent.style.display = 'block';
			shortContent.style.display = 'none';
		}
	};

	ECMUtils.prototype.isEventTarget = function(element, e) {
		if (window.event) e = window.event;
		var srcEl = e.srcElement? e.srcElement : e.target;
		if (element == srcEl) {
			return true;
		}
		return false;
	};

	ECMUtils.prototype.focusCurrentNodeInTree = function(id) {
		var element = document.getElementById(id);
		if (!element) return;
		var sidebar = DOM.findAncestorByClass(element,  "SideContent");
		var top = element.offsetTop;
		sidebar.scrollTop = (top - sidebar.offsetTop);
	};

	ECMUtils.prototype.collapseExpand = function(element) {
		var node = element.parentNode ;
		var subGroup = DOM.findFirstChildByClass(node, "div", "NodeGroup") ;
		if(!subGroup) return false;
		if(subGroup.style.display == "none") {
			if (element.className.match("ExpandIcon")) element.className = element.className.replace("ExpandIcon", "CollapseIcon");
			subGroup.style.display = "block" ;
		} else {
			if (element.className.match("CollapseIcon")) element.className = element.className.replace("CollapseIcon", "ExpandIcon");
			subGroup.style.display = "none" ;
		}
		return true;
	};

	ECMUtils.prototype.collapseExpandPart = function(element) {
		var node = element.parentNode ;
		var subGroup1 = DOM.findFirstChildByClass(node, "div", "NodeGroup1") ;
		var subGroup2 = DOM.findFirstChildByClass(node, "div", "NodeGroup2") ;
		if (subGroup1.style.display == "none") {
			if (element.className == "CollapseIcon") 	element.className = "ExpandIcon";
			subGroup1.style.display = "block";
			subGroup2.style.display = "none";
		} else {
			if (element.className == "ExpandIcon") element.className = "CollapseIcon";
			subGroup1.style.display = "none";
			subGroup2.style.display = "block";
		}
		return true;
	};

	ECMUtils.prototype.filterValue = function(frmId) {
		var form = document.getElementById(frmId) ;
		if (eXo.core.Browser.browserType == "ie") {
			var text = document.createTextNode(form['tempSel'].innerHTML);
			form['result'].appendChild(text);
		}else {
		  form['result'].innerHTML = form['tempSel'].innerHTML ;
		}
		var	filterValue = form['filter'].value ;
		filterValue = filterValue.replace("*", ".*") ;
		var re = new RegExp(filterValue, "i") ;
		var elSel = form['result'];
	  var i;
	  for (i = elSel.length - 1; i>=0; i--) {
	    if (!re.test(elSel.options[i].value)) {
	      elSel.remove(i);
	    }
	  }
	};

	ECMUtils.prototype.convertElemtToHTML = function(id) {
		var elemt = document.getElementById(id) ;
		var text = elemt.innerHTML ;
		text = text.toString() ;

		text = text.replace(/&/g, "&amp;").replace(/"/g, "&quot;")
							 .replace(/</g, "&lt;").replace(/>/g, "&gt;") ;

		elemt.innerHTML = text ;
	};

	ECMUtils.prototype.onKeyAddressBarPress = function() {
		var uiAddressBarControl = document.getElementById("AddressBarControl");
		if(uiAddressBarControl) {
			uiAddressBarControl.onkeypress = Self.onAddressBarEnterPress ;
		}
	};

	ECMUtils.prototype.onKeySimpleSearchPress = function() {
		var uiAddressBarControl = document.getElementById("SimpleSearchControl");
		if(uiAddressBarControl) {
			uiAddressBarControl.onkeypress = Self.onSimpleSearchEnterPress ;
		}
	};

	ECMUtils.prototype.onSimpleSearchEnterPress = function(event) {
		var gotoLocation = document.getElementById("SimpleSearch");
		var event = event || window.event;
		if(gotoLocation && event.keyCode == 13) {
			eval(gotoLocation.href);
			return false;
		}
	};

	ECMUtils.prototype.onAddressBarEnterPress = function(event) {
		var gotoLocation = document.getElementById("GotoLocation");
		var event = event || window.event;
		if(gotoLocation && event.keyCode == 13) {
			eval(gotoLocation.href);
			return false;
		}
	};

  ECMUtils.prototype.insertContentToIframe = function(i) {
    var original = document.getElementById("original" + i);
    var resived = document.getElementById("revised" + i);
    try {
	    if(resived != null) {
        resivedDoc = resived.contentWindow.document;
        resivedDoc.open() ;
				resivedDoc.write(resived.getAttribute("content")) ;
				resivedDoc.close() ;
			}
			if(original != null) {
  			var originaleDoc = original.contentWindow.document;
				originaleDoc.open() ;
				originaleDoc.write(original.getAttribute("content")) ;
				originaleDoc.close() ;
			}
		} catch (ex) {}
  };


	ECMUtils.prototype.replaceToIframe = function(txtAreaId) {
		if (!document.getElementById(txtAreaId)) {
			/*
			 * minh.js.exo
			 * fix bug ECM-1419
			 * this is Java bug.
			 * double call this method.
			 */
			return ;
		}
		var txtArea = document.getElementById(txtAreaId) ;
		var ifrm = document.createElement("IFRAME") ;
		with(ifrm) {
			className = 'ECMIframe' ;
			src = 'javascript:void(0)' ;
			frameBorder = 0 ;
			scrolling = "auto" ;
		}
		var strValue = txtArea.value ;
		txtArea.parentNode.replaceChild(ifrm, txtArea) ;
		try {
			var doc = ifrm.contentWindow.document ;
			doc.open() ;
			doc.write(strValue) ;
			doc.close() ;
		} catch (ex) {}
	} ;

	ECMUtils.prototype.generatePublicWebDAVLink = function(serverInfo, restContextName, repository, workspace, nodePath) {
	  // query parameter s must be encoded.
	  var path = "/";
	  nodePath = nodePath.substr(1).split("\/");
	  for (var i=0; i < nodePath.length; i++) {
	    path += encodeURIComponent(nodePath[i]) + "/";
	  }
	  window.open(serverInfo + "/" + restContextName + "/jcr/" + repository + "/" + workspace + path, '_new');
	} ;

	ECMUtils.prototype.generateWebDAVLink = function(serverInfo,portalName,restContextName,repository,workspace,nodePath,mimetype) {
	  // query parameter s must be encoded.
	  var path = "/";
	  nodePath = nodePath.substr(1).split("\/");
	  for (var i=0; i < nodePath.length; i++) {
	    path += encodeURIComponent(nodePath[i]) + "/";
	  }
	  if(eXo.core.Browser.getBrowserType() == "ie") {
	    if(mimetype == "application/xls" || mimetype == "application/msword" || mimetype =="application/ppt") {
	      window.open(serverInfo + "/" + restContextName + "/private/lnkproducer/openit.lnk?path=/" + repository + "/" + workspace + path, '_new');
	    } else {
	      window.open(serverInfo + "/" + restContextName + "/private/jcr/" + repository + "/" +workspace + path, '_new');
	    }
	  } else {
	    window.open(serverInfo + "/" + restContextName + "/private/jcr/" + repository + "/" + workspace + path, '_new');
	  }
	} ;

	ECMUtils.prototype.generateWebDAVUrl = function(serverInfo, portalName, restContextName, repository, workspace, nodePath, mimetype) {
		my_window = window.open("");
    var downloadLink = serverInfo+ "/" + portalName + "/" + restContextName + "/jcr/" + repository + "/" + workspace + nodePath;
		my_window.document.write('<script> window.location.href = "'+downloadLink+'"; </script>');
	};

	var clip=null;
	ECMUtils.prototype.initClipboard = function(id, level, size) {
		if(eXo.core.Browser.getBrowserType() != "ie") {
			if (size > 0) {
				for(var i=1; i <= size; i++) {
					clip = new ZeroClipboard.Client();
					clip.setHandCursor(true);
					try {
					  clip.glue(id+level+i);
					} catch(err) {}
				}
			}
		}
	}

	ECMUtils.prototype.closeContextMenu = function(element) {
		var contextMenu = document.getElementById("ECMContextMenu");
		if (contextMenu) contextMenu.style.display = "none";
	}

	ECMUtils.prototype.pushToClipboard = function(event, url) {
		if( window.clipboardData && clipboardData.setData ) {
			clipboardData.setData("Text", url);
    } else {
			alert("Internet Explorer required");
		}
		eXo.core.MouseEventManager.docMouseDownEvt(event);
	  return false;
	}

 	ECMUtils.prototype.concatMethod =  function() {
		var oArg = arguments;
		var nSize = oArg.length;
		if (nSize < 2) return;
		var mSelf = oArg[0];
		return function() {
			var aArg = [];
			for (var i = 0; i < arguments.length; ++ i) {
				aArg.push(arguments[i]);
			}
			mSelf.apply(mSelf, aArg);
			for (i = 1; i < nSize; ++ i) {
				var oSet = {
					method: oArg[i].method || function() {},
					param: oArg[i].param || aArg
				}
				oSet.method.apply(oSet.method, oSet.param);
			}
		}
	};

	ECMUtils.prototype.checkAvailableSpace = function() {
		var actionBar = document.getElementById('UIActionBar');
		if (!actionBar) return;
		var prtNode = document.getElementById('DMSMenuItemContainer');
		var uiTabs = DOM.findDescendantsByClass(prtNode, "div", "SubTabItem");
		var listHideIcon = document.getElementById('IconListHideElement');
		var viewBarContainer = document.getElementById("UIViewBarContainer");
		var elementSpace = 9;
		var portletFrag = DOM.findAncestorByClass(actionBar, "PORTLET-FRAGMENT");
		var maxSpace = 0;
		if(eXo.core.Browser.browserType == 'ie') {
			maxSpace = parseInt(actionBar.offsetWidth) - parseInt(viewBarContainer.offsetWidth);
		} else {
			maxSpace = parseInt(portletFrag.offsetWidth) - parseInt(viewBarContainer.offsetWidth);
		}

    var lastIndex = 0;
		for(var i = 0; i <  uiTabs.length; i++){
			uiTabs[i].style.display = "block" ;
			listHideIcon.style.display = "block" ;
			if(elementSpace >= maxSpace - uiTabs[i].offsetWidth - listHideIcon.offsetWidth) {
				listHideIcon.className = "IconListHideElement ShowElementIcon";
				listHideIcon.style.visibility = "visible" ;
				eXo.ecm.ECMUtils.addElementListHide(uiTabs[i]);
				uiTabs[i].style.display = 'none';
			} else {
				listHideIcon.className = "IconListHideElement ShowElementIcon";
				listHideIcon.style.visibility = "hidden" ;
				uiTabs[i].style.display = 'block';
				var split = DOM.findFirstDescendantByClass(uiTabs[i], "div", "Non-Split");
				if (split) {
				  split.className = 'Split';
				}
				elementSpace += uiTabs[i].offsetWidth;
				var subItem = DOM.findFirstDescendantByClass(uiTabs[i], "a", "SubTabIcon");
				eXo.ecm.ECMUtils.removeElementListHide(subItem);
				lastIndex = i;
			}
		}
    var split = DOM.findFirstDescendantByClass(uiTabs[lastIndex], "div", "Split");
    if (split) {
      split.className = 'Non-Split';
    }
		eXo.core.Browser.addOnResizeCallback('ECMresize', function(){eXo.ecm.ECMUtils.checkAvailableSpace();});
	};

	ECMUtils.prototype.addElementListHide = function(obj) {
		var tmpNode = obj.cloneNode(true);
		var subItem = DOM.findFirstDescendantByClass(tmpNode, "a", "SubTabIcon");
		var split = DOM.findFirstDescendantByClass(tmpNode, "div", "Split");
		var listHideIcon = document.getElementById('IconListHideElement');
		var listHideContainer = DOM.findFirstDescendantByClass(listHideIcon, "div", "ListHideContainer");
		var uiTabs = DOM.findDescendantsByClass(listHideContainer, "div", "SubTabItem");
		for(var i = 0; i < uiTabs.length; i++) {
			var hideSubItem = DOM.findFirstDescendantByClass(uiTabs[i], "a", "SubTabIcon");
			if(hideSubItem.className == subItem.className) {
				return;
			}
		}
		if (split) {
		  split.className = 'Non-Split';
		}
		listHideContainer.appendChild(tmpNode);

		if (listHideContainer.innerHTML != "") {
			var clearElement = document.createElement("div");
			clearElement.style.clear = "left";
			listHideContainer.appendChild(clearElement);
		} else {
			listHideContainer.style.display = "none";
		}
	};

	ECMUtils.prototype.removeElementListHide = function(obj) {
		if(!obj) return;
		var listHideIcon = document.getElementById('IconListHideElement');
		var listHideContainer = DOM.findFirstDescendantByClass(listHideIcon, "div", "ListHideContainer");
		var uiTabs = DOM.findDescendantsByClass(listHideContainer, "div", "SubTabItem");
		var tmpNode = false;
		for(var i = 0; i < uiTabs.length; i++) {
			tmpNode = DOM.findFirstDescendantByClass(uiTabs[i], "a", "SubTabIcon");
			if(tmpNode.className == obj.className) {
				listHideContainer.removeChild(uiTabs[i]);
			}
		}
	};

	ECMUtils.prototype.showListHideElements = function(obj,event) {
		event = event || window.event;
		event.cancelBubble = true;
		var listHideContainer = DOM.findFirstDescendantByClass(obj, "div", "ListHideContainer");
		var listItems = DOM.findDescendantsByClass(listHideContainer, "div", "SubTabItem");
		if(listItems && listItems.length > 0) {
			if(listHideContainer.style.display != 'block') {
				obj.style.position = 'relative';
				listHideContainer.style.display = 'block';
				listHideContainer.style.top = obj.offsetHeight + 'px';
				listHideContainer.style.left =  -(listHideContainer.offsetWidth - obj.offsetWidth) + 'px';
			 } else {
				 obj.style.position = 'static';
				 listHideContainer.style.display = 'none';
			 }
			DOM.listHideElements(listHideContainer);
		}
	};

	ECMUtils.prototype.showDocumentInformation = function(obj, event) {
		if(!obj) return;
	  event = event || window.event;
		event.cancelBubble = true;
		var infor = document.getElementById('metadatas');
		if(infor.style.display == 'none') {
	    infor.style.display = 'block';
			infor.style.left = obj.offsetLeft + 'px';
	  } else {
  	  infor.style.display = 'none';
	  }
	  DOM.listHideElements(infor);
	};

	ECMUtils.prototype.onKeyPDFViewerPress = function() {
		var uiPDFViewer = document.getElementById("PageControl");
		if(uiPDFViewer) {
			uiPDFViewer.onkeypress = Self.onGotoPageEnterPress ;
		}
	};

	ECMUtils.prototype.onGotoPageEnterPress = function(event) {
		var gotoPage = document.getElementById("GotoPage");
		var event = event || window.event;
		if(gotoPage && event.keyCode == 13) {
			eval(gotoPage.href);
			return false;
		}
	};

	ECMUtils.prototype.resizeSideBar = function(event) {
		var event = event || window.event;
		eXo.ecm.ECMUtils.currentMouseX = event.clientX;
		var container = document.getElementById("LeftContainer");
		var resizableBlock = DOM.findFirstDescendantByClass(container, "div", "UIResizableBlock");
		eXo.ecm.ECMUtils.resizableBlockWidth = resizableBlock.offsetWidth;
		eXo.ecm.ECMUtils.currentWidth = container.offsetWidth;
		var sideBarContent = DOM.findFirstDescendantByClass(container, "div", "SideBarContent");
		var title = DOM.findFirstDescendantByClass(sideBarContent, "div", "Title");
		eXo.ecm.ECMUtils.currentTitleWidth = title.offsetWidth;

		if(container.style.display == '' || container.style.display == 'block') {
			document.onmousemove = eXo.ecm.ECMUtils.resizeMouseMoveSideBar;
			document.onmouseup = eXo.ecm.ECMUtils.resizeMouseUpSideBar;
		}
	}

	ECMUtils.prototype.resizeMouseMoveSideBar = function(event) {
		var event = event || window.event;
		var container = document.getElementById("LeftContainer");
		var resizableBlock = DOM.findFirstDescendantByClass(container, "div", "UIResizableBlock");
		var deltaX = event.clientX - eXo.ecm.ECMUtils.currentMouseX ;
		eXo.ecm.ECMUtils.savedResizeDistance = deltaX;
		var sideBarContent = DOM.findFirstDescendantByClass(container, "div", "SideBarContent");
		var title = DOM.findFirstDescendantByClass(sideBarContent, "div", "Title");
		title.style.width = eXo.ecm.ECMUtils.currentTitleWidth + deltaX + "px";
		// container.style.width = eXo.ecm.ECMUtils.currentWidth + deltaX + "px";
		// resizableBlock.style.width = eXo.ecm.ECMUtils.resizableBlockWidth + deltaX + "px";
		eXo.ecm.ECMUtils.savedResizableMouseX = eXo.ecm.ECMUtils.resizableBlockWidth + deltaX + "px";
		eXo.ecm.ECMUtils.savedLeftContainer = eXo.ecm.ECMUtils.currentWidth + deltaX + "px";
		eXo.ecm.ECMUtils.isResizedLeft = false;

		var resizeDiv = document.getElementById("ResizeSideBarDiv");
		if (resizeDiv == null) {
			resizeDiv = document.createElement("div");
			resizeDiv.className = "ResizeHandle";
			resizeDiv.id = "ResizeSideBarDiv";
			var workingArea = DOM.findAncestorByClass(container, "UIWorkingArea");
			resizeDiv.style.height = container.offsetHeight + "px";
			workingArea.appendChild(resizeDiv);
		}
		var X_Resize = eXo.core.Browser.findMouseRelativeX(workingArea,event);
		var Y_Resize = eXo.core.Browser.findPosYInContainer(container,workingArea);
		eXo.core.Browser.setPositionInContainer(workingArea, resizeDiv, X_Resize, Y_Resize);
	}

	ECMUtils.prototype.resizeMouseUpSideBar = function(event) {
		document.onmousemove = null;

		// Case of increase width
		if (eXo.ecm.ECMUtils.savedResizeDistance > 0) {
			var documentInfo = document.getElementById("UIDocumentInfo");
			var listGrid = DOM.findFirstDescendantByClass(documentInfo, "div", "UIListGrid");
			if (listGrid)
				listGrid.style.width = listGrid.offsetWidth + eXo.ecm.ECMUtils.savedResizeDistance + "px";
		}

		var container = document.getElementById("LeftContainer");
		var resizableBlock = DOM.findFirstDescendantByClass(container, "div", "UIResizableBlock");
  var workingArea = DOM.findAncestorByClass(container, "UIWorkingArea");
		var allowedWidth = parseInt(workingArea.offsetWidth) / 2;
		// Fix minimium width can be resized
		if ((eXo.ecm.ECMUtils.currentWidth + eXo.ecm.ECMUtils.savedResizeDistance > 50) &
				  (eXo.ecm.ECMUtils.currentWidth + eXo.ecm.ECMUtils.savedResizeDistance <= allowedWidth)) {
			eXo.ecm.ECMUtils.isResizedLeft = true;
			container.style.width = eXo.ecm.ECMUtils.currentWidth + eXo.ecm.ECMUtils.savedResizeDistance + "px";
			resizableBlock.style.width = eXo.ecm.ECMUtils.resizableBlockWidth + eXo.ecm.ECMUtils.savedResizeDistance + "px";
		}

		// Remove new added div
		var workingArea = DOM.findAncestorByClass(container, "UIWorkingArea");
		if (workingArea) {
			var resizeDiv = document.getElementById("ResizeSideBarDiv");
			if (resizeDiv)
				workingArea.removeChild(resizeDiv);
		}

		delete eXo.ecm.ECMUtils.currentWidth;
		delete eXo.ecm.ECMUtils.currentMouseX;
		delete eXo.ecm.ECMUtils.resizableBlockWidth;
		delete eXo.ecm.ECMUtils.savedResizeDistance;
	}

	ECMUtils.prototype.showHideSideBar = function(event) {
	  var container = document.getElementById("LeftContainer");
	  var workingArea = DOM.findAncestorByClass(container, "UIWorkingArea");
	  var resizeButton = DOM.findFirstDescendantByClass(workingArea, "div", "ResizeButton");
	  if(container.style.display == 'none') {
	    container.style.display = 'block';
		resizeButton.className = "ResizeButton";
	  } else {
		container.style.display = 'none';
		resizeButton.className = "ResizeButton ShowLeftContent";
	  }
	}

	ECMUtils.prototype.loadEffectedSideBar = function(id) {
		var container = document.getElementById("LeftContainer");
		var resizableBlock = DOM.findFirstDescendantByClass(container, "div", "UIResizableBlock");
		if(eXo.ecm.ECMUtils.savedLeftContainer && eXo.ecm.ECMUtils.savedResizableMouseX) {
			if (eXo.ecm.ECMUtils.isResizedLeft==true)
					container.style.width = eXo.ecm.ECMUtils.savedLeftContainer;
			resizableBlock.style.width = eXo.ecm.ECMUtils.savedResizableMouseX;
			var documentInfo = document.getElementById("UIDocumentInfo");
			var listGrid = DOM.findFirstDescendantByClass(documentInfo, "div", "UIListGrid");
			if (listGrid)
				listGrid.style.width = listGrid.offsetWidth + 200 + parseInt(eXo.ecm.ECMUtils.savedResizableMouseX) + "px";
		}
		eXo.ecm.ECMUtils.focusCurrentNodeInTree(id);
	}

	ECMUtils.prototype.resizeTreeInSideBar = function(event) {
		var event = event || window.event;
		eXo.ecm.ECMUtils.currentMouseY = event.clientY;
		var workingArea = document.getElementById('UIWorkingArea');
		var uiResizableBlock = DOM.findFirstDescendantByClass(workingArea, "div", "UIResizableBlock");
		var container = document.getElementById("UITreeExplorer");
		//var isOtherTabs=false;
		var listContainers;
		if (!container) {
			listContainers = DOM.findDescendantsByClass(uiResizableBlock, "div", "SideBarContent");
			for (var k=0; k < listContainers.length; k++) {
				if (listContainers[k].parentNode.className!="UIResizableBlock") {
					container = listContainers[k-1];
					//isOtherTabs = true;
					break;
				}
			}
		}
		eXo.ecm.ECMUtils.currentHeight = container.offsetHeight;
		checkRoot();

		eXo.ecm.ECMUtils.resizableHeight = uiResizableBlock.offsetHeight;
		eXo.ecm.ECMUtils.containerResize = container;
		document.onmousemove = eXo.ecm.ECMUtils.resizeMouseMoveItemsInSideBar;
		document.onmouseup = eXo.ecm.ECMUtils.resizeMouseUpItemsInSideBar;
	}

	ECMUtils.prototype.resizeMouseMoveItemsInSideBar = function(event) {
		var event = event || window.event;
		//var container = document.getElementById("UITreeExplorer");
	  var container = eXo.ecm.ECMUtils.containerResize;
		var deltaY = event.clientY - eXo.ecm.ECMUtils.currentMouseY ;
		eXo.ecm.ECMUtils.resizableY = deltaY;

		var resizeDiv = document.getElementById("ResizeVerticalSideBarDiv");
		if (resizeDiv == null) {
			resizeDiv = document.createElement("div");
			resizeDiv.className = "VResizeHandle";
			resizeDiv.id = "ResizeVerticalSideBarDiv";
			var workingArea = DOM.findAncestorByClass(container, "UIWorkingArea");
			var uiResizableBlock = DOM.findFirstDescendantByClass(workingArea, "div", "UIResizableBlock");
			resizeDiv.style.width = container.offsetWidth + "px";
			uiResizableBlock.appendChild(resizeDiv);
		}
		var Y_Resize = eXo.core.Browser.findMouseRelativeY(uiResizableBlock,event);
		var X_Resize = eXo.core.Browser.findPosXInContainer(container,uiResizableBlock);
		eXo.core.Browser.setPositionInContainer(uiResizableBlock, resizeDiv, X_Resize, Y_Resize);
		eXo.ecm.ECMUtils.savedTreeSizeMouseY = eXo.ecm.ECMUtils.currentHeight + deltaY + "px";
	}

	ECMUtils.prototype.resizeMouseUpItemsInSideBar = function(event) {
		document.onmousemove = null;

		// The block are updated
		var workingArea = document.getElementById('UIWorkingArea');
		var resizeSideBar = DOM.findFirstDescendantByClass(workingArea, "div", "ResizeSideBar");
		var sizeBarContainer = DOM.findFirstDescendantByClass(workingArea, "div", "UISideBarContainer");

		// Remove new added div
		var uiResizableBlock = DOM.findFirstDescendantByClass(workingArea, "div", "UIResizableBlock");
		if (uiResizableBlock) {
			var resizeDiv = document.getElementById("ResizeVerticalSideBarDiv");
			if (resizeDiv)
				uiResizableBlock.removeChild(resizeDiv);
		}

		// The bellow block are updated
		sizeBarContainer.style.height = eXo.ecm.ECMUtils.resizableHeight + eXo.ecm.ECMUtils.resizableY + 20 + "px";
		resizeSideBar.style.height = eXo.ecm.ECMUtils.resizableHeight + eXo.ecm.ECMUtils.resizableY + 20 + "px";
		var root = checkRoot();
		// var root = document.getElementById("UIDocumentWorkspace");
		root.style.height = eXo.ecm.ECMUtils.resizableHeight + eXo.ecm.ECMUtils.resizableY + 20 +"px";
		if (eXo.ecm.ECMUtils.resizableHeight + eXo.ecm.ECMUtils.resizableY < eXo.ecm.ECMUtils.defaultHeight) {
			sizeBarContainer.style.height = eXo.ecm.ECMUtils.defaultHeight + 20 + "px";
			resizeSideBar.style.height = eXo.ecm.ECMUtils.defaultHeight + 20 + "px";
		}
		var container = eXo.ecm.ECMUtils.containerResize;
		if (container)
			container.style.height = eXo.ecm.ECMUtils.currentHeight + eXo.ecm.ECMUtils.resizableY + "px"
		delete eXo.ecm.ECMUtils.currentHeight;
		delete eXo.ecm.ECMUtils.currentMouseY;
		delete eXo.ecm.ECMUtils.resizableHeight
		delete eXo.ecm.ECMUtils.resizableY;
		delete eXo.ecm.ECMUtils.containerResize;
	}

	ECMUtils.prototype.showHideItemsInSideBar = function(event) {
	  var workingArea = document.getElementById('UIWorkingArea');
	  var resizeBlock = DOM.findFirstDescendantByClass(workingArea, "div", "UIResizableBlock");
	  var itemArea = document.getElementById("SelectItemArea");
	  var container = document.getElementById("UITreeExplorer");
	  var resizeTreeButton = DOM.findFirstDescendantByClass(resizeBlock, "div", "ResizeTreeButton");

		// var workspace = document.getElementById("UIDocumentWorkspace");
		var workspace = checkRoot();
	  var isOtherTabs;
	  var listContainers;

	  if (!container) {
		listContainers = DOM.findDescendantsByClass(resizeBlock, "div", "SideBarContent");
		for (var k=0; k < listContainers.length; k++) {
			if (listContainers[k].parentNode.className!="UIResizableBlock") {
				container = listContainers[k-1];
				isOtherTabs = true;
				break;
			}
		}
	  }

	  if(typeof(eXo.ecm.ECMUtils.heightOfItemArea) == "undefined") {
	    eXo.ecm.ECMUtils.heightOfItemArea = itemArea.offsetHeight;
	  }

      if(typeof(eXo.ecm.ECMUtils.heightOfTree) == "undefined") {
		eXo.ecm.ECMUtils.heightOfTree = container.offsetHeight;
	  }

	  if(itemArea.style.display == 'none') {
		if (isOtherTabs)
		{
			if (eXo.ecm.ECMUtils.heightOfTree != eXo.ecm.UIListView.initialHeightOfContainer) {
				eXo.ecm.ECMUtils.heightOfTree = eXo.ecm.UIListView.initialHeightOfContainer
				//container.style.height = container.offsetHeight - eXo.ecm.ECMUtils.heightOfItemArea - 10 + "px";
			}
			container.style.height = eXo.ecm.ECMUtils.heightOfTree - 4 + "px";
		}
		else
		{
			// Case of tree explorer tab
			container.style.height = container.offsetHeight - eXo.ecm.ECMUtils.heightOfItemArea - 20 + "px";
		}
	    itemArea.style.display = 'block';
	    eXo.ecm.ECMUtils.savedDisplayStatusOfItemArea = 'block';
		resizeTreeButton.className = "ResizeTreeButton";
	  } else {

		if (isOtherTabs)  {

			//container.style.height = container.offsetHeight + itemArea.offsetHeight + "px";
			container.style.height = workspace.offsetHeight - 20 + "px";
			var previousElement = DOM.findPreviousElementByTagName(container, "div");
			if (previousElement)
				container.style.height = workspace.offsetHeight - 25 - previousElement.offsetHeight + "px";
		}
		else {
			container.style.height = container.offsetHeight + itemArea.offsetHeight - 20 + "px";
		}

		if (container.offsetHeight > workspace.offsetHeight) {
			var exceed = container.offsetHeight - workspace.offsetHeight;
			if (exceed > 0) {

				// Assign default value for treeExplorer div
				container.style.height = workspace.offsetHeight - 20 + "px";
			}
		}
		itemArea.style.display = 'none';
		eXo.ecm.ECMUtils.savedDisplayStatusOfItemArea = 'none';
		resizeTreeButton.className = "ResizeTreeButton ShowContentButton";
	  }
	  eXo.ecm.ECMUtils.reloadHeight();
	}

	ECMUtils.prototype.reloadHeight = function() {
		var workingArea = document.getElementById('UIWorkingArea');
		var sizeBarContainer = DOM.findFirstDescendantByClass(workingArea, "div", "UISideBarContainer");
		var resizeSizeBar = DOM.findFirstDescendantByClass(workingArea, "div", "ResizeSideBar");
		sizeBarContainer.style.height = workingArea.offsetHeight + 'px';
		resizeSizeBar.style.height = workingArea.offsetHeight + 'px';
	}

	ECMUtils.prototype.loadEffectedItemsInSideBar = function() {
	  var container = document.getElementById("UITreeExplorer");
		if(eXo.ecm.ECMUtils.savedTreeSizeMouseY) {
			container.style.height = eXo.ecm.ECMUtils.savedTreeSizeMouseY;
		}
		var itemArea = document.getElementById("SelectItemArea");
		if(eXo.ecm.ECMUtils.savedDisplayStatusOfItemArea) {
		  if(eXo.ecm.ECMUtils.savedDisplayStatusOfItemArea == 'none') {
	      container.style.height = container.offsetHeight + eXo.ecm.ECMUtils.heightOfItemArea + "px";
		    itemArea.style.display = 'none';
		  } else {
	  	  container.style.height = container.offsetHeight - eXo.ecm.ECMUtils.heightOfItemArea + "px";
	  	  itemArea.style.display = 'block';
		  }
		}
	}

	ECMUtils.prototype.disableAutocomplete = function(id) {
		var clickedElement = document.getElementById(id);
		tagNameInput = DOM.findFirstDescendantByClass(clickedElement,"div", "UITagNameInput");
		DOM.findDescendantById(tagNameInput, "names").setAttribute("autocomplete", "off");
	}

	ECMUtils.prototype.selectedPath = function(id) {
	  var select = document.getElementById(id);
	  if (select)
		select.className = select.className + " " + "SelectedNode";
	}

	//private method
	function checkRoot() {
		var workingArea = document.getElementById('UIWorkingArea');
		var root = document.getElementById("UIDocumentInfo");
		if (root) {
			var uiListGrid = eXo.core.DOMUtil.findFirstDescendantByClass(root, "div", "UIListGrid");
			eXo.ecm.ECMUtils.defaultHeight = uiListGrid.offsetHeight;
		} else {
			root = document.getElementById("UIDocumentWorkspace");
			if (root) {
				eXo.ecm.ECMUtils.defaultHeight = root.offsetHeight;
			} else {
				root = eXo.core.DOMUtil.findFirstDescendantByClass(workingArea, "div", "RightContainer");
			  eXo.ecm.ECMUtils.defaultHeight = root.offsetHeight;
			}
		}
		return root;
	}

};

ECMUtils.prototype.showInContextHelp = function(){
  var parentElm = document.getElementById("idAllDrivers");
  var inContextContentHelp = eXo.core.DOMUtil.findFirstDescendantByClass(parentElm,"div","InContextHelpContent");
  if(inContextContentHelp) {
	   setTimeout(function(){
	      inContextContentHelp.style.display = "none";
	   }, 6*1000);
  }
};

eXo.ecm.ECMUtils = new ECMUtils();

