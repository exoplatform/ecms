function UploadForm() {
}

UploadForm.prototype.showUploadForm = function() {
	var uploadContainer = document.getElementById("UploadContainer"); 
	if(eXp.store.currentNode && eXp.store.currentNode.getAttribute('name')) {
		var sPath = eXp.store.currentNode.getAttribute("name");
	} else {
		sPath = "/";
	}
	var popupContainer = document.getElementById("PopupContainer");
	popupContainer.style.display = 'block';
	popupContainer.style.width = "100%";
	popupContainer.style.height = "100%";
	popupContainer.style.position = "absolute";
	popupContainer.style.top = "0px";
	popupContainer.innerHTML = uploadContainer.innerHTML;
	var iFrame = eXo.core.DOMUtil.findFirstDescendantByClass(popupContainer, "iframe", "iFrameUpload");
	var iContent = document.getElementById("iContentUpLoad").innerHTML;
	iContent = iContent.replace(/&amp;/g, "&");
	iContent = iContent.replace(/&lt;/g, "<");
	iContent = iContent.replace(/&gt;/g, ">");
	iContent = iContent.replace(/&quot;/g, "\"");
	with (iFrame.contentWindow) {
			document.open();
			document.write(iContent);
			document.close();
	}
	eXoWCM.UploadForm.showMask(popupContainer, true);
	var uploadForm = eXo.core.DOMUtil.findFirstDescendantByClass(popupContainer, "div", "UploadForm");
	var maskLayer = eXo.core.DOMUtil.findFirstDescendantByClass(popupContainer, "div", "MaskLayer");
	maskLayer.style.zIndex = uploadForm.style.zIndex++;
	uploadForm.style.position = 'absolute';
	var widthUploadForm = (eXo.core.Browser.getBrowserWidth() - uploadForm.offsetWidth)/2;
	var heightUploadForm = (eXo.core.Browser.getBrowserHeight() - uploadForm.offsetHeight)/2;
	uploadForm.style.left = widthUploadForm + "px";
	uploadForm.style.top = heightUploadForm + "px";
	
	var tblActionContainer =  eXo.core.DOMUtil.findFirstDescendantByClass(uploadForm, "table", "ActionContainer");
	var trFolder =  eXo.core.DOMUtil.findFirstDescendantByClass(tblActionContainer, "tr", "PathFolder");
	var spanFolder = eXo.core.DOMUtil.findDescendantsByTagName(trFolder, "span")[0];
	spanFolder.innerHTML += ":"+ decodeURIComponent(decodeURIComponent(eXp.store.currentFolder));
};

UploadForm.prototype.showAlert = function() {
	eXoWCM.UploadForm.removeMask();
	var popupContainer = document.getElementById("PopupContainer");
	popupContainer.style.display = 'block';
	popupContainer.style.width = "100%";
	popupContainer.style.height = "100%";
	popupContainer.style.position = "absolute";
	popupContainer.style.top = "0px";
	var hideContainer = document.getElementById("hideContainer");
	var alertContainer = eXo.core.DOMUtil.findFirstDescendantByClass(hideContainer, "div", "AlertContainer");
	popupContainer.innerHTML = alertContainer.innerHTML;
	eXoWCM.UploadForm.showMask(popupContainer, true);
	var alertForm = eXo.core.DOMUtil.findFirstDescendantByClass(popupContainer, "div", "AlertForm");
	var maskLayer = eXo.core.DOMUtil.findFirstDescendantByClass(popupContainer, "div", "MaskLayer");
	maskLayer.style.zIndex = alertForm.style.zIndex++;
	alertForm.style.position = 'absolute';
	var widthAlertForm = (eXo.core.Browser.getBrowserWidth() - alertForm.offsetWidth)/2;
	var heightAlertForm = (eXo.core.Browser.getBrowserHeight() - alertForm.offsetHeight)/2;
	alertForm.style.left = widthAlertForm + "px";
	alertForm.style.top = heightAlertForm + "px";
}

UploadForm.prototype.showMask = function(popup, isShowPopup) {
	var maskId = popup.id + "MaskLayer" ;
	var mask = document.getElementById(maskId) ;
	if(isShowPopup) {
		if (mask == null) eXo.core.UIMaskLayer.createMask(popup.id, mask, 20) ;			
	} else {
		if(mask != null)	eXo.core.UIMaskLayer.removeMask(mask) ;			
	}
};

