/**
 * Util methods for CloudDrive.
 */
function BrowserUtils() {
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
		(toDocument ? toDocument : document).cookie = name + "=" + encodeURIComponent(value) + expires
				+ "; path=" + (toPath ? toPath : "/") + (toDomain ? "; domain=" + toDomain : "");
	}

	/** Read cookie */
	this.getCookie = function(name, fromDocument) {
		var nameEQ = name + "=";
		var ca = (fromDocument ? fromDocument : document).cookie.split(';');
		for ( var i = 0; i < ca.length; i++) {
			var c = ca[i];
			while (c.charAt(0) == ' ') {
				c = c.substring(1, c.length);
			}
			if (c.indexOf(nameEQ) == 0) {
				return decodeURIComponent(c.substring(nameEQ.length, c.length));
			}
		}
		return null;
	}
};

/**
 * Persistence of deferred tasks. Task expected to be a text with evaluable code. This code will be evaluated
 * in load() method to invoke the registered task on a page load. Tasks stored in cookie for 20min, after this
 * period a task assumed as out dated.
 */
function TaskStore() {
	var COOKIE_NAME = "tasks.exoplatform.org";
	var loaded = false;
	var utils = new BrowserUtils();

	/**
	 * Add on-load callback to the window object.
	 */
	var onLoad = function(fn) {
		if (window.addEventListener) {
			window.addEventListener("load", fn, false); // W3C
		} else if (window.attachEvent) {
			window.attachEvent("onload", fn); // IE8
		} else {
			if (window.onload) {
				var currOnLoad = window.onload;
				var newOnLoad = function() {
					if (currOnLoad)
						currOnLoad();
					fn();
				};
				window.onload = newOnLoad;
			} else {
				window.onload = fn;
			}
		}
	};

	var store = function(tasks) {
		utils.setCookie(COOKIE_NAME, tasks, 20 * 60 * 1000); // 20min
	};

	var removeTask = function(task) {
		var pcookie = utils.getCookie(COOKIE_NAME);
		if (pcookie && pcookie.length > 0) {
			var updated = "";
			var existing = pcookie.split("~");
			for ( var i = 0; i < existing.length; i++) {
				var t = existing[i];
				if (t != task) {
					updated += updated.length > 0 ? "~" + t : t;
				}
			}
			store(updated);
		}
	};

	var addTask = function(task) {
		var pcookie = utils.getCookie(COOKIE_NAME);
		if (pcookie) {
			if (pcookie.indexOf(task) < 0) {
				var tasks = pcookie.length > 0 ? pcookie + "~" + task : task;
				store(tasks);
			}
		} else {
			store(task);
		}
	};

	/**
	 * Load stored tasks.
	 */
	var load = function() {
		// load once per page
		if (loaded)
			return;

		// read cookie and eval each stored code
		var pcookie = utils.getCookie(COOKIE_NAME);
		if (pcookie && pcookie.length > 0) {
			try {
				var tasks = pcookie.split("~");
				for ( var i = 0; i < tasks.length; i++) {
					var task = tasks[i];
					try {
						removeTask(task);
						log("Loading task [" + task + "]");
						eval(task);
					} catch (e) {
						log("Error evaluating task: " + task + ":" + e + ". Skipped.");
					}
				}
			} finally {
				loaded = true;
				log("Tasks loaded.");
			}
		}
	};

	/**
	 * Register task in store.
	 */
	this.add = function(task) {
		if (task) {
			addTask(task);
		} else {
			log("not valid task (code is not defined)");
		}
	};

	/**
	 * Remove task from the store.
	 */
	this.remove = function(task) {
		removeTask(task);
	}

	// constructor logic: load stored tasks after page load
	// setTimeout(function() {
	// onLoad(load);
	// }, 3000);
	$(function() {
		try {
			setTimeout(function() {
				log("Loading deffered tasks");
				load();
			}, 5000);
		} catch (e) {
			log("Error loading tasks", e);
		}
	});
};

// create global instance
if (typeof taskStore == "undefined") {
	taskStore = new TaskStore();
}