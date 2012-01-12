function EcmContentSelector() {	
	this.portalName = eXo.env.portal.portalName;
	this.context = eXo.env.portal.context;
	this.accessMode = eXo.env.portal.accessMode;
	this.userLanguage = eXo.env.portal.language;
	this.userId = eXo.env.portal.userName;
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
	this.currentFolder = "";
	this.xmlHttpRequest = false;
	this.driverName = "";
	this.eventNode = false;
	this.uploadFile = "uploadFile/upload";
	this.controlUpload = "uploadFile/control";
	this.initDriverExpanded ="";
	this.initPathExpanded ="";
	this.initComponentIdExpanded ="";
	this.deleteConfirmationMsg="";
  this.switchView = false;
  if(this.viewType==undefined)
		this.viewType="list";
}
EcmContentSelector.prototype.setDeleteConfirmationMessage = function(msg) {
	this.deleteConfirmationMsg=msg;
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
       if (eXo.ecm.ECS.initDriverExpanded!=null && eXo.ecm.ECS.initDriverExpanded.length>0) {          
         eXo.ecm.ECS.waitAndInitPath(eXo.ecm.ECS.initDriverExpanded, eXo.ecm.ECS.initPathExpanded, eXo.ecm.ECS.initComponentIdExpanded);
       }
    } else {
        alert("There was a problem retrieving the XML data:\n" + eXo.ecm.ECS.xmlHttpRequest.statusText);
        return false;
    }
  }
};

EcmContentSelector.prototype.initRequestXmlTree = function(typeObj, iDriver, iPath, iID) {
  this.initDriverExpanded =iDriver;
  this.initPathExpanded =iPath;
  this.initComponentIdExpanded =iID;
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
		case "editor" :
			eXo.ecm.ECS.typeObj = "editor";
			break;
		default :
			eXo.ecm.ECS.typeObj = false;
			break;
	}	
	eXo.ecm.ECS.isShowFilter();
	var ECS = eXo.ecm.ECS;  
	var command = ECS.cmdEcmDriver+ECS.cmdGetDriver + "lang=" + ECS.userLanguage;
	var url = ECS.hostName+ECS.connector+ command ;    
	eXo.ecm.ECS.ajaxRequest(url);	
};

EcmContentSelector.prototype.buildECSTreeView = function() {
	var xmlTreeNodes = eXo.ecm.ECS.xmlHttpRequest.responseXML;
	var treeHTML = '';
	var nodeList = xmlTreeNodes.getElementsByTagName("Folders");
	for(var i = 0 ; i < nodeList.length; i++)	 {
		var strName = nodeList[i].getAttribute("name") ;
		var id = eXo.ecm.ECS.generateIdDriver(nodeList[i]);
		var isUpload = nodeList[i].getAttribute("isUpload");
		var nodeDriveName = nodeList[i].getAttribute("nodeDriveName");
		if (nodeDriveName==null) nodeDriveName='';

		treeHTML += '<div class="Node" onclick="eXo.ecm.ECS.actionColExp(this);">';
		treeHTML += 	'<div class="ExpandIcon">';		
		treeHTML += 		'<a title="'+decodeURIComponent(strName)+'"href="javascript:void(0);" class="NodeIcon DefaultPageIcon" onclick="eXo.ecm.ECS.renderBreadcrumbs(this);eXo.ecm.ECS.listRootFolder(this);" name="'+decodeURIComponent(strName)+'" id="'+id+'" isUpload="'+isUpload +'" nodeDriveName="' + nodeDriveName +'">';
		treeHTML +=				decodeURIComponent(strName);	
		treeHTML +=			'</a>';
		treeHTML += 	'</div>';			
		treeHTML += '</div>';			
		var tmp = eXo.ecm.ECS.renderSubTree(nodeList[i]);
		if(tmp != '') treeHTML += tmp;
	}
	var uiLeftWorkspace = document.getElementById('LeftWorkspace');	
	if(uiLeftWorkspace) uiLeftWorkspace.innerHTML = treeHTML;
	var contentSelectorPop = document.getElementById('CorrectContentSelectorPopupWindow');
  if(contentSelectorPop) {
		var contentBlock = eXo.core.DOMUtil.findFirstDescendantByClass(contentSelectorPop, 'div' ,'PopupContent');
		if(contentBlock) contentBlock.style.height="";
	}
};

