function WCMUtils(){
	var DOM = eXo.core.DOMUtil;
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

WCMUtils.prototype.hideContextMenu = function(menuItemElem)  {
	var contextMenu = eXo.core.DOMUtil.findAncestorByClass(menuItemElem, "UIRightClickPopupMenu") ;
	contextMenu.style.display = "none" ;
}

WCMUtils.prototype.changePriorityCss = function() {
  priorityField = document.getElementById("priority");
  if(priorityField != null) {
	  if(isNaN(priorityField.value)) {
	  	priorityField.className = "Tip";		
	  }
	  priorityField.onclick = function() { 
	    priorityField.className = "SeoPriority"; 
	    if(isNaN(priorityField.value))
	      priorityField.value = ""; 
	  } ;
	}
}

WCMUtils.prototype.addParamIntoAjaxEventRequest = function(eventReq, extParam) {
    return eventReq.substring(0, eventReq.length - 2) + extParam +  "\')";	
}

WCMUtils.prototype.searchNodeTypeOnKeyPress = function() {
  //process Enter press action
  var element = document.getElementById("NodeTypeText");
  if (element == null) return false;

  element.onkeypress= function(event) {
    var keynum = false;
    if (window.event) { /* IE */
      keynum = window.event.keyCode;
      event = window.event;
    } else if (event.which) { /* Netscape/Firefox/Opera */
      keynum = event.which;
    }
    if (keynum == 0) {
      keynum = event.keyCode;
    } 
    if (keynum == 13) {
    	var divUINodeTypeSearch = eXo.core.DOMUtil.findAncestorByClass(this,"UINodeTypeSearh");
    	var tdButtonCell = eXo.core.DOMUtil.findFirstDescendantByClass(divUINodeTypeSearch, "td", "ButtonCell");
    	var btnSearch = eXo.core.DOMUtil.findDescendantsByTagName(tdButtonCell, "a")[0];
        eval(btnSearch.getAttribute("href"));
        return false;
    }
  }	
}

eXo.ecm.WCMUtils = new WCMUtils();