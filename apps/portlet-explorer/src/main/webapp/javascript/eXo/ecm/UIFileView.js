function UIFileView() {
	this.openDivs = {};
	// eXo.ecm.UIFileView
	
	Self = this;
	Self.columnData = {};
	BROW = eXo.core.Browser;
};
	UIFileView.prototype.temporaryItem = null;
	UIFileView.prototype.itemsSelected = [];
	UIFileView.prototype.allItems = [];
	UIFileView.prototype.contextMenuId = null;
	UIFileView.prototype.actionAreaId = null;
	UIFileView.prototype.enableDragDrop = null;
	UIFileView.prototype.clickCheckBox = false;

	UIFileView.prototype.colorSelected = "#e7f3ff";
	UIFileView.prototype.colorHover = "#f2f8ff";
	
	UIFileView.prototype.t1 = 0;
	UIFileView.prototype.t2 = 0;
	UIFileView.prototype.minBreadcrumbTop = 0;

UIFileView.prototype.clickFolder =  function (folderDiv, link, docListId) {
	if (!folderDiv) return;
    folderDiv.className = "FolderCollapsed" == folderDiv.className ? "FolderExpanded" : "FolderCollapsed";
    var docList = document.getElementById(docListId);
    if ("FolderCollapsed" == folderDiv.className) {
      docList.style.display="none";
    } else {
      if (eXo.ecm.UIFileView.openDivs[docListId]) {
        docList.style.display="block";
      } else {
        eval(decodeURIComponent(link));
        eXo.ecm.UIFileView.openDivs[docListId] = docListId;
        docList.style.display="block";        
      }
    }
}

UIFileView.prototype.clearOpenDivs =  function () {
	eXo.ecm.UIFileView.openDivs = {};
}

UIFileView.prototype.clearSideBar =  function () {
	var sidebar = document.getElementById("LeftContainer");
	sidebar.className = "LeftContainer NoShow";
	eXo.ecm.ECMUtils.showHideSideBar();
	var workingArea = gj(sidebar).parents(".UIWorkingArea:first")[0];
	var resizeButton = gj(workingArea).find("div.ResizeSideBar:first")[0];
	resizeButton.className = "ResizeSideBar NoShow";
}

UIFileView.prototype.showSideBar =  function () {
	var sidebar = document.getElementById("LeftContainer");
	sidebar.className = "LeftContainer";
	eXo.ecm.ECMUtils.showHideSideBar();
	var workingArea = gj(sidebar).parents(".UIWorkingArea:first")[0];
	var resizeButton = gj(workingArea).find("div.ResizeSideBar:first")[0];
	resizeButton.className = "ResizeSideBar";
}

//init event
UIFileView.prototype.initAllEvent = function(actionAreaId, enableDragAndDrop) {
	eXo.ecm.UIFileView.enableDragAndDrop = enableDragAndDrop;
	Self.contextMenuId = "JCRContextMenu";
	Self.actionAreaId = actionAreaId;
	var actionArea = document.getElementById(actionAreaId);
	if (!actionArea) return;
	var mousedown = null;
	var keydown = null;
	Self.allItems = gj(actionArea).find("div.RowView");
	Self.allItems.each(function(index, elem){
		if (!Array.prototype[index]) {
			var item = elem;
			item.storeIndex = index;
			if (item.getAttribute("onmousedown")) {
				mousedown = item.getAttributeNode("onmousedown").value;
				item.setAttribute("mousedown", mousedown);
				item.onmousedown = null;
				item.removeAttribute("onmousedown");
			}
            if (item.getAttribute("onkeydown")) {
                keydown = item.getAttributeNode("onkeydown").value;
                item.setAttribute("keydown", keydown);
                item.onkeydown = null;
                item.removeAttribute("onkeydown");
            }			
			item.onmouseover = Self.mouseOverItem;
			item.onfocus = Self.mouseOverItem;
			item.onmousedown = Self.mouseDownItem;
			item.onkeydown = Self.mouseDownItem;
			item.onmouseup = Self.mouseUpItem;
			item.onmouseout = Self.mouseOutItem;
			item.onblur = Self.mouseOutItem;
			gj(elem).find("input:checkbox").each(function(ii, ee){ee.onmousedown=Self.checkBoxItem;});
		}
	});
	actionArea.onmousedown = Self.mouseDownGround;
	actionArea.onkeydown = Self.mouseDownGround;
	actionArea.onmouseup = Self.mouseUpGround;
	
	var fillOutElement = document.createElement('div');
	fillOutElement.id = "FillOutElement";
	
	var listGrid = gj(actionArea).find("div.UIListGrid:first")[0];
	if (listGrid) {
		listGrid.appendChild(fillOutElement);
	}
	
	//remove context menu
	var contextMenu = document.getElementById(Self.contextMenuId);
	if (contextMenu) contextMenu.parentNode.removeChild(contextMenu);
	//registry action drag drop in tree list
	eXo.ecm.UIFileView.initDragDropForTreeEvent("UIWorkingArea", enableDragAndDrop);		
//	var UIWorkingArea = DOM.findAncestorByClass(actionArea, "UIWorkingArea");
//	var UITreeExplorer = DOM.findFirstDescendantByClass(UIWorkingArea, "div", "UITreeExplorer");
//	if (UITreeExplorer) {
//		DOM.getElementsBy(
//				function(element) {return element.getAttribute("objectId");},
//				"div",
//				UITreeExplorer,
//				function(element) {
//					if (element.getAttribute("onmousedown")) {
//						mousedown = element.getAttributeNode("onmousedown").value;
//						element.setAttribute("mousedown", mousedown);
//					}
//					if (enableDragAndDrop == "true") {
//						element.onmousedown = Self.mouseDownTree;
//						element.onmouseup = Self.mouseUpTree;
//						element.onmouseover = Self.mouseOverTree;
//						element.onmouseout = Self.mouseOutTree;
//					}
//				}
//		);
//	}
};

