function ContentListViewer() {}

ContentListViewer.prototype.initCheckedRadio = function(id) {
	eXo.core.Browser.chkRadioId = id;
};

ContentListViewer.prototype.initCondition = function(formid) {
	var formElement = document.getElementById(formid);
	var radioboxes = [];
	for(var i=0; i < formElement.elements.length;i++){
		if(formElement.elements[i].type=="radio") radioboxes.push(formElement.elements[i]);
	}
	var i = radioboxes.length;
	while(i--){
		radioboxes[i].onclick = eXo.ecm.CLV.chooseCondition;
	}
	if(eXo.core.Browser.chkRadioId && eXo.core.Browser.chkRadioId != "null"){
		var selectedRadio = document.getElementById(eXo.core.Browser.chkRadioId);
	} else{		
		var selectedRadio = radioboxes[0];
	}
	var itemSelectedContainer = eXo.core.DOMUtil.findAncestorByClass(selectedRadio,"ContentSearchForm");
	var itemContainers = eXo.core.DOMUtil.findDescendantsByClass(selectedRadio.form, "div", "ContentSearchForm");
	for(var i = 0 ;i < itemContainers.length; i++){
		eXo.ecm.CLV.setCondition(itemContainers[i], true);
	}
	eXo.ecm.CLV.enableCondition(itemSelectedContainer);
};

ContentListViewer.prototype.chooseCondition = function() {
	var me = this;
	var hiddenField = eXo.core.DOMUtil.findFirstDescendantByClass(me.form,"input","hidden");
	hiddenField.value = me.id;
	var itemSelectedContainer = eXo.core.DOMUtil.findAncestorByClass(me,"ContentSearchForm");
	var itemContainers = eXo.core.DOMUtil.findDescendantsByClass(me.form,"div","ContentSearchForm");
	for(var i=0;i<itemContainers.length;i++){
		eXo.ecm.CLV.setCondition(itemContainers[i],true);
	}
	eXo.ecm.CLV.enableCondition(itemSelectedContainer);
	eXo.ecm.lastCondition = itemSelectedContainer; 
};

ContentListViewer.prototype.enableCondition = function(itemContainer) {
	if(eXo.ecm.lastCondition) eXo.ecm.CLV.setCondition(eXo.ecm.lastCondition,true);
	eXo.ecm.CLV.setCondition(itemContainer,false);
};

ContentListViewer.prototype.setCondition = function(itemContainer,state) {
	var domUtil = eXo.core.DOMUtil;
	var action = domUtil.findDescendantsByTagName(itemContainer,"img");
	if(action && action.length > 0){
		for(var i = 0; i < action.length; i++){
			if(state) {
				action[i].style.visibility = "hidden";
			}	else {
				action[i].style.visibility = "";	
			}	
		}
	}
	
	var action = domUtil.findDescendantsByTagName(itemContainer,"input");
	if(action && (action.length > 0)){
		for(i = 0; i < action.length; i++){
			if(action[i].type != "radio") action[i].disabled = state;
		}
	}
	
	var action = domUtil.findDescendantsByTagName(itemContainer,"select");
	if(action && (action.length > 0)){
		for(i = 0; i < action.length; i++){
			action[i].disabled = state;
		}
	}
};

ContentListViewer.prototype.setHiddenValue = function() {
	var inputHidden = document.getElementById("checkedRadioId");
	if(eXo.core.Browser.chkRadioId == "null") {
		inputHidden.value = "name";
		document.getElementById("name").checked = true;
	} else {
		inputHidden.value = eXo.core.Browser.chkRadioId; 
		document.getElementById(eXo.core.Browser.chkRadioId).checked = true;
	}
};

ContentListViewer.prototype.checkModeViewer = function() {
	var formObj = document.getElementById("UICLVConfig");
	var OrderOptions = eXo.core.DOMUtil.findDescendantsByClass(formObj, "tr", "OrderBlock");
	var viewerModes = eXo.core.DOMUtil.findDescendantsByTagName(formObj, "input");
	for(var i = 0; i < viewerModes.length; i++) {
		if(viewerModes[i].getAttribute("name") == "ViewerMode") {
			if(viewerModes[i].value == "AutoViewerMode") {
				viewerModes[i].onclick = function() {
					for(var j = 0; j < OrderOptions.length; j++) {
						OrderOptions[j].style.display = "";
					}
				};
			} else if(viewerModes[i].value == "ManualViewerMode") {
				viewerModes[i].onclick = function() {
					for(var k = 0; k < OrderOptions.length; k++) {
						OrderOptions[k].style.display = "none";
					}
				};
			}
		}
	}
};

ContentListViewer.prototype.checkContextualFolderInput = function() {
	var formObj = document.getElementById("UICLVConfig");
	var tdContextualFolder = eXo.core.DOMUtil.findDescendantsByClass(formObj, "td", "ContextualRadio")[0];

	var inputs = eXo.core.DOMUtil.getChildrenByTagName(tdContextualFolder, "input");
	var enableInput = inputs[0];
	var disableInput = inputs[1];
	
	var trContextual = eXo.core.DOMUtil.findAncestorByTagName(tdContextualFolder, "tr");
	var trClv = eXo.core.DOMUtil.findNextElementByTagName(trContextual, "tr");
	
	var clvInput = eXo.core.DOMUtil.findDescendantsByTagName(trClv, "input")[0];

	enableInput.setAttribute("onmouseup", "eXo.ecm.CLV.enableClvInput(this)");
	disableInput.setAttribute("onmouseup", "eXo.ecm.CLV.disableClvInput(this)");
	if (enableInput.checked) {
		clvInput.removeAttribute('readonly');
	} else {
		clvInput.setAttribute('readonly', '');
	}
};

ContentListViewer.prototype.enableClvInput = function(obj){
	var trContextual = eXo.core.DOMUtil.findAncestorByTagName(obj, "tr");
	var trClv = eXo.core.DOMUtil.findNextElementByTagName(trContextual, "tr");
	var clvInput = eXo.core.DOMUtil.findDescendantsByTagName(trClv, "input")[0];
	clvInput.removeAttribute('readonly');
};

ContentListViewer.prototype.disableClvInput = function(obj){
	var trContextual = eXo.core.DOMUtil.findAncestorByTagName(obj, "tr");
	var trClv = eXo.core.DOMUtil.findNextElementByTagName(trContextual, "tr");
	var clvInput = eXo.core.DOMUtil.findDescendantsByTagName(trClv, "input")[0];
	clvInput.setAttribute('readonly', '');
};

ContentListViewer.prototype.addURL = function(aDiv) {
  var strHref = aDiv.getAttribute("href");
  var fIdx = strHref.indexOf("&backto");
  if (fIdx < 0 ) fIdx = strHref.indexOf("?backto");
  if (fIdx<0) return;
  var lIdx = strHref.indexOf("&", fIdx+1);
  var lString ="";
  var fString =strHref;
  if (lIdx >0) {
    lString = strHref.substr(lIdx);
    fString = strHref.substr(0, lIdx);
  }
  strHref = fString + escape(location.search)  + lString;
  aDiv.setAttribute("href", strHref);
}

eXo.ecm.CLV = new ContentListViewer();