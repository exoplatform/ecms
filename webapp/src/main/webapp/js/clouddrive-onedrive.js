

(function($, cloudDrive, utils, io) {

    function OneDriveClient() {
        console.log('OneDriveClient init');
        var socket =  io("https://3-westeurope1.pushp.svc.ms/notifications?token=w1-4ef891c6-0a6a-429b-95f5-686c3e29427c");


        socket.on("connect", ()=>console.log("EEEE.......Connected!"));
        socket.on("notification", (data)=>console.log("EEE.....Notification!", data));


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


        var getChange = function (drive) {
            // must be blocking
        };

        //
        // this.onChange = function(drive) {
        //     var process = $.Deferred();
        //
        //     if (drive) {
        //         // utils.log(">>> enabling changes monitor for Cloud Drive " + drive.path);
        //         if (drive.state) {
        //             // Drive supports state - thus we can send connector specific data via it from Java API
        //             // State it is a POJO in JavaAPI. Here it is a JSON object.
        //             pollChanges(process, drive);
        //         } else {
        //             process.reject("Cannot check for changes. No state object for Cloud Drive on " + drive.path);
        //         }
        //     } else {
        //         process.reject("Null drive in onChange()");
        //     }
        //     return process.promise();
        // };

    }
    return new OneDriveClient();



})($, cloudDrive, cloudDriveUtils, socketIO);