UIFileView.prototype.initDragDropForTreeEvent = function(actionAreaId, enableDragAndDrop) {
	//registry action drag drop in tree list
	eXo.ecm.UIFileView.enableDragAndDrop = enableDragAndDrop;
//	var UIWorkingArea =	document.getElementById(actionAreaId);
//	var UITreeExplorer = DOM.findFirstDescendantByClass(UIWorkingArea, "div", "UITreeExplorer");
//	if (UITreeExplorer) {
//		DOM.getElementsBy(
//				function(element) {return element.getAttribute("objectId");},
//				"div",
//				UITreeExplorer,
//				function(element) {
//					if (element.getAttribute("onmousedown") &&!element.getAttribute("mousedown")) {
//						mousedown = element.getAttributeNode("onmousedown").value;
//						element.setAttribute("mousedown", mousedown);
//					}
//        if (element.getAttribute("onkeydown") &&!element.getAttribute("keydown")) {
//          keydown = element.getAttributeNode("onkeydown").value;
//          element.setAttribute("keydown", keydown);
//        }						
////					if (enableDragAndDrop == "true") {
//						element.onmousedown = Self.mouseDownTree;
//						element.onkeydown = Self.mouseDownTree;
//						element.onmouseup = Self.mouseUpTree;
//						element.onmouseover = Self.mouseOverTree;
//						element.onmouseout = Self.mouseOutTree;
//          element.onfocus = Self.mouseOverTree;
//          element.onblur = Self.mouseOutTree;							
////					}
//				}
//		);
		gj("#" + actionAreaId + " div.UITreeExplorer:first").find("div[objectId]").each(			
		function(index, element) {
			if (element.getAttribute("onmousedown") &&!element.getAttribute("mousedown")) {
				mousedown = element.getAttributeNode("onmousedown").value;
				element.setAttribute("mousedown", mousedown);
			}
            if (element.getAttribute("onkeydown") &&!element.getAttribute("keydown")) {
              keydown = element.getAttributeNode("onkeydown").value;
              element.setAttribute("keydown", keydown);
            }			 			
			  element.onmousedown = Self.mouseDownTree;
			  element.onkeydown = Self.mouseDownTree;
			  element.onmouseup = Self.mouseUpTree;
			  element.onmouseover = Self.mouseOverTree;
			  element.onmouseout = Self.mouseOutTree;
	          element.onfocus = Self.mouseOverTree;
	          element.onblur = Self.mouseOutTree;							
		});
};

//event in tree list
UIFileView.prototype.mouseOverTree = function(event) {
	var event = event || window.event;
	var element = this;
	var mobileElement = document.getElementById(Self.mobileId);
	if (mobileElement && mobileElement.move) {
		var expandElement = gj(element).parents(".ExpandIcon:first")[0];
		if(expandElement && expandElement.onclick) {
			if (expandElement.onclick instanceof Function) {
				element.Timeout = setTimeout(function() {expandElement.onclick(event)}, 1000);
			} 
		}
	}
	var scroller = gj(element).parents(".SideContent:first")[0];
scroller.onmousemove = eXo.ecm.UIFileView.setScroll ;
};

UIFileView.prototype.setScroll = function(evt){
  if(Self.enableDragDrop) {
	eXo.ecm.UIFileView.object = this;
    var element = eXo.ecm.UIFileView.object ;
	var pos = evt.pageY - gj(element).offset().top;
	if(element.offsetHeight - pos < 10){
	  element.scrollTop = element.scrollTop + 5;  
	} else if(element.scrollTop > 0 && pos < 10) {
	element.scrollTop = element.scrollTop - 5;  
	}
  }
};

UIFileView.prototype.mouseOutTree = function(event) {
	var element = this;
	clearTimeout(element.Timeout);
};

