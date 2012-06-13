var LinkSelector = {
	init : function() {
		with (window.opener.eXo.env.portal) {
			this.portalName = portalName;
			this.context = context;	
			this.userId = userName;
			this.userLanguage = language;
			var parentLocation = window.opener.location;
			this.hostName = parentLocation.href.substring(0, parentLocation.href.indexOf(parentLocation.pathname));
			this.repositoryName = "repository";
			this.workspaceName = "collaboration";
			this.cmdPortalLink = "/portalLinks/";
			this.cmdGetFolderAndFile = "getFoldersAndFiles?";
			this.resourceType = eXp.getUrlParam("type") || "File";
			this.connector	= eXp.getUrlParam("connector") ||  window.opener.eXo.ecm.WCMUtils.getRestContext();
			this.currentNode = "";
			this.currentFolder = "/";
			this.xmlHttpRequest = false;
			this.eventNode = false;
		}
		this.initGadget();
	}
}; 

LinkSelector.initGadget = function() {
	var command = LinkSelector.cmdPortalLink + LinkSelector.cmdGetFolderAndFile+"type="+LinkSelector.resourceType;
	var url = LinkSelector.hostName + LinkSelector.connector + command;
	LinkSelector.requestPortalLink(url);
};

LinkSelector.requestPortalLink = function(url) {
	if(window.XMLHttpRequest && !(window.ActiveXObject)) {
  	try {
			LinkSelector.xmlHttpRequest = new XMLHttpRequest();
    } catch(e) {
			LinkSelector.xmlHttpRequest = false;
    }
  } else if(window.ActiveXObject) {
     	try {
      	LinkSelector.xmlHttpRequest = new ActiveXObject("Msxml2.XMLHTTP");
    	} catch(e) {
      	try {
        	LinkSelector.xmlHttpRequest = new ActiveXObject("Microsoft.XMLHTTP");
      	} catch(e) {
        	LinkSelector.xmlHttpRequest = false;
      	}
		}
  }
	if(LinkSelector.xmlHttpRequest) {
		LinkSelector.xmlHttpRequest.onreadystatechange = LinkSelector.processLinks;
		LinkSelector.xmlHttpRequest.open("GET", url, true);
		LinkSelector.xmlHttpRequest.send();
	}
};

LinkSelector.processLinks = function() {
	if (LinkSelector.xmlHttpRequest.readyState == 4) {
    if (LinkSelector.xmlHttpRequest.status == 200) {
			LinkSelector.loadLinks();
    } else {
        alert("There was a problem retrieving the XML data:\n" + LinkSelector.xmlHttpRequest.statusText);
        return false;
    }
  }
};

LinkSelector.loadLinks = function() {
	var xmlPortalLink = LinkSelector.xmlHttpRequest.responseXML;
	var nodeList = xmlPortalLink.getElementsByTagName("Folder");
	var treeHTML = '';
	for(var i = 0; i < nodeList.length; i++) {
		var nameFolder = nodeList[i].getAttribute("name");
		treeHTML 		+= 		'<div class="Node" onclick="">';
		treeHTML		+=			'<div class="Expand">';
		treeHTML 		+= 				'<div name="/'+nameFolder+'/" class="IconNode nt_unstructured16x16Icon 16x16Icon" onclick="LinkSelector.listPortalLinks(this);">'	;
		treeHTML		+= 					'<a href="javascript: void(0);">'+nameFolder+'</a>';
		treeHTML		+=				'</div>';
		treeHTML		+=			'</div>';
		treeHTML		+=		'</div>';
		treeHTML		+=		'<div class="ChildrenContainer" style="display:none;">';
		treeHTML		+=		'</div>';
	}
	var uiLeftWorkspace = document.getElementById('LeftWorkspace');	
	if(uiLeftWorkspace) uiLeftWorkspace.innerHTML = treeHTML;
};	

