var tblSavedStyles;

var UIDocumentForm = function() {
	this.Name = 'UIDocumentForm';
}

UIDocumentForm.prototype.AdjustHeight = function() {
	var workingArea = document.getElementById('UIWorkingArea');
	var uiWorkingWorkspace = document.getElementById('UIWorkingWorkspace');
	var uiDocumentWorkspace = document.getElementById('UIDocumentWorkspace');
	var uiDocumentForm = document.getElementById("UIDocumentForm");
	var uiAction = eXo.core.DOMUtil.findFirstDescendantByClass(uiDocumentForm, "div", "UIAction");
	var uiHorizontalTabs = eXo.core.DOMUtil.findFirstDescendantByClass(uiDocumentForm, "div", "UIHorizontalTabs");
	var horizontalLayout = eXo.core.DOMUtil.findFirstDescendantByClass(uiDocumentForm, "div", "HorizontalLayout");

	if (uiWorkingWorkspace.clientWidth != uiDocumentWorkspace.clientWidth) {
		
		var workingAreaHeight = workingArea.offsetHeight;
		if (uiDocumentWorkspace)									
		 	uiDocumentWorkspace.style.height = workingAreaHeight + 2 + 'px';

		var uiActionHeight = 0;
		var uiHorizontalTabsHeight = 0;
		horizontalLayout.style.height = 'auto';
		if (uiAction) {
			uiActionHeight = uiAction.offsetHeight;
		}
	
		if (uiHorizontalTabs) {
			uiHorizontalTabsHeight = uiHorizontalTabs.offsetHeight;
		}
	
		horizontalLayout.style.height = workingAreaHeight - uiActionHeight - uiHorizontalTabsHeight - 10 + 'px';
	}
}

UIDocumentForm.prototype.UpdateGUI = function () {
	var uiWorkingWorkspace = document.getElementById('UIWorkingWorkspace');
	var uiDocumentWorkspace = document.getElementById('UIDocumentWorkspace');
	var uiDocumentForm = document.getElementById("UIDocumentForm");
	var uiAction = eXo.core.DOMUtil.findFirstDescendantByClass(uiDocumentForm, "div", "UIAction");
	var fullscreenDiv = eXo.core.DOMUtil.findFirstDescendantByClass(uiAction, "a", "MaximizeScreen20x20Icon");
	var changeTypeLink = eXo.core.DOMUtil.findFirstDescendantByClass(uiAction, "a", "ChangeTypeLink");

	if (!fullscreenDiv) {
		fullscreenDiv = eXo.core.DOMUtil.findFirstDescendantByClass(uiAction, "a", "MinimizeScreen20x20Icon");
	}
	
	if (uiWorkingWorkspace.clientWidth != uiDocumentWorkspace.clientWidth) {
		fullscreenDiv.className = "MaximizeScreen20x20Icon";
		if (changeTypeLink) {
			changeTypeLink.style.display = "inline-block";
		}
	} else {
		fullscreenDiv.className = "MinimizeScreen20x20Icon";
		if (changeTypeLink) {
			changeTypeLink.style.display = "none";
		}
	}
}

UIDocumentForm.prototype.FullScreenToggle = function(element) {
	var uiWorkingWorkspace = document.getElementById('UIWorkingWorkspace');
	var uiDocumentWorkspace = document.getElementById('UIDocumentWorkspace');
	var uiDocumentForm = document.getElementById("UIDocumentForm");
	var uiAction = eXo.core.DOMUtil.findFirstDescendantByClass(uiDocumentForm, "div", "UIAction");
	var changeTypeLink = eXo.core.DOMUtil.findFirstDescendantByClass(uiAction, "a", "ChangeTypeLink");	
	
	if (!eXo.webui.UIDocForm.horizontalLayout) {	
		eXo.webui.UIDocForm.horizontalLayout = eXo.core.DOMUtil.findFirstDescendantByClass(uiDocumentForm, "div", "HorizontalLayout");
	}

	if (element.className == "MaximizeScreen20x20Icon") {
		element.className = "MinimizeScreen20x20Icon";
	} else {
		element.className = "MaximizeScreen20x20Icon";
	}
	
	if (uiWorkingWorkspace.clientWidth != uiDocumentWorkspace.clientWidth) {
		if (changeTypeLink) {
			changeTypeLink.style.display = "none";
		}
		
		//save style		
		UIDocumentForm.SaveStyles("UIDocumentWorkspace", uiDocumentWorkspace);
		UIDocumentForm.SaveStyles("HorizontalLayout", eXo.webui.UIDocForm.horizontalLayout);

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

		eXo.webui.UIDocForm.horizontalLayout.style.height = 'auto';
		
		window.scrollTo(0, 0)
	} else {
		if (changeTypeLink) {
			changeTypeLink.style.display = "inline-block";
		}
		// Restore original size
		UIDocumentForm.RestoreStyles("UIDocumentWorkspace", uiDocumentWorkspace) ;
		UIDocumentForm.RestoreStyles("HorizontalLayout", eXo.webui.UIDocForm.horizontalLayout);

		delete eXo.webui.UIDocForm.horizontalLayout;
	}
	eXo.webui.UIDocForm.AutoFocus();
}

UIDocumentForm.GetStyleData = function( element ) {
	var objStyleData = new Object() ;

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

UIDocumentForm.SetStyleData = function( element, objStyleData )
{
	element.className = objStyleData.Class || '' ;

	if ( objStyleData.Inline )
		element.style.cssText = objStyleData.Inline;
	else
		element.removeAttribute('style', 0);
}

UIDocumentForm.SaveStyles = function( key, element ) {
	var styleData = this.GetStyleData(element);
	if (!tblSavedStyles) {
		tblSavedStyles = new eXo.core.HashMap();
	}
	if (tblSavedStyles.get(key)) {
		tblSavedStyles.remove(key);
	}
	tblSavedStyles.put(key, styleData);
}

UIDocumentForm.RestoreStyles = function( key, element ) {
	if (!tblSavedStyles.get(key)) {
		return;
	}
	var styleData = tblSavedStyles.get(key);
	UIDocumentForm.SetStyleData(element, styleData);
}

// Returns and object with the "Width" and "Height" properties.
UIDocumentForm.GetViewPaneSize = function( win )
{
	return { Width : win.clientWidth, Height : win.clientHeight } ;
}


//focus to the first element in the form after loading successfuly
UIDocumentForm.prototype.AutoFocus = function() {
  var uiDocumentForm = document.getElementById("UIDocumentForm");
  for (var i = 0; uiDocumentForm.elements[i].type == 'hidden'; i++);
		  
  setTimeout(function() {
    try {
      uiDocumentForm.elements[i].focus();
    } catch(err){}
  }, 200);
};

eXo.webui.UIDocForm = new UIDocumentForm();