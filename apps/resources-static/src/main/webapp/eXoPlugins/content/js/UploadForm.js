function UploadForm() {
}

UploadForm.prototype.showUploadForm = function() {
	var uploadContainer = document.getElementById("UploadContainer"); 
	if(eXo.ecm.ECS.currentNode && eXo.ecm.ECS.currentNode.getAttribute('name')) {
		var sPath = eXo.ecm.ECS.currentNode.getAttribute("name");
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
	eXo.ecm.UploadForm.showMask(popupContainer, true);
	var uploadForm = eXo.core.DOMUtil.findFirstDescendantByClass(popupContainer, "div", "UploadForm");
	var maskLayer = eXo.core.DOMUtil.findFirstDescendantByClass(popupContainer, "div", "MaskLayer");
	if (maskLayer!=null) maskLayer.style.zIndex = uploadForm.style.zIndex++;
	uploadForm.style.position = 'absolute';
	var widthUploadForm = (eXo.core.Browser.getBrowserWidth() - uploadForm.offsetWidth)/2;
	var heightUploadForm = (eXo.core.Browser.getBrowserHeight() - uploadForm.offsetHeight)/2;
	uploadForm.style.left = widthUploadForm + "px";
	uploadForm.style.top = heightUploadForm + "px";
	
	var tblActionContainer =  eXo.core.DOMUtil.findFirstDescendantByClass(uploadForm, "table", "ActionContainer");
	var trFolder =  eXo.core.DOMUtil.findFirstDescendantByClass(tblActionContainer, "tr", "PathFolder");
	var spanFolder = eXo.core.DOMUtil.findDescendantsByTagName(trFolder, "span")[0];
	spanFolder.innerHTML += ":"+ eXo.ecm.ECS.currentFolder;
};

UploadForm.prototype.showAlert = function() {
	eXo.ecm.UploadForm.removeMask();
	var popupContainer = document.getElementById("PopupContainer");
	popupContainer.style.display = 'block';
	popupContainer.style.width = "100%";
	popupContainer.style.height = "100%";
	popupContainer.style.position = "absolute";
	popupContainer.style.top = "0px";
	var hideContainer = document.getElementById("hideContainer");
	var alertContainer = eXo.core.DOMUtil.findFirstDescendantByClass(hideContainer, "div", "AlertContainer");
	popupContainer.innerHTML = alertContainer.innerHTML;
	eXo.ecm.UploadForm.showMask(popupContainer, true);
	var alertForm = eXo.core.DOMUtil.findFirstDescendantByClass(popupContainer, "div", "AlertForm");
	var maskLayer = eXo.core.DOMUtil.findFirstDescendantByClass(popupContainer, "div", "MaskLayer");
	if (maskLayer!=null) maskLayer.style.zIndex = alertForm.style.zIndex++;
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
		if (mask != null) eXo.core.UIMaskLayer.createMask(popup.id, mask, 20) ;			
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
	var repositoryName = eXo.ecm.ECS.repositoryName;
	var workspaceName  = eXo.ecm.ECS.workspaceName;
	var driverName = eXo.ecm.ECS.driverName;
	var strParam = '';
	if (repositoryName !== undefined) strParam += "repositoryName="+ repositoryName;
	if (workspaceName !== undefined)  strParam += "&workspaceName=" + workspaceName;
	if(driverName) strParam += "&driverName="+ driverName;
	strParam +="&currentFolder="+eXo.ecm.ECS.currentFolder+"&currentPortal="+eXo.ecm.ECS.portalName;
	return strParam;
};

UploadForm.prototype.uploadFile = function() {
	eXo.ecm.UploadForm.uploadId = eXp.getID();
	var popupContainer = document.getElementById("PopupContainer");
	var iFrameUpload = eXo.core.DOMUtil.findFirstDescendantByClass(popupContainer, "iframe", "iFrameUpload");
	var formUpload = iFrameUpload.contentWindow.document.getElementsByTagName("form")[0];
	if(!formUpload.file.value == '') {
		var repositoryName = eXo.ecm.ECS.repositoryName;
		var workspaceName  = eXo.ecm.ECS.workspaceName;
		var driverName = eXo.ecm.ECS.driverName;
		var strParam = '';
		if (repositoryName !== undefined) strParam += "repositoryName="+ repositoryName;
		if (workspaceName !== undefined)  strParam += "&workspaceName=" + workspaceName;
		if(driverName) strParam += "&driverName="+ driverName;
		var uploadId = eXo.ecm.UploadForm.uploadId;
		strParam +="&currentFolder="+eXo.ecm.ECS.currentFolder+"&currentPortal="+eXo.ecm.ECS.portalName+"&uploadId="+uploadId;
		if(formUpload) {
			var connector = eXo.ecm.ECS.connector.replace("repositoryName=repository", "/");
			formUpload.action = connector + eXo.ecm.ECS.cmdEcmDriver + eXo.ecm.ECS.uploadFile +"?"+ strParam;
			formUpload.submit();
		}
		eXo.ecm.UploadForm.stopUpload = false;
		var uploadField = eXo.core.DOMUtil.findFirstDescendantByClass(popupContainer, "tr","UploadField");
		uploadField.style.display = "none";
		var UploadInfo = eXo.core.DOMUtil.findFirstDescendantByClass(popupContainer, "tr","UploadInfo");
		UploadInfo.style.display = "";
		var CancelAction = eXo.core.DOMUtil.findFirstDescendantByClass(popupContainer, "tr","CancelAction");
		CancelAction.style.display = "none";
		if(!eXo.ecm.UploadForm.stopUpload) {
				setTimeout(function() {
					var repositoryName = eXo.ecm.ECS.repositoryName;
					var workspaceName  = eXo.ecm.ECS.workspaceName;
					var driverName = eXo.ecm.ECS.driverName;
					var strParam = '';
					if (repositoryName !== undefined) strParam += "repositoryName="+ repositoryName;
					if (workspaceName !== undefined)  strParam += "&workspaceName=" + workspaceName;
					if(driverName) strParam += "&driverName=" + driverName;
					strParam += "&currentFolder="+eXo.ecm.currentFolder;
					strParam += "&currentPortal="+eXo.ecm.portalName;
					strParam +="&action=progress&uploadId="+uploadId;
					var strConnector = eXo.ecm.ECS.connector.replace("/getDrivers?repositoryName=repository", "/");
					var connector = strConnector + eXo.ecm.ECS.cmdEcmDriver + eXo.ecm.ECS.controlUpload + "?"+ strParam + "&language=en";
					var iXML = eXo.ecm.WCMUtils.request(connector);
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
						eXo.ecm.UploadForm.stopUpload = false;
						uploadInfo.className = "UploadInfo Abort";
					} else {
						graphProgress.style.width = 100 + "%";
						numberProgress.innerHTML = 100 + "%";
						eXo.ecm.UploadForm.stopUpload = true;
						uploadInfo.className = "UploadInfo Delete";
						var uploadAction = eXo.core.DOMUtil.findFirstDescendantByClass(popupContainer, "tr", "UploadAction");
						uploadAction.style.display = "";
					}
				}, 1*1000);
		}
	} else {
		eXo.ecm.UploadForm.showAlert();	
	}
};

