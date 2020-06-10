/**
 * 
  Copyright (C) 2003-2016 eXo Platform SAS.
  
  This is free software; you can redistribute it and/or modify it
  under the terms of the GNU Lesser General Public License as
  published by the Free Software Foundation; either version 2.1 of
  the License, or (at your option) any later version.
  
  This software is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  Lesser General Public License for more details.
  
  You should have received a copy of the GNU Lesser General Public
  License along with this software; if not, write to the Free
  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
/**
 * Google Drive support for eXo Cloud Drive.
 * 
 */
(function($, utils) {

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
					// decrease activity box size, set size to fileTypeContent
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
})($, cloudDriveUtils);
