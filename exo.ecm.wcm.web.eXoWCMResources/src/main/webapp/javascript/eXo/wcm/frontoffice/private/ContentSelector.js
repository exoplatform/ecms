function EcmContentSelector() {
	this.portalName = eXo.env.portal.portalName;
	this.context = eXo.env.portal.context;
	this.accessMode = eXo.env.portal.accessMode;
	this.userId = eXo.env.portal.userName; 
	this.userLanguage = eXo.env.portal.language;
	var parentLocation = window.parent.location;
	this.hostName = parentLocation.href.substring(0, parentLocation.href.indexOf(parentLocation.pathname));
	this.repositoryName = "repository";
	this.workspaceName	= "collaboration";
	this.cmdEcmDriver = "/wcmDriver/"
	this.cmdGetDriver = "getDrivers?";
	this.cmdGetFolderAndFile = "getFoldersAndFiles?";
	this.resourceType = this.getUrlParam("Type") || "File";
	this.connector 		= this.getUrlParam("connector") ||  eXo.ecm.WCMUtils.getRestContext();
	this.currentNode 	= "";
	this.currentFolder = "/";
	this.xmlHttpRequest = false;
	this.driverName = "";
	this.eventNode = false;
}

EcmContentSelector.prototype.getUrlParam = function(paramName) {
	var oRegex = new RegExp("[\?&]" + paramName + "=([^&]+)", "i");
	var oMatch = oRegex.exec(window.location.search) ; 
	if (oMatch && oMatch.length > 1) return oMatch[1];
	else return "";
};

EcmContentSelector.prototype.loadScript = function() {
	if (arguments.length < 2) {
		return;
	} else {
		var win = arguments[0];
		var src = arguments[1];
	}
	if (!win || !win.document) return;
	var eScript = win.document.createElement("script");
	eScript.setAttribute("src", src);
	var eHead = win.document.getElementsByTagName("head")[0];
	eHead.appendChild(eScript);
};

EcmContentSelector.prototype.ajaxRequest = function(url) {
  if(window.XMLHttpRequest && !(window.ActiveXObject)) {
  	try {
		eXo.ecm.ECS.xmlHttpRequest = new XMLHttpRequest();
    } catch(e) {
		eXo.ecm.ECS.xmlHttpRequest = false;
    }
  } else if(window.ActiveXObject) {
     	try {
      	eXo.ecm.ECS.xmlHttpRequest = new ActiveXObject("Msxml2.XMLHTTP");
    	} catch(e) {
      	try {
        	eXo.ecm.ECS.xmlHttpRequest = new ActiveXObject("Microsoft.XMLHTTP");
      	} catch(e) {
        	eXo.ecm.ECS.xmlHttpRequest = false;
      	}
	}
  }
	if(eXo.ecm.ECS.xmlHttpRequest) {
		eXo.ecm.ECS.xmlHttpRequest.onreadystatechange = eXo.ecm.ECS.processResponse;
		eXo.ecm.ECS.xmlHttpRequest.open("GET", url, true);
		eXo.ecm.ECS.xmlHttpRequest.send();
	}
};

EcmContentSelector.prototype.processResponse = function() {
	if (eXo.ecm.ECS.xmlHttpRequest.readyState == 4) {
    if (eXo.ecm.ECS.xmlHttpRequest.status == 200) {
			eXo.ecm.ECS.buildECSTreeView();
    } else {
        alert("There was a problem retrieving the XML data:\n" + eXo.ecm.ECS.xmlHttpRequest.statusText);
        return false;
    }
  }
};

EcmContentSelector.prototype.initRequestXmlTree = function(typeObj){
	if(!typeObj) return;
	eXo.ecm.ECS.typeObj = false;
	switch(typeObj) {
		case "folder" :
			eXo.ecm.ECS.typeObj = "folder";
			break;
		case "multi" : 
			eXo.ecm.ECS.typeObj = "multi";
			break;
		case "one" : 
			eXo.ecm.ECS.typeObj = "one";
			break;
		case "fck" :
			eXo.ecm.ECS.typeObj = "fck";
			break;
		default :
			eXo.ecm.ECS.typeObj = false;
			break;
	}
	eXo.ecm.ECS.isShowFilter();
	var ECS = eXo.ecm.ECS;
	var command = ECS.cmdEcmDriver+ECS.cmdGetDriver+"repositoryName="+ECS.repositoryName+"&workspaceName="+ECS.workspaceName+"&userId="+ ECS.userId;
	var url = ECS.hostName+ECS.connector+ command + "&currentPortal=" + ECS.portalName;
	eXo.ecm.ECS.ajaxRequest(url);
};

