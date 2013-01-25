(function(gj, ecm_bootstrap, webuiExt) {
	function ECMUtils() {
		var Self = this;
		var showSideBar = true;
		var editFullScreen = false;
		//set private property;
		var Browser = eXo.core.Browser;
		Self.waitContainerRetry = 0;
		Self.waitContainerRetryMax = 200;
		Self.MiniumLeftContainerWidth = 250;
		Self.MiniumRightContainerWidth = 251;
		Self.waitInterval              = 25;
		Self.UIBrokenCheckingInterval  = 50;
		Self.UIBrokenCheckingHandler   = null;
		var RightClick = eXo.webui.UIRightClickPopupMenu;
		if (!RightClick) {
			RightClick = eXo.webui.UIRightClickPopupMenu;
		}
		//  DOM.hideElements();
		ECMUtils.prototype.popupArray = [];
		ECMUtils.prototype.voteRate = 0;
		ECMUtils.prototype.init = function (portletId) {
			gj(window).resize(function() {
			  Self.loadContainerWidth(); 
			});
			var portlet = document.getElementById(portletId);
			if (!portlet) return;
			RightClick.disableContextMenu(portletId);
			portlet.onclick = function (event) {
			  eXo.ecm.ECMUtils.closeAllPopup();
			}
			portlet.onkeydown = function (event) {
			  eXo.ecm.ECMUtils.closeAllPopup();
			}
			if (document.getElementById("UIPageDesktop")) {
			  Self.fixHeight(portletId);
			  var uiPageDeskTop = document.getElementById("UIPageDesktop");
			  var uiJCRExplorers = gj(uiPageDeskTop).find('div.UIJCRExplorer');
			  if (uiJCRExplorers.length) {
			    for (var i = 0; i < uiJCRExplorers.length; i++) {
			      var uiResizeBlock = gj(uiJCRExplorers[i]).parents(".UIResizableBlock:first")[0];
			      if (uiResizeBlock) uiResizeBlock.style.overflow = "hidden";
			    }
			  }
			} else {
			  Self.controlLayout(portletId);
			  eXo.core.Browser.addOnResizeCallback('controlLayout',

			  function () {
			    eXo.ecm.ECMUtils.controlLayout(portletId);
			  });
			}
		};
	

		/**
		 * @function   getCookie
		 * @return     return a saved cookie with given name or return null if that cookie's field haven't been saved
		 * @author     vinh_nguyen@exoplatform.com
		 */
		ECMUtils.prototype.getCookie = function (c_name) {
			var i, x, y, ARRcookies = document.cookie.split(";");
			for (i = 0; i < ARRcookies.length; i++) {
			  x = ARRcookies[i].substr(0, ARRcookies[i].indexOf("="));
			  y = ARRcookies[i].substr(ARRcookies[i].indexOf("=") + 1);
			  x = x.replace(/^\s+|\s+$/g, "");
			  if (x == c_name) {
			    return unescape(y);
			  }
			}
			return null;
		}

		/**
		 * @function   setCookie
		 * @return     saved cookie with given name
		 * @author     vinh_nguyen@exoplatform.com
		 */
		ECMUtils.prototype.setCookie = function (c_name, value, exdays) {
			var exdate = new Date();
			exdate.setDate(exdate.getDate() + exdays);
			var c_value = escape(value) + ((exdays == null) ? "" : "; expires=" + exdate.toUTCString());
			document.cookie = c_name + "=" + c_value;
		}
		ECMUtils.prototype.fixHeight = function (portletId) {
			var portlet = document.getElementById(portletId);
			var refElement = gj(portlet).parents(".UIApplication:first")[0];
			if (!refElement) return;

			// 30/06/2009
			//Recalculate height of UIResizableBlock in the UISideBarContainer
			//var delta = parseInt(refElement.style.height) - portlet.offsetHeight;

			var uiControl = document.getElementById('UIControl');
			var uiSideBar = document.getElementById('UISideBar');
			if (!uiControl || !uiSideBar) return;
			var uiSideBarControl = gj(uiSideBar).find('div.UISideBarControl:first')[0];
			if (!uiSideBarControl) return;
			var deltaH = refElement.offsetHeight - uiControl.offsetHeight - uiSideBarControl.offsetHeight;
			var resizeObj = gj(portlet).find('div.UIResizableBlock');
			if (resizeObj.length) {
			  for (var i = 0; i < resizeObj.length; i++) {
			    resizeObj[i].style.display = 'block';
			    resizeObj[i].style.height = (resizeObj[i].offsetHeight + deltaH) + "px";
			  }
			}
		};

		ECMUtils.prototype.controlLayout = function (portletId) {
			var portlet = document.getElementById(portletId);
			if (!Self.uiWorkingArea) return;
			var delta = document.body.scrollHeight - gj(window).height();
			if (delta < 0) {
			  var resizeObj = gj(portlet).find('div.UIResizableBlock');
			  if (resizeObj.length) {
			    for (var i = 0; i < resizeObj.length; i++) {
			      resizeObj[i].style.height = resizeObj[i].offsetHeight - delta + "px";
			    }
			  }
			}
			eXo.core.Browser.addOnResizeCallback('controlLayout', function () {
			  eXo.ecm.ECMUtils.controlLayout(portletId)
			});
		};

		ECMUtils.prototype.clickLeftMouse = function (event, clickedElement, position, option) {
			var event = event || window.event;
			event.cancelBubble = true;
			popupSelector = gj(clickedElement).parents(".UIPopupSelector:first")[0];
			showBlock = gj(popupSelector).find("div.UISelectContent:first")[0];
			if (option == 1) {
			  showBlock.style.width = (popupSelector.offsetWidth - 2) + "px";
			}
			if (showBlock.style.display == "block") {
			  eXo.webui.UIPopup.hide(showBlock);
			  return;
			}
			eXo.webui.UIPopup.show(showBlock);
			showBlock.onmousedown = function (event) {
			  var event = event || window.event;
			  event.cancelBubble = true;
			}
			showBlock.onkeydown = function (event) {
			  var event = event || window.event;
			  event.cancelBubble = true;
			}
			Self.popupArray.push(showBlock);
			showBlock.style.top = popupSelector.offsetHeight + "px";
		};

		ECMUtils.prototype.closeAllPopup = function (exceptElement) {
			for (var i = 0; i < Self.popupArray.length; i++) {
		 		if (Self.popupArray[i] == exceptElement) continue; 
				var moreParent = gj(Self.popupArray[i]).parents(".MoreContainerActivated:first")[0];
				if (moreParent) gj(moreParent).removeClass("MoreContainerActivated");
			  Self.popupArray[i].style.display = "none";
			}
			Self.popupArray = [];
		};

		ECMUtils.prototype.initVote = function (voteId, rate) {
			var vote = document.getElementById(voteId);
			voteRate = vote.rate = rate = parseInt(rate);
			var optsContainer = gj(vote).find("div.OptionsContainer:first")[0];
			var options = gj(optsContainer).children("div");
			for (var i = 0; i < options.length; i++) {
			  options[i].onmouseover = Self.overVote;
			  if (i < rate) options[i].className = "RatedVote";
			}

			vote.onmouseover = function () {
			  var optsCon = gj(this).find("div.OptionsContainer:first")[0];
			  var opts = gj(optsCon).children("div");
			  for (var j = 0; j < opts.length; j++) {
			    if (j < this.rate) opts[j].className = "RatedVote";
			    else opts[j].className = "NormalVote";
			  }
			}
			optsContainer.onmouseover = function (event) {
			  var event = event || window.event;
			  event.cancelBubble = true;
			}
		};

		ECMUtils.prototype.overVote = function (event) {
			var optionsContainer = gj(this).parents(".OptionsContainer:first")[0];
			var opts = gj(optionsContainer).children("div");
			var i = opts.length;
			for (--i; i >= 0; i--) {
			  if (opts[i] == this) break;
			  if (i < voteRate) opts[i].className = "RatedVote";
			  else opts[i].className = "NormalVote";
			}
			if (opts[i].className == "OverVote") return;
			for (; i >= 0; i--) {
			  opts[i].className = "OverVote";
			}
		};

		ECMUtils.prototype.showHideExtendedView = function (event) {
			var elemt = document.getElementById("ListExtendedView");
			event = event || window.event;
			event.cancelBubble = true;
			if (elemt.style.display == 'none') {
			  elemt.style.display = 'block';
			} else {
			  elemt.style.display = 'none';
			}
		}

		ECMUtils.prototype.showHideComponent = function (elemtClicked) {

			var nodeReference = gj(elemtClicked).parents(".ShowHideContainer:first")[0];
			var elemt = gj(nodeReference).find("div.ShowHideComponent:first")[0];

			if (elemt.style.display == 'none') {
			  elemtClicked.childNodes[0].style.display = 'none';
			  elemtClicked.childNodes[1].style.display = 'block';
			  elemt.style.display = 'block';
			  eXo.ecm.ECMUtils.setScrollBar();
			} else {
			  elemtClicked.childNodes[0].style.display = 'block';
			  elemtClicked.childNodes[1].style.display = 'none';
			  elemt.style.display = 'none';
			}
		};

		ECMUtils.prototype.setScrollBar = function () {
			try {
			  var parent = document.getElementById('TabContainerParent');
			  if (parent != null) {
			    var elements = gj(parent).find("div.UITabContent");
			    if (elements != null) {
			      for (i = 0; i < elements.length; i++) {
			        var obj = elements[i];
			        if (obj.style.display != "none") {
			          var height = obj.offsetHeight;
			          if (height > 430) {
			            //obj.style.height="470px";
			            obj.style.height = Self.uiWorkingArea.offsetHeight - 50 + "px";
			            obj.style.overflow = "auto";
			          }
			        }
			      }
			    }
			  }
			} catch (err) {}
		}

		ECMUtils.prototype.showHideContentOnRow = function (elemtClicked) {

			var nodeReference = gj(elemtClicked).parents(".Text:first")[0];
			var elemt = gj(nodeReference).find("div.ShowHideComponent:first")[0];
			var shortContent = gj(elemt).find("div.ShortContentPermission:first")[0];
			var fullContent = gj(elemt).find("div.FullContentPermission:first")[0];

			if (shortContent.style.display == 'none') {
			  fullContent.style.display = 'none';
			  shortContent.style.display = 'block';
			} else {
			  fullContent.style.display = 'block';
			  shortContent.style.display = 'none';
			}
		};

		ECMUtils.prototype.isEventTarget = function (element, e) {
			if (window.event) e = window.event;
			var srcEl = e.srcElement ? e.srcElement : e.target;
			if (element == srcEl) {
			  return true;
			}
			return false;
		};

		ECMUtils.prototype.focusCurrentNodeInTree = function (id) {
			var element = document.getElementById(id);
			var uiTreeExplorer = document.getElementById("UITreeExplorer");
			if (!element || !uiTreeExplorer) return;
			var top = element.offsetTop;
			uiTreeExplorer.scrollTop = (top - uiTreeExplorer.offsetTop);
		};

		ECMUtils.prototype.collapseExpand = function (element) {
			var node = element.parentNode;
			var subGroup = gj(node).children("div.NodeGroup:first")[0];
			if (!subGroup) return false;
			if (subGroup.style.display == "none") {
			  if (element.className.match("ExpandIcon")) element.className = element.className.replace("ExpandIcon", "CollapseIcon");
			  subGroup.style.display = "block";
			} else {
			  if (element.className.match("CollapseIcon")) element.className = element.className.replace("CollapseIcon", "ExpandIcon");
			  subGroup.style.display = "none";
			}
			return true;
		};

		ECMUtils.prototype.collapseExpandPart = function (element) {
			var node = element.parentNode;
			var subGroup1 = gj(node).children("div.NodeGroup1:first")[0];
			var subGroup2 = gj(node).children("div.NodeGroup2:first")[0];
			if (subGroup1.style.display == "none") {
			  if (element.className == "CollapseIcon") element.className = "ExpandIcon";
			  subGroup1.style.display = "block";
			  subGroup2.style.display = "none";
			} else {
			  if (element.className == "ExpandIcon") element.className = "CollapseIcon";
			  subGroup1.style.display = "none";
			  subGroup2.style.display = "block";
			}
			return true;
		};

		ECMUtils.prototype.filterValue = function (frmId) {
			var form = document.getElementById(frmId);
			if (eXo.core.Browser.browserType == "ie") {
			  var text = document.createTextNode(form['tempSel'].innerHTML);
			  form['result'].appendChild(text);
			} else {
			  form['result'].innerHTML = form['tempSel'].innerHTML;
			}
			var filterValue = form['filter'].value;
			filterValue = filterValue.replace("*", ".*");
			var re = new RegExp(filterValue, "i");
			var elSel = form['result'];
			var i;
			for (i = elSel.length - 1; i >= 0; i--) {
			  if (!re.test(elSel.options[i].value)) {
			    elSel.remove(i);
			  }
			}
		};

		ECMUtils.prototype.convertElemtToHTML = function (id) {
			var elemt = document.getElementById(id);
			var text = elemt.innerHTML;
			text = text.toString();

			text = text.replace(/&/g, "&amp;").replace(/"/g, "&quot;")
			  .replace(/</g, "&lt;").replace(/>/g, "&gt;");

			elemt.innerHTML = text;
		};

		ECMUtils.prototype.onKeyAddressBarPress = function () {
			var uiAddressBarControl = document.getElementById("AddressBarControl");
			if (uiAddressBarControl) {
			  uiAddressBarControl.onkeypress = Self.onAddressBarEnterPress;
			}
		};

		ECMUtils.prototype.onKeySimpleSearchPress = function () {
			var uiAddressBarControl = document.getElementById("SimpleSearchControl");
			if (uiAddressBarControl) {
			  uiAddressBarControl.onkeypress = Self.onSimpleSearchEnterPress;
			}
		};

		ECMUtils.prototype.onSimpleSearchEnterPress = function (event) {
			var gotoLocation = document.getElementById("SimpleSearch");
			var event = event || window.event;
			if (gotoLocation && event.keyCode == 13) {
			  eval(gotoLocation.href);
			  return false;
			}
		};

		ECMUtils.prototype.onAddressBarEnterPress = function (event) {
			var gotoLocation = document.getElementById("GotoLocation");
			var event = event || window.event;
			if (gotoLocation && event.keyCode == 13) {
			  eval(gotoLocation.href);
			  return false;
			}
		};

		ECMUtils.prototype.insertContentToIframe = function (i) {
			var original = document.getElementById("original" + i);
			var resived = document.getElementById("revised" + i);
			var css = '' +
			  '<style type="text/css">' +
			  'body{margin:0;padding:0;background:transparent;}' +
			  '</style>';
			try {
			  if (resived != null) {
			    resivedDoc = resived.contentWindow.document;
			    resivedDoc.open();
			    resivedDoc.write(css);
			    resivedDoc.write(resived.getAttribute("content"));
			    resivedDoc.close();
			  }
			  if (original != null) {
			    var originaleDoc = original.contentWindow.document;
			    originaleDoc.open();
			    originaleDoc.write(css);
			    originaleDoc.write(original.getAttribute("content"));
			    originaleDoc.close();
			  }
			} catch (ex) {}
		};

		ECMUtils.prototype.generatePublicWebDAVLink = function (serverInfo, restContextName, repository, workspace, nodePath) {
			// query parameter s must be encoded.
			var path = "/";
			nodePath = nodePath.substr(1).split("\/");
			for (var i = 0; i < nodePath.length; i++) {
			  path += encodeURIComponent(nodePath[i]) + "/";
			}
			window.open(serverInfo + "/" + restContextName + "/jcr/" + repository + "/" + workspace + path, '_new');
		};

		ECMUtils.prototype.generateWebDAVLink = function (serverInfo, portalName, restContextName, repository, workspace, nodePath, mimetype) {
			// query parameter s must be encoded.
			var path = "/";
			var fEncode = false;
			nodePath = nodePath.substr(1).split("\/");
			for (var i = 0; i < nodePath.length; i++) {
			  path += encodeURIComponent(nodePath[i]) + "/";
			  fEncode = true;
			}
			if (fEncode) path = path.substr(0, path.length - 1);
			if (eXo.core.Browser.isIE()) {
			  if (mimetype == "application/xls" || mimetype == "application/msword" || mimetype == "application/ppt") {
			    window.open(serverInfo + "/" + restContextName + "/private/lnkproducer/openit.lnk?path=/" + repository + "/" + workspace + path, '_new');
			  } else {
			    eXo.ecm.ECMUtils.generateWebDAVUrl(serverInfo, restContextName, repository, workspace, path, mimetype);
			  }
			} else {
			  window.open(serverInfo + "/" + restContextName + "/private/jcr/" + repository + "/" + workspace + path, '_new');
			}
		};

		ECMUtils.prototype.generateWebDAVUrl = function (serverInfo, restContextName, repository, workspace, nodePath, mimetype) {
			my_window = window.open("");
			var downloadLink = serverInfo + "/" + restContextName + "/jcr/" + repository + "/" + workspace + nodePath;
			my_window.document.write('<script> window.location.href = "' + downloadLink + '"; </script>');
		};

		var clip = null;
		ECMUtils.prototype.initClipboard = function (id, level, size) {
			try {
			  if (eXo.core.Browser.isIE()) {
			    if (size > 0) {
			      for (var i = 1; i <= size; i++) {
			        clip = new ZeroClipboard.Client();
			        clip.setHandCursor(true);
			        try {
			          clip.glue(id + level + i);
			        } catch (err) {}
			      }
			    }
			  }
			}catch (err) {}
		}

		ECMUtils.prototype.closeContextMenu = function (element) {
			var contextMenu = document.getElementById("ECMContextMenu");
			if (contextMenu) contextMenu.style.display = "none";
		}

		ECMUtils.prototype.pushToClipboard = function (event, url) {
			if (window.clipboardData && clipboardData.setData) {
			  clipboardData.setData("Text", url);
			} else {
			  alert("Internet Explorer required");
			}
			eXo.core.MouseEventManager.docMouseDownEvt(event);
			return false;
		}

		ECMUtils.prototype.concatMethod = function () {
			var oArg = arguments;
			var nSize = oArg.length;
			if (nSize < 2) return;
			var mSelf = oArg[0];
			return function () {
			  var aArg = [];
			  for (var i = 0; i < arguments.length; ++i) {
			    aArg.push(arguments[i]);
			  }
			  mSelf.apply(mSelf, aArg);
			  for (i = 1; i < nSize; ++i) {
			    var oSet = {
			      method: oArg[i].method || function () {},
			      param: oArg[i].param || aArg
			    }
			    oSet.method.apply(oSet.method, oSet.param);
			  }
			}
		};
	
		/**
		 * @function        actionbarContainer_OnResize
		 * @purpose         Handle for the resize event of Content Explorer
		 *                  Calculate to determine which actionButton will be visible, which will be put
		 *                  in HiddenActionList
		 * @author          vinh_nguyen@exoplatform.com
		 */
		ECMUtils.prototype.actionbarContainer_OnResize = function () {
			Self.actionBarContainer = gj(Self.uiRightContainer).find('div.UIActionBar:first')[0];
			var actionbar= gj('#UIActionBar');
			if (actionbar) { //VinhNT workaround for the un-expand width of ActionBar problem, should be improved later
			  actionbar.width(actionbar.parent().width()-2);
			}
			var dmsMenuContainer    = gj(Self.uiRightContainer).find('div.DMSMenuItemContainer:first')[0];
			if (!Self.actionBarContainer) return;//No action bar, no resize
			Self.showMoreActionContainer = document.getElementById("ShowMoreActionContainer");
			Self.listHiddenActionContainer = gj(Self.showMoreActionContainer).find("div.ListHiddenActionContainer:first")[0];
			Self.viewBarContainer = gj(Self.uiRightContainer).find("div.UIViewBarContainer:first")[0];
			var allowedSpace = Self.actionBarContainer.offsetWidth - Self.viewBarContainer.offsetWidth -13;
			var sumWidth = 0;
			var flagMore = true;
			var visibleActionChildren = gj(dmsMenuContainer).children(".NormalSubItem");
			var hiddenActionChildren = gj(Self.listHiddenActionContainer).children(".NormalSubItem");
			if (hiddenActionChildren.length > 0) {
			  Self.showMoreActionContainer.style.display = "block";
			  sumWidth = Self.showMoreActionContainer.offsetWidth;
			  flagMore = false;
			} else {
			  Self.showMoreActionContainer.style.display = "none";
			}
			for (var i = 0; i < visibleActionChildren.length; i++) {
			  sumWidth += visibleActionChildren[i].offsetWidth;
			}
			
			var index = visibleActionChildren.length - 1;
			var flag = true;
			var removedItem;
			while (sumWidth > allowedSpace && index >= 0) {
			  if (flagMore) {
			    Self.showMoreActionContainer.style.display = "block";
			    flagMore = false;
			    sumWidth += Self.showMoreActionContainer.offsetWidth;
			  }
			  sumWidth -= visibleActionChildren[index].offsetWidth;
			  removedItem = dmsMenuContainer.removeChild(visibleActionChildren[index]);
			  gj(Self.listHiddenActionContainer).prepend(removedItem);
			  flag = false;
			  index--;
			}
			
		 if (flag) {
		 	  var flagBorderRight = true;
			  hiddenActionChildren = gj(Self.listHiddenActionContainer).children(".NormalSubItem");
			  if (hiddenActionChildren.length <=0) {
			  	Self.showMoreActionContainer.style.display = "none";
			  	flag=false;;
			  }
			  visibleActionChildren = gj(dmsMenuContainer).children(".NormalSubItem");
			  sumWidth = 0;
			  for (var i = 0; i < visibleActionChildren.length; i++) {
			    sumWidth += visibleActionChildren[i].offsetWidth;
			  }
			  remainItem = hiddenActionChildren.length;
			  while (flag) {
				  removedItem = Self.listHiddenActionContainer.removeChild(hiddenActionChildren[0]);
				  gj(dmsMenuContainer).append(removedItem);
				  remainItem--;
				  sumWidth += removedItem.offsetWidth;
				  if (flagBorderRight) {
				  	sumWidth++;
				  	flagBorderRight = false;
				  }
				  if (sumWidth > (allowedSpace - (remainItem>0?Self.showMoreActionContainer.offsetWidth:0)) ) {
				    flag = false;
				    dmsMenuContainer.removeChild(removedItem);
				    gj(Self.listHiddenActionContainer).prepend(removedItem);
				  } else {
				    hiddenActionChildren = gj(Self.listHiddenActionContainer).children(".NormalSubItem");
				    remainItem = hiddenActionChildren.length;
				    if (remainItem <= 0) {
				      Self.showMoreActionContainer.style.display = "none"; 
				      flag = false;
				    }
				  }
				}
			}
			visibleActionChildren = gj(dmsMenuContainer).children(".NormalSubItem");
			for (var i = 0; i < visibleActionChildren.length; i++) {
			   if (i==visibleActionChildren.length-1) {
			    gj(visibleActionChildren[i]).css("border", "none"); 
			   }else {
			   	gj(visibleActionChildren[i]).css("border-right", "1px solid #e1e1e1");       	
			   }
			}
		};
	
		/**
		 * @function        hiddenActionContainerTrigger
		 * @purpose         display/hidden more button of ActionContainer
		 * @author          vinh_nguyen@exoplatform.com
		 */
		ECMUtils.prototype.hiddenActionContainerTrigger = function (event) {
			event = event || window.event;
			event.cancelBubble = true;
			if (!Self.actionBarContainer) Self.actionBarContainer = document.getElementById('DMSMenuItemContainer');
			if (!Self.listHiddenActionContainer) Self.listHiddenActionContainer = gj(Self.actionBarContainer).find("div.ListHiddenActionContainer:first")[0];
			if (!Self.showMoreActionContainer) Self.showMoreActionContainer = document.getElementById("ShowMoreActionContainer");
			var hiddenActionChildren = gj(Self.listHiddenActionContainer).children(".NormalSubItem");
			
			if (hiddenActionChildren.length > 0) {
			  if (Self.listHiddenActionContainer.style.display == "block") {
			    Self.listHiddenActionContainer.style.display = "none";
			    gj(Self.showMoreActionContainer).removeClass("MoreContainerActivated");
			    Self.closeAllPopup(Self.listHiddenActionContainer);
			  } else {
			  	Self.closeAllPopup(Self.listHiddenActionContainer);
			    Self.listHiddenActionContainer.style.display = "block";
			    gj(Self.showMoreActionContainer).addClass("MoreContainerActivated");
			    Self.pushMoreBlock(Self.showMoreActionContainer);
			  }
			} else {
			  Self.showMoreActionContainer.style.display = "none";
			}
			Self.loadContainerWidth();
		}

		ECMUtils.prototype.showDocumentInformation = function (obj, event) {
			if (!obj) return;
			event = event || window.event;
			event.cancelBubble = true;
			var infor = document.getElementById('metadatas');
			if (infor.style.display == 'none') {
			  infor.style.display = 'block';
			  infor.style.left = obj.offsetLeft + 'px';
			} else {
			  infor.style.display = 'none';
			}
			//    DOM.listHideElements(infor);
		};

		ECMUtils.prototype.onKeyPDFViewerPress = function () {
			var uiPDFViewer = document.getElementById("PageControl");
			if (uiPDFViewer) {
			  uiPDFViewer.onkeypress = Self.onGotoPageEnterPress;
			}
		};

		ECMUtils.prototype.onGotoPageEnterPress = function (event) {
			var gotoPage = document.getElementById("GotoPage");
			var event = event || window.event;
			if (gotoPage && event.keyCode == 13) {
			  eval(gotoPage.href);
			  return false;
			}
		};
		ECMUtils.prototype.initWithoutLeftContainer = function () {
			var value = gj(Self.uiWorkingArea).attr("initWithoutLeftContainer")!='true';
			return value;
		}
		ECMUtils.prototype.resizeSideBar = function (event) {
			var event = event || window.event;
			if (Self.initWithoutLeftContainer()) return;
			Self.loadContainerReference();
			var resizableBlock = gj(Self.uiLeftContainer).find("div.UIResizableBlock:first")[0];
			if (Self.uiLeftContainer.style.display=="none") return;
			eXo.ecm.ECMUtils.currentMouseX = event.clientX;
			if (Self.uiResizeSideBar) gj(Self.uiResizeSideBar).addClass("ResizeSideBarDisplay");
			eXo.ecm.ECMUtils.resizableBlockWidth = resizableBlock.offsetWidth;
			eXo.ecm.ECMUtils.currentWidth = Self.uiLeftContainer.offsetWidth;
			var sideBarContent = gj(Self.uiLeftContainer).find("div.SideBarContent:first")[0];
			var title = gj(sideBarContent).find("div.Title:first")[0];
			eXo.ecm.ECMUtils.currentTitleWidth = title.offsetWidth;
			if (Self.UIBrokenCheckingHandler) {
			  clearInterval(Self.UIBrokenCheckingHandler);
			  Self.UIBrokenCheckingHandler = null;
			}
			if (Self.uiLeftContainer.style.display == '' || Self.uiLeftContainer.style.display == 'block') {
			  document.onmousemove = eXo.ecm.ECMUtils.resizeMouseMoveSideBar;
			  document.onmouseup = eXo.ecm.ECMUtils.resizeMouseUpSideBar;
			}
			//VinhNT disable selection of text
			var jcrExpPortlet = document.getElementById("UIJCRExplorer");
			gj(jcrExpPortlet).addClass("UIJCRExplorerNoSelect");
		}

		ECMUtils.prototype.resizeMouseMoveSideBar = function (event) {
			var event = event || window.event;
			if (Self.initWithoutLeftContainer()) return;
			var resizableBlock = gj(Self.uiLeftContainer).find("div.UIResizableBlock:first")[0];
			var deltaX = event.clientX - eXo.ecm.ECMUtils.currentMouseX;
			eXo.ecm.ECMUtils.savedResizeDistance = deltaX;
			var sideBarContent = gj(Self.uiLeftContainer).find("div.SideBarContent:first")[0];
			eXo.ecm.ECMUtils.savedResizableMouseX = eXo.ecm.ECMUtils.resizableBlockWidth + deltaX + "px";
			eXo.ecm.ECMUtils.savedLeftContainer = eXo.ecm.ECMUtils.currentWidth + deltaX + "px";
			eXo.ecm.ECMUtils.isResizedLeft = false;

			var allowedWidth = parseInt(Self.uiWorkingArea.offsetWidth) - Self.MiniumRightContainerWidth;
			// Fix minimium width can be resized
			var X_Pos = eXo.core.Browser.findMouseRelativeX(Self.uiWorkingArea, event);
			X_Pos = X_Pos>0?Math.floor(X_Pos):-Math.ceil(X_Pos);
			if (X_Pos > allowedWidth) {
			  X_Pos = allowedWidth;
			} else if (X_Pos < Self.MiniumLeftContainerWidth) {
			  X_Pos = Self.MiniumLeftContainerWidth;
			}
			Self.loadContainerWidth(X_Pos);
		}

		ECMUtils.prototype.resizeVisibleComponent = function () {
			if (Self.initWithoutLeftContainer()) return;
			var container = document.getElementById("LeftContainer");
			var rightContainer = gj(Self.uiWorkingArea).find("div.RightContainer:first")[0];
			var resizableBlock = gj(container).find("div.UIResizableBlock:first")[0];
			var selectContent = gj(resizableBlock).find("div.UISelectContent:first")[0];

			var selectedItem = gj(selectContent).find("div.SelectedItem:first")[0];
			var lstNormalItem = gj(selectContent).find("div.NormalItem");
			var moreButton = gj(selectContent).find("div.MoreItem:first")[0];

			//count the visible items
			var visibleItemsCount = 0;
			if (selectedItem != null) {
			  visibleItemsCount++;
			}

			if (lstNormalItem != null) {
			  visibleItemsCount += lstNormalItem.length;
			}

			if (moreButton != null && moreButton.style.display == 'block') {
			  visibleItemsCount++;
			}

			var resizableBlockWidth = resizableBlock.offsetWidth;
			var componentWidth = selectedItem.offsetWidth;
			var newVisibleItemCount = Math.floor(resizableBlockWidth / componentWidth);
			if (newVisibleItemCount < visibleItemsCount) {
			  //display 'More' button
			  if (moreButton != null && moreButton.style.display == 'none') {
			    moreButton.style.display = 'block';
			  }

			  var newVisibleComponentCount = newVisibleItemCount - 1; //one space for 'more' button
			  var newVisibleNormalComponentCount = newVisibleComponentCount - 1; //discount the selected component 

			  for (var i = lstNormalItem.length - 1; i >= newVisibleNormalComponentCount; i--) {
			    //move item to dropdown box
			    eXo.ecm.ECMUtils.moveItemToDropDown(lstNormalItem[i]);
			  }
			} else {
			  var lstExtendedComponent = document.getElementById('ListExtendedComponent');
			  var lstHiddenComponent = gj(lstExtendedComponent).children('a');

			  if (lstHiddenComponent.length > 0) {
			    var movedComponentCount = (newVisibleItemCount - visibleItemsCount) > lstHiddenComponent.length ? lstHiddenComponent.length : (newVisibleItemCount - visibleItemsCount);
			    for (var i = 0; i < movedComponentCount; i++) {
			      eXo.ecm.ECMUtils.moveItemToVisible(lstHiddenComponent[i]);
			    }

			    lstHiddenComponent = gj(lstExtendedComponent).children('a');
			    if (lstHiddenComponent.length <= 0) {
			      moreButton.style.display = 'none';
			    }
			  }
			}
			if (!showSideBar) {
			  container.style.display = 'none';
			  var resizeButton = gj(Self.uiWorkingArea).find("div.ResizeButton:first")[0];
			  resizeButton.className = "ResizeButton ShowLeftContent";
			}
		}

		ECMUtils.prototype.moveItemToDropDown = function (movedItem) {
			var lstExtendedComponent = document.getElementById('ListExtendedComponent');
			var iconOfMovedItem = gj(movedItem).children('div')[0];
			var classesOfIcon = iconOfMovedItem.className.split(' ');

			var link = document.createElement('a');
			link.setAttribute('title', movedItem.getAttribute('title'));
			link.className = 'IconPopup ' + classesOfIcon[classesOfIcon.length - 1];
			link.setAttribute('onclick', movedItem.getAttribute('onclick'));
			link.setAttribute('href', 'javascript:void(0);');

			var lstHiddenComponent = gj(lstExtendedComponent).children('a');
			if (lstHiddenComponent.length > 0) {
			  lstExtendedComponent.insertBefore(link, lstHiddenComponent[0]);
			} else {
			  lstExtendedComponent.appendChild(link);
			}

			//remove from visible area
			var parentOfMovedNode = movedItem.parentNode;
			parentOfMovedNode.removeChild(movedItem);
		}

		ECMUtils.prototype.moveItemToVisible = function (movedItem) {

			var container = document.getElementById("LeftContainer");
			var resizableBlock = gj(container).find("div.UIResizableBlock:first")[0];
			var selectContent = gj(resizableBlock).find("div.UISelectContent:first")[0];
			var moreButton = gj(selectContent).find("div.MoreItem:first")[0];

			var normalItem = document.createElement('div');
			normalItem.className = 'NormalItem';
			normalItem.setAttribute('title', movedItem.getAttribute('title'));
			normalItem.setAttribute('onclick', movedItem.getAttribute('onclick'));

			var iconItem = document.createElement('div');
			var lstClassOfMovedItem = movedItem.className.split(' ');
			iconItem.className = 'ItemIcon DefaultIcon ' + lstClassOfMovedItem[lstClassOfMovedItem.length - 1];

			var emptySpan = document.createElement('span');

			iconItem.appendChild(emptySpan);
			normalItem.appendChild(iconItem);

			selectContent.insertBefore(normalItem, moreButton);

			//remove from visible area
			var parentOfMovedNode = movedItem.parentNode;
			parentOfMovedNode.removeChild(movedItem);
		}
		/**
		 * @function        hiddenTabsContainerTrigger
		 * @purpose         display/hidden more button of tabscontainer
		 * @author          vinh_nguyen@exoplatform.com
		 */
		ECMUtils.prototype.hiddenTabsContainerTrigger = function (event) {
			event = event || window.event;
			event.cancelBubble = true;
			Self.uiShowMoreTabsContainer = gj(Self.uiRightContainer).find("div.ShowMoreTabsContainer:first")[0];
			Self.listHideTabsContainer = gj(Self.uiMainTabsContainer).find("div.ListHideTabsContainer:first")[0];
			var hiddenTabsChildren = gj(Self.listHideTabsContainer).children(".UITab");
			if (hiddenTabsChildren.length > 0) {
			  if (Self.listHideTabsContainer.style.display == "block") {
			    Self.listHideTabsContainer.style.display = "none";
			    gj(Self.uiShowMoreTabsContainer).removeClass("MoreContainerActivated");
			    Self.closeAllPopup(Self.listHideTabsContainer);
			  } else {
			  	Self.closeAllPopup(Self.listHideTabsContainer);
			    Self.listHideTabsContainer.style.display = "block";
			    gj(Self.uiShowMoreTabsContainer).addClass("MoreContainerActivated");
			    Self.pushMoreBlock(Self.uiShowMoreTabsContainer);
			  }
			} else {
			  Self.uiShowMoreTabsContainer.style.display = "none";
			}
			Self.loadContainerWidth();
		}

		/**
		 * @function       tabsContainer_OnResize
		 * @purpose        re-arrange the TabsContainer inside Right Container
		 * @author         vinh_nguyen@exoplatform.com
		 */
		ECMUtils.prototype.tabsContainer_OnResize = function () {
			Self.uiMainTabsContainer = gj(Self.uiRightContainer).find("div.MainTabsContainer:first")[0];
			if (!Self.uiMainTabsContainer) return; 
			Self.listHideTabsContainer = gj(Self.uiMainTabsContainer).find("div.ListHideTabsContainer:first")[0];
			Self.uiShowMoreTabsContainer = gj(Self.uiRightContainer).find("div.ShowMoreTabsContainer:first")[0];
			var allowedSpace = Self.uiMainTabsContainer.offsetWidth;
			var visibleTabsChildren = gj(Self.uiMainTabsContainer).children(".UITab");
			var sumWidth = 0;
			var flag = true;
			var flagMore = true;
			var hiddenTabsChildren = gj(Self.listHideTabsContainer).children(".UITab");
			if (hiddenTabsChildren.length > 0) {
			  Self.uiShowMoreTabsContainer.style.display = "block";
			  sumWidth = Self.uiShowMoreTabsContainer.offsetWidth;
			  flagMore = false;
			} else {
			  Self.uiShowMoreTabsContainer.style.display = "none";
			}

			for (var i = 0; i < visibleTabsChildren.length; i++) {
			  sumWidth += visibleTabsChildren[i].offsetWidth;
			}

			var index = visibleTabsChildren.length - 1;
			var flag = true;
			var removedItem;
			while (sumWidth > allowedSpace && index >= 0) {
			  if (gj(visibleTabsChildren[index]).find("div.SelectedActionBarTab:first").length <= 0) {
			    if (flagMore) {
			      Self.uiShowMoreTabsContainer.style.display = "block";
			      flagMore = false;
			      sumWidth += Self.uiShowMoreTabsContainer.offsetWidth;
			    }
			    sumWidth -= visibleTabsChildren[index].offsetWidth;
			    removedItem = Self.uiMainTabsContainer.removeChild(visibleTabsChildren[index]);
			    gj(Self.listHideTabsContainer).prepend(removedItem);
			  }
			  flag = false;
			  index--;
			}
			if (flag) {
			  Self.moveTabFromInvisible2VisibleContainer();
			}
		}

		ECMUtils.prototype.moveTabFromInvisible2VisibleContainer = function () {
			var flag = true;
			var removedItem;
			var moreWidth, remainItem;
			var sumWidth = 0;
			var visibleTabsChildren = gj(Self.uiMainTabsContainer).children(".UITab");
			var hiddenTabsChildren = gj(Self.listHideTabsContainer).children(".UITab");
			remainItem = hiddenTabsChildren.length;
			if (visibleTabsChildren.length > 0) {
			  for (var i = 0; i < visibleTabsChildren.length; i++) {
			    sumWidth += visibleTabsChildren[i].offsetWidth;
			  }
			}
			if (remainItem <= 0) {
			  Self.uiShowMoreTabsContainer.style.display = "none";
			  return false;
			}
			if (Self.uiMainTabsContainer.offsetWidth < sumWidth) return false;
			//Potential bug, seems to be improve more the code below, 
			while (flag) {
			  removedItem = Self.listHideTabsContainer.removeChild(hiddenTabsChildren[0]);
			  gj(Self.uiMainTabsContainer).append(removedItem);
			  sumWidth += removedItem.offsetWidth;
			  if (sumWidth >= (Self.uiMainTabsContainer.offsetWidth - (remainItem>1?Self.uiShowMoreTabsContainer.offsetWidth:0)) ) {
			    flag = false;
			    Self.uiMainTabsContainer.removeChild(removedItem);
			    gj(Self.listHideTabsContainer).prepend(removedItem);
			  } else {
			    hiddenTabsChildren = gj(Self.listHideTabsContainer).children(".UITab");
			    remainItem = hiddenTabsChildren.length;
			    if (remainItem <= 0) {
			    	Self.uiShowMoreTabsContainer.style.display = "none";
			    	flag = false;
			    }
			  }
			}
		}
		/**
		 * @function        loadContainerReference
		 * @purpose         Maintain object reference to some container which are accessed during resize
		 * @author          vinh_nguyen@exoplatform.com
		 */
		ECMUtils.prototype.loadContainerReference = function () {
			Self.uiWorkingArea  = gj(document).find('div.UIWorkingArea:first')[0];
			Self.uiLeftContainer = gj(Self.uiWorkingArea).find('div.LeftContainer:first')[0];
			Self.uiRightContainer = gj(Self.uiWorkingArea).find("div.RightContainer:first")[0];
			Self.uiSideBar = document.getElementById("UISideBar");
			Self.uiDocumentWorkspace = gj(Self.uiRightContainer).find("div.UIDocumentWorkspace:first")[0];
			Self.uiDrivesArea = gj(Self.uiRightContainer).find("div.UIDrivesArea:first")[0];    
			Self.uiResizeSideBar = gj(Self.uiWorkingArea).find("div.ResizeSideBar:first")[0];
		}
		/**
		 * @function   loadContainerWidth
		 * @purpose    Load container width from cookies
		 * @author     vinh_nguyen@exoplatform.com
		 */
		ECMUtils.prototype.loadContainerWidth = function (leftWidth) {
			Self.loadContainerReference();
			var leftContainerWidth, actualLeftContainerWidth;

			if (leftWidth) {
			  leftContainerWidth = leftWidth;
			} else {
			  leftContainerWidth = Self.getCookie(eXo.env.portal.userName + "_leftContainerWidth");
			  showSideBar = Self.getCookie(eXo.env.portal.userName + "_CEShowSideBar")!="false";
			}
			if (Self.initWithoutLeftContainer()) {
			  Self.uiRightContainer.style.width = Self.uiWorkingArea.offsetWidth + "px";
			}else {
			  if (leftContainerWidth) {
			    if (Self.uiWorkingArea.offsetWidth - leftContainerWidth < Self.MiniumRightContainerWidth) {
			      leftContainerWidth = Self.uiWorkingArea.offsetWidth - Self.MiniumRightContainerWidth;
			      Self.setCookie(eXo.env.portal.userName + "_leftContainerWidth", leftContainerWidth, 20);
			    }
			    Self.uiLeftContainer.style.width = (leftContainerWidth - 15) + "px";
			  }
				if (Self.uiLeftContainer.offsetWidth > 0 || Self.uiLeftContainer.offsetHeight > 0) {
				  actualLeftContainerWidth = Self.uiLeftContainer.offsetWidth;
				} else {
				  actualLeftContainerWidth = 0;
				}
				Self.uiSideBar.style.width = (actualLeftContainerWidth - 10) + "px";
			  Self.uiRightContainer.style.width = (Self.uiWorkingArea.offsetWidth - actualLeftContainerWidth - Self.uiResizeSideBar.offsetWidth) + "px";
			  if (showSideBar) {
			  Self.uiResizeSideBar.style.height = Self.uiLeftContainer.offsetHeight + "px";
			}else {
			  Self.uiResizeSideBar.style.height = Self.uiRightContainer.offsetHeight + "px";
			  var resizeButton = gj(Self.uiResizeSideBar).find("div.ResizeButton:first")[0];
			  gj(resizeButton).addClass("ShowLeftContent");
			  gj(Self.uiResizeSideBar).addClass("ResizeNoneBorder");
			}
			}
			if (Self.uiDocumentWorkspace) {
			  Self.uiDocumentWorkspace.style.width = (Self.uiRightContainer.offsetWidth - 10) + "px";
			}
			if (Self.uiDrivesArea) {
				Self.uiDrivesArea.style.width = (Self.uiRightContainer.offsetWidth - 10) + "px";
			}
			Self.resizeVisibleComponent();
			Self.actionbarContainer_OnResize();
			if (Self.documentContainer_OnResize) Self.documentContainer_OnResize();
			Self.tabsContainer_OnResize();
			Self.clearFillOutElement();
			Self.adjustFillOutElement();
			if (eXo.core.Browser.isIE()) {
				var jcrContainer = document.getElementById("UIJCRExplorerPortlet");
				if (jcrContainer) gj(jcrContainer).css("height", "100%");
			}
		}
		/**
		 * 
		 */
		ECMUtils.prototype.pushMoreBlock = function (obj) {
			var blocks= gj(obj).find("div.UIRightClickPopupMenu:first");
			blocks = (blocks==null||blocks.length==0)?gj(obj).find("div.UIDropDownMenu:first"):blocks;
			if (blocks.length >0) {
			  blocks[0].onmousedown = function (event) {
			    var event = event || window.event;
			    event.cancelBubble = true;
			  }
			  blocks[0].onkeydown = function (event) {
			    var event = event || window.event;
			    event.cancelBubble = true;
			  }
			  Self.popupArray.push(blocks[0]);
			  return;
			}
		}
		ECMUtils.prototype.resizeMouseUpSideBar = function (event) {
			if (Self.initWithoutLeftContainer()) return;
			document.onmousemove = null;
			document.onmouseup = null;
			var allowedWidth = parseInt(Self.uiWorkingArea.offsetWidth) - Self.MiniumRightContainerWidth;
			var savedWidth = eXo.core.Browser.findMouseRelativeX(Self.uiWorkingArea, event);

			if (savedWidth > allowedWidth) {
			  savedWidth = allowedWidth;
			} else if (savedWidth < Self.MiniumLeftContainerWidth) {
			  savedWidth = Self.MiniumLeftContainerWidth;
			}
			Self.setCookie(eXo.env.portal.userName + "_leftContainerWidth", savedWidth, 20);
			Self.loadContainerWidth(savedWidth);
			eXo.ecm.ECMUtils.isResizedLeft = true;
			//VinhNT Reenable selection of text
			var jcrExpPortlet = document.getElementById("UIJCRExplorer");
			gj(jcrExpPortlet).removeClass("UIJCRExplorerNoSelect");
			if (Self.uiResizeSideBar) gj(Self.uiResizeSideBar).removeClass("ResizeSideBarDisplay");
			Self.UIBrokenCheckingHandler = window.setInterval("eXo.ecm.ECMUtils.UIBrokenChecking();", Self.UIBrokenCheckingInterval);
			delete eXo.ecm.ECMUtils.currentWidth;
			delete eXo.ecm.ECMUtils.currentMouseX;
			delete eXo.ecm.ECMUtils.resizableBlockWidth;
			delete eXo.ecm.ECMUtils.savedResizeDistance;
		}

		ECMUtils.prototype.showHideSideBar = function (event, isVisible) {
			var event = event || window.event;
			if (Self.initWithoutLeftContainer()) return;
			event.cancelBubble = true;
			var leftContainer = document.getElementById("LeftContainer");
			var rightContainer = gj(Self.uiWorkingArea).find("div.RightContainer:first")[0];
			var resizeButton = gj(Self.uiWorkingArea).find("div.ResizeButton:first")[0];
			// The bellow block are updated
		    if (leftContainer.className == "LeftContainer NoShow") {
		      gj(leftContainer).removeClass("NoShow");			  
		      gj(resizeButton).removeClass("ShowLeftContent");
			  gj(Self.uiResizeSideBar).removeClass("ResizeNoneBorder");
			  Self.uiResizeSideBar.style.height = leftContainer.offsetHeight + "px";
			  showSideBar = true;
			  if (typeof (eXo.ecm.ECMUtils.heightOfItemArea) == "undefined") {
			    var itemArea = document.getElementById("SelectItemArea");
			    if (itemArea && itemArea.style.display=="block") {
			      eXo.ecm.ECMUtils.savedDisplayStatusOfItemArea = "block";
			      eXo.ecm.ECMUtils.heightOfItemArea = itemArea.offsetHeight;
			    } else {
			      eXo.ecm.ECMUtils.heightOfItemArea = 286;
			    }
			 }
			} else {
			  gj(leftContainer).addClass("LeftContainer");
			  gj(resizeButton).addClass("ShowLeftContent");
			  gj(Self.uiResizeSideBar).addClass("ResizeNoneBorder");
			  showSideBar = false;
			}
			Self.setCookie(eXo.env.portal.userName + "_CEShowSideBar", showSideBar, 20);
			Self.loadContainerWidth();
		}

		ECMUtils.prototype.loadEffectedSideBar = function (id) {
			var leftContainer = document.getElementById("LeftContainer");
			var resizableBlock = gj(leftContainer).find("div.UIResizableBlock:first")[0];
			if (eXo.ecm.ECMUtils.savedLeftContainer && eXo.ecm.ECMUtils.savedResizableMouseX) {
			  if (eXo.ecm.ECMUtils.isResizedLeft == true) leftContainer.style.width = eXo.ecm.ECMUtils.savedLeftContainer;
			  var documentInfo = document.getElementById("UIDocumentInfo");
			  var listGrid = gj(documentInfo).find("div.UIListGrid:first")[0];
			  if (listGrid) listGrid.style.width = listGrid.offsetWidth + 200 + parseInt(eXo.ecm.ECMUtils.savedResizableMouseX) + "px";
			}
			eXo.ecm.ECMUtils.focusCurrentNodeInTree(id);
		}

		ECMUtils.prototype.resizeTreeInSideBar = function (event) {
			var event = event || window.event;
			eXo.ecm.ECMUtils.currentMouseY = event.clientY;
			var workingArea = document.getElementById('UIWorkingArea');
			var uiResizableBlock = gj(workingArea).find("div.UIResizableBlock:first")[0];
			var container = document.getElementById("UITreeExplorer");
			//var isOtherTabs=false;
			var listContainers;
			if (!container) {
			  listContainers = gj(uiResizableBlock).find("div.SideBarContent");
			  for (var k = 0; k < listContainers.length; k++) {
			    if (listContainers[k].parentNode.className != "UIResizableBlock") {
			      container = listContainers[k - 1];
			      //isOtherTabs = true;
			      break;
			    }
			  }
			}
			eXo.ecm.ECMUtils.currentHeight = container.offsetHeight;
			checkRoot();

			eXo.ecm.ECMUtils.resizableHeight = uiResizableBlock.offsetHeight;
			eXo.ecm.ECMUtils.containerResize = container;
			document.onmousemove = eXo.ecm.ECMUtils.resizeMouseMoveItemsInSideBar;
			document.onmouseup = eXo.ecm.ECMUtils.resizeMouseUpItemsInSideBar;
		}

		ECMUtils.prototype.resizeMouseMoveItemsInSideBar = function (event) {
			var event = event || window.event;
			//var container = document.getElementById("UITreeExplorer");
			var container = eXo.ecm.ECMUtils.containerResize;
			var deltaY = event.clientY - eXo.ecm.ECMUtils.currentMouseY;
			eXo.ecm.ECMUtils.resizableY = deltaY;

			var resizeDiv = document.getElementById("ResizeVerticalSideBarDiv");
			if (resizeDiv == null) {
			  resizeDiv = document.createElement("div");
			  resizeDiv.className = "VResizeHandle";
			  resizeDiv.id = "ResizeVerticalSideBarDiv";
			  var workingArea = gj(container).parents(".UIWorkingArea:first")[0];
			  var uiResizableBlock = gj(workingArea).find("div.UIResizableBlock:first")[0];
			  resizeDiv.style.width = container.offsetWidth + "px";
			  uiResizableBlock.appendChild(resizeDiv);
			}
			var Y_Resize = eXo.core.Browser.findMouseRelativeY(uiResizableBlock, event);
			var X_Resize = eXo.core.Browser.findPosXInContainer(container, uiResizableBlock);
			eXo.core.Browser.setPositionInContainer(uiResizableBlock, resizeDiv, X_Resize, Y_Resize);
			eXo.ecm.ECMUtils.savedTreeSizeMouseY = eXo.ecm.ECMUtils.currentHeight + deltaY + "px";
		}

		ECMUtils.prototype.resizeMouseUpItemsInSideBar = function (event) {
			document.onmousemove = null;
			// The block are updated
			var workingArea = document.getElementById('UIWorkingArea');
			var sizeBarContainer = gj(workingArea).find("div.UISideBarContainer:first")[0];

			// Remove new added div
			var uiResizableBlock = gj(workingArea).find("div.UIResizableBlock:first")[0];
			if (uiResizableBlock) {
			  var resizeDiv = document.getElementById("ResizeVerticalSideBarDiv");
			  if (resizeDiv) uiResizableBlock.removeChild(resizeDiv);
			}

			// The bellow block are updated
			sizeBarContainer.style.height = eXo.ecm.ECMUtils.resizableHeight + eXo.ecm.ECMUtils.resizableY + 20 + "px";
			var root = checkRoot();
			root.style.height = eXo.ecm.ECMUtils.resizableHeight + eXo.ecm.ECMUtils.resizableY + 20 + "px";
			if (eXo.ecm.ECMUtils.resizableHeight + eXo.ecm.ECMUtils.resizableY < eXo.ecm.ECMUtils.defaultHeight) {
			  sizeBarContainer.style.height = eXo.ecm.ECMUtils.defaultHeight + 20 + "px";
			}
			var container = eXo.ecm.ECMUtils.containerResize;
			if (container) container.style.height = eXo.ecm.ECMUtils.currentHeight + eXo.ecm.ECMUtils.resizableY + "px"
			delete eXo.ecm.ECMUtils.currentHeight;
			delete eXo.ecm.ECMUtils.currentMouseY;
			delete eXo.ecm.ECMUtils.resizableHeight
			delete eXo.ecm.ECMUtils.resizableY;
			delete eXo.ecm.ECMUtils.containerResize;
		}

		ECMUtils.prototype.showHideItemsInSideBar = function (event) {
			var itemArea = document.getElementById("SelectItemArea");

			Self.clearFillOutElement();

			if (itemArea.style.display == 'none') {
			  Self.showSelectItemArea();
			} else {
			  Self.hideSelectItemArea();
			}

			Self.adjustFillOutElement();
		}

		ECMUtils.prototype.showSelectItemArea = function () {
			var treeExplorer = document.getElementById("UITreeExplorer");
			var itemArea = document.getElementById("SelectItemArea");

			var workingArea = document.getElementById('UIWorkingArea');
			var resizableBlock = gj(workingArea).find("div.UIResizableBlock:first")[0];
			var resizeTreeButton = gj(resizableBlock).find("div.ResizeTreeButton:first")[0];

			if (treeExplorer) {
			  Self.collapseTreeExplorer()
			} else {
			  Self.collapseSideBarContent(); //collapse other tabs
			}

			itemArea.style.display = "block";
			eXo.ecm.ECMUtils.savedDisplayStatusOfItemArea = "block";
			resizeTreeButton.className = "ResizeTreeButton";
		}

		ECMUtils.prototype.hideSelectItemArea = function () {
			var treeExplorer = document.getElementById("UITreeExplorer");
			var itemArea = document.getElementById("SelectItemArea");

			var workingArea = document.getElementById('UIWorkingArea');
			var resizableBlock = gj(workingArea).find("div.UIResizableBlock:first")[0];
			var resizeTreeButton = gj(resizableBlock).find("div.ResizeTreeButton:first")[0];

			if (treeExplorer) {
			  Self.expandTreeExplorer();
			} else {
			  Self.expandSideBarContent();
			}

			itemArea.style.display = "none";
			eXo.ecm.ECMUtils.savedDisplayStatusOfItemArea = "none";
			resizeTreeButton.className = "ResizeTreeButton ShowContentButton";
		}

		ECMUtils.prototype.collapseTreeExplorer = function () {
			var treeExplorer = document.getElementById("UITreeExplorer");
			treeExplorer.style.height = treeExplorer.offsetHeight - eXo.ecm.ECMUtils.heightOfItemArea - 20 + "px";
		}

		ECMUtils.prototype.expandTreeExplorer = function () {
			var workingArea = document.getElementById('UIWorkingArea');
			var leftContainer = document.getElementById("LeftContainer");
			var rightContainer = gj(workingArea).find("div.RightContainer:first")[0];

			var resizableBlock = gj(leftContainer).find("div.UIResizableBlock:first")[0];
			var sideBarContent = gj(resizableBlock).find("div.SideBarContent:first")[0];
			var selectContent = gj(resizableBlock).find("div.UISelectContent:first")[0];
			var resizeTreeExplorer = gj(resizableBlock).find("div.ResizeTreeExplorer:first")[0];
			var barContent = gj(sideBarContent).find("div.BarContent:first")[0];

			var treeExplorer = document.getElementById("UITreeExplorer");
			var itemArea = document.getElementById("SelectItemArea");

			if (leftContainer.offsetHeight > rightContainer.offsetHeight) {
			  treeExplorer.style.height = treeExplorer.offsetHeight + itemArea.offsetHeight - 20 + "px";
			} else {
			  //expand the tree explorer to equal to the right container
			  var treeExplorerHeight = rightContainer.offsetHeight;

			  if (selectContent) {
			    treeExplorerHeight -= selectContent.offsetHeight;
			  }

			  if (resizeTreeExplorer) {
			    treeExplorerHeight -= resizeTreeExplorer.offsetHeight;
			  }

			  if (barContent) {
			    treeExplorerHeight -= barContent.offsetHeight;
			  }

			  treeExplorer.style.height = treeExplorerHeight - 28 + 'px';
			}
		}

		ECMUtils.prototype.collapseSideBarContent = function () {
			var container = eXo.ecm.ECMUtils.getContainerToResize();
			container.style.height = eXo.ecm.ECMUtils.initialHeightOfOtherTab - 4 + "px";
		}

		ECMUtils.prototype.expandSideBarContent = function () {
			var workingArea = document.getElementById('UIWorkingArea');
			var leftContainer = document.getElementById("LeftContainer");
			var rightContainer = gj(workingArea).find("div.RightContainer:first")[0];

			var resizableBlock = gj(workingArea).find("div.UIResizableBlock:first")[0];
			var sideBarContent = gj(resizableBlock).find("div.SideBarContent:first")[0];
			var selectContent = gj(resizableBlock).find("div.UISelectContent:first")[0];
			var resizeTreeExplorer = gj(resizableBlock).find("div.ResizeTreeExplorer:first")[0];

			var itemArea = document.getElementById("SelectItemArea");

			var container = eXo.ecm.ECMUtils.getContainerToResize();
			eXo.ecm.ECMUtils.initialHeightOfOtherTab = container.offsetHeight;

			if (leftContainer.offsetHeight > rightContainer.offsetHeight) {
			  container.style.height = container.offsetHeight + itemArea.offsetHeight + "px";
			} else {
			  var previousElement = gj(container).prevAll("div:first")[0];
			  var containerHeight = rightContainer.offsetHeight;

			  if (selectContent) {
			    containerHeight -= selectContent.offsetHeight;
			  }

			  if (resizeTreeExplorer) {
			    containerHeight -= resizeTreeExplorer.offsetHeight;
			  }

			  if (previousElement) {
			    containerHeight -= previousElement.offsetHeight;
			  }

			  container.style.height = containerHeight + "px";
			}
		}

		//get SideBarContent for resizing
		ECMUtils.prototype.getContainerToResize = function () {
			var treeExplorer = document.getElementById("UITreeExplorer");
			if (treeExplorer) {
			  return treeExplorer;
			}

			var workingArea = document.getElementById('UIWorkingArea');
			var resizableBlock = gj(workingArea).find("div.UIResizableBlock:first")[0];

			listContainers = gj(resizableBlock).find("div.SideBarContent");
			for (var k = 0; k < listContainers.length; k++) {
			  if (listContainers[k].parentNode.className != "UIResizableBlock") {
			    return listContainers[k - 1];
			  }
			}
		}

		ECMUtils.prototype.loadEffectedItemsInSideBar = function () {
			window.setTimeout("eXo.ecm.ECMUtils.adjustItemsInSideBar();", 50);
			Self.UIBrokenCheckingHandler = window.setInterval("eXo.ecm.ECMUtils.UIBrokenChecking();", Self.UIBrokenCheckingInterval);
			Self.waitContainerRetry = 0;
			Self.waitForContainer();
		}
	
		ECMUtils.prototype.UIBrokenChecking = function () {
			Self.uiWorkingArea = gj(document).find('div.UIWorkingArea:first')[0];
			Self.uiLeftContainer = gj(Self.uiWorkingArea).find('div.LeftContainer:first')[0];
			Self.uiRightContainer = gj(Self.uiWorkingArea).find("div.RightContainer:first")[0];
			Self.uiResizeSideBar = gj(Self.uiWorkingArea).find("div.ResizeSideBar:first")[0];
			if (Self.uiLeftContainer && Self.uiRightContainer && Self.uiWorkingArea) {
			  if (Self.uiLeftContainer.offsetWidth + Self.uiRightContainer.offsetWidth + Self.uiResizeSideBar.offsetWidth  > Self.uiWorkingArea) {
			  	Self.loadContainerWidth();
			  }else if (Self.uiLeftContainer.offsetWidth + Self.uiRightContainer.offsetWidth + Self.uiResizeSideBar.offsetWidth  < Self.uiWorkingArea -2) {
			  	Self.loadContainerWidth();
			  }else if (Self.uiLeftContainer.offsetTop < Self.uiRightContainer.offsetTop) {
			  	Self.loadContainerWidth();
			  }
			  
			}
		}
		/**
		 * @fucntion   waitForContainer
		 * @purpose    wait for leftContainer, RightContainer and other container related to resize ready
		 * @author     vinh_nguyen@exoplatform.com
		 */
		ECMUtils.prototype.waitForContainer = function () {
			Self.waitContainerRetry++;
			if (Self.waitContainerRetry > Self.waitContainerRetryMax) {
			  var fixToolbar = gj(document).find("div.UIToolbarContainer:first")[0];
			  if (fixToolbar) gj(fixToolbar).css('z-index', 10);
			  console.log("Load Container time out");
			  Self.loadContainerWidth();
			  return;
			}
			Self.uiWorkingArea = gj(document).find('div.UIWorkingArea:first')[0];
			if (!Self.uiWorkingArea) {
			  window.setTimeout("eXo.ecm.ECMUtils.waitForContainer();", Self.waitInterval);
			  return;
			}
			if (!Self.initWithoutLeftContainer()) {
				Self.uiLeftContainer = gj(Self.uiWorkingArea).find('div.LeftContainer:first')[0];
				if (!Self.uiLeftContainer) {
				  window.setTimeout("eXo.ecm.ECMUtils.waitForContainer();", Self.waitInterval);
				  return;
				}
			}
			Self.uiRightContainer = gj(Self.uiWorkingArea).find("div.RightContainer:first")[0];
			if (!Self.uiRightContainer) {
			  window.setTimeout("eXo.ecm.ECMUtils.waitForContainer();", Self.waitInterval);
			  return;
			}
			if (!Self.initWithoutLeftContainer()) {
				Self.uiSideBar = gj(Self.uiWorkingArea).find("div.UISideBar:first")[0];
				if (!Self.uiSideBar) {
				  window.setTimeout("eXo.ecm.ECMUtils.waitForContainer();", Self.waitInterval);
				  return;
				}
			}
			
			Self.uiDocumentWorkspace = gj(Self.uiWorkingArea).find("div.UIDocumentWorkspace:first")[0];
			Self.uiDrivesArea = gj(Self.uiWorkingArea).find("div.UIDrivesArea:first")[0];
			if (!(Self.uiDocumentWorkspace || Self.uiDrivesArea)) {
			  window.setTimeout("eXo.ecm.ECMUtils.waitForContainer();", Self.waitInterval);
			  return;
			}
			Self.loadContainerWidth();
			var fixToolbar = gj(document).find("div.UIToolbarContainer:first")[0];
			if (fixToolbar) gj(fixToolbar).css( 'z-index', 10 );
		}

			    ECMUtils.prototype.activateWCMDeleteNotice = function(nodePathDelete, deleteNotice) {
			      var noticeElem = document.getElementById("wcm-notice");
			      if(nodePathDelete == null || nodePathDelete == '') {
	 	    noticeElem.style.display = "none";
			        noticeElem.innerHTML = "";
			} else {
			        var addressBar = gj("#UIAddressBar");
			        if(addressBar) {
				  var pos = gj("#UIAddressBar").position();            
			          noticeElem.style.display = "block";
			          noticeElem.style.top = (pos.top + 5) + "px";
			        }   
			        noticeElem.style.display = "block";
			        nodePathDelete = " <a href=\""+nodePathDelete+"\">Undo</a>";
			        deleteNotice = deleteNotice + nodePathDelete;
			        noticeElem.innerHTML = deleteNotice;
				noticeElem.style.marginLeft = "-" + noticeElem.offsetWidth/2 + "px";
			      }
		}

		ECMUtils.prototype.activateWCMRestoreNotice = function(restoreNotice) {
			      var noticeElem = document.getElementById("wcm-notice");
			      if(restoreNotice == null || restoreNotice == '') {
	 	    noticeElem.style.display = "none";
			        noticeElem.innerHTML = "";
			} else {
			        var addressBar = gj("#UIAddressBar");
			        if(addressBar) {
				  var pos = gj("#UIAddressBar").position();            
			          noticeElem.style.display = "block";
			          noticeElem.style.top = (pos.top + 5) + "px";
			        }     
			        noticeElem.style.display = "block";            
			        noticeElem.innerHTML = restoreNotice;
				noticeElem.style.marginLeft = "-" + noticeElem.offsetWidth/2 + "px";
			      }
		}
		}

		ECMUtils.prototype.adjustItemsInSideBar = function () {
			//if LeftContainer doesn't exist, do nothing
			var container = document.getElementById("LeftContainer");
			if (!container) {
			  return;
			}

			var treeExplorer = document.getElementById("UITreeExplorer");
			var itemArea = document.getElementById("SelectItemArea");

			if (typeof (eXo.ecm.ECMUtils.heightOfItemArea) == "undefined") {
			  if (itemArea) {
			    eXo.ecm.ECMUtils.heightOfItemArea = itemArea.offsetHeight;
			  } else {
			    eXo.ecm.ECMUtils.heightOfItemArea = 286;
			  }
			  if (eXo.ecm.ECMUtils.heightOfItemArea == 0) { //Still be invisible, set by default value^M
			    eXo.ecm.ECMUtils.heightOfItemArea = 286;
			  }
			}

			//adjust the height of items in side bar
			if (treeExplorer) {
			  eXo.ecm.ECMUtils.adjustTreeExplorer();
			} else {
			  eXo.ecm.ECMUtils.adjustAnotherTab();
			}

			eXo.ecm.ECMUtils.adjustFillOutElement();
		}

		ECMUtils.prototype.adjustTreeExplorer = function () {
			var workingArea = document.getElementById('UIWorkingArea');
			var leftContainer = document.getElementById("LeftContainer");
			var rightContainer = gj(workingArea).find("div.RightContainer:first")[0];

			var resizableBlock = gj(leftContainer).find("div.UIResizableBlock:first")[0];
			var sideBarContent = gj(resizableBlock).find("div.SideBarContent:first")[0];
			var selectContent = gj(resizableBlock).find("div.UISelectContent:first")[0];
			var resizeTreeExplorer = gj(resizableBlock).find("div.ResizeTreeExplorer:first")[0];
			var resizeTreeButton = gj(resizeTreeExplorer).find("div.ResizeTreeButton:first")[0];
			var barContent = gj(sideBarContent).find("div.BarContent:first")[0];

			var treeExplorer = document.getElementById("UITreeExplorer");
			var itemArea = document.getElementById("SelectItemArea");

			//keep some parameters
			if (typeof (eXo.ecm.ECMUtils.initialHeightOfTreeExplorer) == "undefined") {
			  eXo.ecm.ECMUtils.initialHeightOfTreeExplorer = treeExplorer.offsetHeight;
			}

			if (typeof (eXo.ecm.ECMUtils.initialHeightOfLeftContainerTree) == "undefined") {
			  eXo.ecm.ECMUtils.initialHeightOfLeftContainerTree = leftContainer.offsetHeight;
			}

			if (typeof (eXo.ecm.ECMUtils.savedDisplayStatusOfItemArea) == "undefined") {
			  eXo.ecm.ECMUtils.savedDisplayStatusOfItemArea = "block";
			}

			//show or hide the SelectItemArea
			if (itemArea) {
			  itemArea.style.display = eXo.ecm.ECMUtils.savedDisplayStatusOfItemArea;
			  if (eXo.ecm.ECMUtils.savedDisplayStatusOfItemArea == "none") {
			    resizeTreeButton.className = "ResizeTreeButton ShowContentButton";
			  } else {
			    resizeTreeButton.className = "ResizeTreeButton";
			  }
			}

			//adjust the height of tree explorer
			if (!itemArea) {
			  treeExplorer.style.height = eXo.ecm.ECMUtils.initialHeightOfTreeExplorer + eXo.ecm.ECMUtils.heightOfItemArea - 20 + 'px';
			} else if (rightContainer.offsetHeight > eXo.ecm.ECMUtils.initialHeightOfLeftContainerTree) {
			  var treeExplorerHeight = rightContainer.offsetHeight;

			  if (itemArea) {
			    treeExplorerHeight -= itemArea.offsetHeight;
			  }

			  if (selectContent) {
			    treeExplorerHeight -= selectContent.offsetHeight;
			  }

			  if (resizeTreeExplorer) {
			    treeExplorerHeight -= resizeTreeExplorer.offsetHeight;
			  }

			  if (barContent) {
			    treeExplorerHeight -= barContent.offsetHeight;
			  }

			  var treeExplorerFull = gj(treeExplorer).children("div")[0];
			  if (treeExplorerFull.offsetHeight > eXo.ecm.ECMUtils.initialHeightOfTreeExplorer && treeExplorerHeight > treeExplorerFull.offsetHeight) {
			    treeExplorerHeight = treeExplorerFull.offsetHeight
			  }

			  treeExplorer.style.height = treeExplorerHeight - 28 + 'px';
			} else {
			  if (itemArea.style.display == 'none') {
			    treeExplorer.style.height = eXo.ecm.ECMUtils.initialHeightOfTreeExplorer + eXo.ecm.ECMUtils.heightOfItemArea - 20 + 'px';
			  } else {
			    treeExplorer.style.height = eXo.ecm.ECMUtils.initialHeightOfTreeExplorer - 20 + 'px';
			  }
			}
		}

		ECMUtils.prototype.adjustAnotherTab = function () {
			var workingArea = document.getElementById('UIWorkingArea');
			var leftContainer = document.getElementById("LeftContainer");
			var rightContainer = gj(workingArea).find("div.RightContainer:first")[0];

			var resizableBlock = gj(workingArea).find("div.UIResizableBlock:first")[0];
			var sideBarContent = gj(resizableBlock).find("div.SideBarContent:first")[0];
			var selectContent = gj(resizableBlock).find("div.UISelectContent:first")[0];
			var resizeTreeExplorer = gj(resizableBlock).find("div.ResizeTreeExplorer:first")[0];
			var resizeTreeButton = gj(resizeTreeExplorer).find("div.ResizeTreeButton:first")[0];

			var itemArea = document.getElementById("SelectItemArea");
			var container = eXo.ecm.ECMUtils.getContainerToResize();

			//keep some parameters
			eXo.ecm.ECMUtils.initialHeightOfOtherTab = container.offsetHeight;
			eXo.ecm.ECMUtils.initialHeightOfLeftContainerAnotherTab = leftContainer.offsetHeight;

			if (typeof (eXo.ecm.ECMUtils.savedDisplayStatusOfItemArea) == "undefined") {
			  eXo.ecm.ECMUtils.savedDisplayStatusOfItemArea = "block";
			}

			//show or hide the SelectItemArea
			itemArea.style.display = eXo.ecm.ECMUtils.savedDisplayStatusOfItemArea;
			if (eXo.ecm.ECMUtils.savedDisplayStatusOfItemArea == "none") {
			  resizeTreeButton.className = "ResizeTreeButton ShowContentButton";
			} else {
			  resizeTreeButton.className = "ResizeTreeButton";
			}

			//adjust the height of container
			if (itemArea.style.display == 'none') {
			  if (rightContainer.offsetHeight > eXo.ecm.ECMUtils.initialHeightOfLeftContainerAnotherTab) {
			    var previousElement = gj(container).prevAll("div:first")[0];
			    var containerHeight = rightContainer.offsetHeight;

			    if (selectContent) {
			      containerHeight -= selectContent.offsetHeight;
			    }

			    if (resizeTreeExplorer) {
			      containerHeight -= resizeTreeExplorer.offsetHeight;
			    }

			    if (itemArea) {
			      containerHeight -= itemArea.offsetHeight;
			    }

			    if (previousElement) {
			      containerHeight -= previousElement.offsetHeight;
			    }

			    container.style.height = containerHeight + "px";
			  } else {
			    container.style.height = eXo.ecm.ECMUtils.initialHeightOfOtherTab + eXo.ecm.ECMUtils.heightOfItemArea + 'px';
			  }
			}
		}

		ECMUtils.prototype.clearFillOutElement = function () {
			var fillOutElement = document.getElementById('FillOutElement');
			if (fillOutElement) {
			  fillOutElement.style.width = "0px";
			  fillOutElement.style.height = "0px";
			}
		}

		ECMUtils.prototype.adjustFillOutElement = function () {
			if (eXo.ecm.ECMUtils.initWithoutLeftContainer()) return;
			var workingArea = document.getElementById('UIWorkingArea');
			var leftContainer = document.getElementById("LeftContainer");
			var rightContainer = gj(workingArea).find("div.RightContainer:first")[0];
			if (rightContainer.offsetHeight < leftContainer.offsetHeight) {
			  var fillOutElement = document.getElementById('FillOutElement');
			  if (fillOutElement) {
			    fillOutElement.style.width = "0px";
			    fillOutElement.style.height = leftContainer.offsetHeight - rightContainer.offsetHeight + "px";
			  }
			}
		}

		ECMUtils.prototype.disableAutocomplete = function (id) {
			var clickedElement = document.getElementById(id);
			tagNameInput = gj(clickedElement).find("div.UITagNameInput:first")[0];
			gj(tagNameInput).find("#names:first")[0].setAttribute("autocomplete", "off");
		}

		ECMUtils.prototype.selectedPath = function (id) {
			var select = document.getElementById(id);
			if (select) select.className = select.className + " " + "SelectedNode";
		}

		ECMUtils.prototype.hiddenExtendedListTrigger = function (event, obj) {
			var elemt = document.getElementById("ListExtendedComponent");
			var event = event || window.event;
			event.cancelBubble = true;
			if (elemt.style.display == 'none') {
			  elemt.style.display = 'block';
			  Self.pushMoreBlock(obj);
			} else {
			  elemt.style.display = 'none';
			}
		}

		//private method
		function checkRoot() {
			var workingArea = document.getElementById('UIWorkingArea');
			root = document.getElementById("UIDocumentWorkspace");
			if (root) {
			  eXo.ecm.ECMUtils.defaultHeight = root.offsetHeight;
			  var actionBar = document.getElementById('UIActionBar');

			  if (actionBar) {
			    eXo.ecm.ECMUtils.defaultHeight += actionBar.offsetHeight;
			  }

			} else {
			  root = gj(workingArea).find("div.RightContainer:first")[0];
			  eXo.ecm.ECMUtils.defaultHeight = root.offsetHeight;
			}
			return root;
		}

		ECMUtils.prototype.updateListGridWidth = function () {
			var documentInfo = document.getElementById("UIDocumentInfo");
			var listGrid = gj(documentInfo).find("div.UIListGrid:first")[0];
			if (listGrid) {
			  var minimumWidth = eXo.ecm.ECMUtils.getMinimumWidthOfUIListGrid(listGrid);
			  listGrid.style.width = (documentInfo.offsetWidth < minimumWidth) ? minimumWidth + 'px' : 'auto';
			}
		}

		ECMUtils.prototype.getMinimumWidthOfUIListGrid = function (listGrid) {
			if (!listGrid) return 0;
			var titleTable = gj(listGrid).find("div.TitleTable:first")[0];
			var uiClearFix = gj(listGrid).find("div.ClearFix")[0];
			var chidrenItems = gj(uiClearFix).children("div");

			var minimumWidth = 0;
			for (var i = 0; i < chidrenItems.length; i++) {
			  if (chidrenItems[i].className == "LineLeft" || chidrenItems[i].className.indexOf("Column") == 0) {
			    minimumWidth += chidrenItems[i].offsetWidth;
			  }
			}

			minimumWidth += 5; //for border
			return minimumWidth;
		}

	ECMUtils.prototype.showInContextHelp = function () {
		var parentElm = document.getElementById("idAllDrivers");
		var inContextContentHelp = gj(parentElm).find("div.InContextHelpContent:first")[0];
		if (inContextContentHelp) {
			setTimeout(function () {
			  inContextContentHelp.style.display = "none";
			}, 6 * 1000);
		}
	};

	ECMUtils.prototype.onDbClickOnTreeExplorer = function () {
		if (new RegExp("MSIE 8").test(navigator.userAgent)) {
			document.ondblclick = function () {
			  if (window.getSelection) window.getSelection().removeAllRanges();
			  else if (document.selection) document.selection.empty();
			}
		}
	};

	ECMUtils.prototype.displayFullAlternativeText = function (displayDiv) {
		if (displayDiv) {
			var parentDiv = gj(displayDiv).parents("div:first")[0];
			if (parentDiv) {
			  parentDiv.style.display = "none";
			  var closeDiv = gj(parentDiv).nextAll("div:first")[0];
			  if (closeDiv) {
			    closeDiv.style.display = "block";
			  }
			}
		}
	};

	ECMUtils.prototype.collapseAlternativeText = function (displayDiv) {
		if (displayDiv) {
			var parentDiv = gj(displayDiv).parents("div:first")[0];
			if (parentDiv) {
			  parentDiv.style.display = "none";
			  var closeDiv = gj(parentDiv).prevAll("div:first")[0];
			  if (closeDiv) {
			    closeDiv.style.display = "block";
			  }
			}
		}
	};

	ECMUtils.prototype.ajaxRedirect = function(url) {
		url =	url.replace(/&amp;/g, "&") ;
		window.location.href = url ;
	};

	eXo.ecm.ECMUtils = new ECMUtils();
	
	function Instance(ifrm) {
		this.win = ifrm.contentWindow ;
		this.isWYSIWYG = true ;
	};
	
	Instance.prototype.setWYSIWYG = function(bln) {
		this.isWYSIWYG = bln ;
	};
	
	Instance.prototype.getWin = function() {
		return this.win;
	};
	
	Instance.prototype.getDoc = function() {
		return this.win.document;
	};
	
	Instance.prototype.setContent = function(html) {
		this.getDoc().body.innerHTML = html ;
	};
	
	Instance.prototype.getContent = function() {
		return this.getDoc().body.innerHTML ;
	};
	
	Instance.prototype.writeContent = function(html) {
		try {
			var doc = this.getDoc() ;
			doc.open() ;
			doc.write(html) ;
			doc.close() ;
			doc.designMode = 'on' ;
			doc.execCommand("useCSS", false, true);
		} catch (ex) {}
	};
	
	Instance.prototype.execCommand = function(command, user_interface, value) {
	};
	
	function MyToolbar() {
		this.buttonstest = new Object() ;
		this.buttonstest["Default"] = [
			['Source','Bold','Italic','Underline','JustifyLeft','JustifyCenter','JustifyRight','JustifyFull','Undo','Redo','InsertHorizontalRule','SubScript','SuperScript','Indent','Outdent'],
			['FormatBlock','FontSize','Unlink','RemoveFormat','InsertUnorderedList','InsertOrderedList','Cut','Copy','Paste','StrikeThrough']
		] ;
		
		this.buttonstest["Basic"] = [
			['Source','Bold','Italic','Underline','JustifyLeft','JustifyCenter','JustifyRight','JustifyFull','Undo','InsertHorizontalRule','SubScript','SuperScript','FormatBlock','FontSize']
		] ;
	};
	
	MyToolbar.prototype.create = function(instance, mode) {
		var tBarMode = this.buttonstest[mode] ;
		var tBar = document.createElement('DIV') ;
		with(tBar) {
			id = instance + '_Toolbar' ;
			className = 'MyEditorToolbar' ;
		}
		
		var str = '' ;
		for(var i = 0; i < tBarMode.length; i++) {
			var bars = tBarMode[i] ;
			for(var j = 0; j < bars.length; j++) {
				str += this.getButton(instance, bars[j]) ;
			}
			str += '<br />' ;
		}
		tBar.innerHTML += str ;
		return tBar ;
	};
	
	MyToolbar.prototype.getButton = function(instance, btName) {
		switch (btName) {
			case "FormatBlock":
				return '<select name="formatblock" onchange="eXo.ecm.ExoEditor.execCommand(\'' + instance + '\',\'FormatBlock\',false,this.options[this.selectedIndex].value);" class="MySelectList">' +
							'<option value="">-- Format --</option>' +
							'<option value="&lt;p&gt;">Paragraph</option>' +
							'<option value="&lt;address&gt;">Address</option>' +
							'<option value="&lt;pre&gt;">Preformatted</option>' +
							'<option value="&lt;h1&gt;">Heading 1</option>' +
							'<option value="&lt;h2&gt;">Heading 2</option>' +
							'<option value="&lt;h3&gt;">Heading 3</option>' +
							'<option value="&lt;h4&gt;">Heading 4</option>' +
							'<option value="&lt;h5&gt;">Heading 5</option>' +
							'<option value="&lt;h6&gt;">Heading 6</option>' +
							'</select>';
			case "FontSize":
				return '<select name="fontsize" onchange="eXo.ecm.ExoEditor.execCommand(\'' + instance + '\',\'FontSize\',false,this.options[this.selectedIndex].value);" class="MySelectList">' +
							'<option value="0">-- Font Size --</option>' +
							'<option value="1">1 (8 pt)</option>' +
							'<option value="2">2 (10 pt)</option>' +
							'<option value="3">3 (12 pt)</option>' +
							'<option value="4">4 (14 pt)</option>' +
							'<option value="5">5 (18 pt)</option>' +
							'<option value="6">6 (24 pt)</option>' +
							'<option value="7">7 (36 pt)</option>' +
							'</select>';
		}
		return '<a href="#" class="MyButtonNormal" onclick="return eXo.ecm.ExoEditor.execCommand(\'' + instance + '\',\'' + btName + '\');"><img alt="' + btName + '" title="' + btName + '" src="/ecm/skin/ExoEditor/images/toolbar/' + btName + '.gif" /></a>' ;
	};
	var myToolbar = new MyToolbar() ;
	
	function ExoEditor() {
		this.myConfig = {
			width : "400px",
			height : "200px",
			mode : "Default"
		}
		
		var ua = navigator.userAgent ;
		this.isIE = (navigator.appName == "Microsoft Internet Explorer") ;
		this.isGecko = ua.indexOf('Gecko') != -1 ;
		this.instances = new Object() ;
	};
	
	ExoEditor.prototype.init = function(instId, settings) {
		this.createInstance(instId, settings) ;
	};
	
	ExoEditor.prototype.createInstance = function(instId, settings) {
		var txtAreaObj = document.getElementById(instId) ;
		with(txtAreaObj) {
			style.height = settings["height"] || this.myConfig.height ;
		}
	
		var editorObj = document.createElement("DIV") ;
		with(editorObj) {
			className = "MyEditor" ;
			style.width = settings["width"] || this.myConfig.width ;
		}
		editorObj.appendChild(myToolbar.create(instId, settings["mode"] || this.myConfig.mode)) ;
		var ifrm = document.createElement("IFRAME") ;
		with(ifrm) {
			src = 'javascript:void(0)' ;
			width = "100%" ;
			style.height = settings["height"] || this.myConfig.height ;
			frameBorder = 0 ;
			scrolling = "auto" ;
		}
		ifrm.onmouseover = this.onMouseOverEvt ;
		editorObj.appendChild(ifrm) ;
		
		txtAreaObj.style.display = 'none' ;
		txtAreaObj.parentNode.appendChild(editorObj) ;
		editorObj.appendChild(txtAreaObj) ;
		txtAreaObj.style.width = "100%" ;
	
		var inst = new Instance(ifrm) ;
		inst.instId = instId ;
		this.instances[instId] = inst ;
		inst.writeContent(txtAreaObj.value) ;
		this.setEventHandlers(inst) ;
	};
	
	ExoEditor.prototype.onMouseOverEvt = function() {
		try {
			this.contentDocument.designMode = 'on';
		} catch (ex) {}
	}
	 
	ExoEditor.prototype.execCommand = function(instId, cmd, user, value) {
		if(!user) user = false;
		if(!value) value = null ;
		switch(cmd) {
			case "Source":
				this.switchMode(instId) ;
				return false ;
		}
		var doc = this.instances[instId].getDoc() ;
		doc.execCommand(cmd, user, value) ;
		return false;
	};
	
	ExoEditor.prototype.setEventHandlers = function(inst) {
		var doc = inst.getDoc(), evts, i, f = this.addEvent;
	
		if (this.isIE) {
			evts = ['keypress', 'keyup', 'keydown', 'click', 'mouseup', 'mousedown', 'controlselect', 'dblclick'];
			for (i=0; i<evts.length; i++)
				f(doc, evts[i], this.handleEventPatch);
		} else {
			evts = ['keypress', 'keyup', 'keydown', 'click', 'mouseup', 'mousedown', 'dragdrop', 'focus', 'blur'];
			for (i=0; i<evts.length; i++)
				f(doc, evts[i], this.handleEvent);
		}
		if(document.forms && !this.submitTrigger) {
			for(i = 0; i<document.forms.length; i++) {
				var form = document.forms[i];
	
				this.addEvent(form, "submit", this.saveHandler);
				this.submitTrigger = true; // Do it only once
			}
		}
	};
	
	ExoEditor.prototype.handleEventPatch = function() {
		var n, inst, win, e;
		if (eXo.ecm.ExoEditor.selectedInst) {
			win = eXo.ecm.ExoEditor.selectedInst.getWin();
	
			if (win && win.event) {
				e = win.event;
	
				if (!e.target)
					e.target = e.srcElement;
	
				eXo.ecm.ExoEditor.handleEvent(e);
				return;
			}
		}
		
		for (n in eXo.ecm.ExoEditor.instances) {
			inst = eXo.ecm.ExoEditor.instances[n];
			win = inst.getWin();
	
			if (win && win.event) {
				e = win.event;
				eXo.ecm.ExoEditor.selectedInst = inst ;
				if (!e.target)
					e.target = e.srcElement;
	
				eXo.ecm.ExoEditor.handleEvent(e);
				return;
			}
		}
	};
	
	ExoEditor.prototype.handleEvent = function(e) {
		window.status = eXo.ecm.ExoEditor.selectedInst;
		switch (e.type) {
			case "blur":
				window.status += " : blur" ;
				return;
			case "drop":
				window.status += " : drop" ;
				return;
			case "beforepaste":
				window.status += " : beforepaste" ;
				return;
			case "submit":
				window.status += " : submit" ;
				return;
			case "reset":
				return;
			case "keypress":
				window.status += " : keypress" ;
				return;
			case "keyup":
				window.status += " : keyup" ;
				return;
			case "keydown":
				window.status += " : keydown" ;
				return;
			case "mousedown":
				window.status += " : mousedown" ;
				return;
			case "mouseup":
				window.status += " : mouseup" ;
				return;
			case "click":
				window.status += " : click" ;
				return;
			case "dblclick":
				window.status += " : dblclick" ;
				return;
			case "focus":
				window.status += " : focus" ;
				return;
		}
	};
	
	ExoEditor.prototype.saveHandler = function() {
		for(var i in eXo.ecm.ExoEditor.instances) {
			try{eXo.ecm.ExoEditor.save(i) ;}catch(e) {}
			delete eXo.ecm.ExoEditor.instances[i];
		}
		delete eXo.ecm.ExoEditor.selectedInst;
	};
	
	ExoEditor.prototype.save = function(instId) {
		inst = this.instances[instId] ;
		if(inst.getDoc() == null || !inst.isWYSIWYG) return;
		var txtArea = document.getElementById(inst.instId) ;
		if(inst.getContent() == "<br>") txtArea.value = "" ; 
		else txtArea.value = inst.getContent() ;
	};
	
	ExoEditor.prototype.switchMode = function(instId) {
		var inst = this.instances[instId] ;
		var ifrm = inst.getWin().frameElement ;
		var textArea = document.getElementById(instId) ;
		if(inst.isWYSIWYG) {
			textArea.value = inst.getContent() ;
			ifrm.style.display = 'none' ;
			textArea.style.display = 'block' ;
		} else {
			inst.setContent(textArea.value) ;
			textArea.style.display = 'none' ;
			ifrm.style.display = 'block' ;
		}
		inst.isWYSIWYG = !inst.isWYSIWYG;
	};
	
	ExoEditor.prototype.removeInstance = function(instId) {
		var inst = tinyMCE.getInstanceById(editor_id), h, re, ot, tn;
	
		if (inst) {
			inst.switchSettings();
	
			editor_id = inst.editorId;
			h = tinyMCE.getContent(editor_id);
	
			this.removeInstance(inst);
	
			tinyMCE.selectedElement = null;
			tinyMCE.selectedInstance = null;
	
			// Remove element
			re = document.getElementById(editor_id + "_parent");
			ot = inst.oldTargetElement;
			tn = ot.nodeName.toLowerCase();
	
			if (tn == "textarea" || tn == "input") {
				re.parentNode.removeChild(re);
				ot.style.display = "inline";
				ot.value = h;
			} else {
				ot.innerHTML = h;
				ot.style.display = 'block';
				re.parentNode.insertBefore(ot, re);
				re.parentNode.removeChild(re);
			}
		}
	};
	
	ExoEditor.prototype.encodeHTML = function(text) {
		if ( typeof( text ) != "string" )
			text = text.toString() ;
	
		text = text.replace(/&/g, "&amp;").replace(/"/g, "&quot;")
							 .replace(/</g, "&lt;").replace(/>/g, "&gt;") ;
	
		return text ;
	};
	
	ExoEditor.prototype.insertHtmlBefore = function(html, element) {
		if ( element.insertAdjacentHTML )	// IE
			element.insertAdjacentHTML( 'beforeBegin', html ) ;
		else {														// Gecko
			var oRange = document.createRange() ;
			oRange.setStartBefore( element ) ;
			var oFragment = oRange.createContextualFragment( html );
			element.parentNode.insertBefore( oFragment, element ) ;
		}
	};
	
	ExoEditor.prototype.addEvent = function(o, n, h) {
		if (o.attachEvent) o.attachEvent("on" + n, h);
		else o.addEventListener(n, h, false);
	};
	
	ExoEditor.prototype.removeEvent = function(o, n, h) {
		if (o.detachEvent) o.detachEvent("on" + n, h);
		else o.removeEventListener(n, h, false);
	};
	
	ExoEditor.prototype.cancelEvent = function(e) {
		if (!e)	return false;
		if (this.isIE) {
			e.returnValue = false;
			e.cancelBubble = true;
		} else {
			e.preventDefault();
			e.stopPropagation && e.stopPropagation();
		}
	
		return false;
	};
	
	eXo.ecm.ExoEditor = new ExoEditor() ;
	
	function DMSBrowser(){
	}
	
	DMSBrowser.prototype.managerResize = function() {
		if(eXo.core.Browser.currheight != document.documentElement.clientHeight || 
		  eXo.core.Browser.currwidth != document.documentElement.clientWidth) {
	 		clearTimeout(eXo.core.Browser.breakStream) ;
	 		eXo.core.Browser.breakStream = setTimeout(eXo.core.Browser.onResize, 100) ;
	 	}
	 	eXo.core.Browser.currheight = document.documentElement.clientHeight;
	 	eXo.core.Browser.currwidth = document.documentElement.clientWidth;
	}
	
	DMSBrowser.prototype.findPosX = function(obj, isRTL) {
	  var curleft = 0;
	  var tmpObj = obj ;
	  while (tmpObj) {
	    curleft += tmpObj.offsetLeft ;
	    tmpObj = tmpObj.offsetParent ;
	  }
	  // if RTL return right position of obj
	  if(isRTL) return curleft + obj.offsetWidth ;
	  return curleft ;
	} ;
	
	DMSBrowser.prototype.findMouseRelativeX = function(object, e) {
	  var posx = -1 ;
	  var posXObject = eXo.ecm.DMSBrowser.findPosX(object) ;
	  if (!e) e = window.event ;
	  if (e.pageX || e.pageY) {
	    posx = e.pageX - posXObject ;
	  } else if (e.clientX || e.clientY) {
	    posx = e.clientX + document.body.scrollLeft - posXObject ;
	  }
	  return posx ;
	} ;
	
	eXo.ecm.DMSBrowser = new DMSBrowser();
	window.onresize = eXo.ecm.DMSBrowser.managerResize;
	
	return {
		ExoEditor : eXo.ecm.ExoEditor,
		ECMUtils : eXo.ecm.ECMUtils,
		DMSBrowser : eXo.ecm.DMSBrowser
	};
})(gj, ecm_bootstrap, webuiExt);