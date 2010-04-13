function WCMFCKEditor (){
}

WCMFCKEditor.prototype.insertCSSFromTextArea2FCK = function(Instance, ContentCSS) {
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
};

eXo.ecm.WCMFCKEditor = new WCMFCKEditor();