EcmContentSelector.prototype.buildECSTreeView = function() {
	var xmlTreeNodes = eXo.ecm.ECS.xmlHttpRequest.responseXML;
	var treeHTML = '';
	var nodeList = xmlTreeNodes.getElementsByTagName("Folders");
	for(var i = 0 ; i < nodeList.length; i++)	 {
		var strName = nodeList[i].getAttribute("name") ;
		var id = eXo.ecm.ECS.generateIdDriver(nodeList[i]);
		treeHTML += '<div class="Node" onclick="eXo.ecm.ECS.actionColExp(this);">';
		treeHTML += 	'<div class="ExpandIcon">';		
		treeHTML += 		'<a title="'+strName+'"href="javascript:void(0);" class="NodeIcon DefaultPageIcon" onclick="eXo.ecm.ECS.renderBreadcrumbs(this);" name="'+strName+'" id="'+id+'">';
		treeHTML +=			strName;	
		treeHTML +=			'</a>';
		treeHTML += 	'</div>';			
		treeHTML += '</div>';			
		var tmp = eXo.ecm.ECS.renderSubTree(nodeList[i]);
		if(tmp != '') treeHTML += tmp;
	}
	var uiLeftWorkspace = document.getElementById('LeftWorkspace');	
	if(uiLeftWorkspace) uiLeftWorkspace.innerHTML = treeHTML;
};

EcmContentSelector.prototype.getDir = function(currentNode, event) {
	var ECS = eXo.ecm.ECS;
	eXo.ecm.ECS.eventNode = event;
	var repoName = currentNode.getAttribute("repository");
	if(repoName) eXo.ecm.ECS.repositoryName = repoName;
	var wsName	= currentNode.getAttribute("workspace");
	if(wsName) eXo.ecm.ECS.workspaceName =  wsName;
	var connector = ECS.connector;
	var currentFolder;
	var driverName;
	var driverPath = currentNode.getAttribute("driverPath");
	if(driverPath && driverPath != "") {
		driverName =	currentNode.getAttribute('name');
		eXo.ecm.ECS.driverName = driverName;
		currentFolder = '';
	} else {
			var nodeContainer = eXo.core.DOMUtil.findAncestorByClass(currentNode, "ChildrenContainer");
			if(!nodeContainer) return;
			var nodeParent = eXo.core.DOMUtil.findPreviousElementByTagName(nodeContainer, "div");
			if(!nodeParent) return;
			var nodeLink = eXo.core.DOMUtil.findFirstDescendantByClass(nodeParent, "a", "NodeIcon");
			if(nodeLink) {
				if(nodeLink.getAttribute('currentFolder') && nodeLink.getAttribute('currentFolder') != null) {
					currentFolder = nodeLink.getAttribute('currentFolder') + '/' + currentNode.getAttribute('name');
					currentNode.setAttribute("currentFolder", currentFolder);
				}	else {
					currentFolder = currentNode.getAttribute('name');
					currentNode.setAttribute("currentFolder", currentFolder);
				}				
			} else {
				currentFolder = currentNode.getAttribute('name');
				currentNode.setAttribute("currentFolder", currentFolder);
			}
	}
	eXo.ecm.ECS.currentFolder = currentFolder;
	eXo.ecm.ECS.currentNode = currentNode;
	driverName = eXo.ecm.ECS.driverName;
	var filter = '';
	var dropdownlist = document.getElementById("Filter");
	if(dropdownlist) filter = dropdownlist.options[dropdownlist.selectedIndex].value;
	else filter = 'Web Contents';

	var command = ECS.cmdEcmDriver+ECS.cmdGetFolderAndFile+"driverName="+driverName+"&currentFolder="+currentFolder+"&currentPortal="+ECS.portalName+"&repositoryName="+ECS.repositoryName+"&workspaceName="+ECS.workspaceName;
	var url = ECS.hostName + ECS.connector+command+"&userId=" + ECS.userId+"&filterBy="+filter;
	if(eXo.ecm.ECS.strConnection == url) return;	
	eXo.ecm.ECS.strConnection = url;
	eXo.ecm.ECS.renderSubTrees(currentNode, event, url);
	eXo.ecm.ECS.renderBreadcrumbs(currentNode);
};

EcmContentSelector.prototype.renderSubTree = function(currentNode) {
	if(!currentNode) return;
	var nodeList = currentNode.getElementsByTagName('Folder');
	var nodeListParent = currentNode.getElementsByTagName('Folders');
	var treeHTML = '';
	if(nodeList && nodeList.length > 0) {
		treeHTML += '<div class="ChildrenContainer" style="display:none;">'	;
		for(var i = 0; i < nodeList.length; i++) {
			var id = eXo.ecm.ECS.generateIdDriver(nodeList[i]);
			var strName = nodeList[i].getAttribute("name");
			var driverPath = nodeList[i].getAttribute("driverPath");
			var repository =  nodeList[i].getAttribute("repository");
			var workspace =  nodeList[i].getAttribute("workspace");
			treeHTML += '<div class="Node" onclick="eXo.ecm.ECS.actionColExp(this);">';
			treeHTML += 	'<div class="ExpandIcon">';
			treeHTML +=			'<a title="'+strName+'" href="javascript:void(0);" class="NodeIcon DefaultPageIcon" onclick="eXo.ecm.ECS.getDir(this, event);" name="'+strName+'" id="'+id+'"  driverPath="'+driverPath+'" repository="'+repository+'" workspace="'+workspace+'">';
			treeHTML +=				strName;	
			treeHTML += 		'</a>';
			treeHTML +=		'</div>';
			treeHTML +=	'</div>';
		}
		treeHTML += '</div>';
	}
	return treeHTML;
};

