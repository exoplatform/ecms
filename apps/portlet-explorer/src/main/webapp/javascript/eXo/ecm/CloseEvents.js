var b_changed = false;

function changed() {
  b_changed = true;
}

function ajaxGet(url, callback) {
  var bypassActionbar= -1; //url.indexOf("uicomponent=UIActionBar_");
  if (b_changed && bypassActionbar<=0) {
    var answer = confirm("The changes you made will be lost if you navigate away from this page.");
    if (answer) {
      b_changed = false;
    } else {
      return;
    }
    
  }

  if (!callback) callback = null ;
  doRequest("Get", url, null, callback) ;
};

UIForm.prototype.submitForm = function(formId, action, useAjax, callback) {
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
 
  if (CKEDITOR && typeof CKEDITOR == "object") {
	 for ( var name in CKEDITOR.instances) {
		// alert(CKEDITOR.instances["summary"].getData()); 
		document.getElementById(name).value = CKEDITOR.instances[name].getData();
	 }
  }

  form.elements['formOp'].value = action ;
  if(useAjax) {
    b_changed = false;
    ajaxPost(form, callback) ;
  } else {
    form.submit();
  }
} ;
eXo.webui.UIForm = new UIForm();

window.onbeforeunload = function (e) {
  var e = e || window.event;

  // For IE and Firefox
  if (e) {
    e.returnValue = 'Any string';
  }

  // For Safari
  return 'Any string';
};


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
window.onbeforeunload = closeIt;

document.getElementById("UIDocumentForm").onchange = changed;
