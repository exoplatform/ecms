(function(gj, webuiExt, ecm_utils, wcm_utils) {
  var UIFileView = function() {

	this.openDivs = {};
	// eXo.ecm.UIFileView
	
	var Self = this;
	Self.columnData = {};
	var BROW = eXo.core.Browser;
	
	UIFileView.prototype.temporaryItem = null;
	UIFileView.prototype.itemsSelected = [];
	UIFileView.prototype.allItems = [];
	UIFileView.prototype.contextMenuId = null;
	UIFileView.prototype.actionAreaId = null;
	UIFileView.prototype.enableDragDrop = null;
	UIFileView.prototype.clickCheckBox = false;
	UIFileView.prototype.clickTotalCheckBox = false;

	UIFileView.prototype.colorSelected = "#e5e5e5";
	UIFileView.prototype.colorHover = "#eeeeee";
	
	UIFileView.prototype.t1 = 0;
	UIFileView.prototype.t2 = 0;
	UIFileView.prototype.minBreadcrumbTop = 0;
	UIFileView.prototype.minActionbarTop = 0;
	UIFileView.prototype.clickedItem = null;
	UIFileView.prototype.selectBoxType = null;
	UIFileView.prototype.firstTimeClick = false;
	UIFileView.prototype.active="active";

UIFileView.prototype.clickFolder =  function (folderDiv, expandLink, collapseLink, docListId,event) {
	if (!folderDiv) return;
    folderDiv.className = "uiIconArrowRight" == folderDiv.className ? "uiIconArrowDown" : "uiIconArrowRight";
    var docList = document.getElementById(docListId);
    if ("uiIconArrowRight" == folderDiv.className) {
    	eval(decodeURIComponent(collapseLink));
    } else {
    	eval(decodeURIComponent(expandLink));
    }
    var evt = event || window.event;
    evt.cancelBubble = true;
}

UIFileView.prototype.cancelEvent = function(event) {
	event.cancelBubble=true;
}

UIFileView.prototype.clearOpenDivs =  function () {
	eXo.ecm.UIFileView.openDivs = {};
}

UIFileView.prototype.clearItemsSelected = function() {
	Self.itemsSelected = [];
	Self.allItems = [];
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
	Self.allItems = gj(actionArea).find("div.rowView");
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
	
	var listGrid = gj(actionArea).find("div.uiListGrid:first")[0];
	if (listGrid) {
		var fillOutElement = document.createElement('div');
		fillOutElement.className = "FillOutElement";
		gj("div.FillOutElement").remove();
		listGrid.appendChild(fillOutElement);
	}
	
	//remove context menu
	var contextMenu = document.getElementById(Self.contextMenuId);
	if (contextMenu) contextMenu.parentNode.removeChild(contextMenu);
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

//event in item
UIFileView.prototype.mouseOverItem = function(event) {
	var event = event || window.event;
	var element = this;
	if (!element.selected) {
		//element.style.background = Self.colorHover;
		element.temporary = true;
		//eXo.core.Browser.setOpacity(element, 100);
	}
};

UIFileView.prototype.mouseOutItem = function(event) {
	var event = event || window.event;
	var element = this;
	element.temporary = false;
	if (!element.selected) {
		//element.style.background = "none";
		//eXo.core.Browser.setOpacity(element, 85);
	}
};

UIFileView.prototype.mouseDownItem = function(evt) {
	eval("var event = ''");
	event = evt || window.event;
	if (!event) return;
	event.cancelBubble = true;
	var element = Self.clickedItem || this;
	removeMobileElement();
	Self.hideContextMenu();
	Self.firstTimeClick = false;
	var d = new Date();		
Self.t1 = d.getTime();   
	Self.enableDragDrop = true;
	Self.srcPath = element.getAttribute("objectId");
	document.onselectstart = function(){return false};
	var rightClick = (event.which && event.which > 1) || (event.button && event.button == 2);
	if (!rightClick) {
		//console.log('mouseDown: ' + Self.clickCheckBox);
		if (!inArray(Self.itemsSelected, element) && !pressCtrl(event) && !event.shiftKey && !Self.clickCheckBox && !Self.clickTotalCheckBox) {
			Self.clickItem(event, element);
			Self.firstTimeClick = true;
		} else if (Self.clickTotalCheckBox) {
			//toggle current node's check box
			gj("input:checkbox", element).each(function(index, elem) {
				var value = Self.selectBoxType;
				gj(elem).attr("checked", value);
			});
		} 
		else if (!Self.clickCheckBox) {
			gj("input:checkbox", element).each(function(index, elem) {
				gj(elem).attr("checked", !elem.checked);
			});
		}

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
		var coverElement = newElement({className: "uiListGrid"});
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
//	Self.clickedItem = null;
//	Self.clickCheckBox = false;
//	Self.clickTotalCheckBox = false;
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
		if (Self.enableDragDrop && mobileElement && (!pressCtrl(event) || (event.shiftKey && pressCtrl(event)))) {
			mobileElement.style.display = "block";
			var X = event.pageX || event.clientX;
			var Y = event.pageY || event.clientY; 
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
	//element.style.background = Self.colorSelected;
	gj(element).addClass(eXo.ecm.UIFileView.active);
	//uncheck all checkboxes
	var uiDocInfo = gj("#UIDocumentInfo")[0] || gj("#UIDocumentWithTree")[0];
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
	if (!event) return;
	var element = Self.clickedItem || this;
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
				if(pressCtrl(event) && event.shiftKey)
				  Self.postGroupAction(moveAction.getAttribute("symlink"), "&destInfo=" + wsTarget + ":" + idTarget);
				else
				  Self.postGroupAction(moveAction, "&destInfo=" + wsTarget + ":" + idTarget);
				gj(mobileElement).hide();
			}
		} else {
			if ((pressCtrl(event) || Self.clickCheckBox || (Self.clickTotalCheckBox && Self.selectBoxType)) && !element.selected) {
				element.selected = true;
				//for select use shilf key;
				Self.temporaryItem = element;
				Self.itemsSelected.push(element);
				//Dunghm: Check Shift key
				element.setAttribute("isLink",null);
				if(event.shiftKey) element.setAttribute("isLink",true);
			} else if((pressCtrl(event) || Self.clickCheckBox || Self.clickTotalCheckBox) && element.selected) {
				element.selected = null;
				element.setAttribute("isLink",null);
				//element.style.background = "none";
				gj(element).removeClass(eXo.ecm.UIFileView.active);
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
					if(pressCtrl(event)) element.setAttribute("isLink",true);
					Self.itemsSelected.push(Self.allItems[i]);
				}
			} else if (Self.selectBoxType || !Self.clickTotalCheckBox) {
				Self.clickItem(event, element);
			}
			for(var i in Self.itemsSelected) {
				if (Array.prototype[i]) continue;
				//Self.itemsSelected[i].style.background = Self.colorSelected;
				gj(Self.itemsSelected[i]).addClass(eXo.ecm.UIFileView.active);
				//eXo.core.Browser.setOpacity(Self.itemsSelected[i], 100);
			}
		}
		//show menu actions
		if (!Self.clickTotalCheckBox) {
			//event.cancelBubble = true;
			if (inArray(Self.itemsSelected, element)){
				if (Self.itemsSelected.length > 1) {
					Self.showItemContextMenu(event, element);
				} else {//Self.itemsSelected.length==1
					var action = element.getAttribute("mousedown");
					eval(action);
				}
			} else {// if (!inArray(Self.itemsSelected, element) && Self.itemsSelected.length == 1)
				if (Self.itemsSelected.length > 1) {
					Self.showItemContextMenu(event, element);
				} else if (Self.itemsSelected.length == 1) {
					var action = Self.itemsSelected[0].getAttribute("mousedown");
					eval(action);
				}
			}
		}
	} else {
	}
	Self.clickedItem = null;
	Self.clickCheckBox = false;
	Self.clickTotalCheckBox = false;
	Self.firstTimeClick = false
	Self.checkSelectedItemCount();
    eXo.ecm.ECMUtils.loadContainerWidth();
  
	// Close rename popup
	var renameWindowPopup = gj('#UIRenameWindowPopup');
	if (renameWindowPopup) {
	  renameWindowPopup.hide();
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
						if(pressCtrl(event) && event.shiftKey) itemBox.setAttribute("isLink",true);
						//itemBox.style.background = Self.colorSelected;
						gj(itemBox).addClass(eXo.ecm.UIFileView.active);
						//eXo.core.Browser.setOpacity(itemBox, 100);
					} else {
						itemBox.selected = null;
						itemBox.setAttribute("isLink",null);
						//itemBox.style.background = "none";
						gj(itemBox).removeClass(eXo.ecm.UIFileView.active);
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
						if(pressCtrl(event) && event.shiftKey) itemBox.setAttribute("isLink",true);
						//itemBox.style.background = Self.colorSelected;
						gj(itemBox).addClass(eXo.ecm.UIFileView.active);
						//eXo.core.Browser.setOpacity(itemBox, 100);
					} else {
						itemBox.selected = null;
						itemBox.setAttribute("isLink",null);
						//itemBox.style.background = "none";
						gj(itemBox).removeClass(eXo.ecm.UIFileView.active);
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

// working with item context menu
UIFileView.prototype.showItemContextMenu = function (event, element) {
	gj("#UIActionBarTabsContainer").addClass("NoShow");
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
//	    position: "absolute",
	    height: "0px",
	    width: "0px",
//	    top: "-1000px",
	    display: "block"
	  }
	});
	gj("#ActionMenuPlaceHolder").prepend(contextMenu);
    gj(".uiRightClickPopupMenu", contextMenu).addClass("uiFileViewActionBar");
    gj(".uiRightPopupMenuContainer", contextMenu).addClass("clearfix");
    //check lock, unlock action
	var checkUnlock = false;
	var checkRemoveFavourite = false;
	var checkInTrash = false;
	var checkMediaType = false;
	var checkEmptyTrash = false;
	var checkLinkAndTargetInTrash = false;
	var checkExoActionNode = false;
	var checkInStatus = false;
	var isAbleToRestore = true;
	
	for (var i in Self.itemsSelected) {
	  if (Array.prototype[i]) continue;
	  // check if a node is exo:action or not to show nothing on action bar.
	  if (	Self.itemsSelected[i].getAttribute('isExoAction') == "true") {
		  checkExoActionNode = true;
		  break;
	  }  
	  //check symlink and target are in trash to show Delete button only on action bar.
	  else if (Self.itemsSelected[i].getAttribute('isLinkWithTarget') == "true") {
		checkLinkAndTargetInTrash = true; 
		continue;
	  }
	  //check if one of the node can be restored
	  else if (Self.itemsSelected[i].getAttribute('isAbleToRestore') == "false") {
		isAbleToRestore = false; 
		continue;
	  }
	  if (Self.itemsSelected[i].getAttribute('locked') == "true") checkUnlock = true;
	  if (Self.itemsSelected[i].getAttribute('removeFavourite') == "true") checkRemoveFavourite = true;
	  if (Self.itemsSelected[i].getAttribute('inTrash') == "true") checkInTrash = true;
	  if (Self.itemsSelected[i].getAttribute('mediaType') == "true") checkMediaType = true;
	  if (Self.itemsSelected[i].getAttribute('trashHome') == "true") checkEmptyTrash = true;
	  if (Self.itemsSelected[i].getAttribute('isCheckedIn') == "true") checkInStatus = true;
	}
	
	var lockAction = gj(contextMenu).find("i.uiIconEcmsLock:first")[0];
	var unlockAction = gj(contextMenu).find("i.uiIconEcmsUnlock:first")[0];
	var addFavouriteAction = gj(contextMenu).find("i.uiIconEcmsAddToFavourite:first")[0];
	var removeFavouriteAction = gj(contextMenu).find("i.uiIconEcmsRemoveFromFavourite:first")[0];
	var restoreFromTrashAction = gj(contextMenu).find("i.uiIconEcmsRestoreFromTrash:first")[0];
	var emptyTrashAction = gj(contextMenu).find("i.uiIconEcmsEmptyTrash:first")[0];
	var playMediaAction = gj(contextMenu).find("i.uiIconEcmsPlayMedia:first")[0];
	var pasteAction = gj(contextMenu).find("i.uiIconEcmsPaste:first")[0];
	var copyAction = gj(contextMenu).find("i.uiIconEcmsCopy:first")[0];
	var cutAction= gj(contextMenu).find("i.uiIconEcmsCut:first")[0];
	var addSymLinkAction= gj(contextMenu).find("i.uiIconEcmsAddSymLink:first")[0];
	var deleteAction = gj(contextMenu).find("i.uiIconEcmsDelete:first")[0];
	var viewInfoAction = gj(contextMenu).find("i.uiIconEcmsViewInfo:first")[0];
	
	if (checkExoActionNode) {
		// disable all buttons
		deleteAction.parentNode.style.display = "none";
		lockAction.parentNode.style.display = "none";
		unlockAction.parentNode.style.display = "none";
		
		addFavouriteAction.parentNode.style.display = "none";
		removeFavouriteAction.parentNode.style.display = "none";
		
		restoreFromTrashAction.parentNode.style.display = "none";
		playMediaAction.parentNode.style.display = "none";
		
		//emptyTrashAction.parentNode.style.display = "none";
		pasteAction.parentNode.style.display = "none";
		copyAction.parentNode.style.display = "none";
		cutAction.parentNode.style.display = "none";
		
		addSymLinkAction.parentNode.style.display = "none";
	} else if (checkLinkAndTargetInTrash) {
		deleteAction.parentNode.style.display = "block";
		lockAction.parentNode.style.display = "none";
		unlockAction.parentNode.style.display = "none";
		
		addFavouriteAction.parentNode.style.display = "none";
		removeFavouriteAction.parentNode.style.display = "none";
		
		restoreFromTrashAction.parentNode.style.display = "none";
		playMediaAction.parentNode.style.display = "none";
		
		//emptyTrashAction.parentNode.style.display = "none";
		pasteAction.parentNode.style.display = "none";
		copyAction.parentNode.style.display = "none";
		cutAction.parentNode.style.display = "none";
		
		addSymLinkAction.parentNode.style.display = "none";
		
	} else {
		if (checkUnlock) {
		  unlockAction.parentNode.style.display = "block";
		  lockAction.parentNode.style.display = "none";
		} else {
		  unlockAction.parentNode.style.display = "none";
		  lockAction.parentNode.style.display = "block";
		}
		  
		if (checkRemoveFavourite) {
		  removeFavouriteAction.parentNode.style.display = "block";
		  addFavouriteAction.parentNode.style.display = "none";
		} else {
		  addFavouriteAction.parentNode.style.display = "block";
		  removeFavouriteAction.parentNode.style.display = "none";
		}
	   
		if (!checkInTrash || !isAbleToRestore) {
		  restoreFromTrashAction.parentNode.style.display = "none";
		} else {
		  restoreFromTrashAction.parentNode.style.display = "block";
		}
		if (checkInStatus) {
		  addSymLinkAction.parentNode.style.display = "none";
		} else {
		  addSymLinkAction.parentNode.style.display = "block";
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
		
		if (Self.itemsSelected.length > 1) {
		  pasteAction.parentNode.style.display = "none";
		}
	}
    
	//check position popup
	var X = event.pageX || event.clientX;
	var Y = event.pageY || event.clientY;
	var portWidth = gj(window).width();
	var portHeight = gj(window).height();
    var contentMenu = gj(contextMenu).children("div.uiRightClickPopupMenu:first")[0];
	if (event.clientX + contentMenu.offsetWidth > portWidth) X -= contentMenu.offsetWidth;
	if (event.clientY + contentMenu.offsetHeight > portHeight) Y -= contentMenu.offsetHeight + 5;
//	contextMenu.style.top = Y + 5 + "px";
//	contextMenu.style.left = X + 5 + "px";
    var menubar = gj('div.uiFileViewActionBar');
    if (menubar) {
    	menubar.width(gj("div#UIActionBar").width()-2);
    }	
    var moreButton = gj("#ShowMoreActionContainer");
    if (moreButton) {
    	moreButton.hide();
    }
    eXo.ecm.ECMUtils.loadContainerWidth();
};

// hide context menu
UIFileView.prototype.hideContextMenu = function() {
	var contextMenu = document.getElementById(Self.contextMenuId);
	if (contextMenu) contextMenu.style.display = "none";
	
	var contextMenu = document.getElementById('ECMContextMenu');
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
		var actionbar = gj('#UIActionBar');
		if (!breadcrumb) return;
		if (!breadcrumb.offset()) return;
		var breadCrumbOffTop = breadcrumb.offset().top;
		var actionbarOffTop = actionbar.offset().top;
		if (eXo.ecm.UIFileView.minBreadcrumbTop == 0) 
			eXo.ecm.UIFileView.minBreadcrumbTop = breadCrumbOffTop;
		if (eXo.ecm.UIFileView.minActionbarTop == 0) 
			eXo.ecm.UIFileView.minActionbarTop = actionbarOffTop;
		
		var scroll_top = gj(window).scrollTop(); // our current vertical position from the top
		
		if (scroll_top >= eXo.ecm.UIFileView.minActionbarTop) {
			actionbar.css({ 'position': 'fixed','z-index': '2', 'top':0});
			actionbar.width(actionbar.parent().width());
			breadcrumb.css({ 'position': 'fixed', 'top':actionbar.height(), zIndex:1});
			breadcrumb.width(breadcrumb.parent().width());
		} else {
			actionbar.css({ 'position': 'relative' });  
			breadcrumb.css({ 'position': 'relative', 'top' :0 });  
		}   
	};
	stickBreadcrumb;
	gj(window).scroll(stickBreadcrumb);
	var breadcrumb = gj('#FileViewBreadcrumb');
	breadcrumb.width(breadcrumb.parent().width());
	var actionbar= gj('#UIActionBar');
	actionbar.width(actionbar.parent().width());
};

UIFileView.prototype.toggleCheckboxes = function(checkbox, evt) {
	resetArrayItemsSelected();
	gj(Self.allItems).each(function(index, elem){
		Self.selectBoxType = checkbox.checked;
		Self.clickedItem = elem;
		Self.clickTotalCheckBox = true;
		Self.mouseDownItem(evt);
		//-------------------------
		Self.selectBoxType = checkbox.checked;
		Self.clickedItem = elem;
		Self.clickTotalCheckBox = true;
		Self.mouseUpItem(evt);
	});
	//---------------------------
	if (checkbox && checkbox.checked) {
		Self.showItemContextMenu(evt);
	} else {
		Self.hideContextMenu();
	}
	Self.checkSelectedItemCount();
};

UIFileView.prototype.clearCheckboxes = function(evt) {
	var uiFileView = gj(".uiFileView")[0];
	if (uiFileView) {
		resetArrayItemsSelected();
		gj("#UIFileViewCheckBox").attr("checked", false);
		gj("#UIDocumentInfo").find(".checkbox").attr("checked", false);
		//gj("#UIDocumentInfo").find(".rowView").css("backgroundColor","#FFF");
		gj("#UIDocumentInfo").find(".rowView").removeClass(eXo.ecm.UIFileView.active);
		//case with tree
		gj("#UIDocumentWithTree").find(".checkbox").attr("checked", false);
		//gj("#UIDocumentWithTree").find(".rowView").css("backgroundColor","#FFF");
		gj("#UIDocumentWithTree").find(".rowView").removeClass(eXo.ecm.UIFileView.active);
		
		Self.checkSelectedItemCount();
		Self.hideContextMenu();
	}
};

UIFileView.prototype.checkSelectedItemCount = function() {
	if (Self.itemsSelected.length > 1) {
		gj("#FileViewItemCount").html(Self.itemsSelected.length);
		gj("#FileViewStatus").removeClass("NoShow");
	} else {
		gj("#FileViewStatus").addClass("NoShow");
	}
	//---------------------------------------------
	if (Self.itemsSelected.length == 0) {
		gj("#UIActionBarTabsContainer").removeClass("NoShow");		
	}
};

UIFileView.prototype.clickRightMouse = function(event, elemt, menuId, objId, whiteList, opt) {
	gj("#UIActionBarTabsContainer").addClass("NoShow");
    if (!event)
      event = window.event;

    var contextMenu = document.getElementById(menuId);
    contextMenu.objId = objId;

    //help to disable browser context menu
    //when onmouseover is registered after the dom has already displayed, mouseover evt'll not be raised
    var parent = gj(contextMenu).parent();
    if (!document.oncontextmenu) {
    	parent.trigger("mouseover");
    }
    
    var jDoc = gj(document);
    jDoc.trigger("mousedown.RightClickPopUpMenu");    
    //Register closing contextual menu callback on document
    jDoc.one("mousedown.RightClickPopUpMenu", function(e)
    {
//    	Self.hideContextMenu(menuId);
    });

    //The callback registered on document won't be triggered by current 'mousedown' event
    if ( event.stopPropagation ) {
    	event.stopPropagation();
    }
    event.cancelBubble = true;

    if (whiteList) {
      gj(contextMenu).find("a").each(function()
      {
        var item = gj(this);
        if(whiteList.indexOf(item.attr("exo:attr")) > -1 || item.hasClass("dropdown-toggle"))
        {
          item.css("display", "block");
        }
        else
        {
          item.css("display", "none");
        }
      });
    }

    var customItem = gj(elemt).find("li.RightClickCustomItem").eq(0);
    var tmpCustomItem = gj(contextMenu).find("li.RightClickCustomItem").eq(0);
    if(customItem && tmpCustomItem)
    {
      tmpCustomItem.html(customItem.html());
      tmpCustomItem.css("display", "inline");
    }
    else if(tmpCustomItem)
    {
      tmpCustomItem.css("display", "none");
    }
    /*
     * fix bug right click in IE7.
     */
    var fixWidthForIE7 = 0;
    var UIWorkingWorkspace = document.getElementById("UIWorkingWorkspace");
    if (eXo.core.Browser.isIE7() && document.getElementById("UIDockBar")) {
      if (event.clientX > UIWorkingWorkspace.offsetLeft)
        fixWidthForIE7 = UIWorkingWorkspace.offsetLeft;
    }

    eXo.core.Mouse.update(event);
    gj("#ActionMenuPlaceHolder").prepend(contextMenu);
    gj(contextMenu).addClass("uiFileViewActionBar");
    var moreButton = gj("#hiddenMoreButton:first")[0];
    
    // Init url clipboard when click more button
    gj(moreButton).mouseup(function() {
      var timer = setInterval(function()
        {
          if (gj("#hiddenMoreButton .dropdown-menu").is(":visible")) {
            eXo.ecm.ECMUtils.initClipboard();
            clearInterval(timer);
          }
        }
        , 200);
    });

    gj(contextMenu).find("ul:first").append(moreButton);
    eXo.webui.UIPopup.show(contextMenu);
    var menubar = gj('div.uiFileViewActionBar');
    if (menubar) {
    	menubar.width(gj("div#UIActionBar").width()-2);
    }    
    eXo.ecm.ECMUtils.loadContainerWidth();
    var moreButton = gj("#ShowMoreActionContainer");
    if (moreButton) {
    	moreButton.hide();
    }
    
    // Init feature Copy URL to Clipboard in case the action button appears right after select item in file view
    if(gj("#ECMContextMenu .uiIconEcmsCopyUrlToClipboard").is(":visible"))
      eXo.ecm.ECMUtils.initClipboard();
};

function pressCtrl(event) {
	return event.ctrlKey || event.metaKey;
}

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
		//Self.itemsSelected[i].style.background = "none";
		gj(Self.itemsSelected[i]).removeClass(eXo.ecm.UIFileView.active);
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

  };
eXo.ecm.UIFileView = new UIFileView();
  return {
    UIFileView : eXo.ecm.UIFileView
  };
})(gj, webuiExt, ecm_utils, wcm_utils);