EcmContentSelector.prototype.getDir = function(currentNode, event) {  
	var ECS = eXo.ecm.ECS;
	if (event)
		eXo.ecm.ECS.eventNode = event;
	var repoName = currentNode.getAttribute("repository");
	if(repoName) eXo.ecm.ECS.repositoryName = repoName;
	var wsName	= currentNode.getAttribute("workspace");
	if(wsName) eXo.ecm.ECS.workspaceName =  wsName;
	var connector = ECS.connector;
	var currentFolder;
	var driverName;	
	var driverPath = currentNode.getAttribute("driverPath");
	var nodeDriveName = currentNode.getAttribute("nodedrivename");  
	if(driverPath && driverPath != "") {		
		driverName =	currentNode.getAttribute('name');
		eXo.ecm.ECS.driverName = driverName;
		currentFolder = '';		
		eXo.ecm.ECS.showUpload();	
	} else if(currentNode.getAttribute('isUpload')) {
		
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


			eXo.ecm.ECS.showUpload();	
	}	
	
	eXo.ecm.ECS.currentFolder = currentFolder;
	eXo.ecm.ECS.currentNode = currentNode;
	driverName = eXo.ecm.ECS.driverName;
	var filter = '';
	if (nodeDriveName!=null && nodeDriveName.length>0 ) {
		eXo.ecm.ECS.driverName = driverName =nodeDriveName;
	}

	var dropdownlist = document.getElementById("Filter");	
  if(dropdownlist) {
		if(dropdownlist.type=="hidden") filter = dropdownlist.value;		
		else filter = dropdownlist.options[dropdownlist.selectedIndex].value;
	}	else filter = 'All';

	var command = ECS.cmdEcmDriver+ECS.cmdGetFolderAndFile+"driverName="+driverName+"&currentFolder="+encodeURIComponent(currentFolder)+"&currentPortal="+ECS.portalName+"&repositoryName="+ECS.repositoryName+"&workspaceName="+ECS.workspaceName;
	var url = ECS.hostName + ECS.connector+command+"&filterBy="+filter;	
	//if(eXo.ecm.ECS.strConnection == url) return;	
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
			var isUpload = nodeList[i].getAttribute("isUpload");
			var label = nodeList[i].getAttribute("label");
			var nodeDriveName = nodeList[i].getAttribute("nodeDriveName");
			if (nodeDriveName==null) nodeDriveName='';			
			if (!label) label = strName;
			treeHTML += '<div class="Node" onclick="eXo.ecm.ECS.actionColExp(this);">';
			treeHTML += 	'<div class="ExpandIcon">';
			treeHTML +=			'<a title="'+decodeURIComponent(label)+'" href="javascript:void(0);" class="NodeIcon DefaultPageIcon" onclick="eXo.ecm.ECS.getDir(this, event);" name="'+decodeURIComponent(strName)+'" id="'+id+'"  driverPath="'+driverPath+'" repository="'+repository+'" workspace="'+workspace+'">';
			treeHTML +=				label;	
			treeHTML += 		'</a>';
			treeHTML +=		'</div>';
			treeHTML +=	'</div>';
		}
		treeHTML += '</div>';
	}
	return treeHTML;
};

EcmContentSelector.prototype.listRootFolder = function(rootNode) {  
	eXo.ecm.ECS.hideUpload();
	if(eXo.ecm.ECS.typeObj != 'folder') {
		if(eXo.ecm.ECS.typeObj == "multi"){
			eXo.ecm.ECS.listMutilFiles(null);
		} else {		    
			eXo.ecm.ECS.listFiles(null);
		}
		return;
	}	
	
	var rightWS = document.getElementById('RightWorkspace');
	var tblRWS  = eXo.core.DOMUtil.findDescendantsByTagName(rightWS, "table")[0];
	var rowsRWS = eXo.core.DOMUtil.findDescendantsByTagName(tblRWS, "tr");
	if(rowsRWS && rowsRWS.length > 0) {
		for(var i = 0; i < rowsRWS.length; i++) {
			if(i > 0) tblRWS.deleteRow(rowsRWS[i].rowIndex);
		}
	} 
	if(typeof(rootNode) == 'string') rootNode = document.getElementById(rootNode);
	var nodeName = rootNode.getAttribute("name");
	var nodeOnBreadcrumb = document.getElementById(rootNode.getAttribute("id"));
	var ECS = eXo.ecm.ECS;
	var command = ECS.cmdEcmDriver+ECS.cmdGetDriver + "lang=" + ECS.userLanguage;
	var url = ECS.hostName+ECS.connector+ command;
	var xmlTreeNodes = eXo.ecm.WCMUtils.request(url);
	var nodeList = xmlTreeNodes.getElementsByTagName("Folders");
	for(var i = 0; i < nodeList.length; i++) {
		if(nodeList[i].getAttribute("name") == nodeName) {
			var listFolder = nodeList[i].childNodes;
			eXo.ecm.ECS.listFolders(listFolder);
		}
	}
};