UIFileView.prototype.mouseDownTree = function(evt) {
	eval("var event = ''");
	event = evt || window.event;
	var element = this;
	Self.enableDragDrop = true;
	Self.srcPath = element.getAttribute("objectId");
	resetArrayItemsSelected();
	var rightClick = (event.which && event.which > 1) || (event.button && event.button == 2);
	if (rightClick) {
		eval(element.getAttribute("mousedown"));
	} else {
		// init drag drop;
		document.onmousemove = Self.dragItemsSelected;
		document.onmouseup = Self.dropOutActionArea;
		var itemSelected = element.cloneNode(true);
		Self.itemsSelected = new Array(itemSelected);
		//var uiResizableBlock = DOM.findAncestorByClass(element, "UIResizableBlock");
		//if (uiResizableBlock) uiResizableBlock.style.overflow = "hidden";
		
		//create mobile element
		var mobileElement = newElement({
			className: "UIJCRExplorerPortlet",
			id: eXo.generateId('Id'),
			style: {
				position: "absolute",
				display: "none",
				overflow: "hidden",
      padding: "1px",
      background: "white",
      border: "1px solid gray",
      width: element.offsetWidth + 50 + "px",
      height: "25px"
			}
		});
		mobileElement.style.opacity = 65/100;
		Self.mobileId = mobileElement.id;
		var coverElement = newElement({
			className: "UITreeExplorer",
			style: {margin: "0px 3px", padding: "3px 0px"}
		});
		coverElement.appendChild(itemSelected);
		mobileElement.appendChild(coverElement);
		document.body.appendChild(mobileElement);
	}
};

UIFileView.prototype.mouseUpTree = function(evt) {
	eval("var event = ''");
	event = evt || window.event;
	var element = this;
	revertResizableBlock();
	Self.enableDragDrop = null;
	var mobileElement = document.getElementById(Self.mobileId);
	if (!mobileElement && eXo.ecm.UISimpleView && eXo.ecm.UISimpleView.mobileId)
		mobileElement = document.getElementById(eXo.ecm.UISimpleView.mobileId);
	
//	Self.clickItem(event, element);		
	if (mobileElement && mobileElement.move) {
		//post action
		var actionArea = document.getElementById("UIWorkingArea");
		var moveAction = gj(actionArea).find("div.JCRMoveAction:first")[0];
		var wsTarget = element.getAttribute('workspacename');
		var idTarget = element.getAttribute('objectId');
		var targetPath = decodeURIComponent(idTarget);
		var srcPath = Self.srcPath ?  decodeURIComponent(Self.srcPath) :
			decodeURIComponent(eXo.ecm.UISimpleView.srcPath);
//		var regex = new RegExp("^"+decodeURIComponent(idTarget) + "/");
//		alert("^"+decodeURIComponent(idTarget) + "/" + "\n" + "^"+decodeURIComponent(Self.srcPath) + "/");
//		var regex1 = new RegExp("^"+decodeURIComponent(Self.srcPath) + "/");
//		alert(regex.test(decodeURIComponent(Self.srcPath) + "/") + "\n" + regex1.test(decodeURIComponent(idTarget) + "/"))
//		if(regex.test(decodeURIComponent(Self.srcPath) + "/")){
//		  delete Self.srcPath;
//		  return ;
//		}
//		if(regex1.test(decodeURIComponent(idTarget) + "/")) {
//		  delete Self.srcPath;
//		  return;
//		}
		if (targetPath.indexOf(srcPath) == 0) {
			delete Self.srcPath;
			return;
		}
		//Dunghm : check symlink
		if (eXo.ecm.UIFileView.enableDragAndDrop == "true") {
			if(event.ctrlKey && event.shiftKey)
			  Self.postGroupAction(moveAction.getAttribute("symlink"), "&destInfo=" + wsTarget + ":" + idTarget);
			else {
			  Self.postGroupAction(moveAction, "&destInfo=" + wsTarget + ":" + idTarget);
			}
		}			
	}
//	Self.clickItem(event, element);		
};

//event in item
UIFileView.prototype.mouseOverItem = function(event) {
	var event = event || window.event;
	var element = this;
	if (!element.selected) {
		element.style.background = Self.colorHover;
		element.temporary = true;
		//eXo.core.Browser.setOpacity(element, 100);
	}
};

UIFileView.prototype.mouseOutItem = function(event) {
	var event = event || window.event;
	var element = this;
	element.temporary = false;
	if (!element.selected) {
		element.style.background = "none";
		//eXo.core.Browser.setOpacity(element, 85);
	}
};

