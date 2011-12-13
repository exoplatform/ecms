function PluginUtils() {
}

PluginUtils.prototype.request = function(urlRequestXML) {
	var xmlHttpRequest = false;
	if (window.XMLHttpRequest) {
		xmlHttpRequest = new window.XMLHttpRequest();
		xmlHttpRequest.open("GET",urlRequestXML,false);
		xmlHttpRequest.send("");
		return xmlHttpRequest.responseXML;
		}
	else if (ActiveXObject("Microsoft.XMLDOM")) { // for IE
		xmlHttpRequest = new ActiveXObject("Microsoft.XMLDOM");
		xmlHttpRequest.async=false;
		xmlHttpRequest.load(urlRequestXML);
		return xmlHttpRequest;
	}
	alert("There was a problem retrieving the XML data!");
	return null;
};

PluginUtils.prototype.renderTree = function(objXML) {
	var	xmlTreeNodes = eXoWCM.PluginUtils.request(objXML);
	var nodeList = xmlTreeNodes.getElementsByTagName('Folders');
	var treeHTML = '';
	var isUpload = "";
	for(var i = 0 ; i < nodeList.length; i++)	 {
		if(nodeList[i].getAttribute("isUpload")) isUpload = nodeList[i].getAttribute("isUpload");
		else isUpload = "";
		var strName = nodeList[i].getAttribute("name") ;
		var id = eXoWCM.PluginUtils.generateIdDriver(nodeList[i]);
		treeHTML += '<div class="Node" onclick="eXoWCM.PluginUtils.actionColExp(this);">';
		treeHTML += 	'<div class="ExpandIcon">';		
		treeHTML += 		'<a title="'+decodeURIComponent(strName)+'" class="NodeIcon DefaultPageIcon" href="javascript:void(0);" isUpload="'+isUpload+'" onclick="eXoWCM.PluginUtils.renderBreadcrumbs(this);" name="'+decodeURIComponent(strName)+'" id="'+id+'">';
		treeHTML +=			decodeURIComponent(strName);	
		treeHTML +=			'</a>';
		treeHTML += 	'</div>';			
		treeHTML += '</div>';			
		var tmp = eXoWCM.PluginUtils.renderSubTree(nodeList[i]);
		if(tmp != '') treeHTML += tmp;
	}
	var leftWorkSpace = document.getElementById('LeftWorkspace');	
	if(leftWorkSpace) leftWorkSpace.innerHTML = treeHTML;
};

// render children container
PluginUtils.prototype.renderSubTree = function(currentNode) {
	if(!currentNode) return;
	var nodeList = currentNode.getElementsByTagName('Folder');
	var nodeListParent = currentNode.getElementsByTagName('Folders');
	var treeHTML = '';
	if(nodeList && nodeList.length > 0) {
		treeHTML += '<div class="ChildrenContainer" style="display:none;">'	;
		for(var i = 0; i < nodeList.length; i++) {
			var id = eXoWCM.PluginUtils.generateIdDriver(nodeList[i]);
			var strName = nodeList[i].getAttribute("name");
			var label = nodeList[i].getAttribute("label");
			if (!label) label = strName;
			var driverPath = nodeList[i].getAttribute("driverPath");
			treeHTML += '<div class="Node" onclick="eXoWCM.PluginUtils.actionColExp(this);">';
			treeHTML += 	'<div class="ExpandIcon">';
			treeHTML +=			'<a title="'+decodeURIComponent(label)+'" class="NodeIcon DefaultPageIcon" href="javascript:void(0);" onclick="getDir(this, event);" name="'+decodeURIComponent(strName)+'" id="'+id+'"  driverPath="'+driverPath+'">';
			treeHTML +=				label;	
			treeHTML += 		'</a>';
			treeHTML +=		'</div>';
			treeHTML +=	'</div>';
		}
		treeHTML += '</div>';
	}
	return treeHTML;
};