EcmContentSelector.prototype.renderSubTrees = function(currentNode, event, connector) {
	var event = event || window.event;
	event.cancelBubble = true;
	if(!currentNode) return;
	var treeHTML = '';
	var fileList = '';
	var folderList = '';
	var nodeList = currentNode.getElementsByTagName('Folder');
	if(nodeList && nodeList.length > 0) {
		folderList = nodeList;	
		fileList = currentNode.getElementsByTagName('File');
		treeHTML += '<div class="ChildrenContainer" style="display:none;">'	;
		for(var i = 0; i < nodeList.length; i++) {
			var id = eXo.ecm.ECS.generateIdNodes(nodeList[i], currentNode.id);
			var strName = nodeList[i].getAttribute("name");
			treeHTML += '<div class="Node" onclick="eXo.ecm.ECS.actionColExp(this);">';
			treeHTML += 	'<div class="ExpandIcon">';
			treeHTML +=			'<a title="'+ strName +'" class="NodeIcon DefaultPageIcon" href="javascript:void(0);" onclick="eXo.ecm.ECS.getDir(this, event);" name="'+strName+'" id="'+id+'">';
			treeHTML +=				strName;	
			treeHTML += 		'</a>';
			treeHTML +=		'</div>';
			treeHTML +=	'</div>';
		}
		treeHTML += '</div>';
	} else {
		var xmlTreeNodes = eXo.ecm.WCMUtils.request(connector);
		var currentNodeList = xmlTreeNodes.getElementsByTagName('Folder');
		folderList = currentNodeList;
		fileList = xmlTreeNodes.getElementsByTagName('File');
		if(currentNodeList && currentNodeList.length > 0) {
			for(var i = 0; i < currentNodeList.length; i++) {
				var id = eXo.ecm.ECS.generateIdNodes(currentNodeList[i], currentNode.id);
				var	strName	= currentNodeList[i].getAttribute("name");
				treeHTML += '<div class="Node" onclick="eXo.ecm.ECS.actionColExp(this);">';
				treeHTML += 	'<div class="ExpandIcon">';
				treeHTML +=			'<a title="'+strName+'" class="NodeIcon DefaultPageIcon" href="javascript:void(0);" onclick="eXo.ecm.ECS.getDir(this, event);" name="'+strName+'" id="'+id+'">';
				treeHTML +=				strName;	
				treeHTML += 		'</a>';
				treeHTML +=		'</div>';
				treeHTML +=	'</div>';
			}
			var parentNode = eXo.core.DOMUtil.findAncestorByClass(currentNode, "Node");
			var nodeIcon = eXo.core.DOMUtil.findAncestorByTagName(currentNode, "div");
			var nextElementNode = eXo.core.DOMUtil.findNextElementByTagName(parentNode, "div");
			var tmpNode = document.createElement("div");
			tmpNode.className = "ChildrenContainer" ;
			tmpNode.innerHTML = treeHTML;
			if(nextElementNode && nextElementNode.className == "Node") {
				nextElementNode.parentNode.insertBefore(tmpNode, nextElementNode) ;
				nodeIcon.className = 'CollapseIcon';				
				tmpNode.style.display = "block";
			} else if(nextElementNode && nextElementNode.className == "ChildrenContainer"){
				eXo.ecm.ECS.actionColExp(parentNode);
				if(nodeIcon) nodeIcon.className = 'ExpandIcon';
			} else {
				var cldrContainer = eXo.core.DOMUtil.findAncestorByClass(currentNode, "ChildrenContainer");
				nodeIcon.className = 'CollapseIcon';
				cldrContainer.appendChild(tmpNode);
			}
		}
	}
	
	if(eXo.ecm.ECS.typeObj == "folder") {
		eXo.ecm.ECS.listFolders(folderList);
	} else if(eXo.ecm.ECS.typeObj == "multi"){
		eXo.ecm.ECS.listMutilFiles(fileList);
	} else {
		eXo.ecm.ECS.listFiles(fileList);
	}
};

EcmContentSelector.prototype.actionColExp = function(objNode) {
	if(!objNode) return;
	var nextElt = eXo.core.DOMUtil.findNextElementByTagName(objNode, "div");
	var iconElt = eXo.core.DOMUtil.getChildrenByTagName(objNode, "div")[0];
	if(!nextElt || nextElt.className != "ChildrenContainer") return;
	if(nextElt.style.display != 'block') {
		nextElt.style.display = 'block';
		iconElt.className = 'CollapseIcon';
	} else {
		nextElt.style.display = 'none';
		iconElt.className = 'ExpandIcon';
	}
};

