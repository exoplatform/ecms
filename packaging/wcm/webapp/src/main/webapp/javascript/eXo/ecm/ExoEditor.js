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