EcmContentSelector.prototype.renderSubTrees = function(currentNode, event, connector) {  
	var event = event || window.event;
	if (event)
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
			var isUpload = nodeList[i].getAttribute("isUpload");
			var nodeDriveName = nodeList[i].getAttribute("nodeDriveName");
			if (nodeDriveName==null) nodeDriveName='';
			treeHTML += '<div class="Node" onclick="eXo.ecm.ECS.actionColExp(this);">';
			treeHTML += 	'<div class="ExpandIcon">';
			treeHTML +=			'<a title="'+ decodeURIComponent(strName) +'" class="NodeIcon DefaultPageIcon" href="javascript:void(0);" onclick="eXo.ecm.ECS.getDir(this, event);" name="'+decodeURIComponent(strName)+'" id="'+id+'" isUpload="'+isUpload +'" nodeDriveName="' + nodeDriveName +'">';
			treeHTML +=				decodeURIComponent(strName);	
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
				var isUpload = currentNodeList[i].getAttribute("isUpload");
				var nodeDriveName = currentNodeList[i].getAttribute("nodeDriveName");
				if (nodeDriveName==null) nodeDriveName='';
				treeHTML += '<div class="Node" onclick="eXo.ecm.ECS.actionColExp(this);">';
				treeHTML += 	'<div class="ExpandIcon">';
				treeHTML +=			'<a title="'+decodeURIComponent(strName)+'" class="NodeIcon DefaultPageIcon" href="javascript:void(0);" onclick="eXo.ecm.ECS.getDir(this, event);" name="'+decodeURIComponent(strName)+'" id="'+id+'" isUpload="'+isUpload +'" nodeDriveName="' + nodeDriveName +'">';
				treeHTML +=				decodeURIComponent(strName);	
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
	if(!nextElt || nextElt.className != "ChildrenContainer") {		
		var currentNode	= eXo.core.DOMUtil.findFirstDescendantByClass(objNode,"a","NodeIcon");			
		if (currentNode != null) {      
			eXo.ecm.ECS.getDir(currentNode, false);
		} else return;
	}
	if(iconElt.className != 'CollapseIcon') {
		if (nextElt.className == "ChildrenContainer") {
			nextElt.style.display = 'block';	
		}
		iconElt.className = 'CollapseIcon';
	} else if(!eXo.ecm.ECS.switchView) {
		eXo.ecm.ECS.switchView = false;
		if (nextElt.className == "ChildrenContainer") {
			nextElt.style.display = 'none';
		}
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
		var label = currentNode.getAttribute('title');
		if(curName) {
			var tmpNode = document.createElement("div");	
			tmpNode.className = 'BreadcumbTab';
			var strHTML = '';
			var strOnclick = '';
			var node = document.getElementById(currentNode.id);
			if(!node.getAttribute("driverPath") && !node.getAttribute("currentfolder")) {
				strOnclick = "eXo.ecm.ECS.actionBreadcrumbs('"+node.id+"');eXo.ecm.ECS.listRootFolder('"+node.id+"');";		
			} else {
				strOnclick = "eXo.ecm.ECS.actionBreadcrumbs('"+node.id+"');";		
			}
			if(beforeNode == null) {
				strHTML += '<a class="Nomal" href="javascript:void(0);" onclick="'+strOnclick+'">'+decodeURIComponent(label)+'</a>';
				tmpNode.innerHTML = strHTML;
				breadcrumbContainer.appendChild(tmpNode);
			} else {
				strHTML += '<a class="Nomal" href="javascript:void(0);" onclick="'+strOnclick+'">'+decodeURIComponent(label)+'</a>';
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
		currentFolder = '';	
	} else if (element.getAttribute("currentfolder")){
		currentFolder = element.getAttribute("currentfolder");
	} else {
		return;
	}
	eXo.ecm.ECS.currentFolder = currentFolder;
	eXo.ecm.ECS.currentNode = currentNode;
	driverName = eXo.ecm.ECS.driverName;
	var filter = '';
	var dropdownlist = document.getElementById("Filter");
	if(dropdownlist) filter = dropdownlist.options[dropdownlist.selectedIndex].value;
	else filter = 'All';
	if(currentFolder == null) 
	{
		eXo.ecm.ECS.hideUpload();
		currentFolder = '';
	}
	var command = ECS.cmdEcmDriver+ECS.cmdGetFolderAndFile+"driverName="+driverName+"&currentFolder="+currentFolder+"&currentPortal="+ECS.portalName+"&repositoryName="+ECS.repositoryName+"&workspaceName="+ECS.workspaceName;
	var url = ECS.hostName + ECS.connector+command+"&filterBy="+filter;
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
  var ECS = eXo.ecm.ECS;
  //Get view type to display content  
  var viewType = eXo.ecm.ECS.viewType; 
  //view = document.getElementById("viewTypeID").value;    
	var rightWS = document.getElementById('RightWorkspace');  
	if(!list || list.length <= 0) {
		if(viewType=="list") {
			var tblRWS  = eXo.core.DOMUtil.findDescendantsByTagName(rightWS, "table")[0];
			var rowsRWS = eXo.core.DOMUtil.findDescendantsByTagName(tblRWS, "tr");
			if(rowsRWS && rowsRWS.length > 0) {
				for(var i = 0; i < rowsRWS.length; i++) {
					if(i > 0) tblRWS.deleteRow(rowsRWS[i].rowIndex);
				}
			} 
			var tdNoContent = tblRWS.insertRow(1).insertCell(0);
			tdNoContent.innerHTML = "There is no content";
			tdNoContent.className = "Item TRNoContent";
			tdNoContent.setAttribute("colspan",3);
			tdNoContent.userLanguage = "UserLanguage.NoContent";	
			document.getElementById("pageNavPosition").innerHTML = "";
		} else {
			var container = eXo.core.DOMUtil.findFirstDescendantByClass(rightWS,'div','ActionIconsContainer');
			container.innerHTML = "<div class=\"NoContent\" userLanguage=\"UserLanguage.NoContent\">There is no content</div>";
			document.getElementById("pageNavPosition").innerHTML = "";
		}
		return;
	} else {		
    
    if(viewType=="list") {
			var tblRWS  = eXo.core.DOMUtil.findDescendantsByTagName(rightWS, "table")[0];
			if(tblRWS) {
				var rowsRWS = eXo.core.DOMUtil.findDescendantsByTagName(tblRWS, "tr");
				if(rowsRWS && rowsRWS.length > 0) {
					for(var i = 0; i < rowsRWS.length; i++) {
						if(i > 0) tblRWS.deleteRow(rowsRWS[i].rowIndex);
					}
				} 
			} else eXo.ecm.ECS.updateHTML(viewType);			
		} else {
			var container = eXo.core.DOMUtil.findFirstDescendantByClass(rightWS,'div','ActionIconsContainer');
			if(container) container.innerHTML = "";
      else eXo.ecm.ECS.updateHTML(viewType);
		}			
		var listItem = '';
		for(var i = 0; i < list.length; i++) {      
			var url 			= list[i].getAttribute("url");
			url = encodeURIComponent(url);
			var path 			= list[i].getAttribute("path");
			var nodeType	= list[i].getAttribute("nodeType");
      var nodeTypeIcon = nodeType.replace(":", "_") + "48x48Icon default16x16Icon";
			var node = list[i].getAttribute("name");
			node = encodeURIComponent(node);
			var size = 	list[i].getAttribute("size");
			if(size == 0) size = "";
			else size += '&nbsp;kb';
      
      if(viewType=="list") {	        
				var clazz = 'OddItem';
        var tblRWS  = eXo.core.DOMUtil.findDescendantsByTagName(rightWS, "table")[0];
				var clazzItem = eXo.ecm.ECS.getClazzIcon(list[i].getAttribute("nodeType"));
				var newRow = tblRWS.insertRow(i+1);
				newRow.className = clazz;					
				newRow.insertCell(0).innerHTML = '<a class="Item default16x16Icon '+clazzItem+'" url="'+decodeURIComponent(url)+'" path="'+path+'" nodeType="'+nodeType+'" style = "overflow:hidden;" title="'+decodeURIComponent(node)+'" onclick="eXo.ecm.ECS.insertContent(this);">'+decodeURIComponent(node).trunc(15,false)+'</a>';
				newRow.insertCell(1).innerHTML = '<div class="Item">'+ list[i].getAttribute("dateCreated") +'</div>';
				newRow.insertCell(2).innerHTML = '<div class="Item">'+ size +'</div>';
			} else {				  
        var container = eXo.core.DOMUtil.findFirstDescendantByClass(rightWS,'div','ActionIconsContainer');			
				var strViewContent = "";
				var command = ECS.connector + "/thumbnailImage/medium/" + ECS.repositoryName + "/" + ECS.workspaceName + path + "/?reloadnum=" + Math.random();        
				strViewContent += '<div class="ActionIconBox" onclick="eXo.ecm.ECS.insertContent(this);" url="'+decodeURIComponent(url)+'" path="'+path+'" nodeType="'+nodeType+'" title="'+decodeURIComponent(node)+'"><div class="NodeLabel"><div class="ThumbnailImage"><div style="display: block;" class="LoadingProgressIcon"><img alt="Loading Process" src="'+command+'" onerror="var img = eXo.core.DOMUtil.findNextElementByTagName(this.parentNode,\'div\'); img.style.display = \'block\'; this.parentNode.style.display = \'none\';" onload="this.parentNode.style.backgroundImage=\'none\'" /></div><div style="display: none;" class="Icon48x48 default48x48Icon '+nodeTypeIcon+'"></div></div><div class="ActionIconLabel" style="width: auto;"><a class="ActionLabel" onclick="eXo.ecm.ECS.insertContent(this);" url="'+url+'" path="'+path+'" nodeType="'+nodeType+'" title="'+decodeURIComponent(node)+'">'+decodeURIComponent(node)+'</a></div></div>';	        
				container.innerHTML += strViewContent;
			}
		}			
	}	
	if(i > 12) {
		var numberRecords = 12;		
		var viewType = eXo.ecm.ECS.viewType; 
    if(viewType=='list') eXo.ecm.Pager = new Pager("ListRecords", numberRecords);
    else eXo.ecm.Pager = new Pager("ActionIconsContainer", numberRecords);
		eXo.ecm.Pager.init(); 
		eXo.ecm.Pager.showPageNav('pageNavPosition');
		eXo.ecm.Pager.showPage(1);	
	} else {
		document.getElementById("pageNavPosition").innerHTML = "";
	}	
};

EcmContentSelector.prototype.updateHTML = function(viewType) {
  var strViewPresent = "";
	if(viewType=="list") {
		strViewPresent = "<div class=\"ListView\"><table cellspacing=\"0\" style=\"table-layout: fixed; width: 100%;\" id=\"ListRecords\">";
		strViewPresent += "<thead><tr><th class=\"THBar\" userLanguage=\"UserLanguage.FileName\"> Name </th>";
		strViewPresent += "<th class=\"THBar\" style=\"width: 120px;\" userLanguage=\"UserLanguage.CreateDate\"> Date </th>";
		strViewPresent += "<th class=\"THBar\" style=\"width: 80px;\" userLanguage=\"UserLanguage.FileSize\"> Size </th></tr></thead>";
		strViewPresent += "</table></div>";
	} else {
		strViewPresent = "<div class=\"UIThumbnailsView\" style=\"overflow-y: auto; overflow-x: hidden;\"><div class=\"ActionIconsContainer\" id=\"ActionIconsContainer\"></div></div>";
	}
	var rightWS = document.getElementById('RightWorkspace');  
  if(rightWS) {
		rightWS.innerHTML = "";
		strViewPresent += "<div class=\"PageIterator\" id=\"pageNavPosition\"></div><div style=\"clear: left;\"><span></span></div>";
		rightWS.innerHTML = strViewPresent;
	}
}

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
		tdNoContent.userLanguage = "UserLanguage.NoContent";
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
		var label = list[i].getAttribute("label");
		if (!label) label = node;
		var newRow = tblRWS.insertRow(i+1);
		newRow.className = clazz;
		newRow.insertCell(0).innerHTML = '<a class="Item default16x16Icon '+clazzItem+'" url="'+url+'" path="'+path+'" nodeType="'+nodeType+'" onclick="eXo.ecm.ECS.insertContent(this);">'+decodeURIComponent(label)+'</a>';
				
	}
	
	if(i > 12) {
		var numberRecords = 12;
		eXo.ecm.Pager = new Pager("ListRecords", numberRecords);
		eXo.ecm.Pager.init(); 
		eXo.ecm.Pager.showPageNav('pageNavPosition');
		eXo.ecm.Pager.showPage(1);	
	} else {
		document.getElementById("pageNavPosition").innerHTML = "";
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
		tdNoContent.userLanguage = "UserLanguage.NoContent";
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
		var linkTarget = list[i].getAttribute("linkTarget");
		var nodeType	= list[i].getAttribute("nodeType");
		var node = list[i].getAttribute("name");
		var newRow = tblRWS.insertRow(i+1);
		newRow.className = clazz;
		newRow.insertCell(0).innerHTML = '<a class="Item default16x16Icon '+clazzItem+'" url="'+url+'" linkTarget ="' + linkTarget + '" path="'+path+'" nodeType="'+nodeType+'" style = "overflow:hidden;" title="'+decodeURIComponent(node)+'" onclick="eXo.ecm.ECS.addFile2ListContent(this);">'+decodeURIComponent(node)+'</a>';
				
	}
	
	if(i > 12) {
		var numberRecords = 12;
    eXo.ecm.Pager = new Pager("ListRecords", numberRecords);
		eXo.ecm.Pager.init(); 
		eXo.ecm.Pager.showPageNav('pageNavPosition');
		eXo.ecm.Pager.showPage(1);	
	} else {
		document.getElementById("pageNavPosition").innerHTML = "";
	}
};

