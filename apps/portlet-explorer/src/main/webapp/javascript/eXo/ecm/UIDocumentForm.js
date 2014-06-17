(function(gj, ecm_utils) {
	var tblSavedStyles;
	
	var UIDocumentForm = function() {
		this.Name = 'UIDocumentForm';
	}
	
	UIDocumentForm.prototype.AdjustHeight = function() {
		var workingArea = document.getElementById('UIWorkingArea');
		var uiWorkingWorkspace = document.getElementById('UIWorkingWorkspace');
		var uiDocumentWorkspace = document.getElementById('UIDocumentWorkspace');
		var uiDocumentForm = document.getElementById("UIDocumentForm");
		var uiAction = gj(uiDocumentForm).find("div.UIAction:first")[0];
		var uiHorizontalTabs = gj(uiDocumentForm).find("div.UIHorizontalTabs:first")[0];
		var horizontalForm = gj(uiDocumentForm).find("div.form-horizontal:first")[0];

		if (uiWorkingWorkspace.clientWidth != uiDocumentWorkspace.clientWidth) {
			
			var workingAreaHeight = workingArea.offsetHeight;
			if (uiDocumentWorkspace)									
			 	uiDocumentWorkspace.style.height = workingAreaHeight + 2 + 'px';
	
			var uiActionHeight = 0;
			var uiHorizontalTabsHeight = 0;
			horizontalForm.style.height = 'auto';
			if (uiAction) {
				uiActionHeight = uiAction.offsetHeight;
			}
		
			if (uiHorizontalTabs) {
				uiHorizontalTabsHeight = uiHorizontalTabs.offsetHeight;
			}
			horizontalForm.style.height = workingAreaHeight - uiActionHeight - uiHorizontalTabsHeight - 10 + 'px';
		}
	}
	
	UIDocumentForm.prototype.UpdateGUI = function () {
		var uiWorkingWorkspace = document.getElementById('UIWorkingWorkspace');
		var uiDocumentWorkspace = document.getElementById('UIDocumentWorkspace');
		var uiDocumentForm = document.getElementById("UIDocumentForm");
		var uiAction = gj(uiDocumentForm).find("div.UIAction:first")[0];
		var fullscreenDiv = gj(uiAction).find("a.MaximizeScreen20x20Icon:first")[0];
		var changeTypeLink = gj(uiAction).find("a.changeTypeLink:first")[0];
	
		if (!fullscreenDiv) {
			fullscreenDiv = gj(uiAction).find("a.MinimizeScreen20x20Icon:first")[0];
		}
		
		if (uiWorkingWorkspace.clientWidth != uiDocumentWorkspace.clientWidth) {
			if (fullscreenDiv) {
				fullscreenDiv.className = "MaximizeScreen20x20Icon";
			}
			if (changeTypeLink) {
				changeTypeLink.style.display = "inline-block";
			}
		} else {
			if (fullscreenDiv) {
				fullscreenDiv.className = "MinimizeScreen20x20Icon";
			}
			if (changeTypeLink) {
				changeTypeLink.style.display = "none";
			}
		}
	}
	
	UIDocumentForm.prototype.FullScreenToggle = function(element) {
		var uiWorkingWorkspace = document.getElementById('UIWorkingWorkspace');
		var uiDocumentWorkspace = document.getElementById('UIDocumentWorkspace');
		var uiDocumentForm = document.getElementById("UIDocumentForm");
		var uiAction = gj(uiDocumentForm).find("div.UIAction:first")[0];
		var changeTypeLink = gj(uiAction).find("a.changeTypeLink:first")[0];	

		if (!eXo.webui.UIDocForm.horizontalForm) {	
			eXo.webui.UIDocForm.horizontalForm = gj(uiDocumentForm).find("div.form-horizontal:first")[0];
		}

		element = gj(element).find("i")[0];
		
		if (element.className == "uiIconEcmsExpand uiIconEcmsLightGray") {
			element.className = "uiIconEcmsCollapse uiIconEcmsLightGray";
			eXo.ecm.ECMUtils.editFullScreen = true;
		} else {
			element.className = "uiIconEcmsExpand uiIconEcmsLightGray";
			eXo.ecm.ECMUtils.editFullScreen = false;
		}
		
		if (element.className == "uiIconEcmsCollapse uiIconEcmsLightGray") {
			if (changeTypeLink) {
				changeTypeLink.style.display = "none";
			}
			
			//save style		
			eXo.webui.UIDocForm.SaveStyles("UIDocumentWorkspace", uiDocumentWorkspace);
			eXo.webui.UIDocForm.SaveStyles("form-horizontal", eXo.webui.UIDocForm.horizontalForm);

			// Resize.
			var oViewPaneSize = UIDocumentForm.GetViewPaneSize(uiWorkingWorkspace) ;
	
			uiDocumentWorkspace.style.position	= "absolute";
			uiDocumentWorkspace.style.offsetLeft ;		
			uiDocumentWorkspace.style.zIndex	= uiDocumentWorkspace.style.zIndex + 10;
			uiDocumentWorkspace.style.left		= "0px";
			uiDocumentWorkspace.style.top		= "0px";
			uiDocumentWorkspace.style.width		= oViewPaneSize.Width + "px";
			uiDocumentWorkspace.style.height	= oViewPaneSize.Height + "px";
			uiDocumentWorkspace.style.background = '#FFFFFF';

			gj(eXo.webui.UIDocForm.horizontalForm).height('auto');
			gj(eXo.webui.UIDocForm.horizontalForm).addClass("form-horizontal");
			gj(eXo.webui.UIDocForm.horizontalForm).addClass("uiContentBox");
			window.scrollTo(0, 0)
		} else {
			if (changeTypeLink) {
				changeTypeLink.style.display = "inline-block";
			}
			// Restore original size
			eXo.webui.UIDocForm.RestoreStyles("UIDocumentWorkspace", uiDocumentWorkspace) ;
			eXo.webui.UIDocForm.RestoreStyles("form-horizontal", eXo.webui.UIDocForm.horizontalForm);
	        gj(eXo.webui.UIDocForm.horizontalForm).addClass("form-horizontal");
			gj(eXo.webui.UIDocForm.horizontalForm).addClass("uiContentBox");
			delete eXo.webui.UIDocForm.horizontalForm;
		}
		eXo.webui.UIDocForm.AutoFocus();
	}
	
	UIDocumentForm.prototype.GetStyleData = function( element ) {
		var objStyleData = new Object() ;
	
		if (!element) return "";
		if ( element.className.length > 0 )	{
			objStyleData.Class = element.className ;
			element.className = '' ;
		}
		var sInlineStyle = element.style.cssText ;
		if ( sInlineStyle  ) {
			objStyleData.Inline = sInlineStyle ;
			element.setAttribute( 'style', '', 0 ) ;	// 0 : Case Insensitive
		}
		return objStyleData ;
	}
	
	UIDocumentForm.prototype.SetStyleData = function( element, objStyleData )
	{
		element.className = objStyleData.Class || '' ;
	
		if ( objStyleData.Inline )
			element.style.cssText = objStyleData.Inline;
		else
			element.removeAttribute('style', 0);
	}
	
	UIDocumentForm.prototype.SaveStyles = function( key, element ) {
		if (!element) return;
		var styleData = this.GetStyleData(element);
		if(gj.hasData(element)) gj.removeData(element, key);
	        gj.data(element, key, styleData);
	}
	
	UIDocumentForm.prototype.RestoreStyles = function( key, element ) {
		if (!element) return;
		if (!gj.hasData(element)) {
			return;
		}
		var styleData = gj.data(element, key);
		eXo.webui.UIDocForm.SetStyleData(element, styleData);
	}
	
	// Returns and object with the "Width" and "Height" properties.
	UIDocumentForm.GetViewPaneSize = function( win )
	{
		return { Width : win.clientWidth, Height : win.clientHeight } ;
	}
	
	
	//focus to the first element in the form after loading successfuly
	UIDocumentForm.prototype.AutoFocus = function() {
	  var uiDocumentForm = document.getElementById("UIDocumentForm");
	  for (var i = 0; uiDocumentForm.elements[i] && (uiDocumentForm.elements[i].type == 'hidden'); i++);
	  setTimeout(function() {
	    try {
	      uiDocumentForm.elements[i].focus();
	    } catch(err){}
	  }, 200);
	};
	UIDocumentForm.prototype.initFullScreenStatus = function(elementId) {
		var aElement = document.getElementById(elementId);
		if (!aElement ) return;
		if (eXo.ecm.ECMUtils.editFullScreen) {
			this.FullScreenToggle(aElement.parentNode);
		}
	}
	eXo.webui.UIDocForm = new UIDocumentForm();
	return {
		UIDocForm : eXo.webui.UIDocForm
	};
})(gj, ecm_utils);


