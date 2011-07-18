var UIDocumentForm = function() {
	this.Name = 'UIDocumentForm';
	this.SavedStyle = "";
	
}

UIDocumentForm.prototype.AdjustHeight = function() {
	var uiDocumentWorkspace = document.getElementById('UIDocumentWorkspace');
	var uiDocumentForm = document.getElementById("UIDocumentForm");
	var uiAction = eXo.core.DOMUtil.findFirstDescendantByClass(uiDocumentForm, "div", "UIAction");
	var uiHorizontalTabs = eXo.core.DOMUtil.findFirstDescendantByClass(uiDocumentForm, "div", "UIHorizontalTabs");
	var horizontalLayout = eXo.core.DOMUtil.findFirstDescendantByClass(uiDocumentForm, "div", "HorizontalLayout");
	
	var uiActionHeight = 0;
	var uiHorizontalTabsHeight = 0;
	horizontalLayout.style.height = 'auto';
	if (uiAction) {
		uiActionHeight = uiAction.offsetHeight;
	}
	
	if (uiHorizontalTabs) {
		uiHorizontalTabsHeight = uiHorizontalTabs.offsetHeight;
	}
	
	horizontalLayout.style.height = uiDocumentWorkspace.offsetHeight - uiActionHeight - uiHorizontalTabsHeight - 60 + 'px';
}

UIDocumentForm.prototype.UpdateGUI = function () {
	var uiWorkingWorkspace = document.getElementById('UIWorkingWorkspace');
	var uiDocumentWorkspace = document.getElementById('UIDocumentWorkspace');
	var uiDocumentForm = document.getElementById("UIDocumentForm");
	var uiAction = eXo.core.DOMUtil.findFirstDescendantByClass(uiDocumentForm, "div", "UIAction");
	var fullscreenDiv = eXo.core.DOMUtil.findFirstDescendantByClass(uiAction, "div", "MaximizeScreen20x20Icon");
	var changeTypeLink = eXo.core.DOMUtil.findFirstDescendantByClass(uiAction, "a", "ChangeTypeLink");

	if (!fullscreenDiv) {
		fullscreenDiv = eXo.core.DOMUtil.findFirstDescendantByClass(uiAction, "div", "MinimizeScreen20x20Icon");
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
	var uiAction = eXo.core.DOMUtil.findAncestorByClass(element, "UIAction");
	var changeTypeLink = eXo.core.DOMUtil.findFirstDescendantByClass(uiAction, "a", "ChangeTypeLink");
	
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
		this.SavedStyle = UIDocumentForm.SaveStyles(uiDocumentWorkspace);
		
		// Resize.
		var oViewPaneSize = UIDocumentForm.GetViewPaneSize(uiWorkingWorkspace) ;

		uiDocumentWorkspace.style.position	= "absolute";
		uiDocumentWorkspace.style.offsetLeft ;		
		uiDocumentWorkspace.style.zIndex	= uiDocumentWorkspace.style.zIndex + 3;
		uiDocumentWorkspace.style.left		= "0px";
		uiDocumentWorkspace.style.top		= "0px";
		uiDocumentWorkspace.style.width		= oViewPaneSize.Width + "px";
		uiDocumentWorkspace.style.height	= oViewPaneSize.Height + "px";
		uiDocumentWorkspace.style.background = '#FFFFFF';
		
		window.scrollTo(0, 0)
	} else {
		if (changeTypeLink) {
			changeTypeLink.style.display = "inline-block";
		}
		// Restore original size
		UIDocumentForm.RestoreStyles( uiDocumentWorkspace , this.SavedStyle ) ;
	}
	eXo.webui.UIDocForm.AdjustHeight();
	eXo.webui.UIDocForm.AutoFocus();
}

UIDocumentForm.SaveStyles = function( element ) {
	var oSavedStyles = new Object() ;

	if ( element.className.length > 0 )	{
		oSavedStyles.Class = element.className ;
		element.className = '' ;
	}

	var sInlineStyle = element.getAttribute( 'style' ) ;

	if ( sInlineStyle && sInlineStyle.length > 0 ) {
		oSavedStyles.Inline = sInlineStyle ;
		element.setAttribute( 'style', '', 0 ) ;	// 0 : Case Insensitive
	}

	return oSavedStyles ;
}

UIDocumentForm.RestoreStyles = function( element, savedStyles )
{
	element.className = savedStyles.Class || '' ;

	if ( savedStyles.Inline )
		element.setAttribute('style', savedStyles.Inline, 0);	// 0 : Case Insensitive
	else
		element.removeAttribute('style', 0);
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