EcmContentSelector.prototype.getClazzIcon = function(nodeType) {
	var strClassIcon = '';
	if(!nodeType) {
		strClassIcon = "DefaultPageIcon";	
		return strClassIcon;	
	}
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
  if(eXo.ecm.ECS.viewType=="list") {
		var table = document.getElementById(eXo.ecm.Pager.tableName);
		if(navigator.userAgent.indexOf("MSIE") >= 0) { //is IE
			var tBody = eXo.core.DOMUtil.getChildrenByTagName(table, "tbody")[0];
			len = tBody.childNodes.length;
		} else {
			var tHead = eXo.core.DOMUtil.getChildrenByTagName(table, "thead")[0];
			var rowsTHead = eXo.core.DOMUtil.getChildrenByTagName(tHead, "tr");
			len = rowsTHead.length - 1;		
		}
	} else {
		var icon_container = document.getElementById(eXo.ecm.Pager.tableName);    
    icons =  eXo.core.DOMUtil.findChildrenByClass(icon_container ,"div", "ActionIconBox");    
    len = icons.length;		
	}
  var records = len; 
  this.pages = Math.ceil(records / eXo.ecm.Pager.itemsPerPage);
	if(this.pages < 0) this.page =1;
    this.inited = true;
};

Pager.prototype.showRecords = function(from, to) {  
  if(eXo.ecm.ECS.viewType=="list") {
		var rows = null;
		var table = document.getElementById(eXo.ecm.Pager.tableName);
		var len = 0;
		if(navigator.userAgent.indexOf("MSIE") >= 0) { //is IE
			var tBody = eXo.core.DOMUtil.getChildrenByTagName(table, "tbody")[0];
			rows =tBody.childNodes;		
			len = rows.length;
		
			for (var i = 0; i < len; i++) {
				if (i < (from-1) || i > (to-1))  {
					  rows[i].style.display = 'none';
				} else {
					  rows[i].style.display = '';
				}
			}
		}	else {
			var tHead = eXo.core.DOMUtil.getChildrenByTagName(table, "thead")[0];
			rows = eXo.core.DOMUtil.getChildrenByTagName(tHead ,"tr");
			len = rows.length - 1;		  
			for (var i = 1; i < len + 1; i++) {  //starts from 1 to skip table header row
				if (i < from || i > to)  {
					  rows[i].style.display = 'none';
				} else {
					  rows[i].style.display = '';
				}
			}
		}
	} else {
		var icons = null;   
    var icon_container = document.getElementById(eXo.ecm.Pager.tableName);    
    icons =  eXo.core.DOMUtil.findChildrenByClass(icon_container ,"div", "ActionIconBox");    
    len = icons.length;		
		for (var i = 0; i < len; i++) {
			if (i < from || i > to)  {
				  icons[i].style.display = 'none';
			} else {
				  icons[i].style.display = '';
			}
		}    
	}
	if(len <= 12) { document.getElementById("pageNavPosition").innerHTML = "";	return;}
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
	var pagerHtml = '<span>Total page(s) : '+this.pages+'</span> ';
	pagerHtml += '<span onclick="eXo.ecm.Pager.previousPage();" class="pg-normal"> &#171 Prev </span> | ';
    for (var page = 1; page <= this.pages; page++) {
    	pagerHtml += '<span id="pg' + page + '" class="pg-normal" title="'+page+'" onclick="eXo.ecm.Pager.showPage(' + page + ');">' + page + '</span> | ';
    }
	pagerHtml += '<span onclick="eXo.ecm.Pager.nextPage();" class="pg-normal"> Next &#187;</span>';            
    element.innerHTML = pagerHtml;
};

