(function(gj, base) {
	var CoverFlow = function() {
		var Self = this;
		var Browser = eXo.core.Browser;
		CoverFlow.prototype.portletId = null;
		
		CoverFlow.prototype.initEvent = function(portletId) {
			Self.portletId = portletId;
			var portlet = document.getElementById(portletId);
	//		var album = DOM.getChildrenByTagName(portlet, "textarea")[0];
			var album = gj("#" + portletId).children("textarea")[0];
			var iframe = document.createElement("iframe");
			iframe.setAttribute("frameborder", "0");
			iframe.setAttribute("border", "0");
			iframe.style.border = "none";
			var workingArea = gj(portlet).parents(".UIWorkingArea:first")[0];
							
			iframe.style.width = "98%";
			iframe.style.height = workingArea.offsetHeight - 47 + "px";
			iframe.style.margin = "auto";
			portlet.insertBefore(iframe, album);
			var idoc = iframe.contentWindow.document;  
			  idoc.open();
			  idoc.write(album.value);
			  setTimeout(function() {idoc.close()}, 1000);
		};
		
		CoverFlow.prototype.errorCallbackImage = function(obj){
		  var img = gj(obj.parentNode).nextAll("image:first")[0];
		  img.style.display = "block";
		  obj.style.display = "none";
		};
		
		CoverFlow.prototype.setHeight = function() {
			 // lampt's update
			var root = document.getElementById("UIDocumentInfo");
			var workingArea = document.getElementById('UIWorkingArea');			
		 	var documentWorkspace = gj(workingArea).find("div.UIDocumentWorkspace:first")[0];
			var sizeBarContainer = gj(workingArea).find("div.UISideBarContainer:first")[0];
			var resizeSideBar = gj(workingArea).find("div.ResizeSideBar:first")[0];
			var actionBar = document.getElementById('UIActionBar');	
			var actionBaroffsetHeight = 0;
			if(actionBar)
			  actionBaroffsetHeight = actionBar.offsetHeight;
			var page = gj(root).find("div.PageAvailable:first")[0];	
			var view = gj(root).find("div.MCBox:first")[0];	
			var workingAreaHeight = workingArea.offsetHeight;
			sizeBarContainer.style.height = workingAreaHeight + 'px';	
			resizeSideBar.style.height = workingAreaHeight + 'px';	
			if (documentWorkspace)		{		
		 		documentWorkspace.style.height = (workingAreaHeight - actionBaroffsetHeight) + 'px';	
			}	
			if (page) {
				if (parseInt(page.getAttribute('pageAvailable')) > 1) {
	
					if (view) view.style.height = workingAreaHeight - actionBaroffsetHeight - page.offsetHeight + 'px';
				}
			} else {
			  	if (view) view.style.height = workingAreaHeight - actionBaroffsetHeight + 'px';
			}				
	 }
		
	};
	
	eXo.ecm.UICoverFlow = new CoverFlow();
	window.onerror = function() {return false;}
	return {
		UICoverFlow : eXo.ecm.UICoverFlow
	};
})(gj, base);