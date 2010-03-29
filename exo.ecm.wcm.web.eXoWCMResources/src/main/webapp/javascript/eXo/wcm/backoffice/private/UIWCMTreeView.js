function UIWCMTreeView(){
	this.constParentNode = 'UIWCMTreeParentNode';
	this.constChildNode = 'UIWCMTreeChildNode';
	this.constBreadcumbsElement ='UITreeViewBreadcumbsElement';
	this.constComponentId = "$uicomponent.id";
	this.oldId;
	this.ajaxAction = "<%=uicomponent.event('ChangeNode','WCMTreeBuilderParameters')%>";
	while(this.ajaxAction.indexOf('amp;') > -1){
		this.ajaxAction = this.ajaxAction.replace("amp;", "");
	}
}
	
UIWCMTreeView.prototype.replaceAll = function(input, ch1, ch2){
	while(input.indexOf(ch1) > -1){
		input = input.replace(ch1, ch2);
	}
	return input;
};
	
UIWCMTreeView.prototype.request = function(urlRequestXML, objChild) {
	var httpRequest = null;
	var response = null;
	if (window.XMLHttpRequest) {
		httpRequest = new XMLHttpRequest();
	} else if (window.ActiveXObject) {
		httpRequest = new ActiveXObject("Microsoft.XMLHTTP");
	} else {
		alert("There was a problem retrieving the XML data!");
		return;
	}
	try{
		httpRequest.open("GET", urlRequestXML, true);
		httpRequest.onreadystatechange = function() {
			if(httpRequest.readyState == 4){
				if(httpRequest.status == 200){
					response = httpRequest.responseXML;
					var list = response.getElementsByTagName("childNodes");
					if(list && list.length > 0){
						list = list[0].getElementsByTagName("childNode");
						objChild.innerHTML="";
						parentPath = document.getElementById(
													objChild.id.replace(eXo.ecm.UIWCMTreeView.constChildNode, 
																		eXo.ecm.UIWCMTreeView.constParentNode)
																		).getAttribute("treePath");
						var restAction = "";
						for(var i = 0; i < list.length; i ++){;
							workspaceName = list[i].getElementsByTagName("workspaceName")[0].childNodes[0].nodeValue;
							repositoryName = list[i].getElementsByTagName("repositoryName")[0].childNodes[0].nodeValue;
							name = list[i].getElementsByTagName("name")[0].childNodes[0].nodeValue;
							nodePath = list[i].getElementsByTagName("nodePath")[0].childNodes[0].nodeValue;
						
							treePath = parentPath + "/" + name;
							firstId = eXo.ecm.UIWCMTreeView.replaceAll(
														eXo.ecm.UIWCMTreeView.replaceAll(treePath, "/", "rp"), " ", "");
							restAction = "eXo.ecm.UIWCMTreeView.selectNode('"+firstId+"','"+nodePath+"', '"+workspaceName+"', '"+repositoryName+"');" + 
														eXo.ecm.UIWCMTreeView.ajaxAction.replace("WCMTreeBuilderParameters",
														eXo.ecm.UIWCMTreeView.replaceAll(eXo.ecm.UIWCMTreeView.replaceAll(
																																nodePath+"/"+workspaceName+"/"+firstId,"/", "%2F"), " ", "+"));
																																
							objChild.innerHTML += '<div class="NoneSelectNodeIcon" onclick="' + restAction + '" id="'+
																		eXo.ecm.UIWCMTreeView.constParentNode + firstId+'" treePath="'+treePath+'">'+
																		'<div class="DefaultPageIcon">'+name+'</div>'+
																		'</div>'+
																		'<div class="ViewChildNodes" id="'+eXo.ecm.UIWCMTreeView.constChildNode + firstId+'"></div>';
						}
					}
				}else{alert(httpRequest.status +":"+ httpRequest.statusText);}
			}
		}
		httpRequest.send(null);
	}catch(e){alert('Failse: ' + e.message)}
};
	
UIWCMTreeView.prototype.getDir = function(nodePath, workspaceName, repositoryName, objChild) {
	if(objChild.innerHTML && objChild.innerHTML.trim().length > 0 && objChild.innerHTML != "<span></span>") return;
	var parentLocation = window.parent.location;
	hostName = parentLocation.href.substring(0, parentLocation.href.indexOf(parentLocation.pathname));
	var connector = eXo.ecm.WCMUtils.getRestContext() + "/wcmTreeContent/getChildNodes";
	connector += "?nodePath=" + nodePath + "&workspaceName=" + workspaceName + "&repositoryName="+repositoryName;
	try{
		this.request(connector, objChild);
	}catch(e){alert(e.message);}
};
	
