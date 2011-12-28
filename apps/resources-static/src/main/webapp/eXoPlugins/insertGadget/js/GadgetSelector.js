var GadgetSelector = {
	init : function() {
		with (window.opener.eXo.env.portal) {
			this.portalName = portalName;
			this.context = context;	
			this.userId = userName;
			this.userLanguage = language;
			var parentLocation = window.opener.location;
			this.hostName = parentLocation.href.substring(0, parentLocation.href.indexOf(parentLocation.pathname));
		}
		this.repositoryName = "repository";
		this.workspaceName = "collaboration";
		this.cmdGadgetDriver = "/wcmGadget/";
		this.cmdGetFolderAndFile = "getFoldersAndFiles?";
		this.resourceType = eXp.getUrlParam("type") || "File";
		this.connector	= eXp.getUrlParam("connector") ||  window.opener.eXo.ecm.WCMUtils.getRestContext();
		this.currentNode = "";
		this.currentFolder = "/";
		this.xmlHttpRequest = false;
		this.eventNode = false;
		
		var currentEditor = eXp.getUrlParam("currentInstance") || "";
		GadgetSelector.currentEditor = eval('CKEDITOR.instances.'+currentEditor);
		
		this.initGadget();
	}
}; 

GadgetSelector.initGadget = function() {
	var command = GadgetSelector.cmdGadgetDriver + GadgetSelector.cmdGetFolderAndFile+"type="+GadgetSelector.resourceType;
	var url = GadgetSelector.hostName + GadgetSelector.connector + command + "&host="+GadgetSelector.hostName;
	GadgetSelector.requestGadget(url);
};

GadgetSelector.requestGadget = function(url) {
	if(window.XMLHttpRequest && !(window.ActiveXObject)) {
  	try {
			GadgetSelector.xmlHttpRequest = new XMLHttpRequest();
    } catch(e) {
			GadgetSelector.xmlHttpRequest = false;
    }
  } else if(window.ActiveXObject) {
     	try {
      	GadgetSelector.xmlHttpRequest = new ActiveXObject("Msxml2.XMLHTTP");
    	} catch(e) {
      	try {
        	GadgetSelector.xmlHttpRequest = new ActiveXObject("Microsoft.XMLHTTP");
      	} catch(e) {
        	GadgetSelector.xmlHttpRequest = false;
      	}
		}
  }
	if(GadgetSelector.xmlHttpRequest) {
		GadgetSelector.xmlHttpRequest.onreadystatechange = GadgetSelector.processGadgets;
		GadgetSelector.xmlHttpRequest.open("GET", url, true);
		GadgetSelector.xmlHttpRequest.send();
	}
};

GadgetSelector.processGadgets = function() {
	if (GadgetSelector.xmlHttpRequest.readyState == 4) {
    if (GadgetSelector.xmlHttpRequest.status == 200) {
			GadgetSelector.loadGadgets();
    } else {
        alert("There was a problem retrieving the XML data:\n" + GadgetSelector.xmlHttpRequest.statusText);
        return false;
    }
  }
};

