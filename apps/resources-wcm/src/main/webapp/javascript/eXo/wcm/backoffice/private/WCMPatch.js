/**
 * Created by The eXo Platform SAS
 * Author : Phan Le Thanh Chuong
 *          chuong.phan@exoplatform.com; phan.le.thanh.chuong@gmail.com
 * Modified: Copy from portal's UIForm.js, add a condition to check with CKEditor
 *           TODO: Should be removed when update to new version of GateIn.
 * May 12, 2010  
 */

/*ie bug  you cannot have more than one button tag*/
/**
 * A function that submits the form identified by formId, with the specified action
 * If useAjax is true, calls the ajaxPost function from PortalHttpRequest, with the given callback function
 */
eXo.webui.UIForm.submitForm = function(formId, action, useAjax, callback) {
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
      for (var i in CKEDITOR.instances) {
    	CKEDITOR.instances[i].updateElement();
      }
    }
  } catch(e) {}

  form.elements['formOp'].value = action ;
  if(useAjax) ajaxPost(form, callback) ;
  else  form.submit();
} ;