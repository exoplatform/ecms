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
