/**
 * Variable to check if some content has changed
 * - true if some content has changed
 **/
var b_changed = false;

/**
 * Change the current state to inform some content has changed
 **/
function changed() {
  b_changed = true;
}

/**
 * UPDATE AJAX GET METHOD
 * - manage changes popup
 **/

function ajaxGet(url, callback) {
  var bypassActionbar= -1; //url.indexOf("uicomponent=UIActionBar_");
  if (b_changed && bypassActionbar<=0) {
    var answer = confirm(document.getElementById("NavigateConfirmationMsg").innerHTML);
    if (answer) {
      b_changed = false;
    } else {
      return;
    }
    
  }

  if (!callback) callback = null ;
  doRequest("Get", url, null, callback) ;
};

/**
 * UPDATE FORM SUBMIT METHOD
 * - manage changes popup
 * - manage CKeditor update in textareas
 **/
eXo.webui.UIForm.submitForm = function(formId, action, useAjax, callback) {
  if(action.toLowerCase() == "changetype" || action.toLowerCase() == "close" || action.toLowerCase() == "back") {
  	if (eXo.ecm.ECMUtils) {
      eXo.ecm.ECMUtils.editFullScreen = false;
    }
    if (b_changed) {      
		  var answer = null;
		  if (action.toLowerCase() == "changetype") {
		  	answer = confirm(document.getElementById("ChangeTypeConfirmationMsg").innerHTML);
		  } else {
		  	answer = confirm(document.getElementById("CloseConfirmationMsg").innerHTML);
		  }  
      if (answer) {
      	b_changed = false;
      }	else {
      	return;
      }
    }
  }    
 
 if (!callback) callback = null;
 var form = this.getFormElemt(formId) ;
 //TODO need review try-cactch block for form doesn't use FCK
 try {
  if (FCKeditorAPI && typeof FCKeditorAPI == "object") {
    for ( var name in FCKeditorAPI.__Instances ) {
      var oEditor ;
      try {
        oEditor = FCKeditorAPI.__Instances[name] ;
        if (oEditor && oEditor.GetParentForm && oEditor.GetParentForm() == form ) {
          oEditor.UpdateLinkedField() ;
        }
      } catch(e) {
        continue ;
      }
    }
  }
 } catch(e) {}

 try {
  
  if (CKEDITOR && typeof CKEDITOR == "object") {
    for ( var name in CKEDITOR.instances ) {
      var oEditor ;
      try {
        oEditor = CKEDITOR.instances[name] ;
        if (oEditor && document.getElementById(name)) {
            var rendered = eXo.core.DOMUtil.findNextElementByTagName(document.getElementById(name), 'span').id.indexOf('cke');
            if (rendered == 0) document.getElementById(name).value = oEditor.getData();
        }
      } catch(e) {
        continue ;
      }
    }
  }
 } catch(e) {}
  form.elements['formOp'].value = action ;
  if(useAjax) {
    b_changed = false;
    this.ajaxPost(form, callback) ;
  } else {
    form.submit();
  }
} ;

/**
 * END UPDATE FORM
 **/

/**
 * Submits a form by Ajax, with the given action and the given parameters
 * Calls ajaxPost of PortalHttpRequest
 * Note: ie bug  you cannot have more than one button tag
 */
eXo.webui.UIForm.submitEvent = function(formId, action, params) {
  var form = this.getFormElemt(formId) ;
	 try {
	  if (FCKeditorAPI && typeof FCKeditorAPI == "object") {
	 	  for ( var name in FCKeditorAPI.__Instances ) {
	 	  	var oEditor = FCKeditorAPI.__Instances[name] ;
	 	  	if ( oEditor.GetParentForm && oEditor.GetParentForm() == form ) {
	 	  		oEditor.UpdateLinkedField() ;
	 	  	}
	  	}
	  }
	 } catch(e) {}
	 
	  try {

	  if (CKEDITOR && typeof CKEDITOR == "object") {
	    for ( var name in CKEDITOR.instances ) {
	      var oEditor ;
	      try {
	        oEditor = CKEDITOR.instances[name] ;
	        if (oEditor && document.getElementById(name)) {
	            var rendered = eXo.core.DOMUtil.findNextElementByTagName(document.getElementById(name), 'span').id.indexOf('cke');
	            if (rendered == 0) document.getElementById(name).value = oEditor.getData();
	        }
	      } catch(e) {
	        continue ;
	      }
	    }
	  }
	 } catch(e) {}

	 
  form.elements['formOp'].value = action ; 
  if(!form.originalAction) form.originalAction = form.action ; 
	form.action =  form.originalAction +  encodeURI(params) ;
  b_changed = false;
  this.ajaxPost(form) ;
} ;

/**
 * Before we change the url, we check if the content has changed
 * Inform the user with a popup
 **/

function closeIt(e) {
  if (b_changed) {
    var e = e || window.event;
    // For IE and Firefox
    if (e) {
      e.returnValue = 'The changes you made will be lost if you navigate away from this page.';
    }
  
    // For Safari
    return 'The changes you made will be lost if you navigate away from this page.';
  }
}
 
function changeElements(divId) {
  var UIDocForm = document.getElementById(divId);
  if (!UIDocForm) return;
  //add onchange event into <input> tags
  var inputTags = UIDocForm.getElementsByTagName("input");
  for (var i = 0; i < inputTags.length; i++) {
    inputTags[i].attachEvent("onchange", new Function ("changed();"));
  }

  //add onchange event into <select> tags
  var selectTags = UIDocForm.getElementsByTagName("select");
  for (var i = 0; i < selectTags.length; i++) {
    selectTags[i].attachEvent("onchange", new Function ("changed();"));
  }

  //add onchange event into <textarea> tags
  var textareaTags = UIDocForm.getElementsByTagName("textarea");
  for (var i = 0; i < textareaTags.length; i++) {
    textareaTags[i].attachEvent("onchange", new Function ("changed();"));
  }
}


