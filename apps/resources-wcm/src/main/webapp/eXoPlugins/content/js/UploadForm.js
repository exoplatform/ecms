function UploadForm() {
	this.uploadProgressTimer;
	this.existingBehavior = "keepBoth";
	this.document_auto_label_existing = eXo.ecm.WCMUtils.getBundle("DocumentAuto.label.existing",  eXo.env.portal.language);
	this.document_auto_label_cancel   = eXo.ecm.WCMUtils.getBundle("DocumentAuto.label.cancel",  eXo.env.portal.language);
	this.document_auto_label_or				= eXo.ecm.WCMUtils.getBundle("DocumentAuto.label.or",  eXo.env.portal.language)
	this.document_auto_label_createVersion = eXo.ecm.WCMUtils.getBundle("DocumentAuto.label.createVersion",  eXo.env.portal.language);
	this.document_auto_label_replace  = eXo.ecm.WCMUtils.getBundle("DocumentAuto.label.createVersion",  eXo.env.portal.language);
	this.document_auto_label_keepBoth	= eXo.ecm.WCMUtils.getBundle("DocumentAuto.label.keepBoth",  eXo.env.portal.language);
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
	popupContainer.style.width = "500px";
	popupContainer.style.height = "auto";
	popupContainer.style.position = "absolute";
	popupContainer.style.top = "50%";
	popupContainer.innerHTML = uploadContainer.innerHTML;
	var iFrame = gj(popupContainer).find("iframe.iFrameUpload:first")[0];
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
	var uploadForm = gj(popupContainer).find("div.UploadForm:first")[0];
	var maskLayer = gj(popupContainer).find("div.MaskLayer:first")[0];
	if (maskLayer!=null) maskLayer.style.zIndex = uploadForm.style.zIndex++;
	//uploadForm.style.position = 'absolute';
	var widthUploadForm = (gj(window).width() - uploadForm.offsetWidth)/2;
	var heightUploadForm = (gj(window).height() - uploadForm.offsetHeight)/2;
	//uploadForm.style.left = widthUploadForm + "px";
	//uploadForm.style.top = heightUploadForm + "px";
	
	//var tblActionContainer =  gj(uploadForm).find("table.ActionContainer:first")[0];
	//var trFolder =  gj(tblActionContainer).find("tr.PathFolder:first")[0];
	//var spanFolder = gj(trFolder).find("span")[0];
	//spanFolder.innerHTML += ":"+ eXo.ecm.ECS.currentFolder;
	
	
	var uploadBtn = popupContainer.getElementsByClassName("uploadButton")[0];
	var iFrameUpload = gj(popupContainer).find("iframe.iFrameUpload:first")[0];	
	var formUpload = iFrameUpload.contentWindow.document.getElementsByTagName("form")[0];
	var fileUpload = formUpload.file;
			
	gj(uploadBtn).off("click").click(function() {
	  gj(fileUpload).change(function() {
		var fileNameUpload = gj(fileUpload).val();
		if(fileNameUpload == null || fileNameUpload == "") {
			fileNameUpload = "No file selected";
		}
		fileNameUpload = fileNameUpload.replace(/^.*[\\\/]/, '');
		var labelUpload = gj(uploadBtn).find(".noFile");
		gj(labelUpload).text(fileNameUpload);
		//console.log(labelUpload);
		
	  });
	  gj(fileUpload).click();
	  
	})
	
	
};

UploadForm.prototype.alertFilename = function() {
	alert('fsfs');
}