PluginUtils.prototype.renderSubTrees = function(currentNode, event, connector) {
	var event = event || window.event;
	event.cancelBubble = true;
	if(!currentNode) return;
	var nodeList = currentNode.getElementsByTagName('Folder');
	var treeHTML = '';
	var fileList = '';
	if(nodeList && nodeList.length > 0) {
		fileList = currentNode.getElementsByTagName('File');
		treeHTML += '<div class="ChildrenContainer" style="display:none;">'	;
		for(var i = 0; i < nodeList.length; i++) {
			var id = eXoWCM.PluginUtils.generateIdNodes(nodeList[i], currentNode.id);
			var strName = nodeList[i].getAttribute("name");
			treeHTML += '<div class="Node" onclick="eXoWCM.PluginUtils.actionColExp(this);">';
			treeHTML += 	'<div class="ExpandIcon">';
			treeHTML +=			'<a title="'+ decodeURIComponent(strName) +'" class="NodeIcon DefaultPageIcon" href="javascript:void(0);" onclick="getDir(this, event);" name="'+decodeURIComponent(strName)+'" id="'+id+'">';
			treeHTML +=				decodeURIComponent(strName);	
			treeHTML += 		'</a>';
			treeHTML +=		'</div>';
			treeHTML +=	'</div>';
		}
		treeHTML += '</div>';
	} else {
		var xmlTreeNodes = eXoWCM.PluginUtils.request(connector);
		var currentNodeList = xmlTreeNodes.getElementsByTagName('Folder');
		fileList = xmlTreeNodes.getElementsByTagName('File');
		if(currentNodeList && currentNodeList.length > 0) {
			for(var i = 0; i < currentNodeList.length; i++) {
				var id = eXoWCM.PluginUtils.generateIdNodes(currentNodeList[i], currentNode.id);
				var	strName	= currentNodeList[i].getAttribute("name");
				treeHTML += '<div class="Node" onclick="eXoWCM.PluginUtils.actionColExp(this);">';
				treeHTML += 	'<div class="ExpandIcon">';
				treeHTML +=			'<a title="'+decodeURIComponent(strName)+'" class="NodeIcon DefaultPageIcon" href="javascript:void(0);" onclick="getDir(this, event);" name="'+decodeURIComponent(strName)+'" id="'+id+'">';
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
				eXoWCM.PluginUtils.actionColExp(parentNode);
				if(nodeIcon) nodeIcon.className = 'ExpandIcon';
			} else {
				var cldrContainer = eXo.core.DOMUtil.findAncestorByClass(currentNode, "ChildrenContainer");
				nodeIcon.className = 'CollapseIcon';
				cldrContainer.appendChild(tmpNode);
			}
		}
	}
	eXoWCM.PluginUtils.listFiles(fileList);
};

PluginUtils.prototype.actionColExp = function(objNode) {
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

PluginUtils.prototype.listFiles = function(list) {
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
		tdNoContent.setAttribute('colspan', 3);
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
		var clazzItem = eXoWCM.PluginUtils.getClazzIcon(list[i].getAttribute("nodeType"));
		var url 			= list[i].getAttribute("url");
		var nodeType	= list[i].getAttribute("nodeType");
		var node = list[i].getAttribute("name");
		var label = list[i].getAttribute("label");
		if (!label) label = node;
		var newRow = tblRWS.insertRow(i+1);
		newRow.className = clazz;
		newRow.insertCell(0).innerHTML = '<div class="Item '+clazzItem+'" url="'+url+'" nodeType="'+nodeType+'" onclick="eXoWCM.PluginUtils.insertContent(this);">'+decodeURIComponent(label)+'</div>';
		newRow.insertCell(1).innerHTML = '<div class="Item">'+ list[i].getAttribute("dateCreated") +'</div>';
		newRow.insertCell(2).innerHTML = '<div class="Item">'+ list[i].getAttribute("size")+'&nbsp;kb' +'</div>';
		
		if(i > 13) {
			var numberRecords = 0;
			if(eXo.core.Browser.isFF()) numberRecords = 14;
			else numberRecords = 13;
			eXoWCM.Pager = new Pager("ListRecords", numberRecords);
			eXoWCM.Pager.init(); 
			eXoWCM.Pager.showPageNav('pageNavPosition');
			eXoWCM.Pager.showPage(1);	
		} else {
			document.getElementById("pageNavPosition").innerHTML = "";
		}
	}
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
	var table = document.getElementById(eXoWCM.Pager.tableName);
	if(eXo.core.Browser.isFF()) {
		var tHead = eXo.core.DOMUtil.getChildrenByTagName(table, "thead")[0];
		var rowsTHead = eXo.core.DOMUtil.getChildrenByTagName(tHead, "tr");
		len = rowsTHead.length;
	}	else {
		var tBody = eXo.core.DOMUtil.getChildrenByTagName(table, "tbody")[0];
		len = tBody.childNodes.length -1;
	}
    var records = len; 
    this.pages = Math.ceil(records / eXoWCM.Pager.itemsPerPage);
	if(this.pages < 0) this.page =1;
    this.inited = true;
};

