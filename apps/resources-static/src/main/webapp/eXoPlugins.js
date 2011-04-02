var eXp = {
};

eXp.init = function(){
	with (window.opener.eXo.env.portal) {
		eXo.ecm.ECS.portalName = portalName;
		eXo.ecm.ECS.context = context;
		eXo.ecm.ECS.accessMode = accessMode;
		eXo.ecm.ECS.userId = userName;
		eXo.ecm.ECS.userLanguage = language;
	}  
	var parentLocation = window.opener.location;
	eXo.ecm.ECS.hostName = parentLocation.href.substring(0, parentLocation.href.indexOf(parentLocation.pathname));
	eXo.ecm.ECS.repositoryName = "repository";
	eXo.ecm.ECS.workspaceName	= "collaboration";
	eXo.ecm.ECS.cmdEcmDriver = "/wcmDriver/";
	eXo.ecm.ECS.cmdGetDriver = "getDrivers?";
	eXo.ecm.ECS.cmdGetFolderAndFile = "getFoldersAndFiles?";
	eXo.ecm.ECS.resourceType = this.getUrlParam("Type") || "File";
	eXo.ecm.ECS.connector = this.getUrlParam("connector") ||  window.opener.eXo.ecm.WCMUtils.getRestContext();
	eXo.ecm.ECS.currentNode = "";
	eXo.ecm.ECS.currentFolder = "/";
	eXo.ecm.ECS.xmlHttpRequest = false;
	eXo.ecm.ECS.driverName = "";
	eXo.ecm.ECS.eventNode = false;
	eXo.ecm.ECS.isUpload = false;
	var currentEditor = this.getUrlParam("currentInstance") || "";
  var insertContentType = this.getUrlParam("insertContentType") || "";
  var viewType = this.getUrlParam("viewType") || "";  
	var components = this.getUrlParam("components") || "";
	eXo.ecm.ECS.currentEditor = eval('CKEDITOR.instances.'+currentEditor);
	eXo.ecm.ECS.insertContentType = insertContentType;
  eXo.ecm.ECS.viewType = viewType;
	eXo.ecm.ECS.components = components;	
};

eXp.getID = function() {
	return Math.random().toString().substring(2);
};

eXp.getUrlParam = function(paramName){
	var oRegex = new RegExp("[\?&]" + paramName + "=([^&]+)", "i");
	var oMatch = oRegex.exec(window.location.search) ; 
	if (oMatch && oMatch.length > 1) return oMatch[1];
	else return "";
};

eXp.getParameterValueByName = function(parameterName){
	parameterName = parameterName.replace(/[\[]/,"\\\[").replace(/[\]]/,"\\\]");
  var regexS = "[\\?&]"+parameterName+"=([^&#]*)";
  var regex = new RegExp( regexS );
  var results = regex.exec( window.location.href );
  if( results == null )
    return "";
  else
    return results[1];
};

eXp.loadScript = function(){
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

eXp.getNodes = function(DomX, nodeName){
	if (!DomX || !nodeName) return null;
	var node = DomX.getElementsByTagName(nodeName);
	if (node.length) return node;
	else return null;
};

eXp.getSingleNode = function(DomX, nodeName) {
	var nodes = this.getNodes(DomX, nodeName);
	if (nodes[0]) return nodes[0];
	else return null;
};

eXp.getNodeValue = function(node, name) {
	if (node || name !== undefined) {
		var attribute = node.attributes.getNamedItem(name);
		if (attribute) return attribute.value;
	}
	return new String();
};