UploadForm.prototype.removeMask = function() {
	var popupContainer = document.getElementById("PopupContainer");
	popupContainer.innerHTML = "";
	popupContainer.style.display = 'none';
	eXo.core.UIMaskLayer.removeMask(document.getElementById("MaskLayer")) ;
};

// used to instead of build string xmlhttp reuqest
UploadForm.prototype.getStringParam = function() {
	var repositoryName = FCKConfig.repositoryName;
	var workspaceName  = FCKConfig.workspaceName;
	var jcrPath = FCKConfig.jcrPath;
	var driverName = eXp.store.driverName;
	var strParam = '';
	if (repositoryName !== undefined) strParam += "repositoryName="+ repositoryName;
	if (workspaceName !== undefined)  strParam += "&workspaceName=" + workspaceName;
	if(driverName) strParam += "&driverName="+ driverName;
	strParam +="&currentFolder="+eXp.store.currentFolder+"&currentPortal="+eXoPlugin.portalName+"&jcrPath="+jcrPath;
	return strParam;
};

UploadForm.prototype.uploadFile = function() {
	eXoWCM.UploadForm.uploadId = eXp.getID();
	var popupContainer = document.getElementById("PopupContainer");
	var iFrameUpload = eXo.core.DOMUtil.findFirstDescendantByClass(popupContainer, "iframe", "iFrameUpload");
	var formUpload = iFrameUpload.contentWindow.document.getElementsByTagName("form")[0];
	if(!formUpload.file.value == '') {
		var repositoryName = FCKConfig.repositoryName;
		var workspaceName  = FCKConfig.workspaceName;
		var jcrPath = FCKConfig.jcrPath;
		var driverName = eXp.store.driverName;
		var strParam = '';
		if (repositoryName !== undefined) strParam += "repositoryName="+ repositoryName;
		if (workspaceName !== undefined)  strParam += "&workspaceName=" + workspaceName;
		if(driverName) strParam += "&driverName="+ driverName;
		var uploadId = eXoWCM.UploadForm.uploadId;
		strParam +="&currentFolder="+eXp.store.currentFolder+"&currentPortal="+eXoPlugin.portalName+"&jcrPath="+jcrPath+"&uploadId="+uploadId;
		if(formUpload) {
			var connector = eXp.connector.replace("/getDrivers?repositoryName=repository", "/");
			formUpload.action = connector + eXp.command.uploadFile +"?"+ strParam;
			formUpload.submit();
		}
		eXoWCM.UploadForm.stopUpload = false;
		var uploadField = eXo.core.DOMUtil.findFirstDescendantByClass(popupContainer, "tr","UploadField");
		uploadField.style.display = "none";
		var UploadInfo = eXo.core.DOMUtil.findFirstDescendantByClass(popupContainer, "tr","UploadInfo");
		UploadInfo.style.display = "";
		var CancelAction = eXo.core.DOMUtil.findFirstDescendantByClass(popupContainer, "tr","CancelAction");
		CancelAction.style.display = "none";
		if(!eXoWCM.UploadForm.stopUpload) {
				setTimeout(function() {
				var repositoryName = FCKConfig.repositoryName;
				var workspaceName  = FCKConfig.workspaceName;
				var jcrPath = FCKConfig.jcrPath;
				var driverName = eXp.store.driverName;
				var strParam = '';
				if (repositoryName !== undefined) strParam += "repositoryName="+ repositoryName;
				if (workspaceName !== undefined)  strParam += "&workspaceName=" + workspaceName;
				if(driverName) strParam += "&driverName=" + driverName;
				strParam += "&currentFolder="+eXp.store.currentFolder;
				strParam += "&currentPortal="+eXoPlugin.portalName;
				if(jcrPath) strParam += "&jcrPath="+jcrPath; 				
				strParam +="&action=progress&uploadId="+uploadId;
				var strConnector = eXp.connector.replace("/getDrivers?repositoryName=repository", "/");
				var connector = strConnector + eXp.command.controlUpload + "?"+ strParam + "&language=en";
				var iXML = eXoWCM.PluginUtils.request(connector);
				if(!iXML) return;
				var nodeList = iXML.getElementsByTagName("UploadProgress");
				if(!nodeList) return;
				var oProgress;
				if(nodeList.length > 0) oProgress = nodeList[0];
				var nPercent = oProgress.getAttribute("percent");
				var popupContainer = document.getElementById("PopupContainer");
				var uploadInfo = eXo.core.DOMUtil.findFirstDescendantByClass(popupContainer, "tr", "UploadInfo");
				var graphProgress = eXo.core.DOMUtil.findFirstDescendantByClass(uploadInfo, "div", "GraphProgress");
				var numberProgress = eXo.core.DOMUtil.findFirstDescendantByClass(uploadInfo, "div", "NumberProgress");
				if(nPercent * 1 < 100) {
					graphProgress.style.width = nPercent + "%";
					numberProgress.innerHTML = nPercent + "%";
					eXoWCM.UploadForm.stopUpload = false;
					uploadInfo.className = "UploadInfo Abort";
				} else {
					graphProgress.style.width = 100 + "%";
					numberProgress.innerHTML = 100 + "%";
					eXoWCM.UploadForm.stopUpload = true;
					uploadInfo.className = "UploadInfo Delete";
					var uploadAction = eXo.core.DOMUtil.findFirstDescendantByClass(popupContainer, "tr", "UploadAction");
					uploadAction.style.display = "";
				}
			}, 1*1000);
		}
	} else {
		eXoWCM.UploadForm.showAlert();	
	}
};

