(function(gj, base, wcm_utils) {
  var SimpleView = function() {

    //eXo.ecm.UISimpleView

    var Self = this;

    SimpleView.prototype.temporaryItem = null;
    SimpleView.prototype.itemsSelected = [];
    SimpleView.prototype.allItems = [];
    SimpleView.prototype.contextMenuId = null;
    SimpleView.prototype.actionAreaId = null;
    SimpleView.prototype.enableDragDrop = null;

    SimpleView.prototype.colorSelected = "#e7f3ff";
    SimpleView.prototype.colorHover = "#eeeeee";
    SimpleView.prototype.selectedItemClass = "selectedBox";

    SimpleView.prototype.t1 = 0;
    SimpleView.prototype.t2 = 0;

    //init event
    SimpleView.prototype.initAllEvent = function(actionAreaId, enableDragAndDrop) {
      eXo.ecm.UISimpleView.enableDragAndDrop = enableDragAndDrop;
      Self.contextMenuId = "JCRContextMenu";
      Self.actionAreaId = actionAreaId;

      var actionArea = document.getElementById(actionAreaId);
      if (!actionArea) return;
      var mousedown = null;
      var keydown = null;
      Self.allItems = gj(actionArea).find("div.actionIconBox");
      Self.allItems.each(function(i, elem){
        if (!Array.prototype[i]) {
        var item = Self.allItems[i];
        item.storeIndex = i;
        if (item.getAttribute("onmousedown")) {
          mousedown = Self.allItems[i].getAttributeNode("onmousedown").value;
          item.setAttribute("mousedown", mousedown);
          item.onmousedown = null;
          item.removeAttribute("onmousedown");
        }
          if (item.getAttribute("onkeydown")) {
            keydown = Self.allItems[i].getAttributeNode("onkeydown").value;
            item.setAttribute("keydown", keydown);
            item.onmousedown = null;
            item.removeAttribute("onkeydown");
          }
  //      if (enableDragAndDrop == "true") {
          item.onmouseover = Self.mouseOverItem;
          item.onfocus = Self.mouseOverItem;
          item.onmousedown = Self.mouseDownItem;
          item.onkeydown = Self.mouseDownItem;
          item.onmouseup = Self.mouseUpItem;
          item.onmouseout = Self.mouseOutItem;
          item.onblur = Self.mouseOutItem;
  //      }
        //eXo.core.Browser.setOpacity(item, 85);
        }
      });
      actionArea.onmousedown = Self.mouseDownGround;
      actionArea.onkeydown = Self.mouseDownGround;
      actionArea.onmouseup = Self.mouseUpGround;
      actionArea.onmouseover = Self.mouseOverGround;
      actionArea.onmouseout = Self.mouseOutGround;
          actionArea.onfocus = Self.mouseOverGround;
          actionArea.onblur = Self.mouseOutGround;
      gj("div.FillOutElement").remove();
      var fillOutElement = document.createElement('div');
      fillOutElement.className = "FillOutElement";
      actionArea.appendChild(fillOutElement);
      //remove context menu
      var contextMenu = document.getElementById(Self.contextMenuId);
      if (contextMenu) contextMenu.parentNode.removeChild(contextMenu);
      //registry action drag drop in tree list

  //    var UIWorkingArea = DOM.findAncestorByClass(actionArea, "UIWorkingArea");
  //    var UITreeExplorer = DOM.findFirstDescendantByClass(UIWorkingArea, "div", "UITreeExplorer");
  //    if (UITreeExplorer) {
  //      DOM.getElementsBy(
  //          function(element) {return element.getAttribute("objectId");},
  //          "div",
  //          UITreeExplorer,
  //          function(element) {
  //            if (element.getAttribute("onmousedown") && !element.getAttribute("mousedown")) {
  //              mousedown = element.getAttributeNode("onmousedown").value;
  //              element.setAttribute("mousedown", mousedown);
  //            }
  //            if (element.getAttribute("onkeydown") && !element.getAttribute("keydown")) {
  //              keydown = element.getAttributeNode("onkeydown").value;
  //              element.setAttribute("keydown", keydown);
  //            }
  //            element.onmousedown = Self.mouseDownTree;
  //            element.onkeydown = Self.mouseDownTree;
  //            element.onmouseup = Self.mouseUpTree;
  //            element.onmouseover = Self.mouseOverTree;
  //            element.onmouseout = Self.mouseOutTree;
  //            element.onfocus = Self.mouseOverTree;
  //            element.onblur = Self.mouseOutTree;
  //          }
  //      );
  //    }
      gj(actionArea).parents(".uiWorkingArea:first").find("div.uiTreeExplorer:first").find("div[objectId]").each(
      function(index, element) {
        if (element.getAttribute("onmousedown") && !element.getAttribute("mousedown")) {
          mousedown = element.getAttributeNode("onmousedown").value;
          element.setAttribute("mousedown", mousedown);
        }
              if (element.getAttribute("onkeydown") && !element.getAttribute("keydown")) {
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
    SimpleView.prototype.mouseOverTree = function(event) {
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
      scroller.onmousemove = eXo.ecm.UISimpleView.setScroll ;

    };

    SimpleView.prototype.setScroll = function(evt){
      if(Self.enableDragDrop) {
        eXo.ecm.UISimpleView.object = this;
          var element = eXo.ecm.UISimpleView.object ;
        var pos = evt.pageY - gj(element).offset().top;
        if(element.offsetHeight - pos < 10){
          element.scrollTop = element.scrollTop + 5;
        } else if(element.scrollTop > 0 && pos < 10) {
          element.scrollTop = element.scrollTop - 5;
        }
      }
    };

    SimpleView.prototype.mouseOutTree = function(event) {
      var element = this;
      clearTimeout(element.Timeout);
    };

    SimpleView.prototype.mouseDownTree = function(evt) {
      eval("var event =''");
      event = evt || window.event;
      var element = this;
      Self.enableDragDrop = true;
      Self.srcPath = element.getAttribute("objectId");
      resetArrayItemsSelected();
      var rightClick = (event.which && event.which > 1) || (event.button && event.button == 2);
      if (rightClick) {
        eval(element.getAttribute("mousedown"));
        var _objId = element.getAttribute("workspacename")+":"+Self.srcPath;
        eXo.ecm.OpenDocumentInOffice.updateLabel(_objId,null,true);
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

    SimpleView.prototype.mouseUpTree = function(evt) {
      eval("var event = ''");
      event = evt || window.event;
      var element = this;
      revertResizableBlock();
      Self.enableDragDrop = null;
      var mobileElement = document.getElementById(Self.mobileId);
      if (mobileElement && mobileElement.move) {
        //post action
        var actionArea = document.getElementById("UIWorkingArea");
        var moveAction = gj(actionArea).find("div.JCRMoveAction:first")[0];
        var wsTarget = element.getAttribute('workspacename');
        var idTarget = element.getAttribute('objectId');

        var targetPath = decodeURIComponent(idTarget);
        var srcPath = decodeURIComponent(Self.srcPath);
        if (targetPath.indexOf(srcPath) == 0) {
          delete Self.srcPath;
          return;
        }
  //      var regex = new RegExp("^"+decodeURIComponent(idTarget) + "/");
  //      var regex1 = new RegExp("^"+decodeURIComponent(Self.srcPath) + "/");
  //      if(regex.test(decodeURIComponent(Self.srcPath) + "/")){
  //        delete Self.srcPath;
  //        return ;
  //      }
  //      if(regex1.test(decodeURIComponent(idTarget) + "/")) {
  //        delete Self.srcPath;
  //        return;
  //      }

        //Dunghm : check symlink
        if (eXo.ecm.UISimpleView.enableDragAndDrop == "true") {
          if(event.ctrlKey && event.shiftKey)
            Self.postGroupAction(moveAction.getAttribute("symlink"), "&destInfo=" + wsTarget + ":" + idTarget);
          else
            Self.postGroupAction(moveAction, "&destInfo=" + wsTarget + ":" + idTarget);
        }
      }
    };

    //event in item
    SimpleView.prototype.mouseOverItem = function(event) {
      var event = event || window.event;
      var element = this;
      if (!element.selected) {
        element.style.background = Self.colorHover;
        element.temporary = true;
        //eXo.core.Browser.setOpacity(element, 100);
      }
    };

    SimpleView.prototype.mouseOutItem = function(event) {
      var event = event || window.event;
      var element = this;
      element.temporary = false;
      if (!element.selected) {
        element.style.background = "none";
      //eXo.core.Browser.setOpacity(element, 85);
      }
    };

    SimpleView.prototype.mouseDownItem = function(evt) {
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
          className: "UIJCRExplorerPortlet",
          id: eXo.generateId('Id'),
          style: {
              position: "absolute",
              display: "none",
              background: "#dbdbdb",
              border: "1px solid gray"
          }
        });
        mobileElement.style.opacity = 64/100;
        Self.mobileId = mobileElement.getAttribute('id');
        var coverElement = newElement({
          className: "uiThumbnailsView",
          style: {clear: "left"}
        });
        for(var i in Self.itemsSelected) {
          if (Array.prototype[i]) continue;
          coverElement.appendChild( Self.itemsSelected[i].cloneNode(true));
        }
        mobileElement.appendChild(coverElement);
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

    SimpleView.prototype.dragItemsSelected = function(event) {
        var event = event || window.event;
        document.onselectstart = function(){return false;}
        if (eXo.ecm.UISimpleView.enableDragAndDrop != "true")
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

    SimpleView.prototype.dropOutActionArea = function(event) {
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

    SimpleView.prototype.clickItem = function(event, element, callback) {
      var event = event || window.event;
      resetArrayItemsSelected();
      element.selected = true;
      //Dunghm: Check Shift key
      if(event.shiftKey) element.setAttribute("isLink",true);
      else element.setAttribute("isLink",null);
      //for select use shilf key;
      Self.temporaryItem = element;
      Self.itemsSelected = new Array(element);
      gj(element).addClass(eXo.ecm.UISimpleView.selectedItemClass);
    };

    SimpleView.prototype.mouseUpItem = function(evt) {
      eval("var event = ''");
      event = evt || window.event;
      var element = this;
      Self.enableDragDrop = null;
      document.onmousemove = null;
      revertResizableBlock();

      var rightClick = (event.which && event.which > 1) || (event.button && event.button == 2);
      var leftClick = !rightClick;
      if (leftClick) {
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
          if (eXo.ecm.UISimpleView.enableDragAndDrop == "true") {
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
            gj(element).removeClass(eXo.ecm.UISimpleView.selectedItemClass);
            element.setAttribute("isLink",null);
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
            gj(Self.itemsSelected[i]).addClass(eXo.ecm.UISimpleView.selectedItemClass);
          }
        }
      }else {
        event.cancelBubble = true;

      }

      // Init feature Copy URL to Clipboard
      eXo.ecm.ECMUtils.initClipboard();
    };

    //event in ground
    SimpleView.prototype.mouseOverGround = function(event) {
      var event = event || window.event;
      var element = this;
      document.onkeydown = function(event) {
        var event = event || window.event;
        if (event.ctrlKey && event.keyCode == 65) {
          Self.selectAllItems();
          return false;
        }
      }
    };

    SimpleView.prototype.mouseOutGround = function(event) {
      document.onkeydown = function(event) {
        return true;
      }
    };

    SimpleView.prototype.selectAllItems = function() {
      if (Self.allItems) {
        resetArrayItemsSelected();
        Self.allItems.each(function(i, elem){
          if (!Array.prototype[i]) {
            var item = Self.allItems[i];
            item.selected = true;
            gj(item).addClass(eXo.ecm.UISimpleView.selectedItemClass);
            Self.itemsSelected.push(item);
          }
        });
      }
    };

    SimpleView.prototype.mouseDownGround = function(evt) {
      eval("var event = ''");
      event = evt || window.event;
      var element = this;
      element.holdMouse = true;
      Self.hideContextMenu();
      Self.temporaryItem = null;
      document.onselectstart = function(){return false};

      var rightClick = (event.which && event.which > 1) || (event.button && event.button == 2);
      if (!rightClick) {
        resetArrayItemsSelected();
        element.onmousemove = Self.mutipleSelect;
        var mask = gj(element).find("div.Mask:first")[0];
        mask.storeX = eXo.core.Browser.findMouseRelativeX(element, event);
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
        var thumbnailView = gj(element).find("div.uiThumbnailsView:first")[0];
        for( var i = 0 ; i < Self.allItems.length; ++i) {
          Self.allItems[i].posX = Math.abs(eXo.core.Browser.findPosXInContainer(Self.allItems[i], element)) - thumbnailView.scrollLeft;
          Self.allItems[i].posY = Math.abs(eXo.core.Browser.findPosYInContainer(Self.allItems[i], element)) - thumbnailView.scrollTop;
        }
      }
    };

    SimpleView.prototype.mutipleSelect = function(event) {
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

        //IV of +
        if (mask.deltaX < 0 && mask.deltaY > 0) {
          if (mask.offsetHeight > bottom) {
            mask.style.height = bottom + "px";
          }
          mask.style.top = mask.storeY + "px";
          if (mask.offsetWidth > left) {
            mask.style.width = left + "px";
            mask.style.left = 0 + "px";
          } else {
            mask.style.left = mask.X + "px";
          }
          //detect element
          //for (var i in Self.allItems) {
          Self.allItems.each(function(i, elem){
           if (!Array.prototype[i]) {
            var itemBox = Self.allItems[i];
            var posX = itemBox.posX + itemBox.offsetWidth/2;
            var posY = itemBox.posY + itemBox.offsetHeight/2;
            if (mask.X < posX && posX < mask.storeX &&
                posY < mask.Y && mask.storeY < posY) {
              itemBox.selected = true;
              //Dunghm: Check Shift key
              itemBox.setAttribute("isLink",null);
              if(event.ctrlKey && event.shiftKey) itemBox.setAttribute("isLink",true);
              gj(itemBox).addClass(eXo.ecm.UISimpleView.selectedItemClass);
            } else {
              itemBox.selected = null;
              itemBox.setAttribute("isLink",null);
              gj(itemBox).removeClass(eXo.ecm.UISimpleView.selectedItemClass);
            }
           }
          });
        //III of +
        } else if (mask.deltaX < 0 && mask.deltaY < 0) {
          if (mask.offsetHeight > top) {
            mask.style.height = top + "px";
            mask.style.top = 0 + "px";
          } else {
            mask.style.top = mask.Y + "px";
          }
          if (mask.offsetWidth > left) {
            mask.style.width = left + "px";
            mask.style.left = 0 + "px";
          } else {
            mask.style.left = mask.X + "px";
          }
          //detect element
          //for (var i in Self.allItems) {
          Self.allItems.each(function(i, elem){
           if (!Array.prototype[i]) {
            var itemBox = Self.allItems[i];
            var posX = itemBox.posX + itemBox.offsetWidth/2;
            var posY = itemBox.posY + itemBox.offsetHeight/2;
            if (mask.X < posX && posX < mask.storeX &&
                mask.Y < posY && posY < mask.storeY) {
              itemBox.selected = true;
              //Dunghm: Check Shift key
              itemBox.setAttribute("isLink",null);
              if(event.ctrlKey && event.shiftKey) itemBox.setAttribute("isLink",true);
              gj(itemBox).addClass(eXo.ecm.UISimpleView.selectedItemClass);
              //eXo.core.Browser.setOpacity(itemBox, 100);
            } else {
              itemBox.selected = null;
              itemBox.setAttribute("isLink",null);
              gj(itemBox).removeClass(eXo.ecm.UISimpleView.selectedItemClass);
              //eXo.core.Browser.setOpacity(itemBox, 85);
            }
           }
          });
        //II of +
        } else if (mask.deltaX > 0 && mask.deltaY < 0) {
          if (mask.offsetHeight > top) {
            mask.style.height = top + "px";
            mask.style.top = 0 + "px";
          } else {
            mask.style.top = mask.Y + "px";
          }
          if (mask.offsetWidth > right) {
            mask.style.width = right + "px";
          }
          mask.style.left = mask.storeX + "px";
          //detect element
          //for (var i in Self.allItems) {
          Self.allItems.each(function(i, elem){
           if (!Array.prototype[i]) {
            var itemBox = Self.allItems[i];
            var posX = itemBox.posX + itemBox.offsetWidth/2;
            var posY = itemBox.posY + itemBox.offsetHeight/2;
            if ( posX < mask.X && mask.storeX < posX&&
                mask.Y < posY && posY < mask.storeY ) {
                itemBox.selected = true;
                gj(itemBox).addClass(eXo.ecm.UISimpleView.selectedItemClass);
                //eXo.core.Browser.setOpacity(itemBox, 100);
            } else {
              itemBox.selected = null;
              gj(itemBox).removeClass(eXo.ecm.UISimpleView.selectedItemClass);
              //eXo.core.Browser.setOpacity(itemBox, 85);
            }
           }
          });
        //I of +
        } else {
          if (mask.offsetHeight > bottom) {
            mask.style.height = bottom + "px";
          }
          mask.style.top = mask.storeY + "px";
          if (mask.offsetWidth > right) {
            mask.style.width = right + "px";
          }
          mask.style.left = mask.storeX + "px";
          //detect element;
          //for (var i = 0; i < Self.allItems.length; ++ i) {
          Self.allItems.each(function(i, elem){
            var itemBox = Self.allItems[i];
            var posX = itemBox.posX + itemBox.offsetWidth/2;
            var posY = itemBox.posY + itemBox.offsetHeight/2;
            if (mask.storeX < posX && posX < mask.X &&
                mask.storeY < posY && posY < mask.Y) {
              itemBox.selected = true;
              gj(itemBox).addClass(eXo.ecm.UISimpleView.selectedItemClass);
              //eXo.core.Browser.setOpacity(itemBox, 100);
            } else {
              itemBox.selected = null;
              gj(itemBox).removeClass(eXo.ecm.UISimpleView.selectedItemClass);
              //eXo.core.Browser.setOpacity(itemBox, 85);
            }
          });
        }
      }
    };

    SimpleView.prototype.mouseUpGround = function(evt) {
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
    SimpleView.prototype.showItemContextMenu = function (event, element) {
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
      var checkEmptyTrash = false;
      var checkMediaType = false;
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
        if (Self.itemsSelected[i].getAttribute('trashHome') == "true") checkEmptyTrash = true;
        if (Self.itemsSelected[i].getAttribute('mediaType') == "true") checkMediaType = true;
        if (Self.itemsSelected[i].getAttribute('isCheckedIn') == "true") checkInStatus = true;
      }

      var lockAction = gj(contextMenu).find("i.uiIconEcmsLock:first")[0];
      var unlockAction = gj(contextMenu).find("i.uiIconEcmsUnlock:first")[0];
      var addFavouriteAction = gj(contextMenu).find("i.uiIconEcmsAddToFavourite:first")[0];
      var emptyTrashAction = gj(contextMenu).find("i.uiIconEcmsEmptyTrash:first")[0];
      var removeFavouriteAction = gj(contextMenu).find("i.uiIconEcmsRemoveFromFavourite:first")[0];
      var playMediaAction = gj(contextMenu).find("i.uiIconEcmsPlayMedia:first")[0];
      var restoreFromTrashAction = gj(contextMenu).find("i.uiIconEcmsRestoreFromTrash:first")[0];
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
        playMediaAction.parentNode.style.display = "none";
        //emptyTrashAction.parentNode.style.display = "none";
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
  		}else{
  		  restoreFromTrashAction.parentNode.parentNode.style.display = "block";
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
    SimpleView.prototype.showGroundContextMenu = function(event, element) {
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
    SimpleView.prototype.hideContextMenu = function() {
      var contextMenu = document.getElementById(Self.contextMenuId);
      if (contextMenu) contextMenu.style.display = "none";

      // Hide menu from action bar
      var listHideContainer = document.getElementById('ListHideContainer');
      if (listHideContainer) {
        var dropdown = gj(listHideContainer).parents("li.dropdown:first")[0];
        gj(dropdown).removeClass("open");
      }

      //remove default context menu;
      eval(eXo.core.MouseEventManager.onMouseDownHandlers);
      eXo.core.MouseEventManager.onMouseDownHandlers = null;
    };

    SimpleView.prototype.postGroupAction = function(moveActionNode, ext) {
      var objectId = [];
      var workspaceName = [];
      var islink = "";
      var ext = ext? ext : "";
      if(Self.itemsSelected.length) {
        for(var i in Self.itemsSelected) {
          if (Array.prototype[i]) continue;
          var currentNode = Self.itemsSelected[i];
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
    SimpleView.prototype.errorCallback = function(obj){
      var img = gj(obj.parentNode).nextAll("div:first")[0];
      img.style.display = "block";
      obj.parentNode.style.display = "none";
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
        gj(Self.itemsSelected[i]).removeClass(eXo.ecm.UISimpleView.selectedItemClass);
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

    SimpleView.prototype.setHeight = function() {
      var root = document.getElementById("UIDocumentInfo");
      var view = gj(root).find("div.uiThumbnailsView:first")[0];
      var workingArea = document.getElementById('UIWorkingArea');
      var documentWorkspace = gj(workingArea).find("div.UIDocumentWorkspace:first")[0];
      var actionBar = document.getElementById('UIActionBar');
      var actionBaroffsetHeight = 0;
      if(actionBar)
        actionBaroffsetHeight = actionBar.offsetHeight;
      var workingContainer = document.getElementById('UIDocumentContainer');
      var page = gj(root).find("div.PageAvailable:first")[0];
      var sizeBarContainer = gj(workingArea).find("div.UISideBarContainer:first")[0];
      var resizeSideBar = gj(workingArea).find("div.ResizeSideBar:first")[0];

      var workingAreaHeight = workingArea.offsetHeight;

      if (sizeBarContainer)
        sizeBarContainer.style.height = workingAreaHeight + 'px';

      if (resizeSideBar)
        resizeSideBar.style.height = workingAreaHeight + 'px';
      if (documentWorkspace)    {
         documentWorkspace.style.height = (workingAreaHeight - actionBaroffsetHeight) + 'px';
      }

      if (page) {

        if (parseInt(page.getAttribute('pageAvailable')) > 1) {

          if (view) view.style.height = workingAreaHeight - actionBaroffsetHeight - page.offsetHeight + 'px';
        }
      } else {
          if (view) view.style.height = workingAreaHeight - actionBaroffsetHeight + 'px';
      }
    };

    SimpleView.prototype.loadImageOnSuccess = function() {
       gj("#UIDocumentWorkspace .bgThumbnailImage img").one('load', function() {
           if (this.parentNode.parentNode) {
             this.parentNode.parentNode.parentNode.style.backgroundImage = 'none';
             this.parentNode.parentNode.parentNode.style.width = 'auto';
             this.parentNode.parentNode.parentNode.style.height = 'auto';
           }
           this.style.display = 'block';
           gj(this.parentNode).width(gj(this).width());
           gj(this.parentNode).height(gj(this).height());
        }).each(function() {
          if(this.complete) gj(this).load();
           gj(this).parents(".loadingProgressIcon").addClass("hidden-loading");
        });
    };
  };



  eXo.ecm.UISimpleView = new SimpleView();
  return {
    UISimpleView : eXo.ecm.UISimpleView
  };

})(gj, base, wcm_utils);