EcmContentSelector.prototype.insertContent = function(objNode) {  
	if(!objNode) return;
	var rws = document.getElementById("RightWorkspace");
	if(eXo.ecm.ECS.typeObj == "folder" || eXo.ecm.ECS.typeObj == "one") {
		var action = rws.getAttribute("action");
		action = action.substring(0, action.length - 2);
		action += '&objectId=' + encodeURIComponent(eXo.ecm.ECS.driverName) + ":" + encodeURIComponent(eXo.ecm.ECS.repositoryName) + ":" + encodeURIComponent(eXo.ecm.ECS.workspaceName) + ":" + objNode.getAttribute("path") + '\')';
		var temp = action;
		var index = temp.indexOf("%27");
		while(index != -1) {
		   temp = temp.replace("%27","%2527");
		   index = temp.indexOf("%27");
		}
		action = temp;		
		eval(action);
	} else {
		var hostName = eXo.ecm.ECS.hostName;
		var nodeType = objNode.getAttribute('nodeType');
		var url 	= objNode.getAttribute('url');
		var temp = url;
  var index = temp.indexOf("%27");
  while(index != -1) {
   temp = temp.replace("%27","%2527");
 		index = temp.indexOf("%27");
  }
  url = encodeURIComponent(eXo.ecm.ECS.hostName+temp);
  url = decodeURIComponent(url);    
 	var name 	= encodeURIComponent(objNode.title);
 	name = decodeURIComponent(name);
		var strHTML = '';	
		var editor = eXo.ecm.ECS.currentEditor ;    
    if(eXo.ecm.ECS.components=="") {
			if(window.opener.document.getElementById(eXp.getParameterValueByName("browserType"))){		
				strHTML += url;		
				window.opener.document.getElementById(eXp.getParameterValueByName("browserType")).value=strHTML;
			} else {
				if(nodeType.indexOf("image") >=0) {
					strHTML += "<img src=\""+url+"\" name=\""+name+"\" alt=\""+name+"\"/>";
				} else {
					strHTML += "<a href=\"" + url+"\">"+name+"</a>";		
				}		    		    
				editor.insertHtml(strHTML);
				window.close();
				editor.OnAfterSetHTML = window.close();				
			}
		} else {      						
			var newImg = new Image();
			newImg.src = url;
      newImg.onload = function() {
				var height = newImg.height;
				var width = newImg.width;  
        var parent = window.opener.document;
        parent.getElementById(eXo.ecm.ECS.components).src=url;
				parent.getElementById(eXo.ecm.ECS.components).style.display="block";
				parent.getElementById(editor.name+"_txtWidth").value=width;	
				parent.getElementById(editor.name+"_txtHeight").value=height;        
        var elements = parent.getElementsByTagName('*');
       	for(var i=0;i<elements.length;i++)	{        
					if(elements[i].type && elements[i].type=="text") {          
						if(elements[i].id && elements[i].id==editor.name+"_txtUrl") elements[i].value = url;
					}
				}       
				window.close();
				editor.OnAfterSetHTML = window.close();				
			}
		}		
	}
};