GadgetSelector.loadGadgets = function() {
	var xmlFolderGadget = GadgetSelector.xmlHttpRequest.responseXML;
	var nodeList = xmlFolderGadget.getElementsByTagName("Folder");
	GadgetSelector.resourceType = xmlFolderGadget.getElementsByTagName("Connector")[0].getAttribute("resourceType");
	GadgetSelector.currentFolder = xmlFolderGadget.getElementsByTagName("CurrentFolder")[0].getAttribute("name");
	var treeHTML = '';
	for(var i = 0; i < nodeList.length; i++) {
		var nameFolder = nodeList[i].getAttribute("name");
		treeHTML 	  +=	'<div class="NodeGroup">';
		treeHTML 		+= 		'<div class="Node">';
		treeHTML		+=			'<div class="Expand">';
		treeHTML 		+= 				'<div name="/'+GadgetSelector.currentFolder+'/" class="IconNode nt_unstructured16x16Icon 16x16Icon">'	;
		treeHTML		+= 					'<a href="javascript: void(0);">'+nameFolder+'</a>';
		treeHTML		+=				'</div>';
		treeHTML		+=			'</div>';
		treeHTML		+=		'</div>';
		treeHTML		+=	'</div>';
	}
	var uiLeftWorkspace = document.getElementById('LeftWorkspace');	
	if(uiLeftWorkspace) uiLeftWorkspace.innerHTML = treeHTML;

	// right workspace
	var command = GadgetSelector.cmdGadgetDriver + GadgetSelector.cmdGetFolderAndFile+"type="+GadgetSelector.resourceType;
	var url = GadgetSelector.hostName + GadgetSelector.connector + command +"&currentFolder=/"+GadgetSelector.currentFolder+"/&host="+GadgetSelector.hostName;
	var xmlListGadget = eXo.ecm.WCMUtils.request(url);
	var listFileGadget = xmlListGadget.getElementsByTagName("File");
	var tblGadget = '';
	var listGadgetSource = [];
	for(var k = 0; k < listFileGadget.length; k++) {
		var gadget = listFileGadget[k];
		var nameGadget 	= gadget.getAttribute("name");
		var srcImg			= gadget.getAttribute("thumbnail").replace("http://localhost:8080", "");
		listGadgetSource.push({
			"name":nameGadget,
			"metadata":gadget.getAttribute("metadata"),
			"src":gadget.getAttribute("thumbnail").replace("http://localhost:8080", ""),
			"url":GadgetSelector.hostName+gadget.getAttribute("url")
		});
		GadgetSelector.listGadgetSource = listGadgetSource;
		tblGadget 	+=	'<table class="ThumbnailTable">';
		tblGadget		+=		'<tr>';
		tblGadget		+=			'<td>';
		tblGadget		+=				'<img alt="'+nameGadget+'" name="'+nameGadget+'" height="80" src="'+srcImg+'" onclick="GadgetSelector.insertGadget(this);">';		
		tblGadget		+=				'<div>'+nameGadget+'</div>';		
		tblGadget		+=			'</td>';
		tblGadget		+=		'</tr>';		
		tblGadget 	+=	'</table>';
		delete gadgetSource;
	}
	var displayArea = document.getElementById("DisplayArea");
	displayArea.innerHTML = tblGadget;
};

GadgetSelector.getElementsByClassPath = function(root, path) {
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
};	

// private function
GadgetSelector.hasClass = function(element, className) {
	return (new RegExp("(^|\\s+)" + className + "(\\s+|$)").test(element.className)) ;
};

GadgetSelector.getParent = function(element, className) {
	if (!element) return null;
	var parent = element.parentNode;
	while (parent && parent.nodeName != "HTML") {
		if (hasClass(parent, className)) return parent;
		parent =  parent.parentNode;
	}
	return null;
};

GadgetSelector.hideContextMenu = function() {
	var aObjects = GadgetSelector.getElementByClassName('ContextMenu');
	iLength = aObjects.length;
	for (var i = 0; i < iLength; i++) {
		aObjects[i].style.display = 'none';
	}
};

GadgetSelector.insertGadget = function(oGadget) {
	var nameGadget = oGadget.getAttribute("name");
	var gadgetSources = GadgetSelector.listGadgetSource;
	var gadget = false;
	for(var i = 0; i < gadgetSources.length; i++) {
		if(gadgetSources[i].name == nameGadget) {
				gadget = gadgetSources[i];
		}
	}
	var metadata = gadget.metadata;
	var url = gadget.url;
	var src = gadget.src;
	var editor = GadgetSelector.currentEditor;
	var random = new Date().getTime();
	var newTag = editor.document.createElement("div");;
	newTag.setAttribute("class", "TmpElement");
	newTag.setAttribute("id", random);
	newTag.setAttribute('float', 'left');
	var newScript = editor.document.createElement("script");
	if(eXo.core.Browser.browserType == 'ie') {
		newScript.$.text = "eXo.core.Browser.addOnLoadCallback('" + random + "', function() {eXo.gadget.UIGadget.createGadget('" + url + "','" + random + "', " + metadata + ", '{}', 'home', '/eXoGadgetServer/gadgets', 0, 0)}); document.getElementById('icon_"+random+"').style.display = 'none';";
	} else {
		newScript.setText("eXo.core.Browser.addOnLoadCallback('" + random + "', function() {eXo.gadget.UIGadget.createGadget('" + url + "','" + random + "', " + metadata + ", '{}', 'home', '/eXoGadgetServer/gadgets', 0, 0)}); document.getElementById('icon_"+random+"').style.display = 'none';");
	}
	editor.insertElement(newTag);
	editor.insertElement(newScript);
	var oFakeNode = editor.document.createElement("img");
	oFakeNode.className = 'cke_flash' ;
	oFakeNode.setAttribute("id", 'icon_'+random);
	oFakeNode.setAttribute('src',src);
	newTag.insertBeforeMe(oFakeNode);
	window.close();
};