EcmContentSelector.prototype.renderBreadcrumbs = function(currentNode) {
	if(!currentNode) return;
	if(typeof(currentNode) == 'string') currentNode = document.getElementById(currentNode);
	eXo.ecm.ECS.currentNode = currentNode;
	var breadcrumbContainer = document.getElementById("BreadcumbsContainer");
	breadcrumbContainer.innerHTML = '';
	var beforeNode = null;
	while(currentNode.className != "LeftWorkspace") {
		var curName = currentNode.getAttribute('name');
		if(curName) {
			var tmpNode = document.createElement("div");	
			tmpNode.className = 'BreadcumbTab';
			var strHTML = '';
			var strOnclick = '';
			var node = document.getElementById(currentNode.id);
			if(node) strOnclick = "eXo.ecm.ECS.actionBreadcrumbs('"+node.id+"')";		
			if(beforeNode == null) {
				strHTML += '<a class="Nomal" href="javascript:void(0);" onclick="'+strOnclick+'">'+curName+'</a>';
				tmpNode.innerHTML = strHTML;
				breadcrumbContainer.appendChild(tmpNode);
			} else {
				strHTML += '<a class="Nomal" href="javascript:void(0);" onclick="'+strOnclick+'">'+curName+'</a>';
				strHTML += '<div class="RightArrowIcon"><span></span></div>';
				tmpNode.innerHTML = strHTML;
				breadcrumbContainer.insertBefore(tmpNode, beforeNode);
			}
			beforeNode = tmpNode;
		}
		currentNode = currentNode.parentNode;
		if(currentNode != null && currentNode.className == 'ChildrenContainer'){
			currentNode = eXo.core.DOMUtil.findPreviousElementByTagName(currentNode, 'div');
			currentNode = currentNode.getElementsByTagName('div')[0].getElementsByTagName('a')[0];
		}
	}
};

EcmContentSelector.prototype.actionBreadcrumbs = function(nodeId) {
	var ECS = eXo.ecm.ECS;
	var element = document.getElementById(nodeId);
	var node =  eXo.core.DOMUtil.findAncestorByClass(element, "Node");
	eXo.ecm.ECS.actionColExp(node);
	eXo.ecm.ECS.renderBreadcrumbs(element);
	var currentFolder;
	var currentNode = element;
	var driverName;
	if(currentNode.getAttribute("driverPath")) {
		driverName =	currentNode.getAttribute('name');
		eXo.ecm.ECS.driverName = driverName;
		currentFolder = '/';	
	} else {
		currentFolder = element.getAttribute("currentfolder");
	}
	
	eXo.ecm.ECS.currentFolder = currentFolder;
	eXo.ecm.ECS.currentNode = currentNode;
	driverName = eXo.ecm.ECS.driverName;
	var filter = '';
	var dropdownlist = document.getElementById("Filter");
	if(dropdownlist) filter = dropdownlist.options[dropdownlist.selectedIndex].value;
	else filter = 'Web Contents';
	var command = ECS.cmdEcmDriver+ECS.cmdGetFolderAndFile+"driverName="+driverName+"&currentFolder="+currentFolder+"&currentPortal="+ECS.portalName+"&repositoryName="+ECS.repositoryName+"&workspaceName="+ECS.workspaceName;
	var url = ECS.hostName + ECS.connector+command+"&userId=" + ECS.userId+"&filterBy="+filter;
	if(eXo.ecm.ECS.strConnection == url) return;	
	eXo.ecm.ECS.strConnection = url;
	var xmlDoc = eXo.ecm.WCMUtils.request(url);
	if(!xmlDoc) return;
	if(eXo.ecm.ECS.typeObj == "folder") {
		var folderList = xmlDoc.getElementsByTagName("Folder");
		eXo.ecm.ECS.listFolders(folderList);
	} else if(eXo.ecm.ECS.typeObj == "multi") {
		var fileList = xmlDoc.getElementsByTagName("File");
		eXo.ecm.ECS.listMutilFiles(fileList);	
	} else {
		var fileList = xmlDoc.getElementsByTagName("File");
		eXo.ecm.ECS.listFiles(fileList);
	} 
};

