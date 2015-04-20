(function(gj, webuiExt, ecm_utils, wcm_utils) {
  var ListView = function() {

    // eXo.ecm.UIListView

    var Self = this;
    Self.columnData = {};
    var BROW = eXo.core.Browser;

    ListView.prototype.temporaryItem = null;
    ListView.prototype.itemsSelected = [];
    ListView.prototype.allItems = [];
    ListView.prototype.contextMenuId = null;
    ListView.prototype.actionAreaId = null;
    ListView.prototype.enableDragDrop = null;

    ListView.prototype.colorSelected = "#e8e8e8";
    ListView.prototype.colorHover = "#eeeeee";

    ListView.prototype.t1 = 0;
    ListView.prototype.t2 = 0;

    //init event
    ListView.prototype.initAllEvent = function(actionAreaId, enableDragAndDrop) {
      eXo.ecm.UIListView.enableDragAndDrop = enableDragAndDrop;
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
        }
      });
      actionArea.onmousedown = Self.mouseDownGround;
      actionArea.onkeydown = Self.mouseDownGround;
      actionArea.onmouseup = Self.mouseUpGround;
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
      //registry action drag drop in tree list
      eXo.ecm.UIListView.initDragDropForTreeEvent("UIWorkingArea", enableDragAndDrop);    
    };

    ListView.prototype.initDragDropForTreeEvent = function(actionAreaId, enableDragAndDrop) {
      //registry action drag drop in tree list
      eXo.ecm.UIListView.enableDragAndDrop = enableDragAndDrop;
        gj("#" + actionAreaId + " div.uiTreeExplorer:first").find("div[objectId]").each(
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
    ListView.prototype.mouseOverTree = function(event) {
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
      var scroller = gj(element).parents(".uiContentBox:first")[0];
      scroller.onmousemove = eXo.ecm.UIListView.setScroll ;
    };

    ListView.prototype.setScroll = function(evt){
      if(Self.enableDragDrop) {
      eXo.ecm.UIListView.object = this;
        var element = eXo.ecm.UIListView.object ;
      var pos = evt.pageY - gj(element).offset().top;
      if(element.offsetHeight - pos < 10){
        element.scrollTop = element.scrollTop + 5;  
      } else if(element.scrollTop > 0 && pos < 10) {
      element.scrollTop = element.scrollTop - 5;  
      }
      }
    };

    ListView.prototype.mouseOutTree = function(event) {
      var element = this;
      clearTimeout(element.Timeout);
    };

    ListView.prototype.mouseDownTree = function(evt) {
      eval("var event = ''");
      event = evt || window.event;
      var element = this;
      Self.enableDragDrop = true;
      Self.srcPath = element.getAttribute("objectId");
      resetArrayItemsSelected();
      var rightClick = (event.which && event.which > 1) || (event.button && event.button == 2);
      if (rightClick) {
        var _objId = element.getAttribute("workspacename")+":"+Self.srcPath;
        eXo.ecm.OpenDocumentInOffice.updateLabel(_objId,null,true);
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
          className: "uiTreeExplorer",
          style: {margin: "0px 3px", padding: "3px 0px"}
        });
        coverElement.appendChild(itemSelected);
        mobileElement.appendChild(coverElement);
        document.body.appendChild(mobileElement);
      }
      
      // Init feature Copy URL to Clipboard
      eXo.ecm.ECMUtils.initClipboard();
      
      event.preventDefault();
    };

    ListView.prototype.mouseUpTree = function(evt) {
      eval("var event = ''");
      event = evt || window.event;
      var element = this;
      revertResizableBlock();
      Self.enableDragDrop = null;
      var mobileElement = document.getElementById(Self.mobileId);
      if (!mobileElement && eXo.ecm.UISimpleView && eXo.ecm.UISimpleView.mobileId)
        mobileElement = document.getElementById(eXo.ecm.UISimpleView.mobileId);
  
  //    Self.clickItem(event, element);    
      if (mobileElement && mobileElement.move) {
        //post action
        var actionArea = document.getElementById("UIWorkingArea");
        var moveAction = gj(actionArea).find("div.JCRMoveAction:first")[0];
        var wsTarget = element.getAttribute('workspacename');
        var idTarget = element.getAttribute('objectId');
        var targetPath = decodeURIComponent(idTarget);
        var srcPath = Self.srcPath ?  decodeURIComponent(Self.srcPath) :
          decodeURIComponent(eXo.ecm.UISimpleView.srcPath);
  //      var regex = new RegExp("^"+decodeURIComponent(idTarget) + "/");
  //      alert("^"+decodeURIComponent(idTarget) + "/" + "\n" + "^"+decodeURIComponent(Self.srcPath) + "/");
  //      var regex1 = new RegExp("^"+decodeURIComponent(Self.srcPath) + "/");
  //      alert(regex.test(decodeURIComponent(Self.srcPath) + "/") + "\n" + regex1.test(decodeURIComponent(idTarget) + "/"))
  //      if(regex.test(decodeURIComponent(Self.srcPath) + "/")){
  //        delete Self.srcPath;
  //        return ;
  //      }
  //      if(regex1.test(decodeURIComponent(idTarget) + "/")) {
  //        delete Self.srcPath;
  //        return;
  //      }
        if (targetPath.indexOf(srcPath) == 0) {
          delete Self.srcPath;
          return;
        }
        //Dunghm : check symlink
        if (eXo.ecm.UIListView.enableDragAndDrop == "true") {
          if(event.ctrlKey && event.shiftKey)
            Self.postGroupAction(moveAction.getAttribute("symlink"), "&destInfo=" + wsTarget + ":" + idTarget);
          else {
            Self.postGroupAction(moveAction, "&destInfo=" + wsTarget + ":" + idTarget);
          }
        }      
      }
  //    Self.clickItem(event, element);    
    };

    //event in item
    ListView.prototype.mouseOverItem = function(event) {
      var event = event || window.event;
      var element = this;
      if (!element.selected) {
        element.style.background = Self.colorHover;
        element.temporary = true;
        //eXo.core.Browser.setOpacity(element, 100);
      }
    };

    ListView.prototype.mouseOutItem = function(event) {
      var event = event || window.event;
      var element = this;
      element.temporary = false;
      if (!element.selected) {
        element.style.background = "none";
      //  eXo.core.Browser.setOpacity(element, 85);
      }
    };

    ListView.prototype.mouseDownItem = function(evt) {
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
    
        if (!inArray(Self.itemsSelected, element) && !event.ctrlKey && !event.shiftKey) {
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
      }else{
        var _objId = element.getAttribute("workspacename")+":"+Self.srcPath;
        eXo.ecm.OpenDocumentInOffice.updateLabel(_objId);
        event.cancelBubble = true;
        if (inArray(Self.itemsSelected, element) && Self.itemsSelected.length > 1){
          Self.showItemContextMenu(event, element);
        } else {
          Self.clickItem(event, element);
          eval(element.getAttribute("mousedown"));
        }        
      }
    };

    ListView.prototype.dragItemsSelected = function(event) {
        var event = event || window.event;
        document.onselectstart = function(){return false;}
        if (eXo.ecm.UIListView.enableDragAndDrop != "true")
          return;
        var d = new Date();      
        Self.t2 = d.getTime();      
        if((Self.t2-Self.t1)<200) 
          return;
        var mobileElement = document.getElementById(Self.mobileId);
        if (Self.enableDragDrop && mobileElement && (!event.ctrlKey || (event.shiftKey && event.ctrlKey))) {
          mobileElement.style.display = "block";
          var X = event.pageX || event.clientX;
          var Y = event.pageY || event.clientY;
          mobileElement.style.top = Y + 5 + "px";
          mobileElement.style.left = X + 5 + "px";
          mobileElement.move = true;
        }
    };

    ListView.prototype.dropOutActionArea = function(event) {
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

    ListView.prototype.clickItem = function(event, element, callback) {
      var event = event || window.event;
      resetArrayItemsSelected();
      element.selected = true;
      //Dunghm: Check Shift key
      if(event.shiftKey) element.setAttribute("isLink",true);
      else element.setAttribute("isLink",null);
      //for select use shilf key;
      Self.temporaryItem = element;
      Self.itemsSelected = new Array(element);
      // element.style.background = Self.colorSelected;
      //eXo.core.Browser.setOpacity(element, 100);
    };

    ListView.prototype.mouseUpItem = function(evt) {
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
          if (eXo.ecm.UIListView.enableDragAndDrop == "true") {
            if(event.ctrlKey && event.shiftKey)
              Self.postGroupAction(moveAction.getAttribute("symlink"), "&destInfo=" + wsTarget + ":" + idTarget);
            else
              Self.postGroupAction(moveAction, "&destInfo=" + wsTarget + ":" + idTarget);
          }
        } else {
          if (event.ctrlKey && !element.selected) {
            element.selected = true;
            //for select use shilf key;
            Self.temporaryItem = element;
            Self.itemsSelected.push(element);
            //Dunghm: Check Shift key
            element.setAttribute("isLink",null);
            if(event.shiftKey) element.setAttribute("isLink",true);
          } else if(event.ctrlKey && element.selected) {
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
      }
      
      // Init feature Copy URL to Clipboard
      eXo.ecm.ECMUtils.initClipboard();
    };

    //event in ground
    ListView.prototype.mouseDownGround = function(evt) {
      eval("var event = ''");
      event = evt || window.event;
      var element = this;
      element.holdMouse = true;
      Self.hideContextMenu();
      Self.temporaryItem = null;
      document.onselectstart = function(){return false};
  
      var rightClick = (event.which && event.which > 1) || (event.button && event.button == 2);
      if (!rightClick && eXo.ecm.UIListView.objResize == null) {
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
        var listGrid = gj(element).find("div.uiListGrid:first")[0];
        for( var i = 0 ; i < Self.allItems.length; ++i) {
          Self.allItems[i].posX = Math.abs(eXo.core.Browser.findPosXInContainer(Self.allItems[i], element)) - listGrid.scrollLeft;
          Self.allItems[i].posY = Math.abs(eXo.core.Browser.findPosYInContainer(Self.allItems[i], element)) - listGrid.scrollTop;
        }
      }
    };

    ListView.prototype.mutipleSelect = function(event) {
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
          }  else if (mask.deltaX < 0 && mask.deltaY < 0) {
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
            Self.allItems.each(function(i, elem){
             if (!Array.prototype[i]) {
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
            });
          // II  of +
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
            Self.allItems.each(function(i, elem){
             if (!Array.prototype[i]) {
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
            });
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

    ListView.prototype.mouseUpGround = function(evt) {
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
    ListView.prototype.showItemContextMenu = function (event, element) {
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
      var checkLinkAndTargetInTrash = false;
      var checkExoActionNode = false;
      var checkInStatus = false;
  	  
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
      var deleteAction = gj(contextMenu).find("i.uiIconEcmsDelete:first")[0];
  	  var viewInfoAction = gj(contextMenu).find("i.uiIconEcmsViewInfo:first")[0];
  	  var copyAction = gj(contextMenu).find("i.uiIconEcmsCopy:first")[0];
	  var cutAction= gj(contextMenu).find("i.uiIconEcmsCut:first")[0];
	  var addSymLinkAction= gj(contextMenu).find("i.uiIconEcmsAddSymLink:first")[0];

	  if (checkExoActionNode) {
		// disable all buttons
        contextMenu.style.display = "none";
	  } else if (checkLinkAndTargetInTrash) {
		  // just display Delete button.
		  deleteAction.parentNode.style.display = "block";
		  unlockAction.parentNode.parentNode.style.display = "none";
	      lockAction.parentNode.parentNode.style.display = "none";
	      removeFavouriteAction.parentNode.parentNode.style.display = "none";
	      addFavouriteAction.parentNode.parentNode.style.display = "none";
	      restoreFromTrashAction.parentNode.parentNode.style.display = "none";
	      playMediaAction.parentNode.parentNode.style.display = "none";
	      //emptyTrashAction.parentNode.parentNode.style.display = "none";
	      pasteAction.parentNode.parentNode.style.display = "none";
	      copyAction.parentNode.style.display = "none";
		  cutAction.parentNode.style.display = "none";
		  addSymLinkAction.parentNode.style.display = "none";
	  } else {
		  if (checkUnlock) {
		    unlockAction.parentNode.parentNode.style.display = "block";
		    lockAction.parentNode.parentNode.style.display = "none";
		  } else {
		    unlockAction.parentNode.parentNode.style.display = "none";
		    lockAction.parentNode.parentNode.style.display = "block";
		  }

		      if (checkRemoveFavourite) {
		        removeFavouriteAction.parentNode.parentNode.style.display = "block";
		        addFavouriteAction.parentNode.parentNode.style.display = "none";
		      } else {
		        addFavouriteAction.parentNode.parentNode.style.display = "block";
		        removeFavouriteAction.parentNode.parentNode.style.display = "none";
		      }

		      if (!checkInTrash) {
		        restoreFromTrashAction.parentNode.parentNode.style.display = "none";
		      } else {
		        restoreFromTrashAction.parentNode.parentNode.style.display = "block";
		      }
		      if (checkInStatus) {
		        addSymLinkAction.parentNode.style.display = "none";
		      } else {
		        addSymLinkAction.parentNode.style.display = "block";
		      }
		      if (!checkMediaType) {
		        playMediaAction.parentNode.parentNode.style.display = "none";
		      } else {
		        playMediaAction.parentNode.parentNode.style.display = "block";
		      }
		      if (emptyTrashAction) {
		        if (!checkEmptyTrash) {
		          emptyTrashAction.parentNode.parentNode.style.display = "none";
		        } else {
		          emptyTrashAction.parentNode.parentNode.style.display = "block";
		        }
		      }

		      if (Self.itemsSelected.length > 1) {
		        pasteAction.parentNode.parentNode.style.display = "none";
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
      contextMenu.style.top = Y + 5 + "px";
      contextMenu.style.left = X + 5 + "px";

      contextMenu.onmouseup = Self.hideContextMenu;
      document.body.onmousedown = Self.hideContextMenu;
      document.body.onkeydown = Self.hideContextMenu;
    };

    // working with ground context menu
    ListView.prototype.showGroundContextMenu = function(event, element) {
      var event = event || window.event;
      resetArrayItemsSelected();
      if (document.getElementById(Self.contextMenuId)) {
        var contextMenu = document.getElementById(Self.contextMenuId);
        contextMenu.parentNode.removeChild(contextMenu);
      }
      //create context menu
      var actionArea = document.getElementById(Self.actionAreaId);
      var context = gj(actionArea).find("div.groundContextMenu:first")[0];
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
      var X = event.pageX || event.clientX;
      var Y = event.pageY || event.clientY;
      var portWidth = gj(window).width();
      var portHeight = gj(window).height();
      var contentMenu = gj(contextMenu).children("div.uiRightClickPopupMenu:first")[0];
      if (event.clientX + contentMenu.offsetWidth > portWidth) X -= contentMenu.offsetWidth;
      if (event.clientY + contentMenu.offsetHeight > portHeight) Y -= contentMenu.offsetHeight + 5;
      contextMenu.style.top = Y + 5 + "px";
      contextMenu.style.left = X + 5 + "px";
  
      contextMenu.onmouseup = Self.hideContextMenu;
      document.body.onmousedown = Self.hideContextMenu;
      document.body.onkeydown = Self.hideContextMenu;
    };

    // hide context menu
    ListView.prototype.hideContextMenu = function() {
      var contextMenu = document.getElementById(Self.contextMenuId);
      if (contextMenu) contextMenu.style.display = "none";
  
      //remove default context menu;
      eval(eXo.core.MouseEventManager.onMouseDownHandlers);
      eXo.core.MouseEventManager.onMouseDownHandlers = null;
    };

    ListView.prototype.postGroupAction = function(moveActionNode, ext) {
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
        Self.itemsSelected[i].style.background = "";
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
          if (arr[i] == item)  return true;
      }
      return false;
    }
    function revertResizableBlock() {

      //revert status overflow for UIResizableBlock;
      var actionArea = document.getElementById(Self.actionAreaId);
      var uiWorkingArea = gj(actionArea).parents(".UIWorkingArea:first")[0];
      var uiResizableBlock = gj(uiWorkingArea).find("div.UIResizableBlock:first")[0];
  
    }

    ListView.prototype.setHeight = function() {    
      var root = document.getElementById("UIDocumentInfo");
      var view = gj(root).find("div.uiListGrid:first")[0];
      var workingArea = document.getElementById('UIWorkingArea');
      var actionBar = document.getElementById('UIActionBar');  
      var actionBaroffsetHeight = 0;
      if(actionBar)
        actionBaroffsetHeight = actionBar.offsetHeight;
      var documentWorkspace = gj(workingArea).find("div.UIDocumentWorkspace:first")[0];
      var workingContainer = document.getElementById('UIDocumentContainer');                
      var page = gj(root).find("div.PageAvailable:first")[0];
      var title = gj(root).find("div.titleTable:first")[0];
      var sizeBarContainer = gj(workingArea).find("div.UISideBarContainer:first")[0];
      var resizeSizeBar = gj(workingArea).find("div.ResizeSideBar:first")[0];    
      var uiResizableBlock = gj(workingArea).find("div.UIResizableBlock:first")[0];
      var barContent = gj(workingArea).find("div.BarContent:first")[0];  
  
      var workingAreaHeight = workingArea.offsetHeight;
      if (sizeBarContainer)
        sizeBarContainer.style.height = workingAreaHeight + 'px';
      if (resizeSizeBar)
        resizeSizeBar.style.height = workingAreaHeight + 'px';
  
      if (documentWorkspace)                  
         documentWorkspace.style.height = workingAreaHeight - actionBaroffsetHeight + 'px';
       if (page) {
        if (parseInt(page.getAttribute('pageAvailable')) > 1) {
          if (view) {
              if (documentWorkspace) {
                view.style.height = (documentWorkspace.offsetHeight - page.offsetHeight) + 'px';
              }else {
                view.style.height = (workingContainer.offsetHeight - page.offsetHeight) + 'px';
              }
              view.style.overflow = "auto";
            }
        }
      } else {
          if (view) 
            view.style.height = workingAreaHeight - actionBaroffsetHeight + 'px';                    
      }
      var container = document.getElementById("UITreeExplorer");
      if (!container && uiResizableBlock) {
        container = gj(uiResizableBlock).find("div.SideBarContent:first")[0];
        if (container)
          eXo.ecm.UIListView.initialHeightOfContainer = container.offsetHeight;
      }  
  
      var itemArea = document.getElementById("SelectItemArea");
      if (!itemArea & container.className == "uiTreeExplorer") {
        container.style.height = (resizeSizeBar.offsetHeight - barContent.offsetHeight - 30) + "px";
      }  
    }

    ListView.prototype.hideColumn = function(obj, event) {
      var event = event || window.event;
      event.cancelBubble = true;
  
      var listGrid = gj(obj).parents(".uiListGrid:first")[0];
      var rowClazz = gj(listGrid).find("div.rowView,div.Normal");            
      if(!gj.data(Self.columnData)) {
        gj.data(Self.columnData, {}, {});
      }    
      var objResize = obj;                  
      objResize.style.display = "none";
  
      // Resize the whole column
      try {
        rowClazz.each(function(i, elem) {
          var objColumn = gj(rowClazz[i]).find("div." + objResize.className + ":first")[0];
          objColumn.style.display = "none";        
        });
      } catch(err) {}            
    }  


    ListView.prototype.resizeColumn = function(obj, event) {
      //disable text selection on browsers
      document.onmousedown = function(){return false};   
      document.onselectstart = function(){return false};
      var event = event || window.event;
      event.cancelBubble = true;
      Self.objResizingHeader = gj(obj).prevAll("div:first")[0];
      Self.objResizeValue = Self.objResizingHeader.offsetWidth;
      Self.currentMouseX = event.clientX;
      Self.listGrid = gj(Self.objResizingHeader).parents(".uiListGrid:first")[0];        
      document.onmousemove = eXo.ecm.UIListView.resizeMouseMoveListView;    
      document.onmouseup = eXo.ecm.UIListView.resizeMouseUpListView;
    }
  
    ListView.prototype.resizeMouseMoveListView = function(event) {
      var event = event || window.event;
      var objResizeClazz = eXo.ecm.UIListView.objRowClazz;
      var resizeValue = event.clientX - eXo.ecm.UIListView.currentMouseX;    
      
      // Case of resize width lower than allowable minimum.    
      if (eXo.ecm.UIListView.objResizeValue + resizeValue < 8 ) return;        
  
      var resizeDiv = document.getElementById("ResizeDiv");
      if (resizeDiv == null) {
        resizeDiv = document.createElement("div");
        resizeDiv.className = "ResizeHandle";
        resizeDiv.id = "ResizeDiv";      
        var workspace = gj(Self.objResizingHeader).parents(".UIDocumentWorkspace:first")[0];
        resizeDiv.style.height = workspace.offsetHeight + "px";
        var documentInfo = document.getElementById('UIDocumentContainer');
        var firstUIListGrid = gj(documentInfo).find("div.uiListGrid:first")[0];
        firstUIListGrid = gj(documentInfo).find("div.uiListGrid:first")[0].appendChild(resizeDiv);
      }
      var X_Resize = eXo.core.Browser.findMouseRelativeX(Self.listGrid,event);        
      eXo.core.Browser.setPositionInContainer(Self.listGrid, resizeDiv, X_Resize, 0);
    }
    
    ListView.prototype.resizeMouseUpListView = function(event) {
      //enable text selection on browsers
      document.onmousedown = function(){return true};   
      document.onselectstart = function(){return true};
      
      var event = event || window.event;
      event.cancelBubble = true;
      var columnClass = Self.objResizingHeader.className;
      columnClass = gj.trim(columnClass.replace(" column", ""));
      var resizeValue = event.clientX - eXo.ecm.UIListView.currentMouseX;
      var newWidth    = Self.objResizeValue + resizeValue + "px";
      var div2Resize  = gj(Self.listGrid).find("div." + columnClass);
      var i=0;
      for (;div2Resize[i];) {
        div2Resize[i].style.width = newWidth;
        i++;
      }
      
      if (gj(Self.columnData).data(columnClass)) {
        gj(Self.columnData).removeData(columnClass);
      }
      gj(Self.columnData).data(columnClass, newWidth);
    
      // Remove the resize div on mouseUp event
      var resizeDiv = document.getElementById("ResizeDiv");   
      if (Self.listGrid && resizeDiv) 
        Self.listGrid.removeChild(resizeDiv);
      var uiDocumentContainer = document.getElementById('UIDocumentContainer');
      var uiListGrid = gj(uiDocumentContainer).find("div.uiListGrid")[0];
      var tableBox = gj(uiDocumentContainer).find("div.uiBox")[0];
      var headerRow = gj(uiDocumentContainer).find("div.titleTable")[0];
      var colDiv = gj(headerRow).children("div");
      var totalWidth = 0; i=0;
      for (;colDiv[i];) {
        totalWidth += colDiv[i].offsetWidth;
        i++;
      }
      if (totalWidth < uiListGrid.offsetWidth-2) {
        totalWidth = uiListGrid.offsetWidth-2;
      }
      gj(tableBox).css("width", totalWidth + "px");
      //update width of uiListGrid
      eXo.ecm.ECMUtils.updateListGridWidth();  
  
      document.onmousemove = null;
      delete eXo.ecm.UIListView.currentMouseX;
      delete eXo.ecm.UIListView.objResize;    
      delete eXo.ecm.UIListView.objClumnResize;
      delete eXo.ecm.UIListView.widthRightContainer;            
    }  
    ListView.prototype.loadEffectedWidthColumn = function() {
      var objResizeClazz = eXo.ecm.UIListView.objRowClazz;
      var uiDocumentContainer = document.getElementById("UIDocumentContainer");
      var uiListGrid = gj(uiDocumentContainer).find("div.uiListGrid:first")[0];
      if(!gj.data(Self.columnData)) { 
        return;
      }
      gj.each( gj(Self.columnData).data(),function(key, value) {
       var div2Resize  = gj(uiListGrid).find("div." + key);
        for (var i=0; i< div2Resize.length; i++) {
          div2Resize[i].style.width = value;
        }
      });
      var tableBox = gj(uiDocumentContainer).find("div.uiBox")[0];
      var headerRow = gj(uiDocumentContainer).find("div.titleTable")[0];
      var colDiv = gj(headerRow).children("div");
      var totalWidth = 0; i=0;
      for (;colDiv[i];) {
        totalWidth += colDiv[i].offsetWidth;
        i++;
      }
      if (totalWidth < uiListGrid.offsetWidth-2) {
        totalWidth = uiListGrid.offsetWidth-2;
      }
      gj(tableBox).css("width", totalWidth + "px");
    }
    
    eXo.ecm.ECMUtils.documentContainer_OnResize = function(){
      var documentInfo = document.getElementById('UIDocumentInfo');
      var uiListGrid = gj(documentInfo).find("div.uiListGrid")[0];
      if (!uiListGrid) return;
      var tableBox = gj(documentInfo).find("div.uiBox")[0];
      var headerRow = gj(documentInfo).find("div.titleTable")[0];
      var colDiv = gj(headerRow).children("div");
      var totalWidth = 0; i=0;
      for (;colDiv[i];) {
        totalWidth += colDiv[i].offsetWidth;
        i++;
      }
      if (totalWidth < uiListGrid.offsetWidth-2) {
        totalWidth = uiListGrid.offsetWidth-2;
      }
      gj(tableBox).css("width", totalWidth + "px");
      eXo.ecm.ECMUtils.updateListGridWidth();
      var breadcrumb = gj('#FileViewBreadcrumb');
      if (breadcrumb) {
          breadcrumb.width(breadcrumb.parent().width());
        }        
      var actionbar= gj('#UIActionBar');
      actionbar.width(actionbar.parent().width());
      var menubar = gj('div.uiFileViewActionBar');
      if (menubar) {
          menubar.width(gj("div#UIActionBar").width()-2);
      }
      //update width of UIListGrid
      eXo.ecm.ECMUtils.updateListGridWidth();
    }
  };

  eXo.ecm.UIListView = new ListView();
  return {
    UIListView : eXo.ecm.UIListView
  };
})(gj, webuiExt, ecm_utils, wcm_utils);
