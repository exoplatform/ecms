function WCMBreadcrumbPortlet(){
}

WCMBreadcrumbPortlet.prototype.getCurrentNodes = function(navigations, selectedNodeUri) {
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
	if (parent != null) {
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
	}
	return currentNodes;
};

WCMBreadcrumbPortlet.prototype.getBreadcrumbArr = function(navigations, previousURI, wcmContentTitle){
	var breadcrumbNodes = new Array();
	var breadcrumbForNavigations = new Array();
	var previousNodeUris = previousURI.split("/");
	var JsonObj = {};
	
	for (var i in navigations) {
		for (var j in navigations[i].nodes) {
			if(navigations[i].nodes[j].name == previousNodeUris[0]) {
				JsonObj = {
					resolvedLabel: navigations[i].nodes[j].resolvedLabel,
					uri: navigations[i].nodes[j].uri
				}
				breadcrumbForNavigations[0] = navigations[i].nodes[j];
				breadcrumbNodes[0] = JsonObj;
				break;
			}
		}
	}
	
	function getChild(previousNodeUris, children, index) {
		var breadcrumbForNavigations = new Array();
		for (var i in children) {
			if(previousNodeUris[index] == children[i].name) {
				JsonObj = {
					resolvedLabel: children[i].resolvedLabel,
					uri: children[i].uri
				}
				breadcrumbNodes[index] = JsonObj;
				breadcrumbForNavigations[index] = children[i];
				break;
			}
		}
		if (breadcrumbForNavigations[index].length > 0) {
			getChild(previousNodeUris, breadcrumbForNavigations[index].children, ++index); 
		}
	}
	
	if (previousNodeUris.length > 1)
		getChild(previousNodeUris, breadcrumbForNavigations[0].children, 1);
	
	JsonObj = 	{
					resolvedLabel: wcmContentTitle,
					uri: "#"
				}
	breadcrumbNodes[breadcrumbNodes.length] = JsonObj;
		
	return breadcrumbNodes;
};

WCMBreadcrumbPortlet.prototype.getBreadcrumbs = function(){
	var navigations = eXo.env.portal.navigations;
	var selectedNodeUri = eXo.env.portal.selectedNodeUri;
	var wcmContentTitle = eXo.env.portal.wcmContentTitle;
	var previousURI = eXo.env.portal.previousURI;
	var breadcumbs = new Array();
		
	if (wcmContentTitle != 'null') {
		breadcumbs = eXo.ecm.WCMBreadcrumbPortlet.getBreadcrumbArr(navigations, previousURI, wcmContentTitle);
	} else {
		breadcumbs = eXo.ecm.WCMBreadcrumbPortlet.getCurrentNodes(navigations, selectedNodeUri);
	}
	return breadcumbs;
};

eXo.ecm.WCMBreadcrumbPortlet = new WCMBreadcrumbPortlet();