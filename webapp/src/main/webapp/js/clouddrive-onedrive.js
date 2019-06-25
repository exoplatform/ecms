

(function($, cloudDrive, utils) {

    function OneDriveClient() {

        this.initFile = function (file) {

        };

        this.initContext = function (provider) {
            if ('onedrive' == provider.id.trim().toLowerCase()) {
                $(function () {
                    var file = cloudDrive.getContextFile();
                    if (file) {
                        if (file.type.trim().startsWith('image')) {
                            console.log('OneDrive initContext, provider= ' + provider);
                            var $viewer = $('#CloudFileViewer');
                            if ($viewer) {
                                $viewer.prepend( "<p class='onedriveFileViewer'>" +

                                    "<img class='onedriveImgFileViewer' src='"+file.previewLink+"'/>" +

                                    "</p>"

                                        );
                                console.log('$viewer=' + $viewer.html());
                                $viewer.find('iframe').remove();
                            } else {
                                console.log('not viewer!!!!!');
                            }

                        }
                    }
                });

            }
        };
    }
    return new OneDriveClient();



})($, cloudDrive, cloudDriveUtils);