function WCMUtils(){
	this.cmdEcmBundle = "/bundle/";
	this.cmdGetBundle = "getBundle?";
	this.showRightContent = true;
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
  var inContextContentHelp = gj(parentElm).find("div.InContextHelpContent:first")[0];
  var wTmp = 1;
  if(inContextContentHelp){
    if(isIn == "true"){
      inContextContentHelp.style.display = "block";
      var inContextHelpPopup = gj(inContextContentHelp).find("div.InContextHelpPopup:first")[0];
      var contentHelp = gj(inContextHelpPopup).find("div.LeftInContextHelpPopup:first")[0];
      var l = String(contentHelp.innerHTML).length;
      if(l < 100){
        contentHelp.style.width = (20 + l*4) + "px"
        inContextContentHelp.style.width = (20 + l*4 + 36) + "px"
        wTmp = (20 + l*4 + 36);
      } else {
        contentHelp.style.width = "400px"
        inContextContentHelp.style.width = "436px"
        wTmp = 436;
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
		var nodeReference = gj(elemtClicked).parents(".ShowHideContainer:first")[0];    
		var elemt = gj(nodeReference).find("div.ShowHideComponent:first")[0];		
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

WCMUtils.prototype.showHideSideBar = function(event) {
  var leftContainer = document.getElementById("LeftContainer");  
  var rightContainer = document.getElementById("RightContainer");
  var resizeBar = document.getElementById("ResizeSideBar");  
  var seoPopup = document.getElementById("UISEOPopupWindow");
  var formContainer = gj(seoPopup).find("div.FormContainer:first")[0];
  var resizeButton = null;
  if(this.showRightContent)
  	resizeButton = gj(resizeBar).find("div.ShowRightContent:first")[0];
  else
  	resizeButton = gj(resizeBar).find("div.ResizeButton:first")[0];
  if(rightContainer.style.display == 'none') {
  	rightContainer.style.display = 'block';  	
  	resizeButton.className = "ResizeSideBar ShowRightContent";	  
	  seoPopup.style.width = "640px";
	  this.showRightContent = true;
	  leftContainer.style.marginRight="244px";
	  formContainer.style.width = "610px";
	  seoPopup.style.left = seoPopup.offsetLeft - 240 + "px";
  } else {
  	rightContainer.style.display = 'none';
  	seoPopup.style.width = "400px";  	
  	resizeButton.className = "ResizeButton";	
  	this.showRightContent = false;
	  leftContainer.style.marginRight="none";
	  formContainer.style.width = "370px";
	  seoPopup.style.left = seoPopup.offsetLeft + 240 + "px";
  }  
}

WCMUtils.prototype.setScrollBar = function()  {     
    try	{
      var elementWorkingArea = document.getElementById('UIWorkingArea');
      var parent = document.getElementById('TabContainerParent'); 
      if(parent!=null)	{
        var elements  = gj(parent).find("div.UITabContent"); 
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
	var contextMenu = gj(menuItemElem).parents(".UIRightClickPopupMenu:first")[0];
	contextMenu.style.display = "none" ;
}

WCMUtils.prototype.setHeightRightContainer = function() {
	var leftContainer = document.getElementById("LeftContainer");
	var rightContainer = document.getElementById("RightContainer");
	if(gj(leftContainer).height() > 455) rightContainer.style.height = gj(leftContainer).height() + "px";
	var seoPopup = document.getElementById("UISEOPopupWindow");
	var formContainer = gj(seoPopup).find("div.FormContainer:first")[0];
	var resizeButton = null;	
	rightContainer.style.display = 'block';
	seoPopup.style.width = "640px";	  
	leftContainer.style.marginRight="395px";
	formContainer.style.width = "610px";
}

WCMUtils.prototype.showSEOLanguage = function(isShow) {
  var addNewSEO = document.getElementById("addNewSEO");
  var selectSEOLanguage = document.getElementById("selectSEOLanguage");  
  if(isShow) {
    addNewSEO.style.display = "none";
    selectSEOLanguage.style.display = "block";
  } else {
    addNewSEO.style.display = "block";
    selectSEOLanguage.style.display = "none";
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
    	var divUINodeTypeSearch = gj(this).parents(".UINodeTypeSearh:first")[0];
    	var tdButtonCell = gj(divUINodeTypeSearch).find("td.ButtonCell:first")[0];
    	var btnSearch = gj(tdButtonCell).find("a")[0];
        eval(btnSearch.getAttribute("href"));
        return false;
    }
  }	
}

WCMUtils.prototype.addEvent = function(element, eventName, handler) {
    var elementId = typeof element != 'object' ? element : element.id;
    var objElement = document.getElementById(elementId);
    if (eventName.toLowerCase().indexOf("focus") != -1 || eventName.toLowerCase().indexOf("blur") != -1) {
        if (objElement.tabIndex == undefined) {
            objElement.tabIndex = "0";
        }
    }
    if (navigator.userAgent.indexOf("MSIE") >= 0) {
      objElement.attachEvent("on" + eventName, handler);
    } else {
      objElement.addEventListener(eventName, handler, false);
    }
};

WCMUtils.prototype.changeStyleClass = function(element, newStyleClass) {
    var elementId = typeof element != 'object' ? element : element.id;
    var objElement = document.getElementById(elementId);
    objElement.className = newStyleClass;
};

WCMUtils.prototype.replaceToIframe = function(txtAreaId) {
  if (!document.getElementById(txtAreaId)) {
    return ;
  }

  var txtArea = document.getElementById(txtAreaId) ;
  var ifrm = document.createElement("IFRAME") ;
  with(ifrm) {
    className = 'ECMIframe' ;
    src = 'javascript:void(0)' ;
    frameBorder = 0 ;
    scrolling = "auto" ;
  }

  var strValue = txtArea.value ;
  txtArea.parentNode.replaceChild(ifrm, txtArea) ;
  try {
    var doc = ifrm.contentWindow.document ;
    doc.open() ;
    doc.write(strValue) ;
    doc.close() ;
  } catch (ex) {}
};

WCMUtils.prototype.setZIndex = function(index) {
	eXo.webui.UIPopup.zIndex = index;
};

WCMUtils.prototype.getBundle = function(key, lang) {
  var command = this.cmdEcmBundle + this.cmdGetBundle + "key=" + key + "&locale=" + lang;
  var url = eXo.ecm.WCMUtils.getRestContext() + command;
  var mXML = this.request(url);
  var message;
  try {
    message = mXML.getElementsByTagName(key)[0];
    return message.getAttribute("value");
  } catch(err) {
    return "";
  }
};

eXo.ecm.WCMUtils = new WCMUtils();
_module.WCMUtils = eXo.ecm.WCMUtils;