UploadForm.prototype.uploadFileAbort = function() {
	var repositoryName = FCKConfig.repositoryName;
	var workspaceName  = FCKConfig.workspaceName;
	var jcrPath = FCKConfig.jcrPath;
	var strParam ="action=abort&uploadId="+eXoWCM.UploadForm.uploadId+"&currentFolder="+eXp.store.currentFolder+"&currentPortal="+eXoPlugin.portalName;
	if (repositoryName !== undefined) strParam += "&repositoryName="+ repositoryName;
	if (workspaceName !== undefined)  strParam += "&workspaceName=" + workspaceName;
	if(jcrPath) strParam += "&jcrPath="+jcrPath;
	var strConnector = eXp.connector.replace("/getDrivers?repositoryName=repository", "/");
	var connector = strConnector + eXp.command.controlUpload + "?"+strParam;
	eXp.sendRequestUpload(connector);
	eXoWCM.UploadForm.stopUpload = true;
	eXoWCM.UploadForm.removeMask();
	eXoWCM.UploadForm.showUploadForm();
};

UploadForm.prototype.uploadFileCancel = function() {
	var repositoryName = FCKConfig.repositoryName;
	var workspaceName  = FCKConfig.workspaceName;
	var jcrPath = FCKConfig.jcrPath;
	var strParam ="action=delete&uploadId="+eXoWCM.UploadForm.uploadId+"&currentFolder="+eXp.store.currentFolder+"&currentPortal="+eXoPlugin.portalName;
	if (repositoryName !== undefined) strParam += "&repositoryName="+ repositoryName;
	if (workspaceName !== undefined)  strParam += "&workspaceName=" + workspaceName;
	if(jcrPath) strParam += "&jcrPath="+jcrPath;
	var strConnector = eXp.connector.replace("/getDrivers?repositoryName=repository", "/");
	var connector = strConnector + eXp.command.controlUpload + "?"+strParam;
	eXp.sendRequestUpload(connector);
	eXoWCM.UploadForm.removeMask();
};

UploadForm.prototype.uploadFileDelete = function() {
	var repositoryName = FCKConfig.repositoryName;
	var workspaceName  = FCKConfig.workspaceName;
	var jcrPath = FCKConfig.jcrPath;
	var strParam ="action=delete&uploadId="+eXoWCM.UploadForm.uploadId+"&currentFolder="+eXp.store.currentFolder+"&currentPortal="+eXoPlugin.portalName;
	if (repositoryName !== undefined) strParam += "&repositoryName="+ repositoryName;
	if (workspaceName !== undefined)  strParam += "&workspaceName=" + workspaceName;
	if(jcrPath) strParam += "&jcrPath="+jcrPath;
	var strConnector = eXp.connector.replace("/getDrivers?repositoryName=repository", "/");
	var connector = strConnector + eXp.command.controlUpload + "?"+strParam;
	eXp.sendRequestUpload(connector);
	eXoWCM.UploadForm.removeMask();
};

