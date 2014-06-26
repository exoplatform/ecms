/** 
 * Google Drive support for eXo Cloud Drive.
 */
(function($, cloudDrive, utils) {

	/**
	 * Google Drive connector class.
	 */
	function GDrive() {
		// Provider Id for Google Drive
		var PROVIDER_ID = "gdrive";
		
		var internal = function(nodeWorkspace, nodePath) {
			
		};

		/**
		 * Processing of drive connect. Given process it is jQuery promise.
		 */
		this.onConnect = function(process) {
			if (process) {
				utils.log("Connecting Cloud Drive via " + process);
			} else {
				utils.log("WARN: Null drive on connect!");
			}
		};
	}

	var gdrive = new GDrive();

	// TODO do this in clouddrive.js: Load Google Drive styles only in top window (not in iframes of gadgets).
	if (window == top) {
		try {
			// load required styles
			utils.loadStyle("/cloud-drive-gdrive/skin/cloud-drive-gdrive.css");
		} catch(e) {
			utils.log("Error configuring Google Drive style.", e);
		}
	}

	return gdrive;

})($, cloudDrive, cloudDriveUtils);