UIWCMTreeView.prototype.selectNode = function(childId, nodePath, workspaceName, repositoryName) {
	var objParent = document.getElementById(eXo.ecm.UIWCMTreeView.constParentNode + childId);
	if(objParent){
		objParent.style.fontWeight="bold";
		if(objParent.className == "NoneSelectNodeIcon")objParent.className = "SelectNodeIcon";
		else objParent.className = "NoneSelectNodeIcon";
		if(eXo.ecm.UIWCMTreeView.oldId != (eXo.ecm.UIWCMTreeView.constParentNode + childId)){
			oldObject = document.getElementById(eXo.ecm.UIWCMTreeView.oldId);
			if(oldObject) oldObject.style.fontWeight="normal";
			eXo.ecm.UIWCMTreeView.oldId = eXo.ecm.UIWCMTreeView.constParentNode + childId;
			eXo.ecm.UIWCMTreeView.updateBreadcumbsElement(objParent);
		}
	}
	
	var objChild = document.getElementById(eXo.ecm.UIWCMTreeView.constChildNode + childId);
	if(workspaceName && repositoryName) this.getDir(nodePath, workspaceName, repositoryName, objChild);
	if(objChild)
		if(objChild.style.display === "block") objChild.style.display = "none";
		else objChild.style.display = "block";
	
};
	
UIWCMTreeView.prototype.updateBreadcumbsElement = function(objParent) {
	var objBreadcumbs = document.getElementById("UIWCMTreeViewBreadcumbsHomeIcon");
	objBreadcumbs.innerHTML = "";
	var nodeNames = objParent.getAttribute("treePath").split("/");
	for(var i = nodeNames.length - 1; i >=0; i --){
		var action = "";
		newDiv = document.createElement("div");
		newDiv.id = eXo.ecm.UIWCMTreeView.constBreadcumbsElement + i;
		if(i <= 1) newDiv.className ="FirstNode";
		else newDiv.className ="RightArrowIcon";
		if(i != 0) newDiv.innerHTML = nodeNames[i];
		else newDiv.innerHTML = "";
		if(objParent){
			if(objParent.getAttribute("onclick") && objParent.getAttribute("onclick") != null && objParent.getAttribute("onclick") != "null")
				action = objParent.getAttribute("onclick");
		} else action = "javaScript: void(0)";
		newDiv.setAttribute('onclick', action);
		
		brotherDiv = document.getElementById(eXo.ecm.UIWCMTreeView.constBreadcumbsElement + (i + 1));
		if(brotherDiv) objBreadcumbs.insertBefore(newDiv, brotherDiv)
		else objBreadcumbs.appendChild(newDiv);
		
		if(objParent != null)objParent = objParent.parentNode;
		if(objParent && objParent != null && objParent.id && objParent.id != null && objParent.id.indexOf(eXo.ecm.UIWCMTreeView.constChildNode) > -1){
			objParentId = objParent.id.replace(eXo.ecm.UIWCMTreeView.constChildNode, eXo.ecm.UIWCMTreeView.constParentNode);
			objParent = document.getElementById(objParentId);
		}else{
			objParent = null;
		}
	}
	objParent = document.getElementById(eXo.ecm.UIWCMTreeView.constBreadcumbsElement + (nodeNames.length - 1));
	objParent.style.color="#1B59AA";
	objParent.setAttribute('onclick', 'javaScript:void(0)');
};
	
UIWCMTreeView.prototype.OpenOnePath = function(id, isLoad){
	var obj = document.getElementById("UIWCMTreeParentNode"+id);
	if(obj){
		var action = obj.getAttribute("onclick");
		if(action.indexOf("javascript:ajaxGet") > 0) action = action.substring(0, action.indexOf("javascript:ajaxGet"));
		eval(action);
	}else setTimeout('eXo.ecm.UIWCMTreeView.OpenOnePath("'+id+'",'+isLoad+')', 100);
};
	
UIWCMTreeView.prototype.OpenPath = function(path) {		
	var ids = path.split("/");
	var obj;
	for(var i = 0; i < ids.length; i ++){
		if(i == 0){
			obj = document.getElementById("UIWCMTreeParentNode"+ids[i]);
			var parent = obj.parentNode;
			if(parent) parent.style.display="block";
		} 
		eXo.ecm.UIWCMTreeView.OpenOnePath(ids[i]);
	}
};

eXo.ecm.UIWCMTreeView = new UIWCMTreeView();