EcmContentSelector.prototype.listFiles = function(list) {
	var rightWS = document.getElementById('RightWorkspace');
	var tblRWS  = eXo.core.DOMUtil.findDescendantsByTagName(rightWS, "table")[0];
	var rowsRWS = eXo.core.DOMUtil.findDescendantsByTagName(tblRWS, "tr");
	if(rowsRWS && rowsRWS.length > 0) {
		for(var i = 0; i < rowsRWS.length; i++) {
			if(i > 0) tblRWS.deleteRow(rowsRWS[i].rowIndex);
		}
	} 
	if(!list || list.length <= 0) {
		var tdNoContent = tblRWS.insertRow(1).insertCell(0);
		tdNoContent.innerHTML = "There is no content";
		tdNoContent.className = "Item TRNoContent";
		document.getElementById("pageNavPosition").innerHTML = "";
		return;
	}
	var listItem = '';
	var clazz = 'OddItem';
	for(var i = 0; i < list.length; i++) {
		if(clazz == 'EventItem') {
			clazz = 'OddItem';
		} else if(clazz == 'OddItem') {
			clazz = 'EventItem';
		}
		var clazzItem = eXo.ecm.ECS.getClazzIcon(list[i].getAttribute("nodeType"));
		var url 			= list[i].getAttribute("url");
		var path 			= list[i].getAttribute("path");
		var nodeType	= list[i].getAttribute("nodeType");
		var node = list[i].getAttribute("name");
		var newRow = tblRWS.insertRow(i+1);
		newRow.className = clazz;
		newRow.insertCell(0).innerHTML = '<a class="Item '+clazzItem+'" url="'+url+'" path="'+path+'" nodeType="'+nodeType+'" onclick="eXo.ecm.ECS.insertContent(this);">'+node+'</a>';
		
		if(i > 13) {
			var numberRecords = 0;
			if(eXo.core.Browser.isFF()) numberRecords = 14;
			else numberRecords = 13;
			eXo.ecm.Pager = new Pager("ListRecords", numberRecords);
			eXo.ecm.Pager.init(); 
			eXo.ecm.Pager.showPageNav('pageNavPosition');
			eXo.ecm.Pager.showPage(1);	
		} else {
			document.getElementById("pageNavPosition").innerHTML = "";
		}
	}
};

EcmContentSelector.prototype.listFolders = function(list) {
	var rightWS = document.getElementById('RightWorkspace');
	var tblRWS  = eXo.core.DOMUtil.findDescendantsByTagName(rightWS, "table")[0];
	var rowsRWS = eXo.core.DOMUtil.findDescendantsByTagName(tblRWS, "tr");
	if(rowsRWS && rowsRWS.length > 0) {
		for(var i = 0; i < rowsRWS.length; i++) {
			if(i > 0) tblRWS.deleteRow(rowsRWS[i].rowIndex);
		}
	} 
	if(!list || list.length <= 0) {
		var tdNoContent = tblRWS.insertRow(1).insertCell(0);
		tdNoContent.innerHTML = "There is no content";
		tdNoContent.className = "Item TRNoContent";
		document.getElementById("pageNavPosition").innerHTML = "";
		return;
	}
	var listItem = '';
	var clazz = 'OddItem';
	for(var i = 0; i < list.length; i++) {
		if(clazz == 'EventItem') {
			clazz = 'OddItem';
		} else if(clazz == 'OddItem') {
			clazz = 'EventItem';
		}
		var clazzItem = eXo.ecm.ECS.getClazzIcon(list[i].getAttribute("nodeType"));
		var url 			= list[i].getAttribute("url");
		var path 			= list[i].getAttribute("path");
		var nodeType	= list[i].getAttribute("folderType");
		var node = list[i].getAttribute("name");
		var newRow = tblRWS.insertRow(i+1);
		newRow.className = clazz;
		newRow.insertCell(0).innerHTML = '<a class="Item '+clazzItem+'" url="'+url+'" path="'+path+'" nodeType="'+nodeType+'" onclick="eXo.ecm.ECS.insertContent(this);">'+node+'</a>';
		
		if(i > 13) {
			var numberRecords = 0;
			if(eXo.core.Browser.isFF()) numberRecords = 14;
			else numberRecords = 13;
			eXo.ecm.Pager = new Pager("ListRecords", numberRecords);
			eXo.ecm.Pager.init(); 
			eXo.ecm.Pager.showPageNav('pageNavPosition');
			eXo.ecm.Pager.showPage(1);	
		} else {
			document.getElementById("pageNavPosition").innerHTML = "";
		}
	}
};

