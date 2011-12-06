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

WCMUtils.prototype.openPrintPreview = function(urlToOpen) {
	if(urlToOpen.indexOf("?") == -1) {
		return urlToOpen + '?isPrint=true';
	} else {
		return urlToOpen + '&isPrint=true';
	}
};

WCMUtils.prototype.showInContextHelp = function(id, isIn){
  var parentElm = document.getElementById(id);
  var popupHelp = document.getElementById(id+"ID");
  var inContextContentHelp = eXo.core.DOMUtil.findFirstDescendantByClass(parentElm,"div","InContextHelpContent");
  var wTmp = 1;
  if(inContextContentHelp){
    if(isIn == "true"){
      inContextContentHelp.style.display = "block";
      var inContextHelpPopup = eXo.core.DOMUtil.findFirstDescendantByClass(inContextContentHelp,"div","InContextHelpPopup");
      var contentHelp = eXo.core.DOMUtil.findFirstDescendantByClass(popupHelp,"div","InContextHelpContentData");
      var contentPosition = eXo.core.DOMUtil.findFirstDescendantByClass(inContextContentHelp,"div","ContentPosition");
      var l = String(contentHelp.innerHTML).length;
      if(l < 100){
        contentHelp.style.width = (20 + l*4) + "px"
        inContextContentHelp.style.width = (20 + l*4 + 54) + "px"
        wTmp = (20 + l*4 + 54);
        contentPosition.style.height = "auto";
      } else {
        contentHelp.style.width = "400px"
        inContextContentHelp.style.width = "454px"
        wTmp = 454;
        contentPosition.style.height = (contentHelp.offsetHeight - 26) + "px";
      }
      //Firt, set the style is left shown
      inContextContentHelp.style.left = "-"  + (wTmp) + "px";
      popupHelp.className = "LeftInContextHelpPopup";
      //Then check if the left of helpPopupWindows is outside the left of Webcontent
      
      var accumulateLeft = 0;
      var parentObj = inContextContentHelp;
      do {
        accumulateLeft = accumulateLeft  + parentObj.offsetLeft;
        parentObj = parentObj.offsetParent;
      }while (parentObj);
      //If the popup is outside the webcontent, change it to right shown
      if (accumulateLeft <0) {
        inContextContentHelp.style.left = "12px";
        popupHelp.className = "RightInContextHelpPopup";
      }      
    } else {
      inContextContentHelp.style.display = "none";
    }
  }  
};

WCMUtils.prototype.showHideComponent = function(elemtClicked) {		
		var nodeReference = eXo.core.DOMUtil.findAncestorByClass(elemtClicked,  "ShowHideContainer");    
		var elemt = eXo.core.DOMUtil.findFirstDescendantByClass(nodeReference, "div", "ShowHideComponent") ;		
		if(elemt.style.display == 'none') {		
			elemtClicked.childNodes[0].style.display = 'none' ;
			elemtClicked.childNodes[1].style.display = 'block' ;
			elemt.style.display = 'block' ;
			eXo.ecm.WCMUtils.setScrollBar();
		} else {			
			elemtClicked.childNodes[0].style.display = 'block' ;
			elemtClicked.childNodes[1].style.display = 'none' ;
			elemt.style.display = 'none' ;
		}
};

WCMUtils.prototype.setScrollBar = function()  {     
    try	{
      var elementWorkingArea = document.getElementById('UIWorkingArea');
      var parent = document.getElementById('TabContainerParent'); 
      if(parent!=null)	{
        var elements  = eXo.core.DOMUtil.findDescendantsByClass(parent,"div", "UITabContent"); 
        if(elements!=null)	{      
					for(i=0;i<elements.length;i++)
					{    
						var obj = elements[i];        
						if(obj.style.display!="none")	{
							var height = obj.offsetHeight;   							
							if(height>430)	{							                  
								obj.style.height=elementWorkingArea.offsetHeight-50+"px";
							  obj.style.overflow="auto";
							}
						}
					}
				} 
      }     
    }
    catch(err){}
}; 

//focus to the first element in the form after loading successfuly
WCMUtils.prototype.autoFocus = function() {
	var uiDocumentForm = document.getElementById("UIDocumentForm");
	for (var i = 0; uiDocumentForm.elements[i].type == 'hidden'; i++);
	setTimeout(function() {
		try {
			uiDocumentForm.elements[i].focus();
		} catch(err){}
	}, 200);
}

eXo.ecm.WCMUtils = new WCMUtils();