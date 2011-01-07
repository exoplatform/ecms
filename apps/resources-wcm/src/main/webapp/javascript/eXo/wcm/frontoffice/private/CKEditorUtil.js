function CKEditor() {
}

CKEditor.prototype.insertCSS = function(Instance, ContentCSS) {
	if (!Instance) return;
	var eContentCSS = document.getElementById(ContentCSS);
	var sContentCSSId = ContentCSS + "_Inline";
	var count = 1;
	eContentCSS.onblur = updateStyle;
	
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
		if (eXo.core.Browser.isFF()) { //for FF			
			eStyle.innerHTML = sValue;
		} else {
			eStyle.styleSheet.cssText = sValue;
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

CKEditor.prototype.getGadgetToken = function(url, metadata) {
	var tokenURL = eXo.ecm.WCMUtils.getHostName() + eXo.ecm.WCMUtils.getRestContext() + "/wcmGadget/getToken?url=" + url;
	var token = eXo.ecm.WCMUtils.request(tokenURL).getElementsByTagName("token")[0].getAttribute("value");
	metadata.gadgets[0].secureToken = token;
	return metadata;
}

eXo.ecm.CKEditor = new CKEditor();