EcmContentSelector.prototype.listMutilFiles = function(list) {
	var rightWS = document.getElementById('RightWorkspace');
	var tblRWS  = eXo.core.DOMUtil.findDescendantsByTagName(rightWS, "table")[0];
	var rowsRWS = eXo.core.DOMUtil.findDescendantsByTagName(tblRWS, "tr");
	if(rowsRWS && rowsRWS.length > 0) {
		for(var i = 0; i < rowsRWS.length; i++) {
			if(i > 0) tblRWS.deleteRow(rowsRWS[i].rowIndex);
		}
	} 
	if(!list || list.length <= 0) {
		var rowTmp = tblRWS.insertRow(1);
		var tdNoContent = rowTmp.insertCell(0);
		tdNoContent.innerHTML = "There is no content";
		tdNoContent.className = "Item TRNoContent";
		document.getElementById("pageNavPosition").innerHTML = "";
		return;
	}
	var listItem = '';
	var clazz = 'OddItem';
	for(var i = 0; i < list.length; i++) {
		if(clazz == 'EventItem') {
			clazz = 'OddItem';
		} else if(clazz == 'OddItem') {
			clazz = 'EventItem';
		}
		var clazzItem = eXo.ecm.ECS.getClazzIcon(list[i].getAttribute("nodeType"));
		var url 			= list[i].getAttribute("url");
		var path 			= eXo.ecm.ECS.repositoryName+":"+eXo.ecm.ECS.workspaceName+":"+list[i].getAttribute("path");
		var nodeType	= list[i].getAttribute("nodeType");
		var node = list[i].getAttribute("name");
		var newRow = tblRWS.insertRow(i+1);
		newRow.className = clazz;
		newRow.insertCell(0).innerHTML = '<a class="Item '+clazzItem+'" url="'+url+'" path="'+path+'" nodeType="'+nodeType+'" onclick="eXo.ecm.ECS.addFile2ListContent(this);">'+node+'</a>';
		
		if(i > 13) {
			var numberRecords = 0;
			if(eXo.core.Browser.isFF()) numberRecords = 14;
			else numberRecords = 13;
			eXo.ecm.Pager = new Pager("ListRecords", numberRecords);
			eXo.ecm.Pager.init(); 
			eXo.ecm.Pager.showPageNav('pageNavPosition');
			eXo.ecm.Pager.showPage(1);	
		} else {
			document.getElementById("pageNavPosition").innerHTML = "";
		}
	}
};

EcmContentSelector.prototype.getClazzIcon = function(nodeType) {
	if(!nodeType) return;
	var strClassIcon = '';
	strClassIcon = nodeType.replace("/", "_").replace(":", "_") + "16x16Icon";
	return strClassIcon;
};

function Pager(objTable, itemsPerPage) {
	this.tableName = objTable;
    this.itemsPerPage = itemsPerPage;
    this.currentPage = 1;
    this.pages = 0;
    this.inited = false;
}

Pager.prototype.setHeightRightWS = function(list) {
	var leftWorkSpace = document.getElementById("LeftWorkspace");
	var rightWorkSpace = document.getElementById("RightWorkspace");
	if(leftWorkSpace) rightWorkSpace.style.height = leftWorkSpace.offsetHeight + "px";
};

Pager.prototype.init = function() {
	this.setHeightRightWS();
	var len = 0;
	var table = document.getElementById(eXo.ecm.Pager.tableName);
	if(eXo.core.Browser.isFF()) {
		var tHead = eXo.core.DOMUtil.getChildrenByTagName(table, "thead")[0];
		var rowsTHead = eXo.core.DOMUtil.getChildrenByTagName(tHead, "tr");
		len = rowsTHead.length;
	}	else {
		var tBody = eXo.core.DOMUtil.getChildrenByTagName(table, "tbody")[0];
		len = tBody.childNodes.length -1;
	}
    var records = len; 
    this.pages = Math.ceil(records / eXo.ecm.Pager.itemsPerPage);
	if(this.pages < 0) this.page =1;
    this.inited = true;
};

Pager.prototype.showRecords = function(from, to) {
	var rows = null;
	var table = document.getElementById(eXo.ecm.Pager.tableName);
	if(eXo.core.Browser.isFF()) {
		var tHead = eXo.core.DOMUtil.getChildrenByTagName(table, "thead")[0];
		rows = eXo.core.DOMUtil.getChildrenByTagName(tHead ,"tr");
	}	else {
		var tBody = eXo.core.DOMUtil.getChildrenByTagName(table, "tbody")[0];
		rows =tBody.childNodes;
	}
	var len = rows.length;
	if(len <= 14) { document.getElementById("pageNavPosition").innerHTML = "";	return;}

	// i starts from 1 to skip table header row

	for (var i = 1; i < len; i++) {
		if (i < from || i > to)  {
		    rows[i].style.display = 'none';
		} else {
		    rows[i].style.display = '';
		}
	}
};

Pager.prototype.showPage = function(pageNumber) {
	if (! this.inited) {
		alert("not inited");
		return;
    }

    var oldPageAnchor = document.getElementById('pg'+eXo.ecm.Pager.currentPage);
    if(oldPageAnchor) oldPageAnchor.className = 'pg-normal';
    
    this.currentPage = pageNumber;
    var newPageAnchor = document.getElementById('pg'+eXo.ecm.Pager.currentPage);
	if(newPageAnchor)  newPageAnchor.className = 'pg-selected';
    
    var from = (pageNumber - 1) * eXo.ecm.Pager.itemsPerPage + 1;
    var to = from +  eXo.ecm.Pager.itemsPerPage - 1;
    eXo.ecm.Pager.showRecords(from, to);
};