UIFileView.prototype.mouseDownItem = function(evt) {
	eval("var event = ''");
	event = evt || window.event;
	event.cancelBubble = true;
	var element = this;
	removeMobileElement();
	Self.hideContextMenu();
	var d = new Date();		
Self.t1 = d.getTime();   
	Self.enableDragDrop = true;
	Self.srcPath = element.getAttribute("objectId");
	document.onselectstart = function(){return false};
	var rightClick = (event.which && event.which > 1) || (event.button && event.button == 2);
	if (!rightClick) {
		//console.log('mouseDown: ' + Self.clickCheckBox);
		if (!inArray(Self.itemsSelected, element) && !event.ctrlKey && !event.shiftKey && !eXo.ecm.UIFileView.clickCheckBox) {
			Self.clickItem(event, element);
		};

		// init drag drop;
		document.onmousemove = Self.dragItemsSelected;
		document.onmouseup = Self.dropOutActionArea;

		//create mobile element
		var mobileElement = newElement({
			className: "UIJCRExplorerPortlet MoveItem",
			id: eXo.generateId('Id'),
			style: {
					position: "absolute",
					display: "none",
					padding: "1px",
					background: "white",
					border: "1px solid gray",
					width: document.getElementById(Self.actionAreaId).offsetWidth + "px"
			}
		});
		
		mobileElement.style.opacity = 64/100 ;
		Self.mobileId = mobileElement.getAttribute('id');
		var coverElement = newElement({className: "UIListGrid"});
		for(var i in Self.itemsSelected) {
			if (Array.prototype[i]) continue;
			var childNode = Self.itemsSelected[i].cloneNode(true);
			childNode.style.background = "#dbdbdb";
			coverElement.appendChild(childNode);
		}
		var listViewElement = newElement({className: "UIListView"});
		listViewElement.appendChild(coverElement);
		mobileElement.appendChild(listViewElement);
		document.body.appendChild(mobileElement);
	}
};

UIFileView.prototype.dragItemsSelected = function(event) {
		var event = event || window.event;
		document.onselectstart = function(){return false;}
		if (eXo.ecm.UIFileView.enableDragAndDrop != "true")
			return;
		var d = new Date();      
	Self.t2 = d.getTime();      
  if((Self.t2-Self.t1)<200) 
			return;
		var mobileElement = document.getElementById(Self.mobileId);
		if (Self.enableDragDrop && mobileElement && (!event.ctrlKey || (event.shiftKey && event.ctrlKey))) {
			mobileElement.style.display = "block";
			var X = event.pageX;
			var Y = event.pageY;
			mobileElement.style.top = Y + 5 + "px";
			mobileElement.style.left = X + 5 + "px";
			mobileElement.move = true;
		}
};

UIFileView.prototype.dropOutActionArea = function(event) {
	var event = event || window.event;
	Self.enableDragDrop = null;
	revertResizableBlock();
	//use when drop out of action area
	if (document.getElementById(Self.mobileId)) {
			mobileElement = document.getElementById(Self.mobileId);
			mobileElement.parentNode.removeChild(mobileElement);
	}
	document.onmousemove = null;
	document.onmouseup = null;
	document.onselectstart = function(){return true;}
};

UIFileView.prototype.clickItem = function(event, element, callback) {
	var event = event || window.event;
	resetArrayItemsSelected();
	element.selected = true;
	//Dunghm: Check Shift key
	if(event.shiftKey) element.setAttribute("isLink",true);
	else element.setAttribute("isLink",null);
	//for select use shilf key;
	Self.temporaryItem = element;
	Self.itemsSelected = new Array(element);
	element.style.background = Self.colorSelected;
	//uncheck all checkboxes
	var uiDocInfo = gj("#UIDocumentInfo");
	gj("input", uiDocInfo).each(function(index, elem) {
		gj(elem).attr("checked", false);
	});
	//check current checkbox
	gj("input:checkbox", element).each(function(index, elem) {
		gj(elem).attr("checked", true);
	});
	//eXo.core.Browser.setOpacity(element, 100);
};