EcmContentSelector.prototype.insertMultiContent = function(operation, currentpath) {
	var rws = document.getElementById("RightWorkspace");
	var tblContent = document.getElementById("ListFilesContent");
	var rowsContent = eXo.core.DOMUtil.findDescendantsByTagName(tblContent, "tr");
	if (rowsContent.length <= 1) {
		alert("There are no content for now. You have to select at least one.");
	}
	var strContent = "";
	for(var i = 0; i < rowsContent.length; i++) {
		var nodeContent = eXo.core.DOMUtil.findFirstDescendantByClass(rowsContent[i], "a", "Item");
		if(nodeContent) {
			var path = nodeContent.getAttribute("path");
			strContent += encodeURIComponent(path) + ";";
		}
	}
	var action = rws.getAttribute("action");
	
	if (operation) {
		var actionSaveTemp = rws.getAttribute("actionSaveTemp");
		if (actionSaveTemp) {
		  var additionParam = "&driverName=" + this.driverName + "&currentPath=" + encodeURIComponent(currentpath) + '&itemPaths=' + strContent
	  	  action = eXo.ecm.WCMUtils.addParamIntoAjaxEventRequest(actionSaveTemp, additionParam);
		}else return;      
	}else if(action){
		action = action.substring(0, action.length - 2);
		action += '&objectId=' + strContent + '\')';
	} else return;	
	eval(action);  
};

EcmContentSelector.prototype.addFile2ListContent = function(objNode) {  
	var tblListFilesContent = document.getElementById("ListFilesContent");
	var rowsContent = eXo.core.DOMUtil.findDescendantsByTagName(tblListFilesContent, "tr");
	var trNoContent = eXo.core.DOMUtil.findFirstDescendantByClass(tblListFilesContent, "td", "TRNoContent");
	if(trNoContent) tblListFilesContent.deleteRow(trNoContent.parentNode.rowIndex);
	var url = objNode.getAttribute("url");  
	var nodeType	= objNode.getAttribute("nodeType");
	var path = objNode.getAttribute("path");
	var linkTarget = objNode.getAttribute("linkTarget");
	var selectedNodeList = eXo.core.DOMUtil.findDescendantsByClass(tblListFilesContent, "a", "Item");
	for(var i = 0; i < selectedNodeList.length; i++) {
		var selectedNodePath = selectedNodeList[i].getAttribute("linkTarget");
		if(linkTarget == selectedNodePath) {
			alert("This content is already in the list content.");
			return;
		}
	}
	var	clazzItem = objNode.className;
	var newRow = tblListFilesContent.insertRow(tblListFilesContent.children[0].children.length);
	newRow.className = "Item";
	newRow.insertCell(0).innerHTML = '<a class="Item" url="'+url+'" linkTarget ="' + linkTarget +'" path="'+path+'" nodeType="'+nodeType+'style = "overflow:hidden" title="'+decodeURIComponent(objNode)+'">'+decodeURIComponent(path)+'</a>';
	newRow.insertCell(1).innerHTML = '<div class="DeleteIcon" onclick="eXo.ecm.ECS.removeContent(this);"><span></span></div>';
	this.insertMultiContent("SaveTemporary", path);	
};

EcmContentSelector.prototype.addFileSearchListSearch = function() {
};

EcmContentSelector.prototype.loadListContent = function(strArray, strTargetArray) {
	if(!strArray) return;
	if (!strTargetArray) return;
	var tblListFilesContent = document.getElementById("ListFilesContent");
	var arrContent = strArray.split(";");
	var arrTarget = strTargetArray.split(";");
	if (arrContent.length>arrTarget.length) return;
	if(arrContent.length > 0) {
		var trNoContent = eXo.core.DOMUtil.findFirstDescendantByClass(tblListFilesContent, "td", "TRNoContent");
		if(trNoContent) tblListFilesContent.deleteRow(trNoContent.parentNode.rowIndex);
		var clazz = 'OddItem';
		for(var i = 0; i < arrContent.length-1; i++) {
			var path = arrContent[i];
			var target = arrTarget[i];
			var newRow = tblListFilesContent.insertRow(tblListFilesContent.children[0].children.length);
			if(clazz == 'EventItem') {
				clazz = 'OddItem';
			} else if(clazz == 'OddItem') {
				clazz = 'EventItem';
			}
			newRow.className = clazz;
			var strTmpArr = arrContent[i].split('/');
			var nodeName = strTmpArr[strTmpArr.length-1];
			newRow.insertCell(0).innerHTML = '<a class="Item" linkTarget ="'+ target+ '" path="'+path+'">'+decodeURIComponent(nodeName)+'</a>';
			newRow.insertCell(1).innerHTML = '<div  class="DeleteIcon" onclick="eXo.ecm.ECS.removeContent(this);"><span></span></div>';
		}
	}
};

