(function(gj, base) {
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
})(gj, base);
