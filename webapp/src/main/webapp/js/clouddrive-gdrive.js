/**
 * Google Drive support for eXo Cloud Drive.
 * 
 */
(function($, cloudDrive, utils) {

	/**
	 * Google Drive connector class.
	 */
	function GoogleDriveClient() {

		/**
		 * Initialize Google Drive file.
		 */
		this.initFile = function(file) {
			if (file && file.link && (!file.previewLink || file.previewLink.indexOf("//video.google.com/get_player") > 0)) {
				// XXX If file has not preview link, or it's a video player, we construct an one from its
				// alternate link (if it is a view link).
				// The alternate link (and video player) has CORS SAMEORIGIN restriction for non-Google
				// formats.
				// We change it on /preview as proposed in several blogs in Internet - it's not documented
				// stuff.
				var sdkView = "view?usp=drivesdk";
				var sdkViewIndex = file.link.indexOf(sdkView, file.link.length - sdkView.length);
				if (sdkViewIndex !== -1) {
					file.previewLink = file.link.slice(0, sdkViewIndex) + "preview";
				}
			}
		};

		this.initContext = function(provider) {
			// XXX For a case of Activity stream, we fix the UI (CSS) to let Google large icons look
			// smaller, like native ones in eXo
			try {
				$("i[class*='uiIcon64x64applicationvndgoogle-'].uiCloudFileActivity").each(function() {
					var $elem = $(this);
					// add absolute position to the icon (i.uiCloudFileActivity) to fit the activity box
					$elem.css("top", "-30px");
					$elem.css("left", "-35px");
					// to decrease activity box size, set size to fileTypeContent
					// (fileTypeContent>a>i.uiCloudFileActivity)
					$elem.parent().parent().css({
					  "width" : "60px",
					  "height" : "65px"
					});
				});
			} catch(e) {
				utils.log("Error initializing Google Drive UI " + e, e);
			}
		};
	}

	return new GoogleDriveClient();
})($, cloudDrive, cloudDriveUtils);