Pager.prototype.previousPage = function() {
	if (this.currentPage > 1)  eXo.ecm.Pager.showPage(this.currentPage - 1);
};

Pager.prototype.nextPage = function() {
	if (this.currentPage < this.pages) eXo.ecm.Pager.showPage(this.currentPage + 1);
};

Pager.prototype.showPageNav = function(positionId) {
	if (! this.inited) {
		alert("not inited");
		return;
	}
	var element = document.getElementById(positionId);
	var pagerHtml = '<span>Total page : '+this.pages+'</span> ';
	pagerHtml += '<span onclick="eXo.ecm.Pager.previousPage();" class="pg-normal"> &#171 Prev </span> | ';
    for (var page = 1; page <= this.pages; page++) {
        pagerHtml += '<span id="pg' + page + '" class="pg-normal" onclick="eXo.ecm.Pager.showPage(' + page + ');">' + page + '</span> | ';
    }
	pagerHtml += '<span onclick="eXo.ecm.Pager.nextPage();" class="pg-normal"> Next &#187;</span>';            
    element.innerHTML = pagerHtml;
};

EcmContentSelector.prototype.insertContent = function(objNode) {
	if(!objNode) return;
	var rws = document.getElementById("RightWorkspace");
	if(eXo.ecm.ECS.typeObj == "folder") {
		var action = rws.getAttribute("action");
		action = action.substring(0, action.length - 2);
		action += '&objectId=' + eXo.ecm.ECS.repositoryName + ":" + eXo.ecm.ECS.workspaceName + ":" + objNode.getAttribute("path") + '\')';
		eval(action);
	} else if(eXo.ecm.ECS.typeObj == "one") {
		
		// need to improve...
		var path = nodeContent.getAttribute("path");
		
	} if(eXo.ecm.ECS.typeObj == "fck") {
		if(!objContent) return;
		var hostName = eXoPlugin.hostName;
		var nodeType = objContent.getAttribute('nodeType');
		var url 	= objContent.getAttribute('url');
		var name 	= objContent.innerHTML;
		var strHTML = '';	
		if(window.opener.document.getElementById(getParameterValueByName("browserType"))){		
			strHTML += url;		
			window.opener.document.getElementById(getParameterValueByName("browserType")).value=strHTML;
		} else {
			if(nodeType.indexOf("image") >=0) {
				strHTML += "<img src='"+url+"' name='"+name+"' alt='"+name+"'/>";
			} else {
				strHTML += "<a href='" + url+"' style='text-decoration:none;'>"+name+"</a>";		
			}
			FCK.InsertHtml(strHTML);
		}			
		FCK.OnAfterSetHTML = window.close();
	}
};

EcmContentSelector.prototype.insertMultiContent = function() {
	var rws = document.getElementById("RightWorkspace");
	var tblContent = document.getElementById("ListFilesContent");
	var rowsContent = eXo.core.DOMUtil.findDescendantsByTagName(tblContent, "tr");
	var strContent = "";
	for(var i = 0; i < rowsContent.length; i++) {
		var nodeContent = eXo.core.DOMUtil.findFirstDescendantByClass(rowsContent[i], "a", "Item");
		if(nodeContent) {
			var path = nodeContent.getAttribute("path");
			strContent +=  path+";";
		}
	}
	var action = rws.getAttribute("action");
	action = action.substring(0, action.length - 2);
	action += '&objectId=' + strContent + '\')';
	eval(action);
};

EcmContentSelector.prototype.addFile2ListContent = function(objNode) {
	var tblListFilesContent = document.getElementById("ListFilesContent");
	var rowsContent = eXo.core.DOMUtil.findDescendantsByTagName(tblListFilesContent, "tr");
	var trNoContent = eXo.core.DOMUtil.findFirstDescendantByClass(tblListFilesContent, "td", "TRNoContent");
	if(trNoContent) tblListFilesContent.deleteRow(trNoContent.parentNode.rowIndex);
	var url = objNode.getAttribute("url");
	var nodeType	= objNode.getAttribute("nodeType");
	var node = objNode.innerHTML;
	var path = objNode.getAttribute("path");
	var selectedNodeList = eXo.core.DOMUtil.findDescendantsByClass(tblListFilesContent, "a", "Item");
	for(var i = 0; i < selectedNodeList.length; i++) {
		var selectedNodePath = selectedNodeList[i].getAttribute("path");
		if(path == selectedNodePath) {
			alert("Sorry, this content is already in the list content.");
			return;
		}
	} 
	var	clazzItem = objNode.className;
	var newRow = tblListFilesContent.insertRow(1);
	newRow.className = "Item";
	newRow.insertCell(0).innerHTML = '<a class="Item" url="'+url+'" path="'+path+'" nodeType="'+nodeType+'">'+node+'</a>';
	newRow.insertCell(1).innerHTML = '<div class="DeleteIcon" onclick="eXo.ecm.ECS.removeContent(this);"><span></span></div>';	
};

