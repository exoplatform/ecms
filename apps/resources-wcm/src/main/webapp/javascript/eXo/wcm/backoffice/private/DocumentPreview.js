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
            var $commentArea = gj('.commentArea', $uiDocumentPreview);
            var $commentAreaTitle = gj('.title', $commentArea);
            var $commentInputBox = gj('.commentInputBox',$commentArea);
            var $commentList = gj('.commentList', $commentArea);
            var commentAreaHeight = window.innerHeight -30;
            $commentArea.height(commentAreaHeight);
            $commentList.css('max-height', commentAreaHeight - $commentAreaTitle.innerHeight() - $commentInputBox.innerHeight());


            // Media viewer, no preview file
            var $navigationContainer = gj(".navigationContainer", $uiDocumentPreview);
            var $uiContentBox = gj('.uiContentBox', $navigationContainer);
            var $video = gj('.videoContent', $uiContentBox);
            var $flowplayerContentDetail = gj('.ContentDetail', $uiContentBox);
            var $flowplayerPlayerContent = gj('.PlayerContent', $flowplayerContentDetail);
            var $flowplayer = gj('object', $flowplayerPlayerContent);
            var $flashViewer = gj('.FlashViewer', $uiContentBox);
            var $embed = gj('embed', $flashViewer);
            var $windowmediaplayer = gj('#MediaPlayer1', $uiContentBox);

            $navigationContainer.height(pdfDisplayAreaHeight);
            $uiContentBox.height(pdfDisplayAreaHeight);
            $flowplayerContentDetail.height(pdfDisplayAreaHeight);
            $flowplayerPlayerContent.height(pdfDisplayAreaHeight-5);
            $flashViewer.height(pdfDisplayAreaHeight-5);

            $flowplayer.css('max-width', $uiContentBox.width() - 2);
            $flowplayer.css('max-height', $uiContentBox.height() - 3);
            $flowplayer.css('width', '100%');
            $flowplayer.css('height', '100%');

            $video.css('max-width', $uiContentBox.width() - 2);
            $video.css('max-height', $uiContentBox.height() - 3);
            $video.css('width', '100%');
            $video.css('height', 'auto');

            $windowmediaplayer.css('max-width', $uiContentBox.width() - 2);
            $windowmediaplayer.css('max-height', $uiContentBox.height() - 7);
            $windowmediaplayer.css('width', '100%');
            $windowmediaplayer.css('height', '100%');

            $embed.css('max-width', $uiContentBox.width() - 2);
            $embed.css('max-height', $uiContentBox.height() - 3);
            $embed.css('width', '100%');
            $embed.css('height', '100%');

            var $img = gj('a > img', $uiContentBox);
            $img.css('max-width', $uiContentBox.width() - 2);
            $img.css('max-height', $uiContentBox.height() - 3);
            $img.css('width', 'auto');
            $img.css('height', 'auto');

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
