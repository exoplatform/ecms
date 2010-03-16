function getElementByClassName(sClassName) {
	var aElements = document.getElementsByTagName('*');
	var iLength = aElements.length;
	var aResults = [];
	for (var i = 0; i < iLength; i++) {
		if (aElements[i].className.indexOf(sClassName) >= 0) {
			aResults.push(aElements[i]);
		}
	}
	return aResults;
}

function getElementsByClassPath(root, path) {
	var root = document.getElementById(root) || root;
	if (!root.nodeType) return;
	var aLocation = path.split("/");
	var nMap = aLocation.length;
	var aElement = root.getElementsByTagName("*");
	var nLength = aElement.length;
	var oItem;
	var aResult = [];
	for (var o = 0 ; o < nLength; ++ o) {
		oItem = aElement[o];
		if (hasClass(oItem, aLocation[nMap-1])) {
			for (var i = nMap - 2; i >= 0 ; --i) {
				oItem = getParent(oItem, aLocation[i]);
			}
			if (oItem) 	aResult.push(aElement[o]);
		}
	}
	if (aResult.length) return aResult;
	return null;
	
	// private function
	function hasClass(element, className) {
		return (new RegExp("(^|\\s+)" + className + "(\\s+|$)").test(element.className)) ;
	}
	function getParent(element, className) {
		if (!element) return null;
		var parent = element.parentNode;
		while (parent && parent.nodeName != "HTML") {
			if (hasClass(parent, className)) return parent;
			parent =  parent.parentNode;
		}
		return null;
	}
}

	function showSetting() {
		var oSetting = K('explorer').select({where: "className == 'Setting'"})[0];
		if (oSetting.style.display != 'block') {
			oSetting.style.display = 'block';
		} else { 
			oSetting.style.display = 'none';
		}
		elementResize();
	}

	function openTree(obj) {
		var oNodeOpen = obj.parentNode;
		var oNodeGroup = K(oNodeOpen).select({where: "className == 'NodeGroup'"})[0];
		if (oNodeGroup.style.display != "block") {
			oNodeGroup.style.display = "block";
			obj.className = "Expand";
		} else {
			oNodeGroup.style.display = "none";
			obj.className = "Collapse";
		}
	}

	function dateFormat(sFullDate) {
		var sYear = sFullDate.substring(0, 4);
		var sMonth = sFullDate.substring(5, 7);
		var sDay = sFullDate.substring(8, 10);
		var sHour = sFullDate.substring(sFullDate.indexOf('T') + 1, sFullDate.indexOf('T') + 3);
		var sMinute = sFullDate.substring(sFullDate.indexOf('T') + 4, sFullDate.indexOf('T') + 6);
		var sSecond = sFullDate.substring(sFullDate.indexOf('T') + 7, sFullDate.indexOf('T') + 9);
		var sMillisecond = sFullDate.substring(sFullDate.indexOf('T') + 10, sFullDate.indexOf('T') + 13);
		
		var date = new Date();
		date.setDate(sDay);
		date.setMonth(sMonth);
		date.setYear(sYear);
		date.setHours(sHour);
		date.setMinutes(sMinute);
		date.setSeconds(sSecond);
		date.setMilliseconds(sMillisecond);
		
		return (date);
	} 

	function treeInit(sXML) {
	 	try {
	 		var oError = eXp.getSingleNode(sXML, "Message");
			if (oError) {
				var sErrorText = eXp.getNodeValue(oError, "text");
				alert(sErrorText);
				return;
			}
	 	} catch(e) {}
	
		var oExplorer = K('explorer');
		var oStatus = K('statusBar');
		var oStatusFolder = getElementsByClassPath(oStatus, 'Folder')[0];
		var oTree = getElementsByClassPath(oExplorer, 'Navigation/Tree')[0];
		var sHTML = '';
		var oFolders = eXp.getNodes(sXML, "Folder");
		if (oFolders && oFolders.length) {
			var iLength = oFolders.length;
			for (var i = 0 ; i < iLength; i++) {
				var sName = eXp.getNodeValue(oFolders[i], "name");
				var sType = eXp.getNodeValue(oFolders[i], "folderType");
					sType = sType.replace(":", "_") + "16x16Icon";
				var sTreeNode = K('hideContainer').select({where: "className == 'TreeNode'"})[0].innerHTML;
				if (i == iLength - 1) sTreeNode = sTreeNode.replace(/\${sClass}/g, 'LastNode');
				else sTreeNode = sTreeNode.replace(/\${sClass}/g, '');
				sTreeNode = sTreeNode.replace(/\${sName}/g, sName);
				sTreeNode = sTreeNode.replace(/\${sType}/g, sType);
				sTreeNode = sTreeNode.replace(/\${sCurrentFolder}/g, '/');
				sHTML += sTreeNode;
			}
			oStatusFolder.innerHTML = iLength + ' folder(s)';
		} else {
			oStatusFolder.innerHTML = '0 folder(s)';
		}
		var oNodeGroup = getElementsByClassPath(oExplorer, 'Navigation/Tree/NodeGroup')[0];
		oNodeGroup.style.display = "block";
		oNodeGroup.innerHTML = sHTML;
		
		if (eXp.resourceType == 'Gadget') {
			var currentFolder = eXp.getNodes(sXML, "CurrentFolder")[0];
			var nodeValue = eXp.getNodeValue(currentFolder, "name");
			var currentNode = '';
			var nodeList = oNodeGroup.getElementsByTagName("div");
			for(var i = 0; i < nodeList.length; i++)	 {
				var nodeAttribute = nodeList[i].getAttribute("name");
				if(nodeAttribute && nodeAttribute.indexOf(nodeValue) > 0) {
					currentNode = nodeList[i];
				}
			}
			getDir(currentNode);
		}
	}
	
	function sort(sCondition) {
		var oExplorer = K('explorer');
		var oDocument = getElementsByClassPath(oExplorer, 'Workspace/DisplayArea')[0];
		var oThumbnailView;
		if (location.search.indexOf('Thumbnail') >= 0)
			oThumbnailView = true;
		else 
			oThumbnailView = document.getElementsByName("View")[0].checked;
		var oHide = K('hideContainer');
		
		oDocument.innerHTML = '';
		var sHTML = '';
		if (!eXp.store.data.Select) return;
		var aResult;
		if (sCondition)
			aResult	= eXp.store.data.Select({orderBy: sCondition});
		else 
			aResult	= eXp.store.data.Select();
		var iLength = aResult.length;
		for (var i = 0; i < iLength; i++) {
			var HideTreeItem;
			oThumbnailView ? HideTreeItem = getElementsByClassPath(oHide, 'ThumbnailViewItem')[0].innerHTML : HideTreeItem = getElementsByClassPath(oHide, 'ListViewItem')[0].innerHTML;
			HideTreeItem = HideTreeItem.replace(/\${sName}/g, aResult[i].name);
			HideTreeItem = HideTreeItem.replace(/\${sType}/g, aResult[i].type);
			sThumbnail = aResult[i].thumbnail;
			if (!sThumbnail.length) sThumbnail = 'images/no-image.jpg';
			HideTreeItem = HideTreeItem.replace(/\$%7BsThumbnail%7D/g, sThumbnail);	// for FF
			HideTreeItem = HideTreeItem.replace(/\${sThumbnail}/g, sThumbnail);			// for IE
			HideTreeItem = HideTreeItem.replace(/\${sMetadata}/g, aResult[i].metadata.replace(/"/g, '${quote}'));
			HideTreeItem = HideTreeItem.replace(/\${sThumbnailWidth}/g, FCKConfig.thumbnailWidth);
			HideTreeItem = HideTreeItem.replace(/\${sThumbnailHeight}/g, FCKConfig.thumbnailHeight);
			HideTreeItem = HideTreeItem.replace(/\$%7BsURL%7D/g, aResult[i].url);		// for FF
			HideTreeItem = HideTreeItem.replace(/\${sURL}/g, aResult[i].url);				// for IE
			HideTreeItem = HideTreeItem.replace(/\${sDateCreated}/g, aResult[i].date.getDate() + '/' + aResult[i].date.getMonth() + '/' + aResult[i].date.getFullYear() + ' ' + aResult[i].date.getHours() + ':' + aResult[i].date.getMinutes());
			HideTreeItem = HideTreeItem.replace(/\${sURL}/g, aResult[i].url);
			HideTreeItem = HideTreeItem.replace(/\${sSize}/g,  aResult[i].size);
			sHTML += '<input type="hidden" id="checkgen">';
			sHTML += HideTreeItem;
		}
		oDocument.innerHTML += sHTML;
	}
	
	function removeMask() {
		K("PopupContainer").innerHTML = "";
		K("Mask").hide();
	}
	
	function showContextMenu(selection, event, element) {
		if (eXp.disableCreatingFolder && selection == "AddNewDocument") return;
		var oContextMenu = K('contextMenu');
		var oSelection = getElementsByClassPath(oContextMenu, selection)[0];
		oSelection.style.left = K.get.X(event) + "px";
		oSelection.style.top = K.get.Y(event) + "px";
		oSelection.style.display = "block";
		var oActions =  K.select({from: oSelection, where: "className like '%IconItem%'"});
		oSelection.setAttribute("name", element.getAttribute("name"));
		eXp.store.temporaryNode = element;
		return false;
	}

	function hideContextMenu() {
		var aObjects = getElementByClassName('ContextMenu');
		iLength = aObjects.length;
		for (var i = 0; i < iLength; i++) {
			aObjects[i].style.display = 'none';
		}
	}
	
	function showAddForm() {
		var popupContainer = K("PopupContainer").show();
		var formContainer = K("hideContainer").select({where: "className == 'AddFormContainer'"})[0];
		var currenForder = K("contextMenu").select({where: "className like '%AddNewDocument'"})[0].getAttribute("name");
		popupContainer.innerHTML = formContainer.innerHTML.replace(/\${idShort}/g, currenForder);
		K("Mask").add({
			event: "click",
			listener: removeMask
		}).show();
	}
		
	function doAddForm() {
		var popupContainer = K("PopupContainer");
		var sFolderName = popupContainer.select({where: "tagName == 'INPUT' && name == 'fileName'"})[0].value;
		if (isInvalidName(sFolderName)) {
			alert('Invalid folder name!');
			return;
		}
		var sCurrentFolder = popupContainer.select({where: "tagName == 'INPUT' && name == 'hidden'"})[0].value;
		var connector = eXoPlugin.hostName + eXp.connector + 'createFolder';
		var param = eXp.buildParam(
					"type=" + eXp.resourceType,
					"currentFolder=" + sCurrentFolder,
					"newFolderName=" + sFolderName,
					"currentPortal=" + eXoPlugin.portalName,
					buildXParam()
				);
		
		eXp.sendRequest(
			connector,
			param,
			function(sXML) {
				var oError = eXp.getSingleNode(sXML, "Message");
				var sErrorNumber = parseInt(eXp.getNodeValue(oError, "number"));
				var sErrorText = eXp.getNodeValue(oError, "text");
				if (sErrorNumber - 100) {
					alert(sErrorText);
					getDir(eXp.store.currentNode);
				} else {
					alert(sErrorText);
					eXp.store.currentNode = eXp.store.temporaryNode;
					getDir(eXp.store.currentNode);
				}
				removeMask();
			}
		);
	}
	
	function showUploadForm() {
		var uploadContainer = K("UploadContainer");
		var popupContainer = K("PopupContainer");
		popupContainer.style.display = "block";
		if (eXp.store.currentNode && eXp.store.currentNode.getAttribute) {
			var sPath = eXp.store.currentNode.getAttribute("name");
		} else var sPath = "/";
		popupContainer.innerHTML = uploadContainer.innerHTML.replace(/\${idShort}/, sPath);
		var iFrame = popupContainer.select({where: "className == 'iFrameUpload'"})[0];
		var iContent = K("iContentUpLoad").innerHTML;
			iContent = iContent.replace(/&amp;/g, "&");
			iContent = iContent.replace(/&lt;/g, "<");
			iContent = iContent.replace(/&gt;/g, ">");
			iContent = iContent.replace(/&quot;/g, "\"");
		
		with (iFrame.contentWindow) {
			document.open();
			document.write(iContent);
			document.close();
		}
		K.set.event({
			element: K("Mask").show(),
			event: "click",
			listener: removeMask
		});
	}
	
	function showAlert() {
		removeMask();
		var popupContainer = K("PopupContainer").show();
		var alertContainer = K("hideContainer").select({where: "className == 'AlertContainer'"})[0];
		popupContainer.innerHTML = alertContainer.innerHTML;
		K("Mask").add({
			event: "click",
			listener: removeMask
		}).show();
	}
	
	function uploadFile() {
		var popupContainer = K("PopupContainer");
		var iFrameUpload = popupContainer.select({where: "className == 'iFrameUpload'"})[0];
		var formUpload = iFrameUpload.contentWindow.document.getElementsByTagName("form")[0];
		if (!formUpload.file.value == '') {
			uploadFile.id =  eXp.getID();
			var param = eXp.buildParam("uploadId=" + uploadFile.id, "currentFolder=" + eXp.store.currentFolder, "currentPortal=" + eXoPlugin.portalName, buildXParam());
			if (formUpload) {				
				formUpload.action = eXp.connector + eXp.command.uploadFile + "?" + param;
				formUpload.submit();
			}
			uploadFile.stopUpload = false;
			var uploadField = popupContainer.select({where: "className == 'UploadField'"})[0];
			uploadField.style.display = "none";
			var UploadInfo = popupContainer.select({where: "className like 'UploadInfo%'"})[0];
			UploadInfo.style.display = "";
			var CancelAction = popupContainer.select({where: "className == 'CancelAction'"})[0];
			CancelAction.style.display = "none";
			K.set.timeout({
				until: function() {return uploadFile.stopUpload},
				method: function() {
					var connector = eXp.connector + eXp.command.controlUpload;
					var param = eXp.buildParam("action=progress", "uploadId=" + uploadFile.id, "currentFolder=" + eXp.store.currentFolder, "currentPortal=" + eXoPlugin.portalName, buildXParam());
					K.request({
						address: connector,
						data: param,
						method: "GET",
						onSuccess: function() {
							var iXML = this.responseXML;
							if (!iXML) return;
							var oProgress = eXp.getSingleNode(iXML, "UploadProgress");
							var nPercent = eXp.getNodeValue(oProgress, "percent");
							var popupContainer = K("PopupContainer");
							var uploadInfo = popupContainer.select({where: "className like 'UploadInfo%'"})[0];
							var graphProgress = popupContainer.select({where: "className == 'GraphProgress'"})[0];
							var numberProgress = popupContainer.select({where: "className == 'NumberProgress'"})[0];
							if (nPercent * 1 < 100) {
								graphProgress.style.width = nPercent + "%";
								numberProgress.innerHTML = nPercent + "%";
								uploadFile.stopUpload = false;
								uploadInfo.className = "UploadInfo Abort";
							} else {
								graphProgress.style.width = 100 + "%";
								numberProgress.innerHTML = 100 + "%";
								uploadFile.stopUpload = true;
								uploadInfo.className = "UploadInfo Delete";
								var uploadAction = popupContainer.select({where: "className == 'UploadAction'"})[0];
								uploadAction.style.display = "";
							}
						},
						onFailure: function() {
							uploadFile.stopUpload = true;
							alert("upload is failure.");
							showUploadForm();
						}
					});
				}
			});
		} else {
			showAlert();
		}
	}
	
	uploadFile.Abort = function() {
		var connector = eXp.connector + eXp.command.controlUpload;
		var param = eXp.buildParam("action=abort", "uploadId=" + uploadFile.id, "currentFolder=" + eXp.store.currentFolder, "currentPortal=" + eXoPlugin.portalName, buildXParam());
		eXp.sendRequest(connector, param);
		uploadFile.stopUpload = true;
		removeMask();
		showUploadForm();
	};
	
	uploadFile.Cancel = function() {
		var connector = eXp.connector + eXp.command.controlUpload;
		var param = eXp.buildParam("action=delete", "uploadId=" + uploadFile.id, "currentFolder=" + eXp.store.currentFolder, "currentPortal=" + eXoPlugin.portalName, buildXParam());
		eXp.sendRequest(connector, param);
		removeMask();
	};

	uploadFile.Delete = function() {
		var connector = eXp.connector + eXp.command.controlUpload;
		var param = eXp.buildParam("action=delete", "uploadId=" + uploadFile.id, "currentFolder=" + eXp.store.currentFolder, "currentPortal=" + eXoPlugin.portalName, buildXParam());
		eXp.sendRequest(connector, param);
		removeMask();
		showUploadForm();
	};

	uploadFile.Save = function() {
		var popupContainer = K("PopupContainer");
		var nodeName = K("PopupContainer").select({where: "nodeName == 'INPUT' && name == 'fileName'"})[0];
		var iFrameUpload = popupContainer.select({where: "className == 'iFrameUpload'"})[0];
		var formUpload = iFrameUpload.contentWindow.document.getElementsByTagName("form")[0];
		if (isInvalidName(formUpload.file.value) && isInvalidName(nodeName.value)) {
			alert('Invalid file name!');
			return;
		}
		
		var connector = eXp.connector + eXp.command.controlUpload;
		var param = eXp.buildParam("action=save", "uploadId=" + uploadFile.id, "fileName=" + nodeName.value, "currentFolder=" + eXp.store.currentFolder, "currentPortal=" + eXoPlugin.portalName, buildXParam());
		eXp.sendRequest(
			connector,
			param,
			function(sXML) {
				var oError = eXp.getSingleNode(sXML, "Message");
				var sErrorNumber = parseInt(eXp.getNodeValue(oError, "number"));
				var sErrorText = eXp.getNodeValue(oError, "text");
				if (sErrorNumber - 200) {
					alert(sErrorText);
					getDir(eXp.store.currentNode);
				} else {
					alert(sErrorText);
					eXp.store.currentNode = eXp.store.temporaryNode;
					getDir(eXp.store.currentNode);
				}
				removeMask();
			}
		);
		removeMask();
		setTimeout(function(){getDir(eXp.store.currentNode)}, 1000);
	};

function isInvalidName(name) {
	if (name && name.match('[/,[,*,\',",|]') == null && name.indexOf(']') < 0) return false;
	return true;
}