EcmContentSelector.prototype.addFileSearchListSearch = function() {
};

EcmContentSelector.prototype.loadListContent = function(strArray) {
	if(!strArray) return;
	var tblListFilesContent = document.getElementById("ListFilesContent");
	var arrContent = strArray.split(";");
	if(arrContent.length > 0) {
		var trNoContent = eXo.core.DOMUtil.findFirstDescendantByClass(tblListFilesContent, "td", "TRNoContent");
		if(trNoContent) tblListFilesContent.deleteRow(trNoContent.parentNode.rowIndex);
		var clazz = 'OddItem';
		for(var i = 0; i < arrContent.length-1; i++) {
			var path = arrContent[i];
			var newRow = tblListFilesContent.insertRow(1);
			if(clazz == 'EventItem') {
				clazz = 'OddItem';
			} else if(clazz == 'OddItem') {
				clazz = 'EventItem';
			}
			newRow.className = clazz;
			var strTmpArr = arrContent[i].split('/');
			var nodeName = strTmpArr[strTmpArr.length-1];
			newRow.insertCell(0).innerHTML = '<a class="Item" path="'+path+'">'+nodeName+'</a>';
			newRow.insertCell(1).innerHTML = '<div  class="DeleteIcon" onclick="eXo.ecm.ECS.removeContent(this);"><span></span></div>';
		}
	}
};

EcmContentSelector.prototype.removeContent = function(objNode) {
	var tblListFilesContent = document.getElementById("ListFilesContent"); 
	var objRow = eXo.core.DOMUtil.findAncestorByTagName(objNode, "tr");
	tblListFilesContent.deleteRow(objRow.rowIndex);	
	eXo.ecm.ECS.pathContent = false;
}

EcmContentSelector.prototype.changeFilter = function() {
	var rightWS = document.getElementById('RightWorkspace');
	var tblRWS	= eXo.core.DOMUtil.findDescendantsByTagName(rightWS, "table")[0];
	var rowsRWS = eXo.core.DOMUtil.findDescendantsByTagName(tblRWS, "tr");
	if(rowsRWS && rowsRWS.length > 0) {
		for(var i = 0; i < rowsRWS.length; i++) {
			if(i > 0) tblRWS.deleteRow(rowsRWS[i].rowIndex);
		}
	} 
	
	if(eXo.ecm.ECS.typeObj == "folder") {
		eXo.ecm.ECS.listFolders();
	} else if(eXo.ecm.ECS.typeObj == "multi") {
		eXo.ecm.ECS.listMutilFiles();
	} else {
		eXo.ecm.ECS.listFiles();
	}
	
	if(eXo.ecm.ECS.currentNode)	 eXo.ecm.ECS.getDir(eXo.ecm.ECS.currentNode, eXo.ecm.ECS.eventNode);
};

EcmContentSelector.prototype.generateIdDriver = function(objNode) {
	if(!objNode) return;
	var id = '';
	while(objNode.tagName != 'Connector') {
		var curName = objNode.getAttribute("name").replace(" ", "");
		id =  curName+"_"+id;
		objNode = objNode.parentNode;
	}
	return id;
};

EcmContentSelector.prototype.generateIdNodes = function(objNode, idNode) {
	if(!objNode && !idNode) return;
	var id = '';
	while(objNode.tagName != 'Folders') {
		var curName = objNode.getAttribute("name").replace(" ", "");
		id =  idNode+"_"+curName;
		objNode = objNode.parentNode;
	}
	return id;
};

EcmContentSelector.prototype.fixHeightTrees = function() {
	var leftWS = document.getElementById('LeftWorkspace');
	var windowHeight = eXo.core.Browser.getBrowserHeight();
	var root = eXo.core.DOMUtil.findAncestorByClass(leftWS, "UIHomePageDT");
	var titleBar = eXo.core.DOMUtil.findFirstDescendantByClass(root, "div", "TitleBar");
	var uiWorkingWorkspace = eXo.core.DOMUtil.findFirstDescendantByClass(root, "div", "UIWorkingWorkspace");
	var actionBar = eXo.core.DOMUtil.findFirstDescendantByClass(uiWorkingWorkspace, "div", "ActionBar");
	var breadcumbsPortlet = eXo.core.DOMUtil.findFirstDescendantByClass(uiWorkingWorkspace, "div", "BreadcumbsPortlet");
	leftWS.style.height = windowHeight - (titleBar.offsetHeight + actionBar.offsetHeight + breadcumbsPortlet.offsetHeight + 55) + "px";
};

EcmContentSelector.prototype.isShowFilter = function() {
	var selectFilter = document.getElementById("Filter");
	var filterContainer = eXo.core.DOMUtil.findAncestorByClass(selectFilter, "ActionBar");
	if(eXo.ecm.ECS.typeObj == "folder") {
		filterContainer.style.display = "none";
	} 
};

eXo.ecm.ECS = new EcmContentSelector();