UploadForm.prototype.uploadFileSave = function() {
		var popupContainer = document.getElementById("PopupContainer");
		var nodeName = '';
		var nodes = eXo.core.DOMUtil.findDescendantsByTagName(popupContainer, "input");
		for(var i = 0; i < nodes.length;  i++) {
			if(nodes[i].getAttribute("name") == "fileName") {
				nodeName = nodes[i].value;
			}
		}
		var iFrameUpload = eXo.core.DOMUtil.findFirstDescendantByClass(popupContainer, "iframe", "iFrameUpload");
		var formUpload = iFrameUpload.contentWindow.document.getElementsByTagName("form")[0];
		if ((!nodeName && eXoWCM.UploadForm.isInvalidName(formUpload.file.value)) || eXoWCM.UploadForm.isInvalidName(nodeName)) {
			alert('Invalid file name!');
			return;
		}
		
		var repositoryName = FCKConfig.repositoryName;
		var workspaceName  = FCKConfig.workspaceName;
		var jcrPath = FCKConfig.jcrPath;
		var driverName = eXp.store.driverName;
		var strParam = '';
		if (repositoryName !== undefined) strParam += "repositoryName="+ repositoryName;
		if (workspaceName !== undefined)  strParam += "&workspaceName=" + workspaceName;
		if(driverName) strParam += "&driverName=" + driverName;
		strParam += "&currentFolder="+eXp.store.currentFolder;
		strParam += "&currentPortal="+eXoPlugin.portalName;
		strParam += "&userId="+eXoPlugin.userId;
		if(jcrPath) strParam += "&jcrPath="+jcrPath; 				
		var uploadId = eXoWCM.UploadForm.uploadId;
		strParam +="&action=save&uploadId="+uploadId+"&filename="+nodeName;
		var strConnector = eXp.connector.replace("/getDrivers?repositoryName=repository", "/");
		var connector = strConnector + eXp.command.controlUpload + "?"+ strParam + "&language=en";
//		eXp.sendRequest(connector);
		var mXML = eXoWCM.PluginUtils.request(connector);
		var message = mXML.getElementsByTagName("Message")[0];
		if(message) {
			var intNumber = message.getAttribute("number");
			var strText  	= message.getAttribute("text");
			if(parseInt(intNumber) - 200) {
				alert(strText);
				eXoWCM.UploadForm.updateFiles(eXp.store.currentNode);
			} else {
				alert(strText);
				eXp.store.currentNode =	eXp.store.temporaryNode;
				eXoWCM.UploadForm.updateFiles(eXp.store.currentNode);
			}
			eXoWCM.UploadForm.removeMask();
		} else {
	 		eXoWCM.UploadForm.removeMask();
		 	eXoWCM.UploadForm.updateFiles(eXp.store.currentNode.id);
		}
};

UploadForm.prototype.updateFiles = function(nodeId) {
	if(!nodeId) return;
	var node = document.getElementById(nodeId);
	var strConnector = eXp.connector;
	currentFolder = node.getAttribute('currentfolder');
	if (currentFolder == null) currentFolder = '';
	driverName = eXp.store.driverName;
	var strReplace 	= "getFoldersAndFiles?driverName="+driverName+"&currentFolder="+currentFolder+"&";	
	strConnector 	= strConnector.replace("getDrivers?",strReplace);
	var filter = '';
	var dropdownlist = document.getElementById("Pinter");
	if(dropdownlist) filter = dropdownlist.options[dropdownlist.selectedIndex].value;
	else filter = 'Web Contents';
	var connector = eXoPlugin.hostName + strConnector+ "&workspaceName=collaboration&userId=" + eXoPlugin.userId + "&filterBy="+filter;
	var xmlTreeNodes = eXoWCM.PluginUtils.request(connector);
	if(!xmlTreeNodes) return;
	var fileList = xmlTreeNodes.getElementsByTagName('File');
	if(fileList && fileList.length > 0) eXoWCM.PluginUtils.listFiles(fileList);
};

UploadForm.prototype.isInvalidName = function(name) {
	if (name.match('[/,[,*,\',",|,#,%,&,^,+,:]') == null && name.indexOf(']') < 0) return false;
	return true;
}

if(!window.eXoWCM) eXoWCM = new Object();
eXoWCM.UploadForm = new UploadForm();