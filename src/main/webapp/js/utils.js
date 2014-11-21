/**
 * Util methods for CloudDrive.
 */
(function($) {
	function CDUtils() {
		/**
		 * Stuff grabbed from CW's commons.js
		 */
		this.pageBaseUrl = function(theLocation) {
			if (!theLocation) {
				theLocation = window.location;
			}

			var theHostName = theLocation.hostname;
			var theQueryString = theLocation.search;

			if (theLocation.port) {
				theHostName += ":" + theLocation.port;
			}

			return theLocation.protocol + "//" + theHostName;
		};

		/** Set cookie */
		this.setCookie = function(name, value, millis, toDocument, toPath, toDomain) {
			var expires;
			if (millis) {
				var date = new Date();
				date.setTime(date.getTime() + millis);
				expires = "; expires=" + date.toGMTString();
			} else {
				expires = "";
			}
			(toDocument ? toDocument : document).cookie = name + "=" + encodeURIComponent(value) + expires + "; path="
			    + (toPath ? toPath : "/") + (toDomain ? "; domain=" + toDomain : "");
		};

		/** Read cookie */
		this.getCookie = function(name, fromDocument) {
			var nameEQ = name + "=";
			var ca = (fromDocument ? fromDocument : document).cookie.split(';');
			for (var i = 0; i < ca.length; i++) {
				var c = ca[i];
				while (c.charAt(0) == ' ') {
					c = c.substring(1, c.length);
				}
				if (c.indexOf(nameEQ) == 0) {
					return decodeURIComponent(c.substring(nameEQ.length, c.length));
				}
			}
			return null;
		};

		/**
		 * Add style to current document (to the end of head).
		 */
		this.loadStyle = function(cssUrl) {
			if (document.createStyleSheet) {
				document.createStyleSheet(cssUrl); // IE way
			} else {
				if ($("head").find("link[href='"+cssUrl+"']").size() == 0) {
					var headElems = document.getElementsByTagName("head");
					var style = document.createElement("link");
					style.type = "text/css";
					style.rel = "stylesheet";
					style.href = cssUrl;
					headElems[headElems.length - 1].appendChild(style);
					// $("head").append($("<link href='" + cssUrl + "' rel='stylesheet' type='text/css' />"));					
				} // else, already added
			}
		};

		/** For debug logging. */
		this.log = function(msg, e) {
			if (typeof console != "undefined" && typeof console.log != "undefined") {
				console.log(msg);
				if (e && typeof e.stack != "undefined") {
					console.log(e.stack);
				}
			}
		};
	}

	return new CDUtils();
})($);