UploadForm.prototype.uploadFileAbort = function() {
	var repositoryName = eXo.ecm.ECS.repositoryName;
	var workspaceName  = eXo.ecm.ECS.workspaceName;
	var strParam ="action=abort&uploadId="+eXo.ecm.UploadForm.uploadId+"&currentFolder="+eXo.ecm.ECS.currentFolder+"&currentPortal="+eXo.ecm.ECS.portalName;
	if (repositoryName !== undefined) strParam += "&repositoryName="+ repositoryName;
	if (workspaceName !== undefined)  strParam += "&workspaceName=" + workspaceName;
	var strConnector = eXo.ecm.connector.replace("/getDrivers?repositoryName=repository", "/");
	var connector = strConnector + eXo.ecm.ECS.controlUpload + "?"+strParam;
	eXo.ecm.WCMUtils.request(connector);
	eXo.ecm.UploadForm.stopUpload = true;
	eXo.ecm.UploadForm.removeMask();
	eXo.ecm.UploadForm.showUploadForm();
};

UploadForm.prototype.uploadFileCancel = function() {
	var repositoryName = eXo.ecm.ECS.repositoryName;
	var workspaceName  = eXo.ecm.ECS.workspaceName;
	var strParam ="action=delete&uploadId="+eXo.ecm.UploadForm.uploadId+"&currentFolder="+eXo.ecm.ECS.currentFolder+"&currentPortal="+eXo.ecm.ECS.portalName;
	if (repositoryName !== undefined) strParam += "&repositoryName="+ repositoryName;
	if (workspaceName !== undefined)  strParam += "&workspaceName=" + workspaceName;
	var strConnector = eXo.ecm.ECS.connector.replace("/getDrivers?repositoryName=repository", "/");
	var connector = strConnector + eXo.ecm.ECS.cmdEcmDriver + eXo.ecm.ECS.controlUpload + "?"+strParam;
	eXo.ecm.WCMUtils.request(connector);
	eXo.ecm.UploadForm.removeMask();
};

