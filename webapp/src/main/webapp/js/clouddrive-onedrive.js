

(function($, cloudDrive, utils) {

    function OneDriveClient() {

        this.initFile = function (file) {

        };

        this.initContext = function (provider) {
            if ('onedrive' == provider.id.trim().toLowerCase()) {
                $(function () {
                    var file = cloudDrive.getContextFile();
                    if (file) {
                        var $viewer = $('#CloudFileViewer');
                        if (file.type.trim().startsWith('image') && file.previewLink.endsWith('/root/content')) { // image in personal account
                            console.log('OneDrive initContext, provider= ' + provider);
                            if ($viewer) {
                                $viewer.prepend( "<p class='onedriveFileViewer'>" +

                                    "<img class='onedriveImgFileViewer' src='"+file.previewLink+"'/>" +

                                    "</p>"

                                        );
                                console.log('$viewer=' + $viewer.html());
                            } else {
                                console.log('not viewer!!!!!');
                            }

                        }
                        if (file.previewLink.indexOf("embed") == -1) { //
                            $viewer.find('iframe').remove();
                        }
                    }
                });

            }
        };
    }
    return new OneDriveClient();



})($, cloudDrive, cloudDriveUtils);