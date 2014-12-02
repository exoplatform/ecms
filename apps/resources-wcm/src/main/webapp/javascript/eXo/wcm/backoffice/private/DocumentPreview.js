(function(gj, base) {
    function DocumentPreview() {}

    DocumentPreview.prototype.initScreen = function() {
        // Temporarily disable body scroll
        gj('body').css('overflow', 'hidden');

        // Bind Esc key
        var closeEventHandler = function(e) {
            if (e.keyCode == 27) {
                gj(".uiIconClose", document.getElementById('UIDocumentPreview')).trigger("click");
            }
        }
        gj(document).on('keyup', closeEventHandler);

        // Return body scroll, turn off keyup
        gj(".uiIconClose", document.getElementById('UIDocumentPreview')).click(function() {
            setTimeout(function() {
                gj('body').css('overflow', 'visible');
                gj(document).off('keyup', closeEventHandler);
            }, 500);
        });
    };


    eXo.ecm.DocumentPreview = new DocumentPreview();

    return {
        DocumentPreview: eXo.ecm.DocumentPreview
    };
})(gj, base);
