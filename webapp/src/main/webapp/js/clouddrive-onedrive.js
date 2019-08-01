(function ($, cloudDrive, utils, socketIO) {

  function OneDriveClient() {

    var oneDrives = new Map(); // TODO this will not work in IE, use {} 

    function OneDriveSubscription(userId, notificationUrl) {
      var self = this;
      this.changed = false;
      this.userId = userId;

      var socket = socketIO.io(notificationUrl);
      socket.on('notification', function (data) {
        console.log('notification ' + data);
        self.changed = true;
      });
    }

    var processAfterNotification = function (oneDriveSubscription, process, drive) {

      console.log("wait notification");
      var nowTime = new Date().getTime();

      // if (drive.state.expirationDateTime) {
      //   console.log('time left for : ' + ' ' + + (drive.state.expirationDateTime - nowTime));
      // }else{
      //   console.log("drive.state.expirationDateTime = null");
      // }
      if (nowTime >= drive.state.expirationDateTime) {
        console.log('time to renew state');
        renewState(process, drive);
        return;
      }
      if (oneDriveSubscription.changed) {
        oneDriveSubscription.changed = false;
        process.resolve();
      } else {
        setTimeout(function () {
          processAfterNotification(oneDriveSubscription, process, drive);
        }, 1000);
      }
    };


    // var findInMap = function (map, userId) {
    //   var mapIter = map.values();
    //   var element;
    //   while (element = mapIter.next().value) {
    //     if (userId && element.userId) {
    //       console.log('userId ' + userId + ' elementUserId ' + element.userId);
    //
    //       if (userId.includes(element.userId)) {
    //         console.log(element);
    //         return element;
    //       } else {
    //         console.log('not includes: = ' + element.userId);
    //       }
    //     }
    //   }
    //   return null;
    // };

    var renewState = function (process, drive) {
      var newState = cloudDrive.getState(drive);
      newState.done(function (res) {
        drive.state = res;
        if (oneDrives.has(drive.state.creatorId)) {
          oneDrives.delete(drive.state.creatorId);
        }
        process.resolve();
      });
      newState.fail(function (response, status, err) {
        process.reject("Error getting new changes link. " + err + " (" + status + ")");
      });
      return newState;
    };

    this.onChange = function (drive) {
      console.log('onchange');
      var process = $.Deferred();
      if (drive) {
        if (drive.state) {
          var nowTime = new Date().getTime();

          if (nowTime >= drive.state.expirationDateTime) {
            renewState(process, drive);
          } else {
            // if (drive.state.creatorId) {
            //   console.log('creatorId = ' + drive.state.creatorId);
            // }else{
            //   console.log('creatorId = undefined' );
            // }

            if (!oneDrives.has(drive.state.creatorId)) {
              oneDrives.set(drive.state.creatorId, new OneDriveSubscription(drive.state.creatorId, drive.state.url));
            }

            var oneDriveSubscription = oneDrives.get(drive.state.creatorId);
            processAfterNotification(oneDriveSubscription, process, drive);
          }

        } else {
          process.reject("Cannot check for changes. No state object for Cloud Drive on " + drive.path);
        }
      } else {
        process.reject("Null drive in onChange()");
      }
      return process.promise();
    };


    this.initContext = function (provider) {
      $(function () {
        var file = cloudDrive.getContextFile();
        if (file) {
          var $viewer = $('#CloudFileViewer');
          if (file.type.trim().startsWith('image') && file.previewLink && file.previewLink.endsWith('/root/content')) { // image in personal account
            console.log('OneDrive initContext, provider= ' + provider);
            if ($viewer) {
              if ($viewer.has('.onedriveImgFileViewer').length == 0) {
                $viewer.prepend("<p class='onedriveFileViewer'>" +
                  "<img class='onedriveImgFileViewer' src='" + file.previewLink + "'/>" +
                  "</p>"
                );
              }
              // console.log('$viewer=' + $viewer.html()); // TODO this will print a bunch of markup in the console, need it?
            } else {
              console.log('not viewer!!!!!');
            }
          }
          if (file.previewLink && file.previewLink.indexOf("embed") == -1) { //
            $viewer.find('iframe').remove();
          }
        }
      });
    };


    var getChange = function (drive) {
      // must be blocking
    };
  }

  return new OneDriveClient();

})($, cloudDrive, cloudDriveUtils, cloudDriveSocketIO);