Pager.prototype.showRecords = function(from, to) {
	var rows = null;
	var table = document.getElementById(eXoWCM.Pager.tableName);
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

    var oldPageAnchor = document.getElementById('pg'+eXoWCM.Pager.currentPage);
    if(oldPageAnchor) oldPageAnchor.className = 'pg-normal';
    
    this.currentPage = pageNumber;
    var newPageAnchor = document.getElementById('pg'+eXoWCM.Pager.currentPage);
	if(newPageAnchor)  newPageAnchor.className = 'pg-selected';
    
    var from = (pageNumber - 1) * eXoWCM.Pager.itemsPerPage + 1;
    var to = from +  eXoWCM.Pager.itemsPerPage - 1;
    eXoWCM.Pager.showRecords(from, to);
};

Pager.prototype.previousPage = function() {
	if (this.currentPage > 1)  eXoWCM.Pager.showPage(this.currentPage - 1);
};

Pager.prototype.nextPage = function() {
	if (this.currentPage < this.pages) eXoWCM.Pager.showPage(this.currentPage + 1);
};

Pager.prototype.showPageNav = function(positionId) {
	if (! this.inited) {
		alert("not inited");
		return;
	}
	var element = document.getElementById(positionId);
	var pagerHtml = '<span>Total page : '+this.pages+'</span> ';
	pagerHtml += '<span onclick="eXoWCM.Pager.previousPage();" class="pg-normal"> &#171 Prev </span> | ';
    for (var page = 1; page <= this.pages; page++) {
        pagerHtml += '<span id="pg' + page + '" class="pg-normal" onclick="eXoWCM.Pager.showPage(' + page + ');">' + page + '</span> | ';
    }
	pagerHtml += '<span onclick="eXoWCM.Pager.nextPage();" class="pg-normal"> Next &#187;</span>';            
    element.innerHTML = pagerHtml;
};

PluginUtils.prototype.getClazzIcon = function(nodeType) {
	if(!nodeType) return;
	var strClassIcon = '';
	strClassIcon = nodeType.replace("/", "_").replace(":", "_") + "16x16Icon";
	return strClassIcon;
};

