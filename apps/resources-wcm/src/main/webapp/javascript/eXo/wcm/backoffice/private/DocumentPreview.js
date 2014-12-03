(function(gj, base) {
    function DocumentPreview() {}

    DocumentPreview.prototype.initScreen = function() {

        var $uiDocumentPreview = gj("#UIDocumentPreview");

        // Temporarily disable body scroll
        gj('body').css('overflow', 'hidden');

        // Bind Esc key
        var closeEventHandler = function(e) {
            if (e.keyCode == 27) {
                gj(".uiIconClose", $uiDocumentPreview).trigger("click");
            }
        }
        gj(document).on('keyup', closeEventHandler);

        // Resize Event
        var resizeEventHandler = function() {
            // Calculate margin 
            var pdfDisplayAreaHeight = window.innerHeight - 62;
            gj('#UIDocumentPreview #outerContainer').height(pdfDisplayAreaHeight); // pdf viewer
            gj('#UIDocumentPreview .navigationContainer').height(pdfDisplayAreaHeight); // media viewer, no preview file
            gj('#UIDocumentPreview .uiPreviewWebContent').height(pdfDisplayAreaHeight -30) // webcontent
        }
        resizeEventHandler();
        gj(window).on('resize', resizeEventHandler);

        // Return body scroll, turn off keyup
        gj(".uiIconClose", $uiDocumentPreview).click(function() {
            setTimeout(function() {
                gj('body').css('overflow', 'visible');
                gj(document).off('keyup', closeEventHandler);
                gj(window).off('resize', resizeEventHandler);
            }, 500);
        });

    };


    eXo.ecm.DocumentPreview = new DocumentPreview();

    return {
        DocumentPreview: eXo.ecm.DocumentPreview
    };
})(gj, base);