EcmContentSelector.prototype.removeContent = function(objNode) {
	var confirmDelete = confirm(this.deleteConfirmationMsg);
	if (confirmDelete != true) {
		return;
	}
	var tblListFilesContent = document.getElementById("ListFilesContent"); 
	var objRow = eXo.core.DOMUtil.findAncestorByTagName(objNode, "tr");
	tblListFilesContent.deleteRow(objRow.rowIndex);	
	eXo.ecm.ECS.pathContent = false;
	this.insertMultiContent("SaveTemporary", this.initPathExpanded);
}

EcmContentSelector.prototype.changeFilter = function() {
	var rightWS = document.getElementById('RightWorkspace');	
  if(eXo.ecm.ECS.viewType=="list") {
		var tblRWS	= eXo.core.DOMUtil.findDescendantsByTagName(rightWS, "table")[0];
		var rowsRWS = eXo.core.DOMUtil.findDescendantsByTagName(tblRWS, "tr");
		if(rowsRWS && rowsRWS.length > 0) {
			for(var i = 0; i < rowsRWS.length; i++) {
				if(i > 0) tblRWS.deleteRow(rowsRWS[i].rowIndex);
			}
		} 
	} else {
		var container = eXo.core.DOMUtil.findFirstDescendantByClass(rightWS,'div','ActionIconsContainer');
			container.innerHTML = "";
	}
	
	/*if(eXo.ecm.ECS.typeObj == "folder") {
		eXo.ecm.ECS.listFolders();
	} else if(eXo.ecm.ECS.typeObj == "multi") {
		eXo.ecm.ECS.listMutilFiles();
	} else {
		eXo.ecm.ECS.listFiles();
	}*/	
	if(eXo.ecm.ECS.currentNode)	 eXo.ecm.ECS.getDir(eXo.ecm.ECS.currentNode, eXo.ecm.ECS.eventNode);
	
	var filter = document.getElementById('Filter');
	var action = filter.getAttribute("action");
	if(action) {
		action = action.substring(0, action.length - 2);
		action += '&objectId=' + filter.options[filter.selectedIndex].value + '\')';
		eval(action);
	}	
};

EcmContentSelector.prototype.changeViewType = function(viewType) {  
  eXo.ecm.ECS.viewType = viewType;  
  var view = document.getElementById("view");
	view.innerHTML = "";  
  if(viewType=="list") 
		view.innerHTML = "<a onClick=\"eXo.ecm.ECS.changeViewType('thumbnail');\" title=\"Thumbnail View\" class=\"thumbnail-view\" ><span></span></a><a class=\"list-view-selected\" title=\"List View\"><span></span></a><input type=\"hidden\" id=\"viewTypeID\" value=\"list\">";
  else
		view.innerHTML = "<a class=\"thumbnail-view-selected\" title=\"Thumbnail View\"><span></span></a><a onClick=\"eXo.ecm.ECS.changeViewType('list');\" class=\"list-view\" title=\"List View\"><span></span></a><input type=\"hidden\" id=\"viewTypeID\" value=\"thumbnail\">";
  eXo.ecm.ECS.switchView = true;	   
	if(eXo.ecm.ECS.currentNode) eXo.ecm.ECS.getDir(eXo.ecm.ECS.currentNode, eXo.ecm.ECS.eventNode);
  else {
		var strViewPresent = "";
		if(viewType=="list") {
			strViewPresent = "<div class=\"ListView\"><table cellspacing=\"0\" style=\"table-layout: fixed; width: 100%;\" id=\"ListRecords\">";
			strViewPresent += "<thead><tr><th class=\"THBar\" userLanguage=\"UserLanguage.FileName\"> Name </th>";
			strViewPresent += "<th class=\"THBar\" style=\"width: 120px;\" userLanguage=\"UserLanguage.CreateDate\"> Date </th>";
			strViewPresent += "<th class=\"THBar\" style=\"width: 80px;\" userLanguage=\"UserLanguage.FileSize\"> Size </th></tr></thead>";
      strViewPresent += "<tr><td class=\"Item TRNoContent\" colspan=\"3\" userLanguage=\"UserLanguage.NoContent\">There is no content</td></tr>";
			strViewPresent += "</table></div>";
		} else {
			strViewPresent = "<div class=\"UIThumbnailsView\" style=\"overflow-y: auto; overflow-x: hidden;\"><div class=\"ActionIconsContainer\" id=\"ActionIconsContainer\"><div class=\"NoContent\" userLanguage=\"UserLanguage.NoContent\">There is no content</div></div></div>";
		}
		var rightWS = document.getElementById('RightWorkspace');  
		if(rightWS) {
			rightWS.innerHTML = "";
			strViewPresent += "<div class=\"PageIterator\" id=\"pageNavPosition\"></div><div style=\"clear: left;\"><span></span></div>";
			rightWS.innerHTML = strViewPresent;
		}
	}
  eXo.ecm.ECS.switchView = false;
	var filter = document.getElementById('Filter');
	var action = filter.getAttribute("action");
	if(action) {
		action = action.substring(0, action.length - 2);
		action += '&objectId=' + filter.options[filter.selectedIndex].value + '\')';    
		eval(action);
	}	
}

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
	var actionBaroffsetHeight = 0;
	if(actionBar)
	  actionBaroffsetHeight = actionBar.offsetHeight;
	var breadcumbsPortlet = eXo.core.DOMUtil.findFirstDescendantByClass(uiWorkingWorkspace, "div", "BreadcumbsPortlet");
	leftWS.style.height = windowHeight - (titleBar.offsetHeight + actionBaroffsetHeight + breadcumbsPortlet.offsetHeight + 55) + "px";
};

