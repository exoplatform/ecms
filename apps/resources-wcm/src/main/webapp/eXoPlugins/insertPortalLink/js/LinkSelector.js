(function(gj, wcm_utils) {

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
	treeHTML 		+= 		'<div class="uiTreeExplorer" >';
	treeHTML 		+= 			'<ul class="nodeGroup" >';
	for(var i = 0; i < nodeList.length; i++) {
		var nameFolder = nodeList[i].getAttribute("name");		
		treeHTML 		+= 				'<li class="node">';
		treeHTML		+=					'<div class="expandIcon">';
		treeHTML 		+= 						'<div name="/'+nameFolder+'/" onclick="eXo.ecm.LinkSelector.listPortalLinks(this);">'	;
		treeHTML		+= 							'<a href="javascript: void(0);"  data-original-title="'+nameFolder+'" data-placement="bottom" rel="tooltip"><i class="uiIcon16x16FolderDefault "></i>&nbsp;<span>'+nameFolder+'</span></a>';
		treeHTML		+=						'</div>';
		treeHTML		+=					'</div>';	
		treeHTML		+=					'<ul class="ChildrenContainer nodeGroup" style="display:none;">';
		treeHTML		+=					'</ul>';		
		treeHTML		+=				'</li>';	
	}
	treeHTML		+=			'</ul>';
	treeHTML		+=		'</div>';
	var uiLeftWorkspace = document.getElementById('LeftWorkspace');	
	if(uiLeftWorkspace) uiLeftWorkspace.innerHTML = treeHTML;
};	

LinkSelector.listPortalLinks = function(oPortalLink) {
	var parentNode = gj(oPortalLink).parents(".node:first")[0];
	var nodeGroup = gj(parentNode).find("ul:first")[0];
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
		treeHTML += '<li class="node">';
		treeHTML += 	'<div class="expandIcon">';	
		treeHTML +=			'<div name="'+currentFolder+'/" onclick="eXo.ecm.LinkSelector.listPortalLinks(this);">';
		treeHTML +=				'<a href="javascript:void(0);" data-original-title="'+folderLink+'" data-placement="bottom" rel="tooltip"><i class="uiIcon16x16FolderDefault "></i>&nbsp;<span>'+folderLink+'</span></a>';
		treeHTML +=			'</div>';
		treeHTML +=		'</div>';		
		treeHTML +=	'<ul class="ChildrenContainer nodeGroup" style="display:none;">';
		treeHTML +=	'</ul>';
		treeHTML +=	'</li>';
	}
	if (treeHTML.length > 0) {
		nodeGroup.innerHTML = treeHTML;
	} else {
		nodeGroup.style.height = 0;
	}
//	var iconElt = eXo.core.DOMUtil.getChildrenByTagName(parentNode, "div")[0]
	var iconElt = gj(parentNode).children("div")[0];
	if(nodeGroup.style.display != 'block') {
		nodeGroup.style.display = 'block';
		iconElt.className = 'collapseIcon';
	} else {
		nodeGroup.style.display = 'none';
		iconElt.className = 'expandIcon';
	}
	
	var fileLinks = xmlLinks.getElementsByTagName('File');
	LinkSelector.listFileLinks(fileLinks);
};

LinkSelector.listFileLinks = function(fileLinks) {
	var tblRWS  = document.getElementById('ListRecords');
	var rowsRWS = gj(tblRWS).find("tr");
	if(rowsRWS && rowsRWS.length > 0) {
		for(var i = 0; i < rowsRWS.length; i++) {
			if(i > 0) tblRWS.deleteRow(rowsRWS[i].rowIndex);
		}
	} 
	if(!fileLinks || fileLinks.length <= 0) {
		var tdNoContent = tblRWS.insertRow(1).insertCell(0);
		tdNoContent.innerHTML = "There is no content";
		tdNoContent.className = "empty";
		tdNoContent.setAttribute('colspan', 2);
		return;
	}

	for(var i = 0; i < fileLinks.length; i++) {
		var nameLink = fileLinks[i].getAttribute('name');
		var urlLink	 = fileLinks[i].getAttribute('url');
		var dateCreated = fileLinks[i].getAttribute('dateCreated');
		var newRow = tblRWS.insertRow(i+1);
		newRow.insertCell(0).innerHTML = '<div class="item" url="'+urlLink+'" onclick="eXo.ecm.LinkSelector.insertLink(this);" style="cursor: pointer"><i class="uiIcon16x16FileDefault "></i>&nbsp;'+nameLink+'</div>';
		newRow.insertCell(1).innerHTML = '<div class="item">'+ dateCreated +'</div>';
	}
};

LinkSelector.insertLink = function(portalLink) {
	var parent = window.opener.window;
	if (parent.document.getElementById("txtUrl")) {
		parent.document.getElementById("txtUrl").value = portalLink.getAttribute('url');
	}
	window.close();
};

eXo.ecm.LinkSelector = LinkSelector;

	return {
		LinkSelector : eXo.ecm.LinkSelector
	};

})(gj, wcm_utils);

