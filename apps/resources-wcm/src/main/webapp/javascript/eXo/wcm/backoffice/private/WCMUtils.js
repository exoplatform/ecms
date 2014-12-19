(function(gj, base) {
	// WCMUtils
	function WCMUtils(){
		this.cmdEcmBundle = "/bundle/";
		this.cmdGetBundle = "getBundle?";
		this.cmdSocialPeople = "/social/people/";
		this.cmdGetPeople = "getPeopleInfo/";
		
		this.showRightContent = true;
	}
	
	WCMUtils.prototype.getHostName = function() {
    var hostName;
    if(self == top){
      var parentLocation = window.parent.location;
       hostName = parentLocation.href.substring(0, parentLocation.href.indexOf(parentLocation.pathname));
    } else {
      // If window is iframe, location should be parsing from src property
      var url; 
      if (eXo.core.Browser.ie) {
        url = window.frameElement.src;
      } else {
        url = window.src;
      }
      var parser = document.createElement('a');
      parser.href = url;
      hostName =  parser.protocol + "//" + parser.hostname + ":" + parser.port;
    }
    return hostName;
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
			var nodeReference = gj(elemtClicked).parents(".showHideContainer:first")[0];    
			var elemt = gj(nodeReference).find("div.showHideComponent:first")[0];		
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
		var contextMenu = gj(menuItemElem).parents(".uiRightClickPopupMenu:first")[0];
		contextMenu.style.display = "none" ;
	};

	WCMUtils.prototype.setHeightRightContainer = function() {
		var leftContainer = document.getElementById("LeftContainer");
		var rightContainer = document.getElementById("RightContainer");
		if(gj(leftContainer).height() > 455) rightContainer.style.height = gj(leftContainer).height() + "px";
		var seoPopup = document.getElementById("UISEOPopupWindow");
		var formContainer = gj(seoPopup).find("div.formContainer:first")[0];
		var resizeButton = null;	
		rightContainer.style.display = 'block';
		seoPopup.style.width = "640px";
	};

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
	};
	
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
		  	var uiSearchInput = gj(this).parents(".uiSearchInput:first")[0];
		  	var btnSearch = gj(uiSearchInput).find("a")[0];
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
		var isFocusOnCKEditor = false;
		try {
			if(CKEDITOR) {
				for(name in CKEDITOR.instances)
				{
					var editor = CKEDITOR.instances[name];
					if(editor.focusManager.hasFocus) isFocusOnCKEditor = true;
				}
			}
		} catch(err) {}

	    if(!isFocusOnCKEditor) {
	      var elementId = typeof element != 'object' ? element : element.id;
	      var objElement = document.getElementById(elementId);
	      objElement.className = newStyleClass;
	    }
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

	WCMUtils.prototype.initSearch = function(componentId, searchInputName, searchLabel) {
		var uiComponent = document.getElementById(componentId);
		var input = gj(uiComponent).find("input")[0];
		gj(input).attr('autocomplete', 'off');
		gj(input).attr('title', searchLabel);
		gj(input).val(searchLabel);
		eXo.ecm.WCMUtils.decorateInput(input, searchLabel, true);
	};

	WCMUtils.prototype.decorateInput = function(input, defaultValue, defaultCondition) {
		if (gj(input).val() == defaultValue && defaultCondition )
		input.form.onsubmit = function() {
		  return false;
		};
		gj(input).focus(function() {
		  if (gj(this).val() == defaultValue && defaultCondition)
		    gj(this).val('');
		});
		gj(input).blur(function() {
		  if (gj(this).val() == '') {
		    gj(this).val(defaultValue);
		  }
		});
	};
	
		
	WCMUtils.prototype.loadAvartar = function(userId, imgTag) {
	  var command = this.cmdSocialPeople + this.cmdGetPeople + userId + ".json";
	  var restUrl = eXo.ecm.WCMUtils.getRestContext() + command;
		gj.ajax({
			 type: "GET",
			 url: restUrl
		}).complete(function (jqXHR) {
			 if (jqXHR.readyState === 4) {
				 var userData = gj.parseJSON(jqXHR.responseText);
				 gj(imgTag).attr("src", userData.avatarURL);
			 }
		});
	};
	
	WCMUtils.prototype.onLoadComments = function() {
		var comments = gj('#UIDocumentWorkspace').find("div.comments:first")[0];
		gj(comments).find("a.avatarMedium").each(function(i) {
			var commentor = gj(this).attr("commentor");
			var img = gj(this).find("img:first")[0];
			eXo.ecm.WCMUtils.loadAvartar(commentor, img);
		});
	};

	WCMUtils.prototype.loadImageForFileActivityCallback = function(obj){
      		var img = gj(obj).nextAll("a:first")[0];
      		img.style.display = "block";
      		obj.style.display = "none";
      		gj(obj.parentNode).removeClass();
      		gj(obj.parentNode).addClass("fileTypeContent");
      		
    	};

    WCMUtils.prototype.getPlacement = function (element) {
        var offset = gj(element).offset();
        var height = gj(document).outerHeight();
        var width = gj(document).outerWidth();
        var vert = 0.5 * height - offset.top;
        var vertPlacement = vert > 0 ? 'bottom' : 'top';
        var horiz = 0.5 * width - offset.left;
        var horizPlacement = horiz > 0 ? 'right' : 'left';
        var placement = Math.abs(horiz) > Math.abs(vert) ?  horizPlacement : vertPlacement;
        return placement;
    };

    WCMUtils.prototype.pingUserStatus = function() {
      var userStatus = gj("#user-status");
      var frequency = 15;
      var delay = 60;
      if(userStatus.html() != undefined) {
        frequency = gj(userStatus).attr("user-status-ping-frequency");
        delay = gj(userStatus).attr("user-status-offline-delay");
      }
      var pingEvent = window.clearInterval(pingEvent);
      pingEvent = setInterval(gj.proxy(eXo.ecm.WCMUtils.sendPing, eXo.ecm.WCMUtils), frequency*1000);
      eXo.ecm.WCMUtils.sendPing();
    }

    WCMUtils.prototype.sendPing = function() {
      gj.ajax({
        url: "/rest/state/ping/",
        dataType: "json",
        context: this,
        success: function(data){

        },
        error: function(){
     
        }
     });
   };


    WCMUtils.prototype.showPopover = function (element) {
        gj(element).popover({template: '<div class="popover"><div class="arrow"></div><div class="inner"><h3 class="popover-title" style="display:none;"></h3><div class="popover-content"><p></p></div></div></div>'});
        gj(element).popover('show');
    };

    WCMUtils.prototype.hidePopover = function (element) {
        gj(element).popover('hide');
    };

    eXo.ecm.WCMUtils = new WCMUtils();
	
	// SELocalization
	function SELocalization(){
	}
	
	SELocalization.prototype.cleanName = function(title, targetId) {
	  nameField = document.getElementById(targetId);
	  if (!nameField.readOnly) {
	    var portalContext = eXo.env.portal.context;
	    var portalRest = eXo.env.portal.rest;
	    var retText = ajaxAsyncGetRequest(portalContext+"/"+portalRest+"/l11n/cleanName?name="+title, false);
	    nameField.value = retText;
	  }
	};
	
	eXo.ecm.SELocalization = new SELocalization();
	
	// CKEditor
	function CKEditor() {
	}
	
	CKEditor.prototype.insertCSS = function(Instance, ContentCSS) {  
		if (!Instance) return;
		var eContentCSS = document.getElementById(ContentCSS);
		var sContentCSSId = ContentCSS + "_Inline";
		var count = 1;
	  eContentCSS.onblur = updateStyle;
		updateStyle;
		
		function updateStyle() {
			var sValue = eContentCSS.value;    
			if(!sValue)	return;
			var iDoc = CKEDITOR.instances[Instance].document.$;
			var eHead = iDoc.getElementsByTagName("head")[0];
			var eStyle = iDoc.getElementById(sContentCSSId);
			if (eStyle) {
				eHead.removeChild(eStyle);
			}
			eStyle = iDoc.createElement("style");
			eStyle.setAttribute("type", "text/css");
			eStyle.setAttribute("id", sContentCSSId);    
			if (eXo.core.Browser.isIE6() || eXo.core.Browser.isIE7()) { //for IE6 and IE7		
				eStyle.styleSheet.cssText = sValue;						
	    } else {
				eStyle.innerHTML = sValue;
			}
			eHead.appendChild(eStyle);
		};
		
		(function checkCKEditorAPI() {
			if (count <= 5) {
				try {
					updateStyle();
					if (updateStyle.time) {
						clearTimeout(updateStyle.time);
						updateStyle.time = null;
					}
				} catch(e) {
					count++;
					updateStyle.time = setTimeout(checkCKEditorAPI, 500);
				}
			}
		})();
		
	};
	
	eXo.ecm.CKEditor = new CKEditor();
	
	// Util
	window.wcm = function() {}
	wcm.insertCSSFromTextArea2FCK = function(Instance, ContentCSS) {
		if (!Instance) return;
		var eContentCSS = document.getElementById(ContentCSS);
		var sContentCSSId = ContentCSS + "_Inline";
		var count = 1;
		eContentCSS.onblur = updateStyle;
		
		function updateStyle() {
			var sValue = eContentCSS.value;
			if(!sValue)	return;
			var iDoc = FCKeditorAPI.Instances[Instance].EditorWindow.document;
			var eHead = iDoc.getElementsByTagName("head")[0];
			var eStyle = iDoc.getElementById(sContentCSSId);
			if (eStyle) {
				eHead.removeChild(eStyle);
			}
			eStyle = iDoc.createElement("style");
			eStyle.setAttribute("type", "text/css");
			eStyle.setAttribute("id", sContentCSSId);
			if (eXo.core.Browser.isFF()) { //for FF			
				eStyle.innerHTML = sValue;
			} else {
				eStyle.styleSheet.cssText = sValue;
			}
			eHead.appendChild(eStyle);
		};
		
		(function checkFCKEditorAPI() {
			if (count <= 5) {
				try {
					updateStyle();
					if (updateStyle.time) {
						clearTimeout(updateStyle.time);
						updateStyle.time = null;
					}
				} catch(e) {
					count++;
					updateStyle.time = setTimeout(checkFCKEditorAPI, 500);
				}
			}
		})();
	}
	
	Utils = function(){
		Utils.prototype.removeQuickeditingBlock = function(portletID, quickEditingBlockId) {
			var presentation = document.getElementById(portletID);
			var pNode = presentation.parentNode;
			var quickEditingBlock = document.getElementById(quickEditingBlockId);
			if(quickEditingBlock != null) {
				pNode.removeChild(quickEditingBlock);
			}
		};
			
		Utils.prototype.insertQuickeditingBlock = function(portletID, quickEditingBlockId) {
			var presentation = document.getElementById(portletID);		
			var parentNode = presentation.parentNode;
			//var fistChild = eXo.core.DOMUtil.getChildrenByTagName(parentNode, "div")[0];
			var fistChild = gj(parentNode).children("div")[0];
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
	}
	eXo.wcm = new Utils();
	
	function showObject(obj) {
		var element = gj(obj).nextAll("div:first")[0];
		if (!element.style.display || element.style.display == 'none') {
			element.style.display = 'block';
		} else {
			element.style.display = 'none';
		}
	}
	
	function getHostName() {
		var parentLocation = window.parent.location;
		return parentLocation.href.substring(0, parentLocation.href.indexOf(parentLocation.pathname));
	}
	
	function getRuntimeContextPath() {
		return getHostName() + eXo.env.portal.context + '/' + eXo.env.portal.portalName + '/';
	}
	/*--------------------------------------SEARCH------------------------------------*/
	function getKeynum(event) {
	  var keynum = false ;
	  if(window.event) { /* IE */
	    keynum = window.event.keyCode;
	    event = window.event ;
	  } else if(event.which) { /* Netscape/Firefox/Opera */
	    keynum = event.which ;
	  }
	  if(keynum == 0) {
	    keynum = event.keyCode ;
	  }
	  return keynum ;
	}
	
	function quickSearch(resultPageURI) {
		var searchBox = document.getElementById("siteSearchBox");
		var keyWordInput = gj(searchBox).find("input.keyword:first")[0];
		var keyword = encodeURI(keyWordInput.value);
		var resultPageURIDefault = "searchResult";
		var params = "portal=" + eXo.env.portal.portalName + "&keyword=" + keyword;
		var baseURI = getHostName() + eXo.env.portal.context + "/" + eXo.env.portal.portalName; 
		if (resultPageURI != undefined) {
			baseURI = baseURI + "/" + resultPageURI; 
		} else {
			baseURI = baseURI + "/" + resultPageURIDefault;  
		}
		window.location = baseURI + "?" + params;
	}
	
	function quickSearchOnEnter(event, resultPageURI) {
	  var keyNum = getKeynum(event);
	  if (keyNum == 13) {
	    quickSearch(resultPageURI);
	  }
	}
	
	function search(comId) {
		var searchForm = document.getElementById(comId);
		var inputKey = gj(searchForm).find("#keywordInput:first")[0];
		searchForm.onsubmit = function() {return false;};
		inputKey.onkeypress = function(event) {
			var keyNum = getKeynum(event);
			if (keyNum == 13) {
				var searchButton = gj(this.form).find("div.SearchButton:first")[0];
				searchButton.onclick();
	  	 }		
		}
	}	
	
	function keepKeywordOnBoxSearch() {
		var queryRegex = /^portal=[\w%]+&keyword=[\w%]+/;
		var searchBox = document.getElementById("siteSearchBox");
		var keyWordInput = gj(searchBox).find("input.keyword:first")[0];
		var queryString = location.search.substring(1);
		if (!queryString.match(queryRegex)) {return;}
		var portalParam = queryString.split('&')[0];
		var keyword = decodeURI(queryString.substring((portalParam + "keyword=").length +1));
		if (keyword != undefined && keyword.length != 0) {
			keyWordInput.value = unescape(keyword); 
		}
	}
	
	eXo.core.Browser.addOnLoadCallback("keepKeywordOnBoxSearch", keepKeywordOnBoxSearch);
	
	/*------------------Overrite method eXo.webui.UIPopup.init to show popup display center-------------------------------*/
	/*uiPopupWindow.UIPopupWindow.init = function(popupId, isShow, isResizable, showCloseButton, isShowMask) {XoWork
		this.superClass = eXo.webui.UIPopup ;
		var popup = document.getElementById(popupId) ;
		var portalApp = document.getElementById("UIPortalApplication") ;
		if(popup == null) return;
		popup.style.visibility = "hidden" ;
		if(!isShowMask) isShowMask = false; 
		popup.isShowMask = isShowMask ;
		
		//TODO Lambkin: this statement create a bug in select box component in Firefox
		//this.superClass.init(popup) ;
		var contentBlock = gj(popup).find('div.PopupContent:first')[0];
		if((gj(window).height() - 100 ) < contentBlock.offsetHeight) {
			contentBlock.style.height = (gj(window).height() - 100) + "px";
		}
		var popupBar = gj(popup).find('div.PopupTitle:first')[0];
	
		popupBar.onmousedown = this.initDND;
		popupBar.onkeydown = this.initDND;
		
		if(isShow == false) {
			this.superClass.hide(popup) ;
			if(isShowMask) eXo.webui.UIPopupWindow.showMask(popup, false) ;
		} 
		
		if(isResizable) {
			var resizeBtn = gj(popup).find("div.ResizeButton:first")[0];
			resizeBtn.style.display = 'block';
			resizeBtn.onmousedown = this.startResizeEvt;
			resizeBtn.onkeydown = this.startResizeEvt;
			portalApp.onmouseup = this.endResizeEvt;
		}
		
		popup.style.visibility = "hidden" ;
		if(isShow == true) {
			var iframes = gj(popup).find("iframe") ;
			if(iframes.length > 0) {
				setTimeout("eXo.webui.UIPopupWindow.show('" + popupId + "'," + isShowMask + ")", 500) ;
			} else {
			if(popup.offsetHeight == 0){
				setTimeout("eXo.webui.UIPopupWindow.show('" + popupId + "'," + isShowMask + ")", 500) ;
				return ;
			}
				this.show(popup, isShowMask) ;
			}
		}
	} ;
	*/
	/*----------------------------------------------End of overrite-------------------------------------------------------*/
	function initCheckedRadio(id) {
		eXo.core.Browser.chkRadioId = id;
	};
	
	function initCondition(formid){
		var formElement = document.getElementById(formid);
		var radioboxes = [];
		for(var i=0; i < formElement.elements.length;i++){
			if(formElement.elements[i].type=="radio") radioboxes.push(formElement.elements[i]);
		}
		var i = radioboxes.length;
		while(i--){
			radioboxes[i].onclick = chooseCondition;
		}
		if(eXo.core.Browser.chkRadioId && eXo.core.Browser.chkRadioId != "null"){
			var selectedRadio = document.getElementById(eXo.core.Browser.chkRadioId);
		} else{		
			var selectedRadio = radioboxes[0];
		}
		var itemSelectedContainer = gj(selectedRadio).parents(".ContentSearchForm:first")[0];
		var itemContainers = gj(selectedRadio.form).find("div.ContentSearchForm");
		for(var i=1;i<itemContainers.length;i++){
			setCondition(itemContainers[i],true);
		}
		enableCondition(itemSelectedContainer);
	}
	
	function chooseCondition() {
		var me = this;
		var hiddenField = gj(me.form).find("input.hidden:first")[0];
		hiddenField.value = me.id;
		var itemSelectedContainer = gj(me).parents(".ContentSearchForm:first")[0];
		var itemContainers = gj(me.form).find("div.ContentSearchForm");
		for(var i=1;i<itemContainers.length;i++){
			setCondition(itemContainers[i],true);
		}
		enableCondition(itemSelectedContainer);
		window.wcm.lastCondition = itemSelectedContainer; 
	};
	
	function enableCondition(itemContainer) {
		if(window.wcm.lastCondition) setCondition(window.wcm.lastCondition,true);
		setCondition(itemContainer,false);
	};
	
	function setCondition(itemContainer,state) {
		var action = gj(itemContainer).find("img");
		if(action && (action.length > 0)){
			for(var i = 0; i < action.length; i++){
				if(state) {
					action[i].style.visibility = "hidden";
				}	else {
					action[i].style.visibility = "";	
				}	
			}
		}
		var action = gj(itemContainer).find("input");
		if(action && (action.length > 0)){
			for(i = 0; i < action.length; i++){
				if(action[i].type != "radio") action[i].disabled = state;
			}
		}
		var action = gj(itemContainer).find("select");
		if(action && (action.length > 0)){
			for(i = 0; i < action.length; i++){
				action[i].disabled = state;
			}
		}
	};
	function removeCondition() {
		
	};
	
	function setHiddenValue() {
		var inputHidden = document.getElementById("checkedRadioId");
		if(eXo.core.Browser.chkRadioId == "null") {
			inputHidden.value = "name";
			document.getElementById("name").checked = true;
		} else {
			inputHidden.value = eXo.core.Browser.chkRadioId; 
			document.getElementById(eXo.core.Browser.chkRadioId).checked = true;
		}
	}
	
	function showHideOrderBy() {
		var formObj = document.getElementById('UIViewerManagementForm');
		var viewerModeObj = formObj['ViewerMode'];
		var orderXXX = gj(formObj).find('tr.OrderBlock');			
		viewerModeObj[0].onclick = function() {
			for (var i = 0; i < orderXXX.length; i++) {
				orderXXX[i].style.display = '';
			}
		}
		viewerModeObj[1].onclick = function() {
			for (var i = 0; i < orderXXX.length; i++) {
				orderXXX[i].style.display = 'none';
			}
		}
	}  
	
	
	window.showPopupMenu = function (obj) {  
		  var uiNavi = document.getElementById('UIPresentationContainer');	
		  var uiACMENavi = document.getElementById('navigation-generator');
		  var uiWCMNavigationPortlet = gj(uiACMENavi).find("div.UIWCMNavigationPortlet:first")[0];
		 
		  var objParent = gj(obj).parents(".UITab:first")[0];  
		  var menuItemContainer = gj(obj).nextAll("div:first")[0];
	
		  var getNodeURL = obj.getAttribute("exo:getNodeURL");
		  if (getNodeURL && !menuItemContainer) {
		    var jsChilds = ajaxAsyncGetRequest(getNodeURL, false)
		    try {
		      var data = gj.parseJSON(jsChilds);
		      if (isNaN(data.length)) {
						return;
		      }
	
		      var temp = document.createElement("div");
		      temp.innerHTML = generateContainer(data); 		  
		      objParent.appendChild(gj(temp).children("div.ECMMenuItemContainer:first")[0]);
		    } catch (e) {
		      return;
		    }			  
		  }
	
		  //display popup    
		  if(obj.Timeout) clearTimeout(obj.Timeout);  
		  if(menuItemContainer && menuItemContainer.style.display != "block") {
		    objParent.style.position="relative";         
		    menuItemContainer.style.top =  obj.offsetHeight + 'px';   
		    menuItemContainer.style.display = 'block';          
		    menuItemContainer.onmouseout = function(){                              
		      if(base.Browser.browserType == 'ie')  {
		        if(uiNavi) uiNavi.style.position = "relative";
						if(uiWCMNavigationPortlet) uiWCMNavigationPortlet.style.position = "relative";
		      }
		      obj.Timeout = setTimeout(function() {
		        menuItemContainer.style.display = 'none';			
		        menuItemContainer.onmouseover = null;
		        menuItemContainer.onmouseout = null;
		      },1*10);
		    }
		    menuItemContainer.onmouseover = function() {                                              		
		      if(base.Browser.browserType == 'ie')  {
		        if(uiNavi) uiNavi.style.position = "static";
		        if(uiWCMNavigationPortlet) uiWCMNavigationPortlet.style.position = "static";
		      }
		      if(obj.Timeout) clearTimeout(obj.Timeout);
		      obj.Timeout = null;
		    }
		    obj.onmouseout = menuItemContainer.onmouseout;
		    //menuItemContainer.style.width = menuItemContainer.offsetWidth + 'px'; 
		  }  
		}
	
	window.showPopupSubMenu = function(obj) { 
		  var objParent = obj;
		  var subMenuItemContainer = false;   
		  var objParent = gj(obj).parents(".ArrowIcon:first")[0];
		  if(objParent) subMenuItemContainer = gj(objParent).nextAll("div:first")[0]; 
	
		  var getNodeURL = obj.getAttribute("exo:getNodeURL");
		  if (getNodeURL && !subMenuItemContainer) {
		    var jsChilds = ajaxAsyncGetRequest(getNodeURL, false);
		    try {
		      var data = gj.parseJSON(jsChilds);
			if (isNaN(data.length)) {
			  return;
			}
	
			var temp = document.createElement("div");
			temp.innerHTML = generateContainer(data); 	
			var grandParent = gj(objParent).parents(".MenuItem:first")[0];
			grandParent.appendChild(gj(temp).children("div.ECMMenuItemContainer:first")[0]);
		     } catch (e) {
		       return;
		     }			  
		  }
	
		  objParent = gj(obj).parents(".ArrowIcon:first")[0];
		  if(objParent) subMenuItemContainer = gj(objParent).nextAll("div:first")[0]; 
	
		  if(obj.Timeout) clearTimeout(obj.Timeout);	
		  if(subMenuItemContainer && subMenuItemContainer.style.display != "block") {      
		    subMenuItemContainer.style.display = 'block';
		    objParent.className = '';   
		    subMenuItemContainer.onmouseover = function() {	
		      objParent.className = '';	
		      if(objParent.Timeout) clearTimeout(objParent.Timeout);
		      objParent.Timeout =  null;      
		    }
	
		    subMenuItemContainer.onmouseout = function() {
		      objParent.Timeout = setTimeout(function() {                
		        var subobj = gj(subMenuItemContainer).find("div.ECMMenuItemContainer:first")[0];
		        if(subobj) {
		          if(subobj.style.display=='block') {
		            subobj.style.display = 'none'; 									
		            subobj.onmouseover = null;
		            subobj.onmouseout = null;							 
		          } else {
			    subMenuItemContainer.style.display = 'none'; 
			    objParent.className = 'ArrowIcon';				
		            subMenuItemContainer.onmouseover = null;
		            subMenuItemContainer.onmouseout = null;
			  }
		        } else {		
			  subMenuItemContainer.style.display = 'none'; 					
			  objParent.className = 'ArrowIcon';				
			  subMenuItemContainer.onmouseover = null;
			  subMenuItemContainer.onmouseout = null;
		        }
		      }, 1*10);      
		    }	
		    obj.onmouseout = subMenuItemContainer.onmouseout;	
		    subMenuItemContainer.style.width = subMenuItemContainer.offsetWidth + 'px';	
		    subMenuItemContainer.style.left = objParent.offsetLeft + objParent.offsetWidth + 'px';
		    subMenuItemContainer.style.top =  base.Browser.findPosYInContainer(objParent,subMenuItemContainer.offsetParent) + 'px';
		  }
		};
	
	
	function requestAjax(url) {
		var xmlHttpRequest = false;
	  if(window.XMLHttpRequest) {
			try {
				xmlHttpRequest = new XMLHttpRequest();
			} catch(e) {
				xmlHttpRequest = false;
			}
	  } else if(window.ActiveXObject) {
	     	try {
	      	xmlHttpRequest = new ActiveXObject("Msxml2.XMLHTTP");
	    	} catch(e) {
	      	try {
	        	xmlHttpRequest = new ActiveXObject("Microsoft.XMLHTTP");
	      	} catch(e) {
	        	xmlHttpRequest = false;
	      	}
			}
	  }
		if(xmlHttpRequest) {
			xmlHttpRequest.open("GET", url, false);
			xmlHttpRequest.send();
			return xmlHttpRequest.responseXML;
		}
	}	
	
	return {
		WCMUtils : eXo.ecm.WCMUtils,
		CKEditor : eXo.ecm.CKEditor,
		SELocalization : eXo.ecm.SELocalization
	};
})(gj, base);
