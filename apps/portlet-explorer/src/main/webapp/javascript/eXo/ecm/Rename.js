(function(gj, wcm_utils) {
	function Rename() {
	  this.refreshAction = "";
	
	  this.restContext = eXo.ecm.WCMUtils.getRestContext();
	  this.renameConnector = "/contents/rename";
	  this.cmdGetObjectId = "/getObjectId?";
	  this.cmdRename = "/rename?";
	
	  this.contextObj = "";          // Store value of ECMContextMenu.obj
	  this.objectid = "";            // Store objectid value used to find html elements of rename node
	  this.currentNodePath = "";     // Store current node path
	};
	
	/** Store refresh action **/
	Rename.prototype.init = function(refreshAction) {
	  this.refreshAction = refreshAction;
	};
	
	/**
	Init rename form popup.
	**/
	Rename.prototype.showRenamePopup = function() {
	  // Prepare some data
	  this.contextObj = document.getElementById('ECMContextMenu').objId;
	  this.currentNodePath = this.getNodePath(false);
	
	  // Build url to call rest service to get a partern to search html markup of renamed node
	  var nodePath = encodeURIComponent(this.currentNodePath);
	  var command = this.renameConnector + this.cmdGetObjectId + "nodePath=" + nodePath;
	  var url = this.restContext + command;
	  gj.ajax({
	    url:url,
	    success: function(objectid) {
	      eXo.ecm.Rename.objectid = objectid;
	
	      // Show rename form and set value to textbox
	      var renameWindowPopup = document.getElementById('UIRenameWindowPopup');
	      gj(renameWindowPopup).remove();
	      var formMarkup = eXo.ecm.Rename.createFormMarkup();
	      var workingArea = document.getElementById('UIWorkingArea');
	      gj(workingArea).append(formMarkup);
	      renameWindowPopup = document.getElementById('UIRenameWindowPopup');
	
	      document.getElementById('renameField').value = eXo.ecm.Rename.getNodeTitle(false);
	
	      // Show rename to properiate position
	      var popupOffset = eXo.ecm.Rename.getPopupPosition();
	      gj(renameWindowPopup).offset(popupOffset);
	
	      // Focus and select textbox
	      gj('#renameField').select();
	
	      // Bind events
	      eXo.ecm.Rename.bindEvent();
	    }
	  });
	};
	
	/*
	Create rename form markup.
	*/
	Rename.prototype.createFormMarkup = function() {
	  var formMarkup = '';
	  formMarkup += '<div class="UIPopupWindow uiPopup uiRenameForm" id="UIRenameWindowPopup" style="width: auto; visibility: visible; z-index: 15;">';
	  formMarkup += '    <div class="popupContent">';
	  formMarkup += '       <input name="renameField" type="text" id="renameField" value ="">';
	  formMarkup += '         <button type="button" id = "renameLink" class="btn btn-primary" onclick="eXo.ecm.Rename.clickSave();">' + eXo.ecm.WCMUtils.getBundle('RenameConnector.label.rename', eXo.env.portal.language) + '</button>';
	  formMarkup += '         <button type="button" class="btn" onclick="eXo.ecm.Rename.closePopup();eXo.ecm.UIFileView.clearCheckboxes();">' + eXo.ecm.WCMUtils.getBundle('RenameConnector.label.cancel', eXo.env.portal.language) + '</button>';
	  formMarkup += '    </div>';
	  formMarkup += '</div>';

	  return formMarkup;
	};
	
	/*
	Bind events to rename form.
	*/
	Rename.prototype.bindEvent = function() {
	  renameWindowPopup = document.getElementById('UIRenameWindowPopup');
	
	  // Bind event enter key
	  gj('#renameField').keypress(function(e) {
	    if(e.keyCode == 13) {
	      gj('#renameLink').click();
	    }
	  });
	
	  // Bind Esc key
	  gj(renameWindowPopup).keyup(function(e) {
	    if(e.keyCode == 27) {
	      gj(renameWindowPopup).hide();
	    }
	  });
	
	  // Bind scoll event
	  var treeExplorer = document.getElementById('UITreeExplorer');
	  if (treeExplorer) {
			var treeExplorerOffset = gj(treeExplorer).offset();
			var treeExplorerHeight = gj(treeExplorer).height();
			var treeExplorerBottom = treeExplorerOffset.top + treeExplorerHeight;
			gj('#UITreeExplorer').scroll(function () {
			  var rightClickedElement = eXo.ecm.Rename.getRightClickedElement();
			  if (!rightClickedElement) return;
			  var rightClickedElementHeight = gj(rightClickedElement).height();
			  var popupOffset = eXo.ecm.Rename.getPopupPosition();
			  var rightClickedElementBottom = popupOffset.top + rightClickedElementHeight;
	
			  if ((popupOffset.top < treeExplorerOffset.top) || (treeExplorerBottom < rightClickedElementBottom)) {
			    gj(renameWindowPopup).hide();
			    gj('#UITreeExplorer').unbind('scroll');
			  } else {
			    gj(renameWindowPopup).offset(popupOffset);
			  }
			});
	  }
	  
	  // Dismiss rename form when clicking outside
	  gj(document).mouseup(function (e) {
	    var container = gj(renameWindowPopup);
	    if (!container.is(e.target) && container.has(e.target).length == 0 && gj(container).is(":visible")) {
	      container.hide();
	      eXo.ecm.UIFileView.clearCheckboxes();
	      gj(this).unbind('mouseup');
	    }
	  });
	};
	
	/**
	Save Button onClick event Handler.
	**/
	Rename.prototype.clickSave = function() {
	  // Close rename popup
	  this.closePopup();
	
	  // Backup old title and get new title
	  var oldTitle = this.getNodeTitle(true);
	  var newTitle = document.getElementById('renameField').value;
	
	  // Check if no change in rename textbox, stop renaming
	  if (newTitle == this.getNodeTitle(false)) return;
	
	  // Update status to in progress renaming
	  this.setNodeTitle(eXo.ecm.WCMUtils.getBundle('RenameConnector.msg.renaming', eXo.env.portal.language), true);
	  // set invisible file extention part in file view
	  if (gj("#UIDocumentInfo .uiFileView").length > 0) {
	    var elements = this.getElementsOfRenamedNode();
	    var fileExtension = gj(elements[0]).find("span.fileExtension:first")[0];
	    gj(fileExtension).hide();
	    
	  }
	
	  // Build url to request rest service to execute rename on server
	  var oldPath = this.getNodePath(true);
	  newTitle = encodeURIComponent(newTitle);
	  var command = this.renameConnector + this.cmdRename + "oldPath=" + encodeURIComponent(oldPath) + "&newTitle=" + newTitle;
	  var url = this.restContext + command;
	  gj.ajax({
	    url:url,
	    dataType: "text"
	  }).done(function(uuid) {
	    // Refesh explorer after rename succeeds
	    eXo.ecm.Rename.refreshExplorer(uuid);
	  }).fail(function() {
	    // Back to old title after rename fails
	    eXo.ecm.Rename.setNodeTitle(oldTitle, false);
	  });
	};
	
	/*
	Close Rename Popup.
	*/
	Rename.prototype.closePopup = function() {
	  var renameWindowPopup = document.getElementById('UIRenameWindowPopup');
	  if (renameWindowPopup) {
	    gj(renameWindowPopup).hide();
	  }
	};
	
	/*
	Set text value for html elements of renamed nodes.
	@param nodeLabelValue: text value
	@param isEmphasized: text is emphasized
	*/
	Rename.prototype.setNodeTitle = function(nodeLabelValue, isEmphasized) {
	  var elements = this.getTextElementsOfRenamedNode();
	  for (var i = 0; i < elements.length; i++) {
	    gj(elements[i]).text(nodeLabelValue);
	    if (isEmphasized) {
	      gj(elements[i]).css({'font-style' : 'italic'});
	    } else {
	      gj(elements[i]).css({'font-weight' : 'normal', 'font-style' : 'normal'});
	    }
	  }
	};
	
	/*
	Update lastest status of Content Explorer for renamed node.
	@param uuid: uuid of renamed node
	*/
	Rename.prototype.refreshExplorer = function(uuid) {
	  // Encode first
	  var oldPath = encodeURIComponent(this.currentNodePath);
	
	  var url = decodeURIComponent(this.refreshAction);
	  url = url.substr(0, url.length - 2) + "&oldPath=" + oldPath + "&uuid=" + uuid + "')";
	  eval(url);
	};
	
	/*
	Check if current selected node is nt:file.
	*/
	Rename.prototype.isFile = function() {
	  var elements = this.getElementsOfRenamedNode();
	  var isFile = (gj(elements[0]).attr('isFile').toLowerCase() == 'true');
	  return isFile;
	};
	
	/*
	Call RenameConnector rest service to get objectid of renamed node.
	*/
	Rename.prototype.getObjectId = function() {
	  var nodePath = encodeURI(this.currentNodePath);
	
	  var command = this.renameConnector + this.cmdGetObjectId + "nodePath=" + nodePath;
	  var url = this.restContext + command;
	  var objectId = ajaxAsyncGetRequest(url, false);
	  return objectId;
	};
	
	/*
	Get current exo:title of renamed node.
	@param withFileExt: option if including extension of file
	*/
	Rename.prototype.getNodeTitle = function(withFileExt) {
	  var elements = this.getTextElementsOfRenamedNode();
	  var label = gj.trim(gj(elements[0]).text());
	
	  // Check if include extention of file
	  if (label.indexOf('.') != -1 && this.isFile() && !withFileExt) {
	    label = label.substring(0, label.lastIndexOf('.'));
	  }
	
	  return label;
	};
	
	/*
	Get renamed html text elements. Including both sides(tree explorer and right panel)
	*/
	Rename.prototype.getTextElementsOfRenamedNode = function() {
	  var textElements = new Array();
	  var elements = this.getElementsOfRenamedNode();
	  for (var i=0; i< elements.length; i++) {
	    textElements.push(gj(elements[i]).find("span.nodeName:first")[0]);
	  }
	  return textElements;
	};
	
	/*
	Get renamed html elements. Including both sides(tree explorer and right panel)
	*/
	Rename.prototype.getElementsOfRenamedNode = function() {
	  var elements = new Array();
	  var workingArea = document.getElementById('UIWorkingArea');
	  gj(workingArea).find("div[objectid='" + this.objectid + "']").each(function(i) {
	    elements.push(this);
	  });
	  return elements;
	};
	
	/*
	Get node path of renamed node.
	@param withWorkspace: specify if path result including workspace
	*/
	Rename.prototype.getNodePath = function(withWorkspace) {
	  var nodePath = this.contextObj.replace(/'/g, "\\'");
	  if (!withWorkspace) {
	    nodePath = nodePath.substring(nodePath.lastIndexOf(":") + 1);
	  }
	  return nodePath;
	};
	
	/*
	Get position to show rename form popup.
	*/
	Rename.prototype.getPopupPosition = function() {
	  // Get offset of right clicked element.
	  var clickedElmt =  this.getRightClickedElement();
	  var popupOffset = gj(clickedElmt).offset();
	  if (!popupOffset) {
		  popupOffset = gj(eXo.ecm.UIFileView.itemsSelected[0]).offset();
	  }
	  popupOffset.top -= 15;
	  return popupOffset;
	};
	
	/*
	Get element which is right clicked on to rename.
	*/
	Rename.prototype.getRightClickedElement = function() {
	  // Get offset of resize bar.
	  var workingArea = document.getElementById('UIWorkingArea');
	  var resizeSideBar = gj(workingArea).find("div.resizeBar:first")[0];
	  var resizeSideBarOffset = gj(resizeSideBar).offset();
	
	  // Get offset of context menu.
	  var contextMenu = document.getElementById('ECMContextMenu');
	  var contextOffset = gj(contextMenu).show().offset();
	  gj(contextMenu).hide();
	
	  // Get right clicked element.
	  var clickedElmt;
	  var elements = this.getTextElementsOfRenamedNode();
	  if (elements.length == 1) return elements[0];
	  for (var i = 0; i < elements.length; i++) {
	    var elmtOffset = gj(elements[i]).offset();
	    if ((elmtOffset.left < resizeSideBarOffset.left && contextOffset.left < resizeSideBarOffset.left)
	      ||(elmtOffset.left > resizeSideBarOffset.left && contextOffset.left > resizeSideBarOffset.left)) {
	        clickedElmt = elements[i];
	      break;
	    }
	  }
	
	  return clickedElmt;
	};
	
	eXo.ecm.Rename = new Rename();
	return {
		Rename : eXo.ecm.Rename
	};
})(gj, wcm_utils);
