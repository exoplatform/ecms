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
  
  try {
    if (CKEDITOR && typeof CKEDITOR == "object") {
      for (var i in CKEDITOR.instances) {
        if(document.getElementById(i)) CKEDITOR.instances[i].updateElement();
      }
    }
  } catch(e) {}

  form.elements['formOp'].value = action ;
  if(useAjax) this.ajaxPost(form, callback) ;
  else  form.submit();
} ;

/*****************************************************************************************/

function getUrlParam(paramName) {
	var oRegex = new RegExp("[\?&]" + paramName + "=([^&]+)", "i");
	var oMatch = oRegex.exec(window.location.search) ; 
	if (oMatch && oMatch.length > 1) return oMatch[1];
	else return "";
}

/*
* This is the main entry method for every Ajax calls to the eXo Portal
*
* It is simply a dispatcher method that fills some init fields before 
* calling the doRequest() method
* 
* Modified: add all parameters in current URL to current request.
*/
function ajaxGet(url, callback) {
  if (!callback) callback = null ;
	var path = getUrlParam("path");
	if(path) url += "&path="+path;
  doRequest("Get", url, null, callback) ;
} ;

/*
* This method is called when a HTTP POST should be done but in an AJAX
* case some maniputalions are needed
* Once the content of the form is placed into a string object, the call
* is delegated to the doRequest() method 
*/
UIForm.prototype.ajaxPost = function (formElement, callback) {
//function ajaxPost(formElement, callback) {
  if (!callback) callback = null ;
  var queryString = eXo.webui.UIForm.serializeForm(formElement) ;
  var url = formElement.action + "&ajaxRequest=true" ;
  //url += "&" + location.search.substring(1);
	var path = getUrlParam("path");
	if(path) url += "&path="+path;
  doRequest("POST", url, queryString, callback) ;
} ;