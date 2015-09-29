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
			if (file && file.link && !file.previewLink) {
				// If file has not preview link we construct an one from its alternate link (if it is a view link).
				// The alternate link has CORS SAMEORIGIN restriction for non-Google formats.
				// We change it on /preview as proposed in several blogs in Internet - it's not documented stuff.
				var sdkView = "view?usp=drivesdk";
				var sdkViewIndex = file.link.indexOf(sdkView, file.link.length - sdkView.length);
				if (sdkViewIndex !== -1) {
					file.previewLink = file.link.slice(0, sdkViewIndex) + "preview";
				}
			}
		};
	}

	return new GoogleDriveClient();
})($, cloudDrive, cloudDriveUtils);
