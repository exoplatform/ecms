(function (gj, base) {
  function DocumentPreview() {
  }

  DocumentPreview.prototype.initScreen = function () {

    // Temporarily disable body scroll
    gj('body').css('overflow', 'hidden');

    // Bind Esc key
    gj('#UIDocumentPreview').keyup(function (e) {
      if (e.keyCode == 27) {
        gj(".uiIconClose", this).trigger("click");
      }
    });

    // Return body scroll
    gj(".uiIconClose", document.getElementById('UIDocumentPreview')).click(function () {
      gj('body').css('overflow', 'visible');
    });
  };

  eXo.ecm.DocumentPreview = new DocumentPreview();

  return {
    DocumentPreview: eXo.ecm.DocumentPreview
  };
})(gj, base);