UIFileView.prototype.mouseUpItem = function(evt) {
	eval("var event=''");
	event = evt || window.event;
	var element = this;
	Self.enableDragDrop = null;
	document.onmousemove = null;
	revertResizableBlock();
	var rightClick = (event.which && event.which > 1) || (event.button && event.button == 2);
	var leftClick = !rightClick;
	if (leftClick) {
	  if(Self.rootNode == element){
	    delete Self.rootNode;
	    return ;
	  }
		var mobileElement = document.getElementById(Self.mobileId);
		if (mobileElement && mobileElement.move && element.temporary) {
			//post action
			var actionArea = document.getElementById("UIWorkingArea");
			var moveAction = gj(actionArea).find("div.JCRMoveAction:first")[0];
			var wsTarget = element.getAttribute('workspacename');
			var idTarget = element.getAttribute('objectId');
			//Dunghm: check symlink
			var regex = new RegExp("^"+idTarget);
			if(regex.test(Self.srcPath)){
			  delete Self.srcPath;
			  return ;
			}
			if (eXo.ecm.UIFileView.enableDragAndDrop == "true") {
				if(event.ctrlKey && event.shiftKey)
				  Self.postGroupAction(moveAction.getAttribute("symlink"), "&destInfo=" + wsTarget + ":" + idTarget);
				else
				  Self.postGroupAction(moveAction, "&destInfo=" + wsTarget + ":" + idTarget);
			}
		} else {
			if ((event.ctrlKey || Self.clickCheckBox) && !element.selected) {
				element.selected = true;
				//for select use shilf key;
				Self.temporaryItem = element;
				Self.itemsSelected.push(element);
				//Dunghm: Check Shift key
				element.setAttribute("isLink",null);
				if(event.shiftKey) element.setAttribute("isLink",true);
			} else if((event.ctrlKey || Self.clickCheckBox) && element.selected) {
				element.selected = null;
				element.setAttribute("isLink",null);
				element.style.background = "none";
				removeItem(Self.itemsSelected, element);
			} else if (event.shiftKey) {
				//use shift key to select;
				//need clear temporaryItem when mousedown in ground;
				var lowIndex = 0;
				var heightIndex = element.storeIndex;
				if (Self.temporaryItem) {
					lowIndex = Math.min(Self.temporaryItem.storeIndex,  element.storeIndex);
					heightIndex = Math.max(Self.temporaryItem.storeIndex,  element.storeIndex);
				}
				resetArrayItemsSelected();
				for (var i = lowIndex; i <= heightIndex; i++) {
					Self.allItems[i].selected = true;
					//Dunghm: Check Shift key
					element.setAttribute("isLink",null);
					if(event.ctrlKey) element.setAttribute("isLink",true);
					Self.itemsSelected.push(Self.allItems[i]);
				}
			} else {
				Self.clickItem(event, element);
			}
			for(var i in Self.itemsSelected) {
				if (Array.prototype[i]) continue;
				Self.itemsSelected[i].style.background = Self.colorSelected;
				//eXo.core.Browser.setOpacity(Self.itemsSelected[i], 100);
			}
		}
	} else {
		event.cancelBubble = true;
		if (inArray(Self.itemsSelected, element) && Self.itemsSelected.length > 1){
			Self.showItemContextMenu(event, element);
		} else {
			Self.clickItem(event, element);
			eval(element.getAttribute("mousedown"));
		}
	}
	Self.clickCheckBox = false;
};

//event in ground
UIFileView.prototype.mouseDownGround = function(evt) {
	eval("var event = ''");
	event = evt || window.event;
	var element = this;
	element.holdMouse = true;
	Self.hideContextMenu();
	Self.temporaryItem = null;
	document.onselectstart = function(){return false};
	
	var rightClick = (event.which && event.which > 1) || (event.button && event.button == 2);
	if (!rightClick && eXo.ecm.UIFileView.objResize == null) {
		resetArrayItemsSelected();
		element.onmousemove = Self.mutipleSelect;
		var mask = gj(element).find("div.Mask:first")[0];
		mask.storeX = eXo.ecm.DMSBrowser.findMouseRelativeX(element, event);
		mask.storeY = eXo.core.Browser.findMouseRelativeY(element, event);
		addStyle(mask, {
			left: mask.storeX + "px",
			top: mask.storeY + "px",
			zIndex: 1,
			width: "0px",
			height: "0px",
			backgroundColor: "gray",
			border: "1px dotted black"
		});
		mask.style.opacity = 17/100;
		
		//store position for all item
		var listGrid = gj(element).find("div.UIListGrid:first")[0];
		for( var i = 0 ; i < Self.allItems.length; ++i) {
			Self.allItems[i].posX = Math.abs(eXo.core.Browser.findPosXInContainer(Self.allItems[i], element)) - listGrid.scrollLeft;
			Self.allItems[i].posY = Math.abs(eXo.core.Browser.findPosYInContainer(Self.allItems[i], element)) - listGrid.scrollTop;
		}
	}
};