EcmContentSelector.prototype.isShowFilter = function() {
	var selectFilter = document.getElementById("Filter");
	var filterContainer = eXo.core.DOMUtil.findAncestorByClass(selectFilter, "ActionBar");
	if(eXo.ecm.ECS.typeObj == "folder") {
		filterContainer.style.display = "none";
	} 
};

EcmContentSelector.prototype.showUpload = function() {
	var upload = document.getElementById("UploadItem");
	if(!upload) return;
	upload.style.display = 'block';
};

EcmContentSelector.prototype.hideUpload = function() {
	var upload = document.getElementById("UploadItem");
	if(!upload) return;
	upload.style.display = 'none';
};

EcmContentSelector.prototype.languageInit = function() {
	if (eXp.userLanguage) {
		var aElements = document.getElementsByTagName("*");
		for (var i = 0 ; i < aElements.length; ++i) {
			if (aElements[i].getAttribute && aElements[i].getAttribute("userLanguage")) {
				var userLanguage = eval(aElements[i].getAttribute("userLanguage"));
				if (userLanguage) {
					var textNode = document.createTextNode(userLanguage);
					aElements[i].innerHTML = "";
					aElements[i].appendChild(textNode);
				}
			}
		}
	} else {
		eXoPlugin.loadScript(window, "lang/en.js");
		setTimeout(languageInit, 1000);
	}
};

EcmContentSelector.prototype.initPath = function(initDrive, initPath, componentId) {
	setTimeout("eXo.ecm.ECS.waitAndInitPath(\"" + initDrive + "\",\"" + initPath + "\",\"" + componentId +"\")", 1000);
}

EcmContentSelector.prototype.waitAndInitPath = function(initDrive, initPath, componentId) {
	initDrive = initDrive.replace(/ /g, "");
	initPath = initPath.replace(/ /g, "");
	var contentBrowsePanel = document.getElementById(componentId);
	var leftWorkspace = eXo.core.DOMUtil.findDescendantsByClass(contentBrowsePanel, "div", "LeftWorkspace")[0];
	var tagADrives = eXo.core.DOMUtil.findDescendantsByTagName(leftWorkspace, "a");
	for (var i = 0; i < tagADrives.length; ++i) {
		if (tagADrives[i].getAttribute("id")) {
			var id = tagADrives[i].getAttribute("id");
			if (id && (id.indexOf('_' + initDrive + '_') >= 0)) {
				var nodeContainer = eXo.core.DOMUtil.findAncestorByClass(tagADrives[i], "ChildrenContainer");
				var nodeParent = eXo.core.DOMUtil.findAncestorByClass(tagADrives[i], "Node");
				if(!nodeContainer) return;
				var nodeADriveType = nodeContainer.previousSibling;
				if(!nodeADriveType) return;
				var nodeLink = nodeADriveType.firstChild.firstChild;
				eXo.ecm.ECS.renderBreadcrumbs(nodeLink);
				eXo.ecm.ECS.listRootFolder(nodeLink);
				eXo.ecm.ECS.actionColExp(nodeADriveType);				
				var event = false;
				if (initPath && initPath != "" && initPath != "/")
				  eXo.ecm.ECS.getDir(tagADrives[i], event);
//				eXo.ecm.ECS.actionColExp(nodeParent);
				eXo.ecm.ECS.expandTree('_' + initDrive + '_', initPath, nodeParent);
				return;
			}
		}
	}
};

EcmContentSelector.prototype.expandTree = function(preStr, path, nodeParent) { 
	var nextElt = eXo.core.DOMUtil.findNextElementByTagName(nodeParent, "div");	
	if(!nextElt || nextElt.className != "ChildrenContainer" || !path || path == "") {
		return;
	}
	path = path.substring(1, path.length);
	var slashIndex = path.indexOf('/');
	preStr = preStr + "_" + path.substring(0, slashIndex);
	path = path.substring(slashIndex, path.length);
	if (path.indexOf('/') < 0) {
		var height = 0;
		var node = nodeParent;
		while (node.getAttribute("id") != "LeftWorkspace") {
			if (!node.previousSibling) {
				node = node.parentNode;
			}
			if (node.getAttribute("id") != "LeftWorkspace")	node = node.previousSibling;
			if (node.className == "Node") {
				height += 27;
			}
		}
		var leftWS = document.getElementById("LeftWorkspace");		
		leftWS.scrollTop = height;
		return;
	}
	var tagADrives = eXo.core.DOMUtil.findDescendantsByTagName(nextElt, "a");
	for (var i = 0; i < tagADrives.length; ++i) {
		if (tagADrives[i].getAttribute("id")) {
			var id = tagADrives[i].getAttribute("id");
			if (id && (id.indexOf(preStr) >= 0)) {
				nodeParent = eXo.core.DOMUtil.findAncestorByClass(tagADrives[i], "Node");
				var event = false;				
				eXo.ecm.ECS.getDir(tagADrives[i], event);
				//eXo.ecm.ECS.actionColExp(nodeParent);
				eXo.ecm.ECS.expandTree(preStr, path, nodeParent);
				return;
			}
		}
	}
};
String.prototype.trunc =
    function(n,useWordBoundary){
        var toLong = this.length>n,
            s_ = toLong ? this.substr(0,n-1) : this;
        s_ = useWordBoundary && toLong ? s_.substr(0,s_.lastIndexOf(' ')) : s_;
        return  toLong ? s_ +'...' : s_;
};

eXo.ecm.ECS = new EcmContentSelector();
