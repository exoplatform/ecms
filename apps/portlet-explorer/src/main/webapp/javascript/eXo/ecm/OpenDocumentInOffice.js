(function(gj, ecmWebdav) {
  var OpenDocumentInOffice = function() {}

  OpenDocumentInOffice.prototype.openDocument = function(filePath){

    var documentManager = eXo.ecm.ECMWebDav.WebDAV.Client.DocManager;
    if (documentManager.IsMicrosoftOfficeAvailable() && documentManager.IsMicrosoftOfficeDocument(filePath)) {
      documentManager.MicrosoftOfficeEditDocument(filePath);
    } else {
      documentManager.JavaEditDocument(filePath, null, "/ecmexplorer/applet/ITHitMountOpenDocument.jar");
    }

  }

  OpenDocumentInOffice.prototype.updateLabel = function(objId){
    gj.ajax({
      url: "/portal/rest/office/updateDocumentLabel?objId=" + objId,
      dataType: "text",
      type: "GET",
      async: false
      //	timeout:1000 * 10
    })
      .success(function (data) {
        data = gj.parseJSON(data);
        var openDocument = gj(".uiIconEcmsOpenDocument").parent();
        var html = "<i class=\"uiIconEcmsOpenDocument "+data.ico+"\"></i>";

        if(data.type==="Open_Word")
          html+=eXo.ecm.WCMUtils.getBundle('OpenInOfficeConnector.label.open-in-word', eXo.env.portal.language);
        if(data.type==="Open_Excel")
          html+=eXo.ecm.WCMUtils.getBundle('OpenInOfficeConnector.label.open-in-excel', eXo.env.portal.language);
        if(data.type==="Open_Powerpoint")
          html+=eXo.ecm.WCMUtils.getBundle('OpenInOfficeConnector.label.open-in-powerpoint', eXo.env.portal.language);
        if(data.type==="Open_Desktop")
          html+=eXo.ecm.WCMUtils.getBundle('OpenInOfficeConnector.label.open-in-desktop', eXo.env.portal.language);

        openDocument.html(html);
        openDocument.attr("onclick", "eXo.ecm.OpenDocumentInOffice.openDocument('"+data.filePath+"')");
      });

  }

  eXo.ecm.OpenDocumentInOffice = new OpenDocumentInOffice();
  return {
    OpenDocumentInOffice : eXo.ecm.OpenDocumentInOffice
  };

})(gj, ecmWebdav);