UIFileView.prototype.mutipleSelect = function(event) {
	var event = event || window.event;
	var element = this;
	var mask = gj(element).find("div.Mask:first")[0];
	
	var top = mask.storeY - 2;
	var right = element.offsetWidth - mask.storeX - 2;
	var bottom = element.offsetHeight - mask.storeY - 2;
	var left = mask.storeX - 2;
	
	if (element.holdMouse) {
			resetArrayItemsSelected();
			//select mutiple item by mouse
			mask.X = eXo.ecm.DMSBrowser.findMouseRelativeX(element, event);
			mask.Y = eXo.core.Browser.findMouseRelativeY(element, event);
			mask.deltaX = mask.X - mask.storeX;
			mask.deltaY = mask.Y - mask.storeY;
			
			mask.style.width = Math.abs(mask.deltaX) + "px";
			mask.style.height = Math.abs(mask.deltaY) + "px";
			// IV of +
			if (mask.deltaX < 0 && mask.deltaY > 0) {
				if (mask.offsetHeight > bottom) {
					mask.style.height = bottom + "px";
				}
				mask.style.top = mask.storeY -20 + "px";	
				if (mask.offsetWidth > left) {
					mask.style.width = left + "px";
					mask.style.left = 0 + "px";
				} else {
					mask.style.left = mask.X + "px";
				}
			// III of +
			}	else if (mask.deltaX < 0 && mask.deltaY < 0) {
				if (mask.offsetHeight > top) {
					mask.style.height = top + "px";
					mask.style.top = 0 + "px";
				} else {
					mask.style.top = mask.Y - 20 + "px";
				}
				
				if (mask.offsetWidth > left) {
					mask.style.width = left + "px";
					mask.style.left = 0 + "px";
				} else {
					mask.style.left = mask.X + "px";
				}
				//detect element 
				for (var i in Self.allItems) {
					if (Array.prototype[i]) continue;
					var itemBox = Self.allItems[i];
					var posX = itemBox.posX + itemBox.offsetWidth/2;
					var posY = itemBox.posY + itemBox.offsetHeight/2;
					if (mask.Y < posY && posY < mask.storeY) {
						itemBox.selected = true;
						//Dunghm: Check Shift key
						itemBox.setAttribute("isLink",null);
						if(event.ctrlKey && event.shiftKey) itemBox.setAttribute("isLink",true);
						itemBox.style.background = Self.colorSelected;
						//eXo.core.Browser.setOpacity(itemBox, 100);
					} else {
						itemBox.selected = null;
						itemBox.setAttribute("isLink",null);
						itemBox.style.background = "none";
						//eXo.core.Browser.setOpacity(itemBox, 85);
					}
				}
			// II	of +
			} else if (mask.deltaX > 0 && mask.deltaY < 0) {
				if (mask.offsetHeight > top) {
					mask.style.height = top + "px";
					mask.style.top = 0 + "px";
				} else {
					mask.style.top = mask.Y - 20 + "px";
				}	
				if (mask.offsetWidth > right) {
					mask.style.width = right + "px";
				} 
				mask.style.left = mask.storeX + "px";
				//detect element;
				for (var i in Self.allItems) {
					if (Array.prototype[i]) continue;
					var itemBox = Self.allItems[i];
					var posX = itemBox.posX + itemBox.offsetWidth/2;
					var posY = itemBox.posY + itemBox.offsetHeight/2;
					if (mask.Y < posY && posY < mask.storeY ) {
						itemBox.selected = true;
						//Dunghm: Check Shift key
						itemBox.setAttribute("isLink",null);
						if(event.ctrlKey && event.shiftKey) itemBox.setAttribute("isLink",true);
						itemBox.style.background = Self.colorSelected;
						//eXo.core.Browser.setOpacity(itemBox, 100);
					} else {
						itemBox.selected = null;
						itemBox.setAttribute("isLink",null);
						itemBox.style.background = "none";
						//eXo.core.Browser.setOpacity(itemBox, 85);
					}
				}
			// I of +
			} else {
				if (mask.offsetHeight > bottom) {
					mask.style.height = bottom + "px";
				}
				mask.style.top = mask.storeY -20 + "px";	
				if (mask.offsetWidth > right) {
					mask.style.width = right + "px";
				}
				mask.style.left = mask.storeX + "px";
			}
	}
};

UIFileView.prototype.mouseUpGround = function(evt) {
	eval("var event = ''");
	event = evt || window.event;
	var element = this;
	element.holdMouse = null;
	element.onmousemove = null;
	revertResizableBlock();
	removeMobileElement();
	Self.enableDragDrop = null;
	document.onselectstart = function(){return true};
	
	var mask = gj(element).find("div.Mask:first")[0];
	addStyle(mask, {width: "0px", height: "0px", top: "0px", left: "0px", border: "none"});
	//collect item
	var item = null;
	for(var i in Self.allItems) {
		if (Array.prototype[i]) continue;
		item = Self.allItems[i];
		if (item.selected && !inArray(Self.itemsSelected, item)) Self.itemsSelected.push(item);
	}
	//show context menu
	var rightClick = (event.which && event.which > 1) || (event.button && event.button == 2);
	if (rightClick) Self.showGroundContextMenu(event, element);
};

