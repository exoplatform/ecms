(function (gj, base) {
  function DocumentPreview() {
  }

  eXo.ecm.DocumentPreview = new DocumentPreview();
  DocumentPreview.prototype.bindClose = function() {
    // Bind Esc key
    gj('#UIDocumentPreview').keyup(function(e) {
      if(e.keyCode == 27) {
        alert("xxx");
        gj(".uiIconClose", this).trigger("click");
      }
    });
  };

  return {
    DocumentPreview : eXo.ecm.DocumentPreview
  };
})(gj, base);