UploadForm.prototype.showAlert = function() {
	eXo.ecm.UploadForm.removeMask();
	var popupContainer = document.getElementById("PopupContainer");
	//popupContainer.style.display = 'block';
	//popupContainer.style.width = "100%";
	//popupContainer.style.height = "100%";
	//popupContainer.style.position = "absolute";
	//popupContainer.style.top = "0px";
	gj(popupContainer).attr("style", "display: block; height: 100px;left: 50%; position: absolute; top: 50%;" +
	"visibility:   visible; width: 400px;z-index: 7;; transform: -moz-translate(-50%, -50%); " +
	"-webkit-transform: translate(-50%, -50%); -ms-transform: translate(-50%, -50%); " +
	"-o-transform: translate(-50%, -50%); transform: translate(-50%, -50%);");
	var hideContainer = document.getElementById("hideContainer");
	var alertContainer = gj(hideContainer).find("div.AlertContainer:first")[0];
	popupContainer.innerHTML = alertContainer.innerHTML;
	eXo.ecm.UploadForm.showMask(popupContainer, true);
	var alertForm = gj(popupContainer).find("div.AlertForm:first")[0];
	var maskLayer = gj(popupContainer).find("div.MaskLayer:first")[0];
	if (maskLayer!=null) maskLayer.style.zIndex = alertForm.style.zIndex++;
	//alertForm.style.position = 'absolute';
	//var widthAlertForm = (gj(window).width() - alertForm.offsetWidth)/2;
	//var heightAlertForm = (gj(window).height() - alertForm.offsetHeight)/2;
	//alertForm.style.left = widthAlertForm + "px";
	//alertForm.style.top = heightAlertForm + "px";
	gj(alertForm).attr("style", "top:50%; left:50%; position: absolute; transform: -moz-translate(-50%, -50%); " +
	"-webkit-transform: translate(-50%, -50%); -ms-transform: translate(-50%, -50%); " +
	"-o-transform: translate(-50%, -50%); transform: translate(-50%, -50%);")
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
	var iFrameUpload = gj(popupContainer).find("iframe.iFrameUpload:first")[0];
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
		var uploadBtn = popupContainer.getElementsByClassName("uploadButton")[0];
		uploadBtn.style.display = "none";
		var uploadField = gj(popupContainer).find("tr.UploadField:first")[0];
		uploadField.style.display = "none";
		var UploadInfo = gj(popupContainer).find("tr.UploadInfo:first")[0];
		UploadInfo.style.display = "";
		var uiUploadAction = gj(popupContainer).find("div.uiActionBorder:first")[0];
		uiUploadAction.style.display = "none";
		if(!eXo.ecm.UploadForm.stopUpload) {
				this.uploadProgressTimer = setInterval(function() {
					var repositoryName = eXo.ecm.ECS.repositoryName;
					var workspaceName  = eXo.ecm.ECS.workspaceName;
					var driverName = eXo.ecm.ECS.driverName;
					var strParam = '';
					if (repositoryName !== undefined) strParam += "repositoryName="+ repositoryName;
					if (workspaceName !== undefined)  strParam += "&workspaceName=" + workspaceName;
					if(driverName) strParam += "&driverName=" + driverName;
					strParam += "&currentFolder="+eXo.ecm.ECS.currentFolder;
					strParam += "&currentPortal="+eXo.ecm.ECS.portalName;
					strParam +="&action=progress&uploadId="+uploadId;
					var strConnector = eXo.ecm.ECS.connector.replace("/getDrivers?repositoryName=repository", "/");
					var connector = strConnector + eXo.ecm.ECS.cmdEcmDriver + eXo.ecm.ECS.controlUpload + "?"+ strParam + "&language=" + eXo.ecm.ECS.userLanguage;
					var iXML = eXo.ecm.WCMUtils.request(connector);
					if(!iXML) return;
					// Get message if any error while uploading
					var message = iXML.getElementsByTagName("Message")[0];
					if(message) {
						var strText = message.getAttribute("text");
						alert(strText);
						eXo.ecm.UploadForm.removeMask();
						eXo.ecm.UploadForm.showUploadForm();
						return;
					}
					var nodeList = iXML.getElementsByTagName("UploadProgress");
					if(!nodeList) return;
					var oProgress;
					if(nodeList.length > 0) oProgress = nodeList[0];
					var nPercent = oProgress.getAttribute("percent");
					var popupContainer = document.getElementById("PopupContainer");
					var uploadInfo = gj(popupContainer).find("tr.UploadInfo:first")[0];
					var graphProgress = gj(uploadInfo).find("div.GraphProgress:first")[0];
					var numberProgress = gj(uploadInfo).find("div.NumberProgress:first")[0];
					if(nPercent * 1 < 100) {
						graphProgress.style.width = nPercent + "%";
						numberProgress.innerHTML = nPercent + "%";
						eXo.ecm.UploadForm.stopUpload = false;
						uploadInfo.className = "UploadInfo Abort";
					} else {
						graphProgress.style.width = 100 + "%";
						numberProgress.innerHTML = 100 + "%";
						eXo.ecm.UploadForm.stopUpload = true;
						var fileName = gj(iXML.getElementsByTagName("UploadProgress")).attr("fileName");
						//console.log(fileName);
						uploadInfo.className = "UploadInfo Delete";
						var uploadAction = gj(popupContainer).find("tr.UploadAction:first")[0];
						gj(uploadAction).find("#fileName").val(fileName);
						uploadAction.style.display = "";
						clearInterval(eXo.ecm.UploadForm.uploadProgressTimer);
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
	var strConnector = eXo.ecm.ECS.connector.replace("/getDrivers?repositoryName=repository", "/");
	var connector = strConnector + eXo.ecm.ECS.controlUpload + "?"+strParam;
	eXo.ecm.WCMUtils.request(connector);
	eXo.ecm.UploadForm.stopUpload = true;
	eXo.ecm.UploadForm.removeMask();
	eXo.ecm.UploadForm.showUploadForm();
	clearInterval(this.uploadProgressTimer);
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

var checkSupportVersion = function(listFiles, fileName){
	for (var i = 0; i < listFiles.length; i++) {
		if(listFiles[i].name === fileName && listFiles[i].isVersionSupport === "true"){
			return true;
		}
	}
	return false;
}

var checkVersExistedFile = function(listFiles, fileName){
	for (var i = 0; i < listFiles.length; i++) {
		if(listFiles[i].name === fileName && listFiles[i].isVersioned === "true"){
			return true;
		}
	}
	return false;
}

UploadForm.prototype.preUploadFileSave = function() {
	var uploadInfo = gj("#PopupContainer").find("tr.UploadAction:first")[0];
	var fileName = gj(uploadInfo).find("#fileName").val();

	if(eXo.ecm.ECS.lstFileName.indexOf(fileName) != -1
			&& checkSupportVersion(eXo.ecm.ECS.lstFiles, fileName)){
		gj("#auto-versioning-actions").remove();
		var documentAuto = "<div id=\"auto-versioning-actions\" class=\"alert alert-warning clearfix hidden\">";
		documentAuto += "<div class=\"fileNameBox\"> <i class=\"uiIconWarning\"></i>"+eXo.ecm.UploadForm.document_auto_label_existing+"<span class=\"fileName\" ></span></div>";
		documentAuto += "<a href=\"javascript:void(0)\" class=\"pull-right action cancel\">"+eXo.ecm.UploadForm.document_auto_label_cancel+" </a>";
		documentAuto += "<span class=\"pull-right\">&nbsp;"+eXo.ecm.UploadForm.document_auto_label_or+"&nbsp; </span>";
		if(checkVersExistedFile(eXo.ecm.ECS.lstFiles, fileName)) {
			documentAuto += "<a href=\"javascript:void(0)\" class=\"pull-right action create-version\">"+eXo.ecm.UploadForm.document_auto_label_createVersion+"</a>";
		}else {
			documentAuto += "<a href=\"javascript:void(0)\" class=\"pull-right action replace\"> "+eXo.ecm.UploadForm.document_auto_label_replace+"</a>";
		}
		documentAuto += "<span class=\"pull-right\">,&nbsp;</span>";
		documentAuto += "<a href=\"javascript:void(0)\" class=\"pull-right action keep-both\">"+eXo.ecm.UploadForm.document_auto_label_keepBoth+"</a>";
		documentAuto += "</div>";

		gj(uploadInfo).children('td').prepend(documentAuto);
		gj("#auto-versioning-actions").closest("#PopupContainer").addClass("versioning-popup");

		gj("#auto-versioning-actions .cancel").bind("click", function(){
			gj("#auto-versioning-actions").hide();
		})

		gj("#auto-versioning-actions .keep-both").unbind();
		gj("#auto-versioning-actions .keep-both").bind("click", function(){
			gj("#auto-versioning-actions").hide();
			eXo.ecm.UploadForm.existingBehavior = "keep";
			eXo.ecm.UploadForm.uploadFileSave();
		})

		gj("#auto-versioning-actions .create-version").unbind();
		gj("#auto-versioning-actions .create-version").bind("click", function(){
			gj("#auto-versioning-actions").hide();
			eXo.ecm.UploadForm.existingBehavior = "createVersion";
			eXo.ecm.UploadForm.uploadFileSave();
		})

		gj("#auto-versioning-actions .replace").unbind();
		gj("#auto-versioning-actions .replace").bind("click", function(){
			gj("#auto-versioning-actions").hide();
			eXo.ecm.UploadForm.existingBehavior = "replace";
			eXo.ecm.UploadForm.uploadFileSave();
		})
		gj("#auto-versioning-actions").show();
	}else{
		eXo.ecm.UploadForm.uploadFileSave();
	}
}
UploadForm.prototype.uploadFileSave = function() {
		var popupContainer = document.getElementById("PopupContainer");
		var nodeName = '';
		var nodes = gj(popupContainer).find("input");
		for(var i = 0; i < nodes.length;  i++) {
			if(nodes[i].getAttribute("name") == "fileName") {
				nodeName = nodes[i].value;
			}
		}
		var iFrameUpload = gj(popupContainer).find("iframe.iFrameUpload:first")[0];
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
		strParam +="&action=save&uploadId="+uploadId+"&fileName="+nodeName;
		strParam +="&existenceAction="+eXo.ecm.UploadForm.existingBehavior;
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