// working with item context menu
UIFileView.prototype.showItemContextMenu = function (event, element) {
var event = event || window.event;
event.cancelBubble = true;
if (document.getElementById(Self.contextMenuId)) {
  var contextMenu = document.getElementById(Self.contextMenuId);
  contextMenu.parentNode.removeChild(contextMenu);
}
//create context menu
var actionArea = document.getElementById(Self.actionAreaId);
var context = gj(actionArea).find("div.ItemContextMenu:first")[0];
var contextMenu = newElement({
  innerHTML: context.innerHTML,
  id: Self.contextMenuId,
  style: {
    position: "absolute",
    height: "0px",
    width: "0px",
    top: "-1000px",
    display: "block"
  }
});
document.body.appendChild(contextMenu);

//check lock, unlock action
var checkUnlock = false;
var checkRemoveFavourite = false;
var checkInTrash = false;
var checkMediaType = false;
var checkEmptyTrash = false;
for (var i in Self.itemsSelected) {
  if (Array.prototype[i]) continue;
  if (Self.itemsSelected[i].getAttribute('locked') == "true") checkUnlock = true;
  if (Self.itemsSelected[i].getAttribute('removeFavourite') == "true") checkRemoveFavourite = true;
  if (Self.itemsSelected[i].getAttribute('inTrash') == "true") checkInTrash = true;
  if (Self.itemsSelected[i].getAttribute('mediaType') == "true") checkMediaType = true;
  if (Self.itemsSelected[i].getAttribute('trashHome') == "true") checkEmptyTrash = true;
}
var lockAction = gj(contextMenu).find("div.Lock16x16Icon:first")[0];
var unlockAction = gj(contextMenu).find("div.Unlock16x16Icon:first")[0];

if (checkUnlock) {
  unlockAction.parentNode.style.display = "block";
  lockAction.parentNode.style.display = "none";
} else {
  unlockAction.parentNode.style.display = "none";
  lockAction.parentNode.style.display = "block";
}

var addFavouriteAction = gj(contextMenu).find("div.AddToFavourite16x16Icon:first")[0];
var removeFavouriteAction = gj(contextMenu).find("div.RemoveFromFavourite16x16Icon:first")[0];
if (checkRemoveFavourite) {
  removeFavouriteAction.parentNode.style.display = "block";
  addFavouriteAction.parentNode.style.display = "none";
} else {
  addFavouriteAction.parentNode.style.display = "block";
  removeFavouriteAction.parentNode.style.display = "none";
}
var restoreFromTrashAction = gj(contextMenu).find("div.RestoreFromTrash16x16Icon:first")[0];
var emptyTrashAction = gj(contextMenu).find("div.EmptyTrash16x16Icon:first")[0];
var playMediaAction = gj(contextMenu).find("div.PlayMedia16x16Icon:first")[0];

if (!checkInTrash) {
  restoreFromTrashAction.parentNode.style.display = "none";
} else {
  restoreFromTrashAction.parentNode.style.display = "block";
}
if (!checkMediaType) {
  playMediaAction.parentNode.style.display = "none";
} else {
  playMediaAction.parentNode.style.display = "block";
}
if (emptyTrashAction) {
  if (!checkEmptyTrash) {
    emptyTrashAction.parentNode.style.display = "none";
  } else {
    emptyTrashAction.parentNode.style.display = "block";
  }
}
var pasteAction = gj(contextMenu).find("div.Paste16x16Icon:first")[0];

if (Self.itemsSelected.length > 1) {
  pasteAction.parentNode.style.display = "none";
}
//check position popup
var X = event.pageX;
var Y = event.pageY;
var portWidth = gj(window).width();
var portHeight = gj(window).height();
var contentMenu = gj(contextMenu).children("div.UIRightClickPopupMenu:first")[0];
if (event.clientX + contentMenu.offsetWidth > portWidth) X -= contentMenu.offsetWidth;
if (event.clientY + contentMenu.offsetHeight > portHeight) Y -= contentMenu.offsetHeight + 5;
contextMenu.style.top = Y + 5 + "px";
contextMenu.style.left = X + 5 + "px";

contextMenu.onmouseup = Self.hideContextMenu;
document.body.onmousedown = Self.hideContextMenu;
document.body.onkeydown = Self.hideContextMenu;
};

// working with ground context menu
UIFileView.prototype.showGroundContextMenu = function(event, element) {
	var event = event || window.event;
	resetArrayItemsSelected();
	if (document.getElementById(Self.contextMenuId)) {
		var contextMenu = document.getElementById(Self.contextMenuId);
		contextMenu.parentNode.removeChild(contextMenu);
	}
	//create context menu
	var actionArea = document.getElementById(Self.actionAreaId);
	var context = gj(actionArea).find("div.GroundContextMenu:first")[0];
	var contextMenu = newElement({
		innerHTML: context.innerHTML,
		id: Self.contextMenuId,
		style: {
			position: "absolute",
			height: "0px",
			width: "0px",
			top: "-1000px",
			display: "block"
		}
	});
	document.body.appendChild(contextMenu);
	
	//check position popup
	var X = event.pageX;
	var Y = event.pageY;
	var portWidth = gj(window).width();
	var portHeight = gj(window).height();
	var contentMenu = gj(contextMenu).children("div.UIRightClickPopupMenu:first")[0];
	if (event.clientX + contentMenu.offsetWidth > portWidth) X -= contentMenu.offsetWidth;
	if (event.clientY + contentMenu.offsetHeight > portHeight) Y -= contentMenu.offsetHeight + 5;
	contextMenu.style.top = Y + 5 + "px";
	contextMenu.style.left = X + 5 + "px";
	
	contextMenu.onmouseup = Self.hideContextMenu;
	document.body.onmousedown = Self.hideContextMenu;
	document.body.onkeydown = Self.hideContextMenu;
};

