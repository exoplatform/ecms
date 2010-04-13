function WCMUtils(){
}

WCMUtils.prototype.getHostName = function() {
	var parentLocation = window.parent.location;
	return parentLocation.href.substring(0, parentLocation.href.indexOf(parentLocation.pathname));
};

WCMUtils.prototype.request = function(url) {
	var xmlHttpRequest = false;
	if (window.XMLHttpRequest) {
		xmlHttpRequest = new window.XMLHttpRequest();
		xmlHttpRequest.open("GET",url,false);
		xmlHttpRequest.send("");
		return xmlHttpRequest.responseXML;
		}
	else if (ActiveXObject("Microsoft.XMLDOM")) { // for IE
		xmlHttpRequest = new ActiveXObject("Microsoft.XMLDOM");
		xmlHttpRequest.async=false;
		xmlHttpRequest.load(urlRequestXML);
		return xmlHttpRequest;
	}
	return null;
};

WCMUtils.prototype.getCurrentNodes = function(navigations, selectedNodeUri) {
	var currentNodes = new Array();
	var currentNodeUris = new Array();	
	currentNodeUris = selectedNodeUri.split("/");	
	for (var i in navigations) {
		for (var j in navigations[i].nodes) {
			if(navigations[i].nodes[j].name == currentNodeUris[0]) {
				currentNodes[0] = navigations[i].nodes[j];
				break;
			}
		}
	}		
	var parent = currentNodes[0];	
	for(var k = 1; k<currentNodeUris.length; k++) {		
		if(parent.children == 'null')	{		
			break;
		}		
		for(var n in parent.children) {	
			var node = parent.children[n];			
			if(currentNodeUris[k] == node.name) {
				currentNodes[k]=node;
				parent = node;			
				break;
			}
		}
	}
	return currentNodes;
};

WCMUtils.prototype.getRestContext = function() {
	return eXo.env.portal.context + "/" + eXo.env.portal.rest; 
};

eXo.ecm.WCMUtils = new WCMUtils();