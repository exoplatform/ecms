(function(gj) {
	var InlineEditor = {
	  init : function() {
	    var parentLocation = window.location;
	    this.hostName = parentLocation.href.substring(0, parentLocation.href.indexOf(parentLocation.pathname));
	    this.xmlHttpRequest = false;
	    this.command = "/contents/editing"
	    this.defaultMethod = "POST";
	    this.languageLoaded = false;
	    this.InternalServerErrorMsg="";
	    this.EmptyTitleErrorMsg = "";
	    this.isModified = false;
	    this.editorName = "";
	    this.draft = "";
	    this.publishLink = "";
	  }
	  
	};
	
	InlineEditor.loadLanguage =function (msg) {
	  if (InlineEditor.languageLoaded) return;
	  if (msg[0]) InlineEditor.InternalServerErrorMsg =decodeURI(msg[0]);
	  if (msg[1]) InlineEditor.EmptyTitleErrorMsg = decodeURI(msg[1]);
	}

	InlineEditor.removeHoverClass = function() {
		gj('.containerHoverClass').removeClass('containerHoverClass');	
		gj('.containerHoverClassInner').removeClass('containerHoverClassInner');		
	}

	InlineEditor.modifyInlineContent = function(e) {
          InlineEditor.init();
          InlineEditor.isModified = true;
	}
	InlineEditor.saveInlineContent = function(e) {
		if(InlineEditor.isModified) {
		  	var container = e.container;
			InlineEditor.editorName = e.name;
			var repo = container.getAttribute("repo");
			var workspace = container.getAttribute("workspace");
			var uuid = container.getAttribute("uuid");
			var sitename = container.getAttribute("sitename");
			var language = container.getAttribute("language");
			var propertyname = container.getAttribute("propertyname");
			InlineEditor.draft = container.getAttribute("draftmsg");
			InlineEditor.publishLink = container.getAttribute("fastpublishlink");
			var data = "";
			if(propertyname.indexOf("exo:title") >= 0)
			  data = e.editable().getText();
			else
			  data = e.getData();
			var params =""; 
	  		params = "newValue=" + encodeURIComponent(data);
			gj(container.$).append("<i class='uiWaitting'></i>");			
                        var parentContainer = gj(container.$).parent();
                        var offset = parentContainer.offset();
			gj(parentContainer).append("<div class='markLayerInline' style='width:"+gj(parentContainer).outerWidth()+"px; height:"+gj(parentContainer).outerHeight()+"px; top:"+offset.top+"px; left:"+offset.left+"px'></div>");
    
	  		InlineEditor.presentationRequestChangePropertyPOST("/property?", propertyname, repo, 
			workspace, uuid, sitename, language, params);  
			InlineEditor.isModified = false;      
		} 
  		return false;
	}

	InlineEditor.onEnterRequest =function () {
	  if (window.event && window.event.keyCode == 13){
	    return false;
	  }
	}
	InlineEditor.presentationSwitchBlock = function (block2hidden, block2show) {
	  var titleBlock = document.getElementById(block2hidden);
	  var editBlock = document.getElementById(block2show);
	  if (titleBlock!=null && editBlock!=null) {
	    titleBlock.style.display="none";
	    editBlock.style.display="block";
	  }  
	}
	
	InlineEditor.presentationRequestChangeTitle = function (oldTitleID, newTitleID, repo, workspace, uuid, block2hidden, block2show, siteName, language, isCKEDITOR){
	  var functionName ="/title?"
	  var strTitle ="";
	  var params ="";
	  if (isCKEDITOR) {
	    strTitle = CKEDITOR.instances[newTitleID].getData();
	  }else {
	    strTitle= document.getElementById(newTitleID).value;
	  }
	  var strOldTitle = document.getElementById(oldTitleID).innerHTML;
	  if (strTitle==null) { 
	    alert(InlineEditor.EmptyTitleErrorMsg);
	    return false;
	  }
	  if (strTitle.length ==0) {
	    alert(InlineEditor.EmptyTitleErrorMsg);
	    return false;
	  }
	  if (strTitle==strOldTitle) {
	    InlineEditor.presentationSwitchBlock(block2hidden, block2show);
	    return false;
	  }
	  params = "newValue=" + encodeURIComponent(strTitle);
	  InlineEditor.presentationRequestChangePropertyPOST(functionName, null, repo, workspace, uuid, siteName, language, params);
	  return false;
	}
	
	InlineEditor.presentationRequestChangeSummary = function (oldSummary, newSummaryID, repo, workspace, uuid, block2hidden, block2show, siteName, language, isCKEDITOR){
	  var functionName ="/summary?"
	  var params =""; 
	  var strSummary ="";
	  if (isCKEDITOR) {
	    strSummary = CKEDITOR.instances[newSummaryID].getData();
	  }else {
	    strSummary= document.getElementById(newSummaryID).value;
	  }  
	  
	  params = "newValue=" + encodeURIComponent(strSummary);
	  InlineEditor.presentationRequestChangePropertyPOST(functionName, null, repo, workspace, uuid, siteName, language, params);  
	  return false;
	}
	
	InlineEditor.presentationRequestChangeText = function (oldText, newTextID, repo, workspace, uuid, block2hidden, block2show, siteName, language, isCKEDITOR){
	  var functionName ="/text?"
	  var params =""; 
	  var strText ="";
	  if (isCKEDITOR) {
	    strText = CKEDITOR.instances[newTextID].getData();
	  }else {
	    strText= document.getElementById(newTextID).value;
	  }  
	  
	  params = "newValue=" + encodeURIComponent(strText);
	  InlineEditor.presentationRequestChangePropertyPOST(functionName, null, repo, workspace, uuid, siteName, language, params);  
	  return false;
	}
	
	InlineEditor.presentationRequestChangeProperty = function (functionName, propertyName, oldText, newTextID, repo, workspace, uuid, block2hidden, block2show, siteName, language, isCKEDITOR){  
	  var params =""; 
	  var strText ="";
	  if (isCKEDITOR) {
	    strText = CKEDITOR.instances[newTextID].getData();
	  }else {
	    strText= document.getElementById(newTextID).value;
	  }  
	  params = "newValue=" + encodeURIComponent(strText);
	  InlineEditor.presentationRequestChangePropertyPOST(functionName, propertyName, repo, workspace, uuid, siteName, language, params);  
	  return false;
	}
	
	InlineEditor.presentationRequestChangePropertyPOST = function (functionName, propertyName, repo, workspace, uuid,  siteName, language, params){
	  var url = InlineEditor.hostName + eXo.env.portal.context + "/" + eXo.env.portal.rest + InlineEditor.command + functionName;
	  url = url + "repositoryName="+repo + "&workspaceName=" + workspace + "&nodeUIID=" + uuid + "&siteName=" + siteName + "&language=" + language;
	  if (propertyName!=null) {
	    url = url + "&propertyName=" +encodeURIComponent( propertyName);
	  }
	  InlineEditor.presentationAjaxRequest(url, params, "POST");
	}
	
	InlineEditor.presentationAjaxRequest = function (url, params, method) {
	  if(window.XMLHttpRequest && !(window.ActiveXObject)) {
	    try {
	      InlineEditor.xmlHttpRequest = new XMLHttpRequest();
	    } catch(e) {
	      InlineEditor.xmlHttpRequest = false;
	    }
	  } else if(window.ActiveXObject) {
	      try {
	        InlineEditor.xmlHttpRequest = new ActiveXObject("Msxml2.XMLHTTP");
	      } catch(e) {
	        try {
	          InlineEditor.xmlHttpRequest = new ActiveXObject("Microsoft.XMLHTTP");
	        } catch(e) {
	          InlineEditor.xmlHttpRequest = false;
	        }
	    }
	  }
	  if(InlineEditor.xmlHttpRequest) {
	    try {
	      InlineEditor.xmlHttpRequest.status = 200;
	    }catch (e) {
	    }
	    InlineEditor.xmlHttpRequest.onreadystatechange = InlineEditor.presentationAjaxResponse;
	    if (method) {
	      InlineEditor.xmlHttpRequest.open(method, url, true);
	      InlineEditor.xmlHttpRequest.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
	    }else {
	      InlineEditor.xmlHttpRequest.open(InlineEditor.defaultMethod, url, true);
	    }
	    if (params) {
	      InlineEditor.xmlHttpRequest.send(params);
	    }else {
	      InlineEditor.xmlHttpRequest.send();
	    }
	  }
	};
	
	InlineEditor.presentationAjaxResponse = function (){
	    var xmlTreeNodes = InlineEditor.xmlHttpRequest.responseXML;
	    if(xmlTreeNodes) {
		    var nodeList = xmlTreeNodes.getElementsByTagName("bundle");   
		    var locale_message = nodeList[0].getAttribute("message"); 
		    if (InlineEditor.xmlHttpRequest.readyState == 4) {
		      if (InlineEditor.xmlHttpRequest.status == 200) {
			if(locale_message == "OK") {
				gj('.uiWaitting').remove();
				gj('.markLayerInline').remove();
                                var inlineEditor = CKEDITOR.instances[InlineEditor.editorName];
				inlineEditor.updateElement();
				var container = inlineEditor.container.$;
                                var tmpContainer = container;
                                var parent = gj(tmpContainer).parent().get(0);
				var edittingContainer = null;
				while(!edittingContainer && parent ) {
					edittingContainer = gj(parent).find(".edittingToolBarContainer, .edittingContent").get(0);
					if(!edittingContainer) {
					  tmpContainer = parent;
					  parent = gj(tmpContainer).parent().get(0);
					} 
				}
				if(edittingContainer) {
					var currentState = gj(edittingContainer).find(".edittingCurrentState").get(0);
                                        var spanElem = gj(currentState).find("span").get(0);
                                        gj(spanElem).remove();
 				        gj(currentState).append('<span class="draftText">'+InlineEditor.draft+'</span>');
					
					var btrGroup = gj(edittingContainer).find(".btn-group").get(0);
					if(gj(btrGroup).find("a").size() <3) {
						var aElem = gj(btrGroup).find("a").get(0);
                                        	gj(aElem).before("<a class=\"btn\" href=\""+InlineEditor.publishLink+"\" rel=\"tooltip\" data-placement=\"bottom\" data-original-title=\"Publish\">            <i class=\"uiIconEcmsPublish\"></i>          </a>");
					}
				}
				gj(document).ready(function() { gj("*[rel='tooltip']").tooltip();});

			}
			else alert(locale_message);
		      }
		    }else {
		      try{
			if (InlineEditor.xmlHttpRequest.status!=200) {
			  alert(InlineEditor.InternalServerErrorMsg + "\n" + InlineEditor.xmlHttpRequest.statusText);
			}      
		      }catch (e) {
		      }
		    }
	     }
	}
	InlineEditor.init();
	window.InlineEditor = InlineEditor;
	
	function WCMQuickEdit() {
	}

	WCMQuickEdit.prototype.removeQuickeditingBlock = function(portletID, quickEditingBlockId) {
		var presentation = document.getElementById(portletID);
		var pNode = presentation.parentNode;
		var quickEditingBlock = document.getElementById(quickEditingBlockId);
		if(quickEditingBlock != null) {
			pNode.removeChild(quickEditingBlock);
		}
	};

	WCMQuickEdit.prototype.insertQuickeditingBlock = function(portletID, quickEditingBlockId) {
		var presentation = document.getElementById(portletID);		
		var parentNode = presentation.parentNode;
		var fistChild = gj(parentNode).children("div:first")[0]; 
		if (fistChild.id == quickEditingBlockId) {
			var quickEditingBlock = document.getElementById(quickEditingBlockId);
			quickEditingBlock.parentNode.removeChild(quickEditingBlock);
		}
		var quickEditingBlock = document.getElementById(quickEditingBlockId);		
		if(quickEditingBlock != null) {
			if(eXo.core.Browser.browserType == "ie") {
				var portalName = eXo.env.portal.portalName;
				if(portalName != "classic") {
					if(portletID == (portalName+"-signin")) quickEditingBlock.style.left = presentation.offsetWidth + quickEditingBlock.offsetWidth + 'px';
				} else {
					if(portletID == (portalName+"-logo") || portletID == (portalName+"-signin")) {
						quickEditingBlock.style.left = presentation.offsetWidth + quickEditingBlock.offsetWidth + 'px';
					}
				}
			}
			parentNode.insertBefore(quickEditingBlock, presentation);
		}
	};

	eXo.ecm.QuickEdit = new WCMQuickEdit();
	return {
		InlineEditor : InlineEditor,
		QuickEdit : eXo.ecm.QuickEdit
	};
})(gj);