PluginUtils.prototype.renderBreadcrumbs = function(currentNode) {
	if(!currentNode) return;
	if(typeof(currentNode) == 'string') currentNode = document.getElementById(currentNode);
	if(currentNode.getAttribute("isUpload")) document.getElementById("UploadItem").style.display = "none";
	eXp.store.currentNode = currentNode;
	var breadscrumbsContainer = document.getElementById("BreadcumbsContainer");
	breadscrumbsContainer.innerHTML = '';
	var beforeNode = null;
	while(currentNode.className != "LeftWorkspace") {
		var curName = currentNode.getAttribute('name');
		var label = currentNode.getAttribute('title');
		if(curName != null) {
			var tmpNode = document.createElement("div");	
			tmpNode.className = 'BreadcumbTab';
			var strHTML = '';
			var strOnclick = '';
			var node = document.getElementById(currentNode.id);
			if(node) strOnclick = "eXoWCM.PluginUtils.actionBreadcrumbs('"+node.id+"')";		
			if(beforeNode == null) {
				strHTML += '<a class="Nomal" href="javascript:void(0);" onclick="'+strOnclick+'">'+decodeURIComponent(label)+'</a>';
				tmpNode.innerHTML = strHTML;
				breadscrumbsContainer.appendChild(tmpNode);
			} else {
				strHTML += '<a class="Nomal" href="javascript:void(0);" onclick="'+strOnclick+'">'+decodeURIComponent(label)+'</a>';
				strHTML += '<div class="RightArrowIcon"><span></span></div>';
				tmpNode.innerHTML = strHTML;
				breadscrumbsContainer.insertBefore(tmpNode, beforeNode);
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

PluginUtils.prototype.generateIdDriver = function(objNode) {
	if(!objNode) return;
	var id = '';
	while(objNode.tagName != 'Connector') {
		var curName = objNode.getAttribute("name").replace(" ", "");
		id =  curName+"_"+id;
		objNode = objNode.parentNode;
	}
	return id;
};

PluginUtils.prototype.generateIdNodes = function(objNode, idNode) {
	if(!objNode && !idNode) return;
	var id = '';
	while(objNode.tagName != 'Folders') {
		var curName = objNode.getAttribute("name").replace(" ", "");
		id =  idNode+"_"+curName;
		objNode = objNode.parentNode;
	}
	return id;
};

PluginUtils.prototype.actionBreadcrumbs = function(nodeId) {
	var element = document.getElementById(nodeId);
	var node =  eXo.core.DOMUtil.findAncestorByClass(element, "Node");
	eXoWCM.PluginUtils.actionColExp(node);
	eXoWCM.PluginUtils.renderBreadcrumbs(element);

	var strConnector = eXp.connector;
	var currentFolder;
	var currentNode = element;
	var driverName;
	var uploadItem = document.getElementById("UploadItem");	
	if(uploadItem) uploadItem.style.display = "block";
	if(currentNode.getAttribute("driverPath")) {
		driverName =	currentNode.getAttribute('name');
		eXp.store.driverName = driverName;
		currentFolder = '/';	
	} else {
		currentFolder = element.getAttribute("currentfolder");
	}
	
	eXp.store.currentFolder = currentFolder;
	eXp.store.currentNode = currentNode;
	driverName = eXp.store.driverName;
	var strReplace 	= "getFoldersAndFiles?driverName="+driverName+"&currentFolder="+currentFolder+"&currentPortal="+eXoPlugin.portalName+"&" ;	
	strConnector 		= strConnector.replace("getDrivers?",strReplace);
	var filter = '';
	var dropdownlist = document.getElementById("Pinter");
	if(dropdownlist) filter = dropdownlist.options[dropdownlist.selectedIndex].value;
	else filter = 'Web Contents';
	var connector = eXoPlugin.hostName + strConnector+ "&workspaceName=collaboration&userId=" + eXoPlugin.userId + "&filterBy="+filter;
	if(eXp.strConnection == connector) return;	
	eXp.strConnection = connector;
	var xmlDoc = eXoWCM.PluginUtils.request(connector);
	if(!xmlDoc) return;
	var fileList = xmlDoc.getElementsByTagName("File");
	eXoWCM.PluginUtils.listFiles(fileList);
};

PluginUtils.prototype.insertContent = function(objContent) {
	if(!objContent) return;
	var hostName = eXoPlugin.hostName;
	var nodeType = objContent.getAttribute('nodeType');
	var url 	= objContent.getAttribute('url');
	var temp = url;
 var index = temp.indexOf("%27");
 while(index != -1){
  temp = temp.replace("%27","%2527");
		index = temp.indexOf("%27");
 }
 url = temp;
 var name 	= encodeURIComponent(objContent.innerHTML);	
	var strHTML = '';	
	if(window.opener.document.getElementById(getParameterValueByName("browserType"))){		
		strHTML += url;		
		window.opener.document.getElementById(getParameterValueByName("browserType")).value=strHTML;
	} else {
		if(nodeType.indexOf("image") >=0) {
			strHTML += "<img src=\""+url+"\" name=\""+name+"\" alt=\""+name+"\"/>";
		} else {
			strHTML += "<a href=\"" + url+"\" style='text-decoration:none;'>"+name+"</a>";		
		}
		FCK.InsertHtml(strHTML);
	}			
	FCK.OnAfterSetHTML = window.close();
};
function getParameterValueByName( parameterName )
{
  parameterName = parameterName.replace(/[\[]/,"\\\[").replace(/[\]]/,"\\\]");
  var regexS = "[\\?&]"+parameterName+"=([^&#]*)";
  var regex = new RegExp( regexS );
  var results = regex.exec( window.location.href );
  if( results == null )
    return "";
  else
    return results[1];
}
PluginUtils.prototype.showSettings = function(obj) {
	if(!obj) return;
	if(obj.Timeout) clearTimeout(obj.Timeout);
	var settingContainer = eXo.core.DOMUtil.findFirstDescendantByClass(obj, "div", "SettingContainer");
	var popupMenu = eXo.core.DOMUtil.findFirstChildByClass(settingContainer, "div", "UIRightClickPopupMenu");
	if(popupMenu && popupMenu.style.display != "block") {
		popupMenu.style.display = 'block';
		popupMenu.onmouseout = function(){
			obj.Timeout = setTimeout(function() {
				popupMenu.style.display = 'none';
				popupMenu.onmouseover = null;
				popupMenu.onmouseout  = null;
			},1*1000);
		};
	
		popupMenu.onmouseover = function() {
			if(obj.Timeout) clearTimeout(obj.Timeout);
			obj.Timeout = null;
		};
		eXo.core.DOMUtil.hideElements();
	}
};

PluginUtils.prototype.showSubMenuSettings = function(obj) {
	if(!obj) return;
	if(obj.Timeout) clearTimeout(obj.Timeout);	
	var childrenContainer = eXo.core.DOMUtil.findFirstDescendantByClass(obj, "div", "ChildrenContainer");
	if(childrenContainer) {
		var viewSubMenuContainer = eXo.core.DOMUtil.findFirstChildByClass(childrenContainer, "div", "UIRightClickPopupMenu");
		if(viewSubMenuContainer && viewSubMenuContainer.style.display != 'block') {
			viewSubMenuContainer.style.display = 'block';
			viewSubMenuContainer.style.left 	= -viewSubMenuContainer.offsetWidth + 'px';
			viewSubMenuContainer.style.top 		= -obj.offsetHeight + 'px';
			viewSubMenuContainer.onmouseout = function() {
				obj.Timeout = setTimeout(function() {
					viewSubMenuContainer.style.display = 'none';
					viewSubMenuContainer.onmouseover = null;
					viewSubMenuContainer.onmouseout = null;
				}, 1*1000);
			};
			viewSubMenuContainer.onmouseover = function() {
				if(obj.Timeout) clearTimeout(obj.Timeout);
				obj.Timeout = null;
			};
			eXo.core.DOMUtil.hideElements();
		}
	}
};

PluginUtils.prototype.changeFilter = function() {
	var rightWS = document.getElementById('RightWorkspace');
	var tblRWS	= eXo.core.DOMUtil.findDescendantsByTagName(rightWS, "table")[0];
	var rowsRWS = eXo.core.DOMUtil.findDescendantsByTagName(tblRWS, "tr");
	if(rowsRWS && rowsRWS.length > 0) {
		for(var i = 0; i < rowsRWS.length; i++) {
			if(i > 0) tblRWS.deleteRow(rowsRWS[i].rowIndex);
		}
	} 
	eXoWCM.PluginUtils.listFiles();
	if(eXp.store.currentNode)	 getDir(eXp.store.currentNode, eXp.store.eventNode);
}

PluginUtils.prototype.fixHeightTrees = function() {
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

if(!window.eXoWCM) eXoWCM = new Object();
eXoWCM.PluginUtils = new PluginUtils();