LinkSelector.listPortalLinks = function(oPortalLink) {
	var parentNode = eXo.core.DOMUtil.findAncestorByClass(oPortalLink, "Node");
	var nodeGroup = eXo.core.DOMUtil.findNextElementByTagName(parentNode, "div");
	var namePortalLink = oPortalLink.getAttribute('name');
	var command = LinkSelector.cmdPortalLink + LinkSelector.cmdGetFolderAndFile+"type="+LinkSelector.resourceType+"&currentFolder="+namePortalLink;
	var url = LinkSelector.hostName + LinkSelector.connector + command+"&lang="+LinkSelector.userLanguage;
	var xmlLinks = eXo.ecm.WCMUtils.request(url);
	var folderLinks = xmlLinks.getElementsByTagName('Folder');
	if(folderLinks.length < 0)	return;
	var treeHTML = '';
	for(var i = 0; i < folderLinks.length; i++) {
		var folderLink = folderLinks[i].getAttribute('name');
		var currentFolder = namePortalLink + folderLink;
		treeHTML += '<div class="Node">';
		treeHTML += 	'<div class="Expand">';	
		treeHTML +=			'<div name="'+currentFolder+'/" class="IconNode nt_unstructured16x16Icon 16x16Icon" onclick="LinkSelector.listPortalLinks(this);">';
		treeHTML +=				'<a href="javascript:void(0);">'+folderLink+'</a>';
		treeHTML +=			'</div>';
		treeHTML +=		'</div>';
		treeHTML +=	'</div>';
		treeHTML +=	'<div class="ChildrenContainer" style="display:none;">';
		treeHTML +=	'</div>';
	}
	if (treeHTML.length > 0) {
		nodeGroup.innerHTML = treeHTML;
	} else {
		nodeGroup.style.height = 0;
	}
	var iconElt = eXo.core.DOMUtil.getChildrenByTagName(parentNode, "div")[0]
	if(nodeGroup.style.display != 'block') {
		nodeGroup.style.display = 'block';
		iconElt.className = 'Collapse';
	} else {
		nodeGroup.style.display = 'none';
		iconElt.className = 'Expand';
	}
	
	var fileLinks = xmlLinks.getElementsByTagName('File');
	LinkSelector.listFileLinks(fileLinks);
};

LinkSelector.listFileLinks = function(fileLinks) {
	var tblRWS  = document.getElementById('ListRecords');
	var rowsRWS = eXo.core.DOMUtil.findDescendantsByTagName(tblRWS, "tr");
	if(rowsRWS && rowsRWS.length > 0) {
		for(var i = 0; i < rowsRWS.length; i++) {
			if(i > 0) tblRWS.deleteRow(rowsRWS[i].rowIndex);
		}
	} 
	if(!fileLinks || fileLinks.length <= 0) {
		var tdNoContent = tblRWS.insertRow(1).insertCell(0);
		tdNoContent.innerHTML = "There is no content";
		tdNoContent.className = "Item TRNoContent";
		tdNoContent.setAttribute('colspan', 2);
		return;
	}

	var clazz = 'EventItem';
	for(var i = 0; i < fileLinks.length; i++) {
		if(clazz == 'EventItem') {
			clazz = 'OddItem';
		} else if(clazz == 'OddItem') {
			clazz = 'EventItem';
		}
		var nameLink = fileLinks[i].getAttribute('name');
		var urlLink	 = fileLinks[i].getAttribute('url');
		var dateCreated = fileLinks[i].getAttribute('dateCreated');
		var newRow = tblRWS.insertRow(i+1);
		newRow.className = clazz;
		newRow.insertCell(0).innerHTML = '<div class="Item default16x16Icon" url="'+urlLink+'" onclick="LinkSelector.insertLink(this);">'+nameLink+'</div>';
		newRow.insertCell(1).innerHTML = '<div class="Item">'+ dateCreated +'</div>';
	}
};

LinkSelector.insertLink = function(portalLink) {
	var parent = window.opener.window;
	if (parent.document.getElementById("txtUrl")) {
		parent.document.getElementById("txtUrl").value = portalLink.getAttribute('url');
	}
	window.close();
};