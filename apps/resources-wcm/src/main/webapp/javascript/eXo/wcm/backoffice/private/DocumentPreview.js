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
            gj('#outerContainer', $uiDocumentPreview).height(pdfDisplayAreaHeight); // pdf viewer

            // Media viewer, no preview file
            var $navigationContainer = gj(".navigationContainer", $uiDocumentPreview);
            var $uiContentBox = gj('.uiContentBox', $navigationContainer);
            var $video = gj('.videoContent', $uiContentBox);
            $navigationContainer.height(pdfDisplayAreaHeight);
            $uiContentBox.height(pdfDisplayAreaHeight);
            $video.css('max-width', $uiContentBox.width() - 2);
            $video.css('max-height', $uiContentBox.height() - 3);
            $video.css('width', '100%');
            $video.css('height', 'auto');

            gj('.uiPreviewWebContent', $uiDocumentPreview).height(pdfDisplayAreaHeight - 30) // webcontent
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
