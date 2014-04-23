(function(gj, wcm_utils) {
	function EcmContentSelector() {	
		this.portalName = eXo.env.portal.portalName;
		this.context = eXo.env.portal.context;
		this.accessMode = eXo.env.portal.accessMode;
		this.userLanguage = eXo.env.portal.language;
		this.userId = eXo.env.portal.userName;
		this.hostName = eXo.ecm.WCMUtils.getHostName();
		this.repositoryName = "repository";
		this.workspaceName	= "collaboration";
		this.cmdEcmBundle = "/bundle/";
		this.cmdGetBundle = "getBundle?";
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
			var tmp = eXo.ecm.ECS.renderSubTree(nodeList[i]);
			if (tmp == '') {
				treeHTML += '<div class="node">';
				treeHTML += 	'<div class="emptyIcon">';
			} else {
				treeHTML += '<div class="node" onclick="eXo.ecm.ECS.actionColExp(this, event);">';
				treeHTML += 	'<div class="expandIcon">';
			}
			treeHTML += 		'<a rel="tooltip" data-placement="bottom" title="'+decodeURIComponent(strName)+'"href="javascript:void(0);" class="nodeIcon" onclick="eXo.ecm.ECS.renderBreadcrumbs(this);eXo.ecm.ECS.listRootFolder(this);" name="'+decodeURIComponent(strName)+'" id="'+id+'" isUpload="'+isUpload +'" nodeDriveName="' + nodeDriveName +'">';
			treeHTML +=				'<i class="uiIcon16x16FolderDefault"></i>';
			treeHTML += 			'<span class="nodeName"> ';
			treeHTML += 				decodeURIComponent(strName);
			treeHTML +=				'</span>';	
			treeHTML +=			'</a>';
			treeHTML += 	'</div>';			
			treeHTML += '</div>';			

			if(tmp != '') treeHTML += tmp;
		}
		var uiLeftWorkspace = document.getElementById('LeftWorkspace');	
		if(uiLeftWorkspace) gj(uiLeftWorkspace).html(treeHTML);
		var contentSelectorPop = document.getElementById('CorrectContentSelectorPopupWindow');
	  if(contentSelectorPop) {
			var contentBlock = gj(contentSelectorPop).find('div.PopupContent:first')[0];
			if(contentBlock) contentBlock.style.height="";
		}
		gj(document).ready(function() { gj("*[rel='tooltip']").tooltip();});
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
			
				var nodeContainer = gj(currentNode).parents(".ChildrenContainer:first")[0];
				if(!nodeContainer) return;
				var nodeParent = gj(nodeContainer).prevAll("div:first")[0];
				if(!nodeParent) return;
				var nodeLink = gj(nodeParent).find("a.nodeIcon:first")[0];
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
		gj(document).ready(function() { gj("*[rel='tooltip']").tooltip();});
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
				var hasFolderChild = nodeList[i].getAttribute("hasFolderChild");
				if (nodeDriveName==null) nodeDriveName='';			
				if (!label) label = strName;
				treeHTML += '<div class="node" onclick="eXo.ecm.ECS.actionColExp(this, event);">';
				if (hasFolderChild == "true") {
					treeHTML += 	'<div class="expandIcon">';
					treeHTML +=			'<a rel="tooltip" data-placement="bottom" title="'+decodeURIComponent(label)+'" href="javascript:void(0);" class="nodeIcon" onclick="eXo.ecm.ECS.getDir(this, event);" name="'+decodeURIComponent(strName)+'" id="'+id+'"  driverPath="'+driverPath+ '" hasFolderChild="' + hasFolderChild + '" repository="'+repository+'" workspace="'+workspace+'">';
				} else {
					treeHTML += 	'<div class="emptyIcon">';
					treeHTML +=			'<a rel="tooltip" data-placement="bottom" title="'+decodeURIComponent(label)+'" href="javascript:void(0);" class="nodeIcon" name="'+decodeURIComponent(strName)+'" id="'+id+'"  driverPath="'+driverPath+'" repository="'+repository+'" workspace="'+workspace+'">';
				}
				treeHTML +=				'<i class="uiIconEcms16x16DriveGeneral"></i>';
				treeHTML += 			'<span class="nodeName"> ';
				treeHTML += 				label;	
				treeHTML +=				'</span>';	
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
		var tblRWS  = gj(rightWS).find("table")[0];
		var rowsRWS = gj(tblRWS).find("tr");
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
				var strTitle = nodeList[i].getAttribute("title");
				if (!strTitle) strTitle = nodeList[i].getAttribute("data-original-title");
				var strName = nodeList[i].getAttribute("name");
				var isUpload = nodeList[i].getAttribute("isUpload");
				var nodeDriveName = nodeList[i].getAttribute("nodeDriveName");
				var hasFolderChild = nodeList[i].getAttribute("hasFolderChild");
				var nodeTypeCssClass = nodeList[i].getAttribute("nodeTypeCssClass");
				if (nodeDriveName==null) nodeDriveName='';
				treeHTML += '<div class="node" onclick="eXo.ecm.ECS.actionColExp(this, event);">';
				if (hasFolderChild == true) {
					treeHTML += 	'<div class="expandIcon">';
					treeHTML +=			'<a rel="tooltip" data-placement="bottom" title="'+ decodeURIComponent(strTitle) +'" href="javascript:void(0);" class="nodeIcon" onclick="eXo.ecm.ECS.getDir(this, event);" name="'+decodeURIComponent(strName)+'" id="'+id+ '" hasFolderChild="' + hasFolderChild + '" isUpload="'+isUpload +'" nodeDriveName="' + nodeDriveName +'">';
				} else {
					treeHTML += 	'<div class="emptyIcon">';
					treeHTML +=			'<a rel="tooltip" data-placement="bottom" title="'+ decodeURIComponent(strTitle) +'" href="javascript:void(0);" class="nodeIcon" name="'+decodeURIComponent(strName)+'" id="'+id +'" isUpload="'+isUpload +'" nodeDriveName="' + nodeDriveName +'">';
				}
				treeHTML +=				'<i class="' + nodeTypeCssClass + '"></i>';
				treeHTML += 			'<span class="nodeName"> ';
				treeHTML += 				decodeURIComponent(strTitle);
				treeHTML +=				'</span>';	
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
					var strTitle = currentNodeList[i].getAttribute("title");
					if (!strTitle) strTitle = currentNodeList[i].getAttribute("data-original-title");
					var	strName	= currentNodeList[i].getAttribute("name");
					var isUpload = currentNodeList[i].getAttribute("isUpload");
					var nodeDriveName = currentNodeList[i].getAttribute("nodeDriveName");
					var hasFolderChild = currentNodeList[i].getAttribute("hasFolderChild");
					var nodeTypeCssClass = currentNodeList[i].getAttribute("nodeTypeCssClass");
					if (nodeDriveName==null) nodeDriveName='';
					treeHTML += '<div class="node" onclick="eXo.ecm.ECS.actionColExp(this, event);">';
					if (hasFolderChild == "true") {
						treeHTML += 	'<div class="expandIcon">';
						treeHTML +=			'<a rel="tooltip" data-placement="bottom" title="'+decodeURIComponent(strTitle)+'" href="javascript:void(0);" class="nodeIcon" onclick="eXo.ecm.ECS.getDir(this, event);" name="'+decodeURIComponent(strName)+'" id="'+id+ '" hasFolderChild="' + hasFolderChild + '" isUpload="'+isUpload +'" nodeDriveName="' + nodeDriveName +'">';
					} else {
						treeHTML += 	'<div class="emptyIcon">';
						treeHTML +=			'<a rel="tooltip" data-placement="bottom" title="'+decodeURIComponent(strTitle)+'" href="javascript:void(0);" class="nodeIcon" name="'+decodeURIComponent(strName)+'" id="'+id + '" isUpload="'+isUpload +'" nodeDriveName="' + nodeDriveName +'">';
					}
					treeHTML +=				'<i class="' + nodeTypeCssClass + '"></i>';
					treeHTML += 			'<span class="nodeName"> ';
					treeHTML += 				decodeURIComponent(strTitle);
					treeHTML +=				'</span>';	
					treeHTML += 		'</a>';
					treeHTML +=		'</div>';
					treeHTML +=	'</div>';
				}
				var parentNode = gj(currentNode).parents(".node:first")[0];
				var nodeIcon = gj(currentNode).parents("div:first")[0];
				var nextElementNode = gj(parentNode).nextAll("div:first")[0];
				var tmpNode = document.createElement("div");
				tmpNode.className = "ChildrenContainer" ;      
				gj(tmpNode).html(treeHTML);
				if(nextElementNode && nextElementNode.className == "node") {        
					nextElementNode.parentNode.insertBefore(tmpNode, nextElementNode) ;
					nodeIcon.className = 'collapseIcon';				
					tmpNode.style.display = "block";
				} else if(nextElementNode && nextElementNode.className == "ChildrenContainer"){				
					eXo.ecm.ECS.actionColExp(parentNode);
				} else {				
					var cldrContainer = gj(currentNode).parents(".ChildrenContainer:first")[0];
					nodeIcon.className = 'collapseIcon';
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
	
	EcmContentSelector.prototype.actionColExp = function(objNode, event) {
		if(!objNode) return;
		var nextElt = gj(objNode).nextAll("div:first")[0];
		var iconElt = gj(objNode).children("div")[0];
		var aElt = gj(iconElt).children("a")[0];
		var hasFolderChild = aElt.getAttribute("hasFolderChild");
		if(!nextElt || nextElt.className != "ChildrenContainer") {
			var currentNode	= gj(objNode).find("a.nodeIcon:first")[0];
			if (currentNode != null) {      
				eXo.ecm.ECS.getDir(currentNode, false);
			} else return;
		}
		if(iconElt.className != 'collapseIcon') {
			if (nextElt!=null && nextElt.className == "ChildrenContainer") {
				nextElt.style.display = 'block';	
			}
			if ((event && iconElt.className != 'emptyIcon') || !event)
				iconElt.className = 'collapseIcon';
		} else if(!eXo.ecm.ECS.switchView) {
			eXo.ecm.ECS.switchView = false;
			if (nextElt!=null && nextElt.className == "ChildrenContainer") {
				nextElt.style.display = 'none';
			}
			if ((event && iconElt.className != 'emptyIcon') || !event)
				iconElt.className = 'expandIcon';
		}
	};
	
	EcmContentSelector.prototype.renderBreadcrumbs = function(currentNode) {
		if(!currentNode) return;
		if(typeof(currentNode) == 'string') currentNode = document.getElementById(currentNode);
		eXo.ecm.ECS.currentNode = currentNode;
		var breadcrumbContainer = document.getElementById("BreadcumbsContainer");
		gj(breadcrumbContainer).html('<li><i class="uiIconHome uiIconLightGray"></i></li>');
		var beforeNode = null;
		while(currentNode != null && currentNode.className != "leftWorkspace") {
			var curName = gj(currentNode).attr('name');
			var label = gj(currentNode).attr('title');
			if (!label) label = gj(currentNode).attr('data-original-title');
			if(curName) {
				var tmpNode = document.createElement("li");	
				var strHTML = '';
				var strOnclick = '';
				var node = document.getElementById(currentNode.id);
				if(!node.getAttribute("driverPath") && !node.getAttribute("currentfolder")) {
					strOnclick = "eXo.ecm.ECS.actionBreadcrumbs('"+node.id+"');eXo.ecm.ECS.listRootFolder('"+node.id+"');";		
				} else {
					strOnclick = "eXo.ecm.ECS.actionBreadcrumbs('"+node.id+"');";		
				}
				strHTML += '<i class="uiIconMiniArrowRight"></i>';
				if(beforeNode == null) {
					strHTML += '<a class="active" href="javascript:void(0);" onclick="'+strOnclick+'">'+decodeURIComponent(label)+'</a>';
					gj(tmpNode).html(strHTML);
					breadcrumbContainer.appendChild(tmpNode);
				} else {
					strHTML += '<a class="" href="javascript:void(0);" onclick="'+strOnclick+'">'+decodeURIComponent(label)+'</a>';
					gj(tmpNode).html(strHTML);
					breadcrumbContainer.insertBefore(tmpNode, beforeNode);
				}
				beforeNode = tmpNode;
			}
			currentNode = currentNode.parentNode;
			if(currentNode != null && currentNode.className == 'ChildrenContainer'){
				currentNode = gj(currentNode).prevAll('div:first')[0];
				currentNode = currentNode.getElementsByTagName('div')[0].getElementsByTagName('a')[0];
			}
		}
	};
	
	EcmContentSelector.prototype.actionBreadcrumbs = function(nodeId) {
		var ECS = eXo.ecm.ECS;
		var element = document.getElementById(nodeId);
		var node =  gj(element).parents(".node:first")[0];
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
				var tblRWS  = gj(rightWS).find("table")[0];
				var rowsRWS = gj(tblRWS).find("tr");
				if(rowsRWS && rowsRWS.length > 0) {
					for(var i = 0; i < rowsRWS.length; i++) {
						if(i > 0) tblRWS.deleteRow(rowsRWS[i].rowIndex);
					}
				} 
				var tdNoContent = tblRWS.tBodies[0].insertRow(0).insertCell(0);
				gj(tdNoContent).html("There is no content");
				tdNoContent.className = "item noContent empty center";
				tdNoContent.setAttribute("colspan",3);
				tdNoContent.userLanguage = "UserLanguage.NoContent";	
				gj("#pageNavPosition").html("");
			} else {
				var container = gj(rightWS).find('div.actionIconsContainer:first')[0];
				gj(container).html("<div class=\"noContent\" userLanguage=\"UserLanguage.NoContent\">There is no content</div>");
				gj("#pageNavPosition").html("");
			}
			return;
		} else {		
	    
			if(viewType=="list") {
				var tblRWS  = gj(rightWS).find("table")[0];
				if(tblRWS) {
					var rowsRWS = gj(tblRWS).find("tr");
					if(rowsRWS && rowsRWS.length > 0) {
						for(var i = 0; i < rowsRWS.length; i++) {
							if(i > 0) tblRWS.deleteRow(rowsRWS[i].rowIndex);
						}
					} 
				} else eXo.ecm.ECS.updateHTML(viewType);			
			} else {
				var container = gj(rightWS).find('div.actionIconsContainer:first')[0];
				if(container) gj(container).html("");
				else eXo.ecm.ECS.updateHTML(viewType);
			}
			var container = gj(rightWS).find('div.actionIconsContainer:first')[0];
			if(container) {
				container.style.display = "none";
			}			
			var listItem = '';
			var strViewContent = "";
			// Depends on Paginators will be used or not. 'longDesc' if paginator, 'src' else.
			var imageAttribute = "src";
                       	if(list.length > 8) {
				// Paginator will be used
				imageAttribute = "longDesc";
			}			
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
					var tblRWS  = gj(rightWS).find("table")[0];
					var clazzItem = eXo.ecm.ECS.getClazzIcon(list[i].getAttribute("nodeType"));
					var newRow = tblRWS.tBodies[0].insertRow(i);
					newRow.className = clazz;
					gj(newRow.insertCell(0)).html('<a url="'+decodeURIComponent(url)+'" path="'+path+'" nodeType="'+nodeType+'" style = "overflow:hidden;" rel="tooltip" data-placement="bottom" title="'+decodeURIComponent(node)+'" onclick="eXo.ecm.ECS.insertContent(this);">'+decodeURIComponent(node)+'</a>');
					gj(newRow.insertCell(1)).html('<div class="Item">'+ list[i].getAttribute("dateCreated") +'</div>');
					gj(newRow.insertCell(2)).html('<div class="Item">'+ size +'</div>');
				} else {				  
					var randomId = Math.random();
					var command = ECS.connector + "/thumbnailImage/medium/" + ECS.repositoryName + "/" + ECS.workspaceName + path + "/?reloadnum=" + randomId;        
					strViewContent += '<div id="'+randomId+'" class="actionIconBox" onclick="eXo.ecm.ECS.insertContent(this);" url="'+decodeURIComponent(url)+'" path="'+path+'" nodeType="'+nodeType+'" rel="tooltip" data-placement="bottom" title="'+decodeURIComponent(node)+'"><div class="nodeLabel"><div class="thumbnailImage"><div style="display: block;" class="LoadingProgressIcon"><img alt="Loading Process" id="thumbnail'+randomId+'" '+imageAttribute+'="'+command+'" onerror="var img = gj(this.parentNode).next(\'i:first\')[0]; img.style.display = \'block\'; this.parentNode.style.display = \'none\';" onload="this.parentNode.style.backgroundImage=\'none\'" /></div><i style="display: none;" class="uiIcon64x64FileDefault uiIcon64x64nt_file  '+nodeTypeIcon+'"></i></div><div class="actionIconLabel" style="width: auto;"><a class="actionLabel" onclick="eXo.ecm.ECS.insertContent(this);" url="'+url+'" path="'+path+'" nodeType="'+nodeType+'" rel="tooltip" data-placement="bottom" title="'+decodeURIComponent(node)+'">'+decodeURIComponent(node)+'</a></div></div></div>';
				}
			}
			if(container) {
				gj(container).html(strViewContent);
				container.style.display = "";
			}			
		}	
		if(viewType=="list") {
			if(i > 9) {
				var numberRecords = 9;		
				var viewType = eXo.ecm.ECS.viewType; 
				eXo.ecm.Pager = new Pager("ListRecords", numberRecords);
				eXo.ecm.Pager.init(); 
				eXo.ecm.Pager.showPageNav('pageNavPosition');
				eXo.ecm.Pager.showPage(1);	
			} else {
				gj("#pageNavPosition").html("");
			}
		}
		else {
			if(i > 8) {
				var numberRecords = 8;		
				var viewType = eXo.ecm.ECS.viewType; 
				eXo.ecm.Pager = new Pager("ActionIconsContainer", numberRecords);
				eXo.ecm.Pager.init(); 
				eXo.ecm.Pager.showPageNav('pageNavPosition');
				eXo.ecm.Pager.showPage(1);	
			} else {
				gj("#pageNavPosition").html("");
			}
		}	
	};
	
	EcmContentSelector.prototype.updateHTML = function(viewType) {
	  var strViewPresent = "";
		if(viewType=="list") {
			strViewPresent = "<div class=\"listView\"><table class=\"uiGrid table table-hover table-striped\" id=\"ListRecords\">";
			strViewPresent += "<thead><tr><th userLanguage=\"UserLanguage.FileName\"> Name </th>";
			strViewPresent += "<th class=\"span2\" userLanguage=\"UserLanguage.CreateDate\"> Date </th>";
			strViewPresent += "<th class=\"span1\" userLanguage=\"UserLanguage.FileSize\"> Size </th></tr></thead><tbody></tbody>";
			strViewPresent += "</table></div>";

		} else {
			strViewPresent = "<div class=\"uiThumbnailsView\" style=\"overflow-y: auto; overflow-x: hidden;\"><div class=\"actionIconsContainer\" id=\"ActionIconsContainer\"></div></div>";
		}
		var rightWS = document.getElementById('RightWorkspace');  
	  if(rightWS) {
			gj(rightWS).html("");
			strViewPresent += "<div class=\"PageIterator\" id=\"pageNavPosition\"></div><div style=\"clear: left;\"><span></span></div>";
			gj(rightWS).html(strViewPresent);
		}
	}
	
	EcmContentSelector.prototype.listFolders = function(list) {
		var rightWS = document.getElementById('RightWorkspace');
		var tblRWS  = gj(rightWS).find("table")[0];
		var rowsRWS = gj(tblRWS).find("tr");
		if(rowsRWS && rowsRWS.length > 0) {
			for(var i = 0; i < rowsRWS.length; i++) {
				if(i > 0) tblRWS.deleteRow(rowsRWS[i].rowIndex);
			}
		} 
		if(!list || list.length <= 0) {
			var tdNoContent = tblRWS.tBodies[0].insertRow(0).insertCell(0);
			gj(tdNoContent).html("There is no content");
			tdNoContent.className = "item noContent empty center";
			tdNoContent.userLanguage = "UserLanguage.NoContent";
			gj("#pageNavPosition").html("");
			return;
		}
		var listItem = '';
		for(var i = 0; i < list.length; i++) {
			var clazzItem = eXo.ecm.ECS.getClazzIcon(list[i].getAttribute("nodeType"));
			var url 			= list[i].getAttribute("url");
			var path 			= list[i].getAttribute("path");
			var nodeType	= list[i].getAttribute("folderType");
			var node = list[i].getAttribute("title");
			var label = list[i].getAttribute("label");
			if (!label) label = node;
			var newRow = tblRWS.tBodies[0].insertRow(i);
			gj(newRow.insertCell(0)).html('<a class="Item" url="'+url+'" path="'+path+'" nodeType="'+nodeType+'" onclick="eXo.ecm.ECS.insertContent(this);">'+decodeURIComponent(label)+'</a>');
					
		}
		
		if(i > 9) {
			var numberRecords = 9;
			eXo.ecm.Pager = new Pager("ListRecords", numberRecords);
			eXo.ecm.Pager.init(); 
			eXo.ecm.Pager.showPageNav('pageNavPosition');
			eXo.ecm.Pager.showPage(1);	
		} else {
			gj("#pageNavPosition").html("");
		}
	};
	
	EcmContentSelector.prototype.listMutilFiles = function(list) {
		var rightWS = document.getElementById('RightWorkspace');
		var tblRWS  = gj(rightWS).find("table")[0];
		var rowsRWS = gj(tblRWS).find("tr");
		if(rowsRWS && rowsRWS.length > 0) {
			for(var i = 0; i < rowsRWS.length; i++) {
				if(i > 0) tblRWS.deleteRow(rowsRWS[i].rowIndex);
			}
		} 
		if(!list || list.length <= 0) {
			var rowTmp = tblRWS.tBodies[0].insertRow(0);
			var tdNoContent = rowTmp.insertCell(0);
			gj(tdNoContent).html("There is no content");
			tdNoContent.className = "item noContent empty center";
			tdNoContent.userLanguage = "UserLanguage.NoContent";
			gj("#pageNavPosition").html("");
			return;
		}
		var listItem = '';
		for(var i = 0; i < list.length; i++) {
			var clazzItem = eXo.ecm.ECS.getClazzIcon(list[i].getAttribute("nodeType"));
			var url 			= list[i].getAttribute("url");
			var path 			= eXo.ecm.ECS.repositoryName+":"+eXo.ecm.ECS.workspaceName+":"+list[i].getAttribute("path");
			var linkTarget = list[i].getAttribute("linkTarget");
			var nodeType	= list[i].getAttribute("nodeType");
			var node = list[i].getAttribute("name");
			var newRow = tblRWS.tBodies[0].insertRow(i);
			gj(newRow.insertCell(0)).html('<a class="Item" url="'+url+'" linkTarget ="' + linkTarget + '" path="'+path+'" nodeType="'+nodeType+'" style = "overflow:hidden;" rel="tooltip" data-placement="bottom" title="'+decodeURIComponent(node)+'" onclick="eXo.ecm.ECS.addFile2ListContent(this);">'+decodeURIComponent(node)+'</a>');
					
		}
		
		if(i > 9) {
			var numberRecords = 9;
	    eXo.ecm.Pager = new Pager("ListRecords", numberRecords);
			eXo.ecm.Pager.init(); 
			eXo.ecm.Pager.showPageNav('pageNavPosition');
			eXo.ecm.Pager.showPage(1);	
		} else {
			gj("#pageNavPosition").html("");
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
		if(leftWorkSpace) rightWorkSpace.style.height = leftWorkSpace.offsetHeight - 30 + "px";
	};
	
	Pager.prototype.init = function() { 
		this.setHeightRightWS();
		var len = 0;
	  if(eXo.ecm.ECS.viewType=="list") {
			var table = document.getElementById(eXo.ecm.Pager.tableName);
			if(navigator.userAgent.indexOf("MSIE") >= 0) { //is IE
				var tBody = gj(table).children("tbody")[0];
				len = tBody.childNodes.length;
			} else {
				var rows;
				var tHead = gj(table).children("thead")[0];
				var tbody = gj(table).children("tbody")[0];
				if (tbody) {
					rows = gj(tbody).children("tr");
				} else {
					rows = gj(tHead).children("tr");
				}
				len = rows.length;
			}
		} else {
			var icon_container = document.getElementById(eXo.ecm.Pager.tableName);    
	    icons =  gj(icon_container).find("div.actionIconBox");    
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
	//			var tHead = eXo.core.DOMUtil.getChildrenByTagName(table, "thead")[0];
			var tHead = gj(table).children("thead")[0];
			var tbody = gj(table).children("tbody")[0];
			if (tbody) {
				rows = gj(tbody).children("tr");
			} else {
				rows = gj(tHead).children("tr");
			}
			len = rows.length - 1;		  
			for (var i = 0; i < len + 1  ; i++) {  //starts from 1 to skip table header row
				if (i < from - 1 || i > to - 1 )  {
					  rows[i].style.display = 'none';
				} else {
					  gj(rows[i]).css("display", "");
				}
			}
		} else {
			var icons = null;   
	    var icon_container = document.getElementById(eXo.ecm.Pager.tableName);
	    icons =  gj(icon_container).find("div.actionIconBox");    
	    len = icons.length;		
			for (var i = 0; i < len; i++) {
				if (i < from-1 || i > to-1)  {   //from starts at 1 and icons table starts at 0. Need to shift 1
					  icons[i].style.display = 'none';
				} else {
					  icons[i].style.display = '';
					  var thumnailImg = document.getElementById("thumbnail"+icons[i].id);
					  if(thumnailImg.src || thumnailImg.src == '') {
						  thumnailImg.src = thumnailImg.longDesc;
					  }					  
				}
			}    
		}
		if(len <= 8) { gj("#pageNavPosition").html(""); return;}
	};
	
	Pager.prototype.showPage = function(pageNumber) {
		if (! this.inited) {
			alert("not inited");
			return;
	    }
	    this.currentPage = pageNumber;
	    eXo.ecm.Pager.showPageNav('pageNavPosition');
	    var pageNavPosition = gj("#pageNavPosition");
	    var prev = gj(pageNavPosition).find('.Previous:first')[0];
	    var next = gj(pageNavPosition).find('.Next:first')[0];
	    if(pageNumber == 1) { 
			prev.parentNode.className = 'disabled';
			next.parentNode.className = '';
		} else if(pageNumber == this.pages) {
			next.parentNode.className = 'disabled';
			prev.parentNode.className = '';
		}
	    else {
			prev.parentNode.className = '';
			next.parentNode.className = '';
		}
	    if(document.getElementById('pg'+eXo.ecm.Pager.currentPage)) {
			var oldPageAnchor = document.getElementById('pg'+eXo.ecm.Pager.currentPage).parentNode;
			if(oldPageAnchor) oldPageAnchor.className = '';
		}
	    
	    
	    if(document.getElementById('pg'+eXo.ecm.Pager.currentPage)) {
			var newPageAnchor = document.getElementById('pg'+eXo.ecm.Pager.currentPage).parentNode;
			if(newPageAnchor)  newPageAnchor.className = 'active';
		}
	    
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
		var pagerHtml = '';
		
		var min = 1;
	    var max = this.pages;
	    var dot1 = dot2 = -1;
	    
	    if (this.pages > 5) {
			if (this.currentPage < 4) {
			   max = 3;
			   dot1 = 4;
			} else if (this.currentPage >= this.pages - 2) {
			   min = this.pages - 2;
			   dot1 = min - 1;
			} else {
			   min = this.currentPage - 1;
			   max = this.currentPage + 1;
			   dot1 = 2;
			   dot2 = this.pages - 1;
			}		
		}
		
		pagerHtml += '<div class="pagination uiPageIterator clearfix"><ul class="pull-right">';		
		pagerHtml += '<li><a onclick="eXo.ecm.Pager.previousPage();" class="Previous Page" rel="tooltip" data-placement="bottom" data-original-title="Previous Page"><i class="uiIconPrevArrow"></i></a></li>';
		
		for(var i = 1 ; i <= this.pages; i++) { 		
			if (i == 1 && min > 1) 
			  pagerHtml += '<li><a onclick="eXo.ecm.Pager.showPage(' + i + ');" id="pg' + i + '" >' + i + '</a></li>';
			else if (i == min) {
			   for (j = min; j <= max; j++) {
				 pagerHtml += '<li><a onclick="eXo.ecm.Pager.showPage(' + j + ');" id="pg' + j + '" >' + j + '</a></li>';      
			   }
			} else if (i == dot1 || i == dot2) {
					pagerHtml += '<li class="disabled"><a href="#">...</a></li>';	   
			} else if (i == this.pages && max < this.pages) 
			  pagerHtml += '<li><a onclick="eXo.ecm.Pager.showPage(' + this.pages + ');" id="pg' + this.pages + '">' + this.pages + '</a></li>';      
	    }
		
		
		pagerHtml += '<li><a onclick="eXo.ecm.Pager.nextPage();" class="Next Page" rel="tooltip" data-placement="bottom" data-original-title="Next Page"><i class="uiIconNextArrow"></i></a></li>';		
		pagerHtml += '</ul><p class="pull-right"><span>Total pages:</span> <span class="pagesTotalNumber">'+this.pages+'</span></p></div>';
		
			 
		          
	    gj(element).html(pagerHtml);
	};
	
	EcmContentSelector.prototype.insertContent = function(objNode) {  
		if(!objNode) return;
		var rws = document.getElementById("RightWorkspace");
		if(eXo.ecm.ECS.typeObj == "folder" || eXo.ecm.ECS.typeObj == "one") {
			var action = rws.getAttribute("action");
			action = action.substring(0, action.length - 2);
			action += '&objectId=' + encodeURIComponent(eXo.ecm.ECS.driverName) + ":" + encodeURIComponent(eXo.ecm.ECS.repositoryName) + ":" + encodeURIComponent(eXo.ecm.ECS.workspaceName) + ":" + encodeURIComponent(objNode.getAttribute("path")) + '\')';
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
	    url = encodeURIComponent(temp);
	    url = decodeURIComponent(url);    
	 	  var name 	= encodeURIComponent(objNode.title);
	 	  if (name == "") name = objNode.innerHTML;
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
       url = decodeURIComponent(url);

       function loadInfoImage () {
         var height = newImg.height;
         var width = newImg.width;

         var document = window.opener.document;
         var parentClassName = ".cke_editor_"+eXo.ecm.ECS.currentEditor.name+"_dialog";
         var parent = null;
         
         gj(document).find(parentClassName).each(function(index) {
		 if(gj(this).css('display')=='block') {
			 parent = this;
		 }
	 });
         gj(parent).find("div[name*='info']").find("input").each(function(index) {
	   if(index==0) {
   	     this.src = url;
 	     this.value = url;
  	     this.style.display="block";
	   } else if(index == 2) {
   	     this.value = width;
           } else if(index == 3) {
	     this.value = height;
	   }
	 });
         document.getElementById(eXo.ecm.ECS.components).src=url;
         document.getElementById(eXo.ecm.ECS.components).style.display="block";
         
         window.close();
         editor.OnAfterSetHTML = window.close();
       }
     }
     newImg.onload = loadInfoImage;
     newImg.src = url;
     if (newImg.complete)
     {
       loadInfoImage();
     }
   }
  };
	
	EcmContentSelector.prototype.insertMultiContent = function(operation, currentpath) {
		var rws = document.getElementById("RightWorkspace");
		var tblContent = document.getElementById("ListFilesContent");
		var rowsContent = gj(tblContent).find("tr");
		if (rowsContent.length <= 1) {
	    var msg = eXo.ecm.WCMUtils.getBundle('ContentSelector.msg.no-file-selected', eXo.ecm.ECS.userLanguage);
	    alert(msg);
	    return;
	  }
		var strContent = "";
		for(var i = 0; i < rowsContent.length; i++) {
			var nodeContent = gj(rowsContent[i]).find("a.Item:first")[0];
			if(nodeContent) {
				var path = nodeContent.getAttribute("path");
				strContent += encodeURIComponent(path) + ";";
			}
		}
		var action = rws.getAttribute("action");
		//strContent = escape(strContent);
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
		var rowsContent = gj(tblListFilesContent).find("tr");
		var tdNoContent = gj(tblListFilesContent).find("td.noContent:first")[0];
		if(tdNoContent) tblListFilesContent.deleteRow(tdNoContent.parentNode.rowIndex);
		var url = objNode.getAttribute("url");  
		var nodeType	= objNode.getAttribute("nodeType");
		var path = objNode.getAttribute("path");
		var title = objNode.getAttribute("title");
		if (!title) title = objNode.getAttribute("data-original-title");
		var linkTarget = objNode.getAttribute("linkTarget");
		var selectedNodeList = gj(tblListFilesContent).find("a.Item");
		for(var i = 0; i < selectedNodeList.length; i++) {
			var selectedNodePath = selectedNodeList[i].getAttribute("linkTarget");
			if(linkTarget == selectedNodePath) {
				alert("This content is already in the list content.");
				return;
			}
		}
		var	clazzItem = objNode.className;
		var newRow = tblListFilesContent.tBodies[0].insertRow(tblListFilesContent.children[0].children.length - 1);
		newRow.className = "Item";
		gj(newRow.insertCell(0)).html('<a class="Item" url="'+url+'" linkTarget ="' + linkTarget +'" path="'+path+'" nodeType="'+nodeType+' style = "overflow:hidden" rel="tooltip" data-placement="bottom" title="'+decodeURIComponent(title)+'">'+eXo.ecm.ECS.safe_tags_regex(title)+'</a>');
		var actionCell = newRow.insertCell(1);
		gj(actionCell).html('<a class="actionIcon" onclick="eXo.ecm.ECS.removeContent(this);"><i class="uiIconDelete uiIconLightGray""></i></a>');
		actionCell.className = "center";
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
			var tdNoContent = gj(tblListFilesContent).find("td.noContent:first")[0];
			if(tdNoContent) tblListFilesContent.deleteRow(tdNoContent.parentNode.rowIndex);
			for(var i = 0; i < arrContent.length-1; i++) {
				var path = arrContent[i];
				var target = arrTarget[i];
				if (target == "") continue;
				var newRow = tblListFilesContent.tBodies[0].insertRow(tblListFilesContent.children[0].children.length - 1);
				var strTmpArr = arrContent[i].split('/');
				var nodeName = strTmpArr[strTmpArr.length-1];
				gj(newRow.insertCell(0)).html('<a class="Item" linkTarget ="'+ target+ '" path="'+path+'">'+eXo.ecm.ECS.safe_tags_regex(decodeURIComponent(nodeName))+'</a>');
				var actionCell = newRow.insertCell(1);
				gj(actionCell).html('<a class="actionIcon" onclick="eXo.ecm.ECS.removeContent(this);"><i class="uiIconDelete uiIconLightGray""></i></a>');
				actionCell.className = "center";
			}
		}
	};
	
	EcmContentSelector.prototype.removeContent = function(objNode) {
		var confirmDelete = confirm(this.deleteConfirmationMsg);
		if (confirmDelete != true) {
			return;
		}
		var tblListFilesContent = document.getElementById("ListFilesContent"); 
		var objRow = gj(objNode).parents("tr:first")[0];
		tblListFilesContent.deleteRow(objRow.rowIndex);	
		eXo.ecm.ECS.pathContent = false;
		this.insertMultiContent("SaveTemporary", this.initPathExpanded);
	}
	
	EcmContentSelector.prototype.changeFilter = function() {
		var rightWS = document.getElementById('RightWorkspace');	
	  if(eXo.ecm.ECS.viewType=="list") {
			var tblRWS	= gj(rightWS).find("table")[0];
			var rowsRWS = gj(tblRWS).find("tr");
			if(rowsRWS && rowsRWS.length > 0) {
				for(var i = 0; i < rowsRWS.length; i++) {
					if(i > 0) tblRWS.deleteRow(rowsRWS[i].rowIndex);
				}
			} 
		} else {
			var container = gj(rightWS).find('div.actionIconsContainer:first')[0];
				gj(container).html("");
		}
		
		
		if(eXo.ecm.ECS.currentNode)	 eXo.ecm.ECS.getDir(eXo.ecm.ECS.currentNode, eXo.ecm.ECS.eventNode);
	};
	
	EcmContentSelector.prototype.changeViewType = function(viewType) {  
	  eXo.ecm.ECS.viewType = viewType;  
	  var view = document.getElementById("view");
		gj(view).html("");  
	  if(viewType=="list") {
		  gj("#enableListViewBtn").attr('class', 'btn active'); gj('#enableThumbnailViewBtn').attr('class', 'btn');
	  }
	  else {
		  gj("#enableThumbnailViewBtn").attr('class', 'btn active'); gj('#enableListViewBtn').attr('class', 'btn');
	  }

	  eXo.ecm.ECS.switchView = true;	   
		if(eXo.ecm.ECS.currentNode) eXo.ecm.ECS.getDir(eXo.ecm.ECS.currentNode, eXo.ecm.ECS.eventNode);
	  else {
			var strViewPresent = "";
			if(viewType=="list") {
				strViewPresent = "<div class=\"listView\"><table class=\"uiGrid table table-hover table-striped\" id=\"ListRecords\">";
				strViewPresent += "<thead><tr><th userLanguage=\"UserLanguage.FileName\"> Name </th>";
				strViewPresent += "<th class=\"span2\" userLanguage=\"UserLanguage.CreateDate\"> Date </th>";
				strViewPresent += "<th class=\"span1\" userLanguage=\"UserLanguage.FileSize\"> Size </th></tr></thead>";
				strViewPresent += "<tr><td class=\"center empty\" colspan=\"3\" userLanguage=\"UserLanguage.NoContent\">There is no content</td></tr></table></div>";
			} else {
				strViewPresent = "<div class=\"uiThumbnailsView\" style=\"overflow-y: auto; overflow-x: hidden;\"><div class=\"actionIconsContainer\" id=\"ActionIconsContainer\"><div class=\"NoContent\" userLanguage=\"UserLanguage.NoContent\">There is no content</div></div></div>";
			}
			var rightWS = document.getElementById('RightWorkspace');  
			if(rightWS) {
				gj(rightWS).html("");
				strViewPresent += "<div class=\"PageIterator\" id=\"pageNavPosition\"></div><div style=\"clear: left;\"><span></span></div>";
				gj(rightWS).html(strViewPresent);
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
		if (!leftWS) return;
		var rightWS = document.getElementById('RightWorkspace'); 
		var windowHeight = gj(window).height();
		var root = gj(leftWS).parents(".UIHomePageDT:first")[0];
		var titleBar = gj(root).find("h6.TitleBar:first")[0];
		var uiWorkingWorkspace = gj(root).find("div.uiWorkingWorkspace:first")[0];
		var actionBar = gj(uiWorkingWorkspace).find("div.actionBar:first")[0];
		var actionBaroffsetHeight = 0;
		if(actionBar)
		  actionBaroffsetHeight = actionBar.offsetHeight;
		var breadcumbsPortlet = gj(uiWorkingWorkspace).find("div.breadcumbsPortlet:first")[0];
		leftWS.style.height = windowHeight - (titleBar.offsetHeight + actionBaroffsetHeight + breadcumbsPortlet.offsetHeight + 55) + "px";
		if(rightWS)
		  rightWS.style.height = windowHeight - (titleBar.offsetHeight + actionBaroffsetHeight + breadcumbsPortlet.offsetHeight + 55) + "px";
	};
	
	EcmContentSelector.prototype.isShowFilter = function() {
		var selectFilter = document.getElementById("Filter");
		var filterContainer = gj(selectFilter).parents(".actionBar:first")[0];
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
						gj(aElements[i]).html("");
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
		var leftWorkspace = gj(contentBrowsePanel).find("div.leftWorkspace")[0];
		var tagADrives = gj(leftWorkspace).find("a");
		for (var i = 0; i < tagADrives.length; ++i) {
			if (tagADrives[i].getAttribute("id")) {
				var id = tagADrives[i].getAttribute("id");
				if (id && (id.indexOf('_' + initDrive + '_') >= 0)) {
					var nodeContainer = gj(tagADrives[i]).parents(".ChildrenContainer:first")[0];
					var nodeParent = gj(tagADrives[i]).parents(".node:first")[0];
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
		var nextElt = gj(nodeParent).nextAll("div:first")[0];	
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
				if (node.className == "node") {
					height += 27;
				}
			}
			var leftWS = document.getElementById("LeftWorkspace");		
			leftWS.scrollTop = height;
			return;
		}
		var tagADrives = gj(nextElt).find("a");
		for (var i = 0; i < tagADrives.length; ++i) {
			if (tagADrives[i].getAttribute("id")) {
				var id = tagADrives[i].getAttribute("id");
				if (id && (id.indexOf(preStr) >= 0)) {
					nodeParent = gj(tagADrives[i]).parents(".node:first")[0];
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
	
	EcmContentSelector.prototype.safe_tags_regex = function(str) {
	  return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
	 }
	
	eXo.ecm.ECS = new EcmContentSelector();
	window.onresize = eXo.ecm.ECS.fixHeightTrees;
	return {
		ECS : eXo.ecm.ECS
	};
})(gj, wcm_utils);