// hide context menu
UIFileView.prototype.hideContextMenu = function() {
	var contextMenu = document.getElementById(Self.contextMenuId);
	if (contextMenu) contextMenu.style.display = "none";
	
	//remove default context menu;
	eval(eXo.core.MouseEventManager.onMouseDownHandlers);
	eXo.core.MouseEventManager.onMouseDownHandlers = null;
};

UIFileView.prototype.postGroupAction = function(moveActionNode, ext) {
	var objectId = [];
	var workspaceName = [];
	var islink = "";
	var ext = ext? ext : "";
	var itemsSelected = Self.itemsSelected;		
	if (!itemsSelected || itemsSelected.length == 0)
	itemsSelected = eXo.ecm.UISimpleView.itemsSelected;
	
	if(itemsSelected.length) {
		for(var i in itemsSelected) {
			if (Array.prototype[i]) continue;
			var currentNode = itemsSelected[i];
			currentNode.isSelect = false;
			//Dunghm: Check Shift key
			var islinkValue = currentNode.getAttribute("isLink");
			if (islinkValue && (islinkValue != "") && (islinkValue != "null")) islink += islinkValue ;

			var oid = currentNode.getAttribute("objectId");
			var wsname = currentNode.getAttribute("workspaceName");
			if (oid) objectId.push(wsname + ":" + oid);
			else objectId.push("");
		}
		//Dunghm: Check Shift key
		var url = (typeof(moveActionNode) == "string")?moveActionNode:moveActionNode.getAttribute("request");
		if(islink && islink != "") {
		  url = moveActionNode.getAttribute("symlink");
			ext += "&isLink="+true;
		}
		var additionParam = "&objectId=" + objectId.join(";") + ext;
		url = eXo.ecm.WCMUtils.addParamIntoAjaxEventRequest(url, additionParam);
		eval(url); 
	}
};

UIFileView.prototype.checkBoxItem = function() {
	eXo.ecm.UIFileView.clickCheckBox = true;
	//console.log('check: ' + Self.clickCheckBox)
};

UIFileView.prototype.initStickBreadcrumb = function() {
	var stickBreadcrumb = function() {
		var breadcrumb = gj('#FileViewBreadcrumb');
		var breadCrumbOffTop = breadcrumb.offset().top;
		if (eXo.ecm.UIFileView.minBreadcrumbTop == 0) {
			eXo.ecm.UIFileView.minBreadcrumbTop = breadCrumbOffTop;
		}
		var scroll_top = gj(window).scrollTop(); // our current vertical position from the top
		if (scroll_top >= eXo.ecm.UIFileView.minBreadcrumbTop) {
			breadcrumb.css({ 'position': 'fixed', 'top':0, zIndex:100});
			breadcrumb.width(breadcrumb.parent().width());
		} else {
			breadcrumb.css({ 'position': 'relative' });  
		}   
	};
	stickBreadcrumb;
	gj(window).scroll(stickBreadcrumb);
	var breadcrumb = gj('#FileViewBreadcrumb');
	breadcrumb.width(breadcrumb.parent().width()-2);
};

//private method
function newElement(option) {
	var div = document.createElement('div');
	addStyle(div, option.style);
	delete option.style;
	for (var o in option) {
		div[o] = option[o];
	}
	return div;
}
function addStyle(element, style) {
	if (!element) return;
	for (var o in style) {
		if (Object.prototype[o]) continue;
		element.style[o] = style[o];
	}
}
function removeMobileElement() {
		var mobileElement = document.getElementById(Self.mobileId);
		if (mobileElement) document.body.removeChild(mobileElement);
}
function resetArrayItemsSelected() {
	for(var i in Self.itemsSelected) {
		if (Array.prototype[i]) continue;
		Self.itemsSelected[i].selected = null;
		Self.itemsSelected[i].style.background = "none";
		//eXo.core.Browser.setOpacity(Self.itemsSelected[i], 85);
	}
	Self.itemsSelected = new Array();
}
function removeItem(arr, item) {
	for(var i = 0, nSize = arr.length; i < nSize; ++i) {
		if (arr[i] == item) {
			arr.splice(i, 1);
			break;
		}
	}
}
function inArray(arr, item) {
	for(var i = 0, nSize = arr.length; i < nSize; ++i) {
			if (arr[i] == item)	return true;
	}
	return false;
}

function revertResizableBlock() {
	
	//revert status overflow for UIResizableBlock;
	var actionArea = document.getElementById(Self.actionAreaId);
	var uiWorkingArea = gj(actionArea).parents(".UIWorkingArea:first")[0];
	var uiResizableBlock = gj(uiWorkingArea).find("div.UIResizableBlock:first")[0];
	
}


eXo.ecm.UIFileView = new UIFileView();
_module.UIFileView = eXo.ecm.UIFileView;
