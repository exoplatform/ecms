/** For debug logging. It's global function. */
function log(msg, e) {
	if (typeof console != "undefined" && typeof console.log != "undefined") {
		console.log(msg);
		if (e && typeof e.stack != "undefined") {
			console.log(e.stack);
		}
	}
}

/**
 * Load CloudDrive and its dependencies asynchronously and only in top window (not in iframes of gadgets).
 */
if (window == top) {
	var cloudDriveLoader = {}; // global

	function testIf(condition) {
		var tmp = "_testIfCond" + new Date().getMilliseconds();
		try {
			eval("cloudDriveLoader." + tmp + " = " + condition);
			return cloudDriveLoader[tmp];
		} finally {
			delete cloudDriveLoader[tmp];
		}
		return false;
	}

	function waitFor(condition, success, failure, timeout, interval) {
		var isTimeout = false;

		var timer = setTimeout(function() {
			isTimeout = true;
		}, timeout && timeout > 0 ? timeout : 5000);

		var ticker = setInterval(function() {
			if (testIf(condition)) {
				clearInterval(ticker);
				clearTimeout(timer);
				if (success)
					success();
			} else if (isTimeout) {
				clearInterval(ticker);
				if (failure)
					failure();
			}
		}, interval && interval > 0 ? interval : 100);
	}

	function loadScript(scriptUrl, moduleName, doneCallback, failCallback) {
		var cond = "typeof " + moduleName + " != 'undefined'";
		if (moduleName && testIf(cond)) {
			return; // already loaded
		}

		var script = document.createElement("script");
		script.type = "text/javascript";
		script.src = scriptUrl;
		var headElems = document.getElementsByTagName("head");
		headElems[headElems.length - 1].appendChild(script);

		if (moduleName && (doneCallback || failCallback)) {
			if (testIf(cond)) {
				doneCallback();
			} else {
				waitFor(cond, doneCallback, failCallback, 5000, 10);
			}
		}
	}

	function loadStyle(cssUrl) {
		if (document.createStyleSheet) {
			document.createStyleSheet(cssUrl); // IE way
		} else {
			var style = document.createElement("link");
			style.type = "text/css";
			style.rel = "stylesheet";
			style.href = cssUrl;
			var headElems = document.getElementsByTagName("head");
			headElems[headElems.length - 1].appendChild(style);
			// $("head").append($("<link href='" + cssUrl + "' rel='stylesheet' type='text/css' />"));
		}
	}

	setTimeout(function() {
		try {
			// load required styles
			loadStyle("http://ajax.googleapis.com/ajax/libs/jqueryui/1.8/themes/base/jquery-ui.css");
			loadStyle("/cloud-drive/skin/cloud-drive-skin/jquery.pnotify.default.css");
			loadStyle("/cloud-drive/skin/cloud-drive-skin/jquery.pnotify.default.icons.css");
			loadStyle("/cloud-drive/skin/cloud-drive-skin/cloud-drive.css");

			// required local scripts (eXo.loadJS injects script text to the page body, thus we don't use it)
			waitFor("$", function() {
				loadScript("/cloud-drive/js/jquery.pnotify.min.js", "$.pnotify", function() {
					$.pnotify.defaults.styling = "jqueryui"; // use jQuery UI css
					$.pnotify.defaults.history = false; // no history roller in the right corner

					loadScript("/cloud-drive/js/utils.js", "taskStore", function() {
						loadScript("/cloud-drive/js/clouddrive.js", "cloudDrive", function() {
							// ok
						}, function() {
							log("Failed to load CloudDrive core script");
						});
					}, function() {
						log("Failed to load CloudDrive utils");
					});
				}, function() {
					log("Failed to load Pinest Notify");
				});
			}, function() {
				log("Failed to load jQuery");
			});
		} catch (e) {
			log("Error loading CloudDrive ", e);
		}
	}, 2);
}