UploadForm.prototype.uploadFileDelete = function() {
	var repositoryName = eXo.ecm.ECS.repositoryName;
	var workspaceName  = eXo.ecm.ECS.workspaceName;
	var strParam ="action=delete&uploadId="+eXo.ecm.UploadForm.uploadId+"&currentFolder="+eXo.ecm.ECS.currentFolder+"&currentPortal="+eXo.ecm.ECS.portalName;
	if (repositoryName !== undefined) strParam += "&repositoryName="+ repositoryName;
	if (workspaceName !== undefined)  strParam += "&workspaceName=" + workspaceName;
	var strConnector = eXo.ecm.ECS.connector.replace("/getDrivers?repositoryName=repository", "/");
	var connector = strConnector + eXo.ecm.ECS.cmdEcmDriver + eXo.ecm.ECS.controlUpload + "?"+strParam;
	eXo.ecm.WCMUtils.request(connector);
	eXo.ecm.UploadForm.removeMask();
	eXo.ecm.UploadForm.showUploadForm();
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
		var filename = formUpload.file.value;
    try {
    	var m = filename.match(/(.*)[\/\\]([^\/\\]+\.\w+)$/);        
    	if(m[1]&&m[2]) filename = m[2];  
		} catch(e) {}         
		if ((!nodeName && eXo.ecm.UploadForm.isInvalidName(filename)) || eXo.ecm.UploadForm.isInvalidName(nodeName)) {
			alert('Invalid file name!');
			return;
		}
		
		var repositoryName = eXo.ecm.ECS.repositoryName;
		var workspaceName  = eXo.ecm.ECS.workspaceName;
		var driverName = eXo.ecm.ECS.driverName;
		var strParam = '';
		if (repositoryName !== undefined) strParam += "repositoryName="+ repositoryName;
		if (workspaceName !== undefined)  strParam += "&workspaceName=" + workspaceName;
		if(driverName) strParam += "&driverName=" + driverName;
		strParam += "&currentFolder="+eXo.ecm.ECS.currentFolder;
		strParam += "&currentPortal="+eXo.ecm.ECS.portalName;
		strParam += "&userId="+eXo.ecm.ECS.userId;
		var uploadId = eXo.ecm.UploadForm.uploadId;
		strParam +="&action=save&uploadId="+uploadId+"&filename="+nodeName;
		var strConnector = eXo.ecm.ECS.connector.replace("/getDrivers?repositoryName=repository", "/");
//		var strConnector = eXo.ecm.ECS.connector.replace("/getDrivers?repositoryName=repository", "/");
		var connector = strConnector + eXo.ecm.ECS.cmdEcmDriver + eXo.ecm.ECS.controlUpload + "?"+ strParam + "&language="+eXo.ecm.ECS.userLanguage;
//		eXp.sendRequest(connector);
		var mXML = eXo.ecm.WCMUtils.request(connector);
    try {      
			var message = mXML.getElementsByTagName("Message")[0];
			if(message) {
				var intNumber = message.getAttribute("number");
				var strText  	= message.getAttribute("text");
				if(parseInt(intNumber) - 200) {
					alert(strText);
					eXo.ecm.UploadForm.updateFiles(eXo.ecm.ECS.currentNode);
				} else {
					alert(strText);
					eXo.ecm.ECS.currentNode =	eXo.ecm.ECS.temporaryNode;
					eXo.ecm.UploadForm.updateFiles(eXo.ecm.ECS.currentNode);
				}
				eXo.ecm.UploadForm.removeMask();
			} else {        
		 		eXo.ecm.UploadForm.removeMask();
			 	eXo.ecm.UploadForm.updateFiles(eXo.ecm.ECS.currentNode.id);
			}
		} catch(e) {      
			eXo.ecm.UploadForm.removeMask();
			eXo.ecm.UploadForm.updateFiles(eXo.ecm.ECS.currentNode.id);
		}
};

UploadForm.prototype.updateFiles = function(nodeId) {
	if(!nodeId) return;
	var node = document.getElementById(nodeId);
	var strConnector = eXo.ecm.ECS.connector + eXo.ecm.ECS.cmdEcmDriver;
	currentFolder = node.getAttribute('currentfolder');
	if (currentFolder == null) currentFolder = '';
	driverName = eXo.ecm.ECS.driverName;
	strConnector 	+= eXo.ecm.ECS.cmdGetFolderAndFile+"driverName="+driverName+"&currentFolder="+currentFolder+"&";	
	var dropdownlist = document.getElementById("Filter");	
  if(dropdownlist) {
		if(dropdownlist.type=="hidden") filter = dropdownlist.value;		
		else filter = dropdownlist.options[dropdownlist.selectedIndex].value;
	}	else filter = 'Web Contents';
	var connector = eXo.ecm.ECS.hostName + strConnector+"repositoryName="+eXo.ecm.ECS.repositoryName+"&workspaceName="+eXo.ecm.ECS.workspaceName+"&userId=" + eXo.ecm.ECS.userId + "&filterBy="+filter;
	var xmlTreeNodes = eXo.ecm.WCMUtils.request(connector);
	if(!xmlTreeNodes) return;
	var fileList = xmlTreeNodes.getElementsByTagName('File');
	if(fileList && fileList.length > 0) eXo.ecm.ECS.listFiles(fileList);
};

UploadForm.prototype.isInvalidName = function(name) {
	if (name.match('[/,[,*,\',",|,#,%,&,^,+,:]') == null && name.indexOf(']') < 0) return false;
	return true;
}

eXo.ecm.UploadForm = new UploadForm();
