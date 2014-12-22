/**
 * Created by The eXo Platform SEA
 * Author : eXoPlatform
 * toannh@exoplatform.com
 * On 12/16/14
 * Open document js
 *
 * handle openDocument by ITHIT library
 * handle event click, rightclick on ECMS, AS
 *
 */

(function(gj) {
  var OpenDocumentInOffice = function() {}

  OpenDocumentInOffice.prototype.openDocument = function(absolutePath, workspace, filePath){
    if(eXo.ecm.ECMWebDav !== undefined) {
      eXo.ecm.ECMWebDav.WebDAV.Client.DocManager.ShowMicrosoftOfficeWarning();
      var documentManager = eXo.ecm.ECMWebDav.WebDAV.Client.DocManager;
      var openStatus = false;
      if (documentManager.IsMicrosoftOfficeAvailable() && documentManager.IsMicrosoftOfficeDocument(absolutePath)) {
        if (!('ActiveXObject' in window)) absolutePath += '\0';
        openStatus = documentManager.MicrosoftOfficeEditDocument(absolutePath);
      } else {
        openStatus = documentManager.JavaEditDocument(absolutePath, null, "/open-document/applet/ITHitMountOpenDocument.jar");
      }
    }
  }

  /*
   * Checkout a versioned document when open successfully with desktop application to edit.
   */
  OpenDocumentInOffice.prototype.checkout = function(workspace, filePath){
    gj.ajax({
      url: "/portal/rest/office/checkout?filePath=" + filePath+"&workspace="+workspace,
      dataType: "text",
      type: "GET",
      async: true
    })
        .success(function (data) { });
  };

  /*Lock item */
  OpenDocumentInOffice.prototype.lockItem = function(filePath){

  } //end lock function

  /**
   * Update OpenDocument button's label
   * objId is workspace_name ':' node_path of file
   * activityId is id of activity in home page
   * rightClick update button when right click (context-menu)
   */
  OpenDocumentInOffice.prototype.updateLabel = function(objId, activityId, rightClick){
    console.log("update from core");
    var currentDocumentObj = {};
    gj.ajax({
      url: "/portal/rest/office/updateDocumentTitle?objId=" + objId+"&lang="+eXo.env.portal.language,
      dataType: "text",
      type: "GET",
      async: false
      //	timeout:1000 * 10
    })
        .success(function (data) {
          data = gj.parseJSON(data);
          var elClass = "uiIconEcmsOpenDocument";
          var isRightClick="";
          if(activityId != null && activityId != "undefined" && activityId != "") elClass +="_"+activityId;
          if(rightClick) isRightClick="#ECMContextMenu";
          var openDocument = gj(isRightClick+" ."+elClass).parent();
          var html = "<i class=\"uiIcon16x16FileDefault uiIcon16x16nt_file "+data.ico+" "+elClass+"\"></i>\n"+data.title;
          openDocument.html(html);

          if(eXo.ecm.ECMWebDav !== undefined) {
            console.log("ITHIT detected!");
            if(activityId != null && activityId != "undefined" && activityId != ""){ // update 4 activities
              var _filePath = openDocument.attr("href");
              openDocument.attr("href", "javascript:eXo.ecm.OpenDocumentInOffice.openDocument('"+_filePath+"')");
            }

          }else{
            console.log("ITHIT not detected!");

            openDocument.parent().removeAttr("onclick");
            openDocument.attr("href", "/rest/lnkproducer/openit.lnk?path=/"+data.repository+"/"+data.workspace+data.filePath);
          }

          gj(".detailContainer").find('.openDocument').html(data.title);
          currentDocumentObj = '{"title":"'+data.title+'", "ico": "'+data.ico+'"}';
        });

    setCookie("_currentDocument", currentDocumentObj, 1);
  }

  /**
   * Close all popup
   */

  OpenDocumentInOffice.prototype.closePopup = function(){

  }

  OpenDocumentInOffice.prototype.openDocument_doClick = function(){
    gj("#uiActionsBarContainer .uiIconEcmsOpenDocument").parent().click();
  }

  /**
   * Set value to browser's cookie
   */
  function setCookie(cname, cvalue, exdays) {
    var d = new Date();
    d.setTime(d.getTime() + (exdays*24*60*60*1000));
    var expires = "expires="+d.toUTCString();
    document.cookie = cname + "=" + cvalue + "; " + expires;
  }

  /**
   *get cookie by key
   */
  function getCookie(cname) {
    var name = cname + "=";
    var ca = document.cookie.split(';');
    for(var i=0; i<ca.length; i++) {
      var c = ca[i];
      while (c.charAt(0)==' ') c = c.substring(1);
      if (c.indexOf(name) != -1) return c.substring(name.length,c.length);
    }
    return "";
  }


  var bindASActionBar = function(){
    gj("#UIActivitiesLoader .uiContentActivity").each(function(){

      var activityId        = gj(this).attr("id").replace("activityContainer", "");
      var activityActionBar = gj(this).find(".actionBar .pull-left");
      var linkTitle         = gj(this).find(".linkTitle").html().trim();

      //add OpenDocument button
      var openDocumentButton = gj(activityActionBar).find(".uiIconEcmsOpenDocument");
      if(openDocumentButton.length === 0){
        var html  = "<i class=\"uiIconEcmsOpenDocument_"+activityId+"\" </i>\n Open...";
        var documentLink = "#";
        var workspace="";
        var filePath="";
        gj.ajax({
          url: "/portal/rest/office/getActivity?activityId=" + activityId+"&lang="+eXo.env.portal.language,
          dataType: "text",
          type: "GET",
          async: false
        })
            .success(function (data) {
              data = gj.parseJSON(data);
              documentLink = data.absolutePath;
              workspace = data.workspace;
              filePath = data.filePath;
              // draw OpenXXX button
              gj(activityActionBar).prepend("<li><a href=\"javascript:void(0);\" onclick=\"eXo.ecm.OpenDocumentInOffice.openDocument	('"+documentLink+"', '"+workspace+"', '"+filePath+"')\">"+html+"</a></li>");
            });

        eXo.ecm.OpenDocumentInOffice.updateLabel(linkTitle, activityId);
      }

      //remove last btn
      // gj(activityActionBar).find(".uiIconEdit").parents().eq(1).remove();


    });

  }

  gj(document).ready(function() {
    bindASActionBar();
  });

  eXo.ecm.OpenDocumentInOffice = new OpenDocumentInOffice();
  return {
    OpenDocumentInOffice : eXo.ecm.OpenDocumentInOffice
  };

})(gj);