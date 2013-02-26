/**
 * Stuff grabbed from CW's commons.js
 */
function pageBaseUrl(theLocation) {
	if (!theLocation) {
		theLocation = window.location;
	}

	var theHostName = theLocation.hostname;
	var theQueryString = theLocation.search;

	if (theLocation.port) {
		theHostName += ":" + theLocation.port;
	}

	return theLocation.protocol + "//" + theHostName;
}

var prefixUrl = pageBaseUrl(location);

/**
 * Singleton to handle CloudDrive connections.
 */
function CloudDrive() {
	var contextNode;
	var contextDrive;
	var excluded = {};
	var updateProvider;
	var activeSyncs = []; // array of drives doing synchronization

	var initRequestDefaults = function(request, callbacks) {
		var process = $.Deferred();

		// stuff in textStatus is less interesting: it can be "timeout", "error", "abort", and "parsererror",
		// "success" or smth like that
		request.fail(function(jqXHR, textStatus, err) {
			if (callbacks.fail && jqXHR.status != 309) {
				// check if response isn't JSON
				var data;
				try {
					data = $.parseJSON(jqXHR.responseText);
					if (typeof data == "string") {
						// not JSON
						data = jqXHR.responseText;
					}
				} catch (e) {
					// not JSON
					data = jqXHR.responseText;
				}
				// in err - textual portion of the HTTP status, such as "Not Found" or "Internal Server Error."
				callbacks.fail(data, jqXHR.status, err);
			}
		});
		// hacking jQuery for statusCode handling
		var jQueryStatusCode = request.statusCode;
		request.statusCode = function(map) {
			var user502 = map[502];
			if (!user502 && callbacks.fail) {
				map[502] = function() {
					// treat 502 as request error also
					callbacks.fail("Bad gateway", "error", 502);
				}
			}
			return jQueryStatusCode(map);
		};
		request.done(function(data, textStatus, jqXHR) {
			if (callbacks.done) {
				callbacks.done(data, jqXHR.status, textStatus);
			}
		});
		request.always(function(jqXHR, textStatus) {
			if (callbacks.always) {
				callbacks.always(jqXHR.status, textStatus);
			}
		});
	};

	var initRequest = function(request) {
		var process = $.Deferred();

		// stuff in textStatus is less interesting: it can be "timeout", "error", "abort", and "parsererror",
		// "success" or smth like that
		request.fail(function(jqXHR, textStatus, err) {
			if (jqXHR.status != 309) {
				// check if response isn't JSON
				var data;
				try {
					data = $.parseJSON(jqXHR.responseText);
					if (typeof data == "string") {
						// not JSON
						data = jqXHR.responseText;
					}
				} catch (e) {
					// not JSON
					data = jqXHR.responseText;
				}
				// in err - textual portion of the HTTP status, such as "Not Found" or "Internal Server Error."
				process.reject(data, jqXHR.status, err);
			}
		});
		// hacking jQuery for statusCode handling
		var jQueryStatusCode = request.statusCode;
		request.statusCode = function(map) {
			var user502 = map[502];
			if (!user502 && callbacks.fail) {
				map[502] = function() {
					// treat 502 as request error also
					process.fail("Bad gateway", 502, "error");
				}
			}
			return jQueryStatusCode(map);
		};

		request.done(function(data, textStatus, jqXHR) {
			process.resolve(data, jqXHR.status, textStatus);
		});

		request.always(function(data_jqXHR, textStatus, jqXHR_errorThrown) {
			var status;
			if (data_jqXHR && data_jqXHR.status) {
				status = data_jqXHR.status;
			} else if (jqXHR_errorThrown && jqXHR_errorThrown.status) {
				status = jqXHR_errorThrown.status;
			} else {
				status = 200; // what else we could to do
			}
			process.always(status, textStatus);
		});

		return process.promise();
	};

	// TODO not used currently
	var getProvider = function(providerId, callbacks) {
		var request = $.ajax({
			async : false,// for avoid the popup blocker
			type : "GET",
			url : prefixUrl + "/portal/rest/clouddrive/provider/" + providerId,
			dataType : "json"
		});

		initRequestDefaults(request, callbacks);
	}

	var connectPost = function(workspace, path, callbacks) {
		var request = $.ajax({
			type : "POST",
			url : prefixUrl + "/portal/rest/clouddrive/connect",
			dataType : "json",
			data : {
				workspace : workspace,
				path : path
			},
			xhrFields : {
				withCredentials : true
			}
		});

		return initRequest(request, callbacks);
	}

	var connectInit = function(providerId, callbacks) {
		var request = $.ajax({
			// async : false,
			type : "GET",
			url : prefixUrl + "/portal/rest/clouddrive/connect/init/" + providerId,
			dataType : "json"
		});

		initRequestDefaults(request, callbacks);
	}

	// TODO deprecated
	var authExo = function(key, provider) {
		var request = $.ajax({
			type : "GET",
			url : prefixUrl + "/portal/rest/clouddrive/connect/" + provider.id,
			dataType : "html",
			data : {
				code : key
			}
		});

		initRequestDefaults(request, {
			fail : function(err) {
				log("ERROR: Cloud Drive user authentication failed " + err);
				cloudDriveUI.connectError("User authentication failed. " + err);
			},
			done : function(data) {
				connectNode(provider);
			}
		});
	}

	var getDrive = function(workspace, path, callbacks) {
		var request = $.ajax({
			async : false,
			type : "GET",
			url : prefixUrl + "/portal/rest/clouddrive/drive",
			dataType : "json",
			data : {
				workspace : workspace,
				path : path
			}
		});

		initRequestDefaults(request, callbacks);
	}

	var getFile = function(workspace, path, callbacks) {
		var request = $.ajax({
			async : false,
			type : "GET",
			url : prefixUrl + "/portal/rest/clouddrive/drive/file",
			dataType : "json",
			data : {
				workspace : workspace,
				path : path
			}
		});
		initRequestDefaults(request, callbacks);
	}

	var synchronizePost = function(workspace, path, callbacks) {
		var request = $.ajax({
			async : true, // use false for avoid the popup blocker
			type : "POST",
			url : prefixUrl + "/portal/rest/clouddrive/drive/synchronize",
			dataType : "json",
			data : {
				workspace : workspace,
				path : path
			}
		});

		return initRequest(request, callbacks);
	}

	var serviceGet = function(url) {
		var request = $.ajax({
			async : true,
			type : "GET",
			url : url,
			dataType : "json"
		});
		return initRequest(request);
	}

	var connectDrive = function(providerId, authUrl) {
		var authWindow;
		var auth;
		if (authUrl) {
			// use user interaction for authentication
			authWindow = cloudDriveUI.connectDriveWindow(authUrl);
		} else {
			// function to call for auth using authUrl from provider
			auth = serviceGet;
		}

		// 1 initialize connect workflow
		var process = $.Deferred();
		connectInit(providerId, {
			done : function(provider) {
				log(provider.name + " connect initialized.");
				if (auth) {
					auth(provider.authUrl);
				}
				// 2 wait for authentication
				waitAuth(provider, {
					done : function() {
						log(provider.name + " user authenticated.");
						// 3 and finally connect the drive
						var userNode = getConnectNode();
						if (userNode) {
							log("Connecting Cloud Drive to node " + userNode.path + " in " + userNode.workspace);

							var post = connectPost(userNode.workspace, userNode.path);
							post.done(function(state, status) {
								log("Connect requested: " + status + ". ");
								if (state) {
									if (status == 201) {
										log("DONE: " + provider.name + " just connected.");
										contextDrive = state.drive;
										process.resolve(state);
									} else if (status == 202) {
										var check = connectCheck(state.serviceUrl);
										check.fail(function(error) {
											process.reject(error);
										});
										check.progress(function(state) {
											process.notify(state);
										});
										check.done(function(state) {
											contextDrive = state.drive;
											process.resolve(state);
										});
									} else {
										log("WARN: unexpected state returned from connect service " + status);
									}
								} else {
									log("ERROR: " + provider.name + " connect return null state.");
									process.reject("Cannot connect " + provider.name + ". Server return empty response.");
								}
							});
							post.fail(function(state, error, errorText) {
								log("ERROR: " + provider.name + " connect failed: " + error + ". ");
								// JSON.stringify(state));
								if (typeof state === "string") {
									process.reject(state);
								} else {
									process.reject(state && state.error ? state.error : error + " " + errorText);
								}
							});
						} else {
							process.reject("Connect to " + provider.name + " canceled.");
						}
					},
					error : function(error) {
						log("ERROR: " + provider.name + " authentication error: " + error);
						process.reject(error);
					},
					timeout : function() {
						log("ERROR: " + provider.name + " user not authenticated in 2 minutes.");
						process.reject("Authentication timeout.");
					}
				});
			},
			fail : function(error) {
				log("ERROR: Connect to Cloud Drive cannot be initiated. " + error);
				if (authWindow && !authWindow.closed) {
					authWindow.close();
				}
				process.reject(error);
			}
		});
		return process.promise();
	};

	var waitAuth = function(provider, callbacks) {
		var i = 0;
		var intervalId = setInterval(function() {
			var connectId = getCookie("cloud-drive-connect-id");
			if (connectId) {
				intervalId = clearInterval(intervalId);
				callbacks.done();
			} else {
				var error = getCookie("cloud-drive-error");
				if (error) {
					intervalId = clearInterval(intervalId);
					callbacks.error(error);
				} else if (i > 120) {
					// if open more 2min - close it and tread as not authenticated/allowed
					intervalId = clearInterval(intervalId);
					callbacks.timeout();
				}
			}
			i++;
		}, 1000);
	}

	// TODO Deprecated
	var connectNode = function(provider, callbacks) {
		var userNode = getConnectNode();
		log("Connecting Cloud Drive to node " + userNode.path + " in " + userNode.workspace);
		connectPost(userNode.workspace, userNode.path, {
			done : function(state, status) {
				log("Connect requested: " + status + ". ");
				// JSON.stringify(state));
				if (status == 202) {
					// connect in progress
					callbacks.started(state);
					connectCheck(state.serviceUrl, callbacks);
				} else if (status == 201) {
					// drive connected
					callbacks.done(state);
				} else {
					log("WARN: unexpected state returned from connect service " + status);
				}
			},
			fail : function(state, error, errorText) {
				log("Connect error: " + error + ". " + JSON.stringify(state));
				if (typeof state === "string") {
					callbacks.error(state);
				} else {
					callbacks.error(state && state.error ? state.error : error + " " + errorText);
				}
			}
		});
	}

	var connectCheck = function(checkUrl) {
		var process = $.Deferred();
		var serviceUrl = checkUrl;
		// if Accepted start Interval to wait for Created
		var intervalId = setInterval(function() {
			// use serviceUrl to check until 201/200 will be returned or an error
			var check = serviceGet(serviceUrl);
			check.done(function(state, status) {
				if (status == "204") {
					// No content - not a cloud drive or drive not connected, or not to this user.
					// This also might mean an error as connect not active but the drive not connected.
					process.reject("Drive not connected. Check if no other connection active and try again.");
				} else if (state && state.serviceUrl) {
					serviceUrl = state.serviceUrl;
					if (status == "201" || status == "200") {
						// created or ok - drive successfully connected
						// or appears as already connected (by another request)
						process.resolve(state);
						log("DONE: " + status + " " + state.drive.provider.name + " connected successfully.");
					} else if (status == "202") {
						// else inform progress and continue to wait created
						process.notify(state);
						log("PROGRESS: " + status + " " + state.drive.provider.name + " connectCheck progress "
								+ state.progress);
					} else {
						// unexpected status, wait for created
						log("WARN: unexpected status in connectCheck:" + status);
					}
				} else {
					log("ERROR: " + status + " connectCheck return wrong state.");
					var driveName;
					if (state.drive && state.drive.provider && state.drive.provider.name) {
						driveName = state.drive.provider.name;
					} else {
						driveName = "Cloud Drive";
					}
					process.reject("Cannot connect " + driveName + ". Server return wrong state.");
				}
			});
			check.fail(function(state, error, errorText) {
				log("ERROR: Connect check error: " + error + ". " + JSON.stringify(state));
				if (typeof state === "string") {
					process.reject(state);
				} else {
					process.reject(state && state.error ? state.error : error + " " + errorText);
				}
			});
		}, 3333);

		// finally clear interval
		process.always(function() {
			intervalId = clearInterval(intervalId);
		});

		return process.promise();
	};

	var getPortalUser = function() {
		return eXo.env.portal.userName;
	}

	var getConnectNode = function() {
		if (contextNode) {
			// using predefined
			var cn = contextNode;
			contextNode = null;
			return cn;
		}
	}

	var setConnectNode = function(connectNode) {
		contextNode = connectNode;
	}

	var getFileLink = function(nodePath) {
		var file = contextDrive.files[nodePath];
		return file ? file.link : null;
	}

	var resetContext = function() {
		contextNode = null;
	};

	var addExcluded = function(path) {
		excluded[path] = true;
	};

	var isExcluded = function(path) {
		return excluded[path] === true;
	};

	/**
	 * Synchronize documents view.
	 */
	this.synchronize = function(elem, objectId) {
		try {
			if (contextNode) {
				var nodePath = contextNode.path;
				var nodeWorkspace = contextNode.workspace;
				log("Synchronizing Cloud Drive on " + nodeWorkspace + ":" + nodePath);

				var process = $.Deferred();
				cloudDriveUI.synchronizeProcess(process.promise());

				var initiator = $.Deferred();
				initiator.done(function() {
					// sync and load all files related to this drive
					var sync = synchronizePost(nodeWorkspace, nodePath);
					sync.contextDrive = contextDrive;
					activeSyncs.push(sync);
					sync.done(function(drive) {
						var files = 0;
						var folders = 0;
						var changed = 0;
						for ( var fpath in drive.files) {
							if (drive.files.hasOwnProperty(fpath)) {
								changed++;
								if (drive.files[fpath].folder) {
									folders++;
								} else {
									files++;
								}
							}
						}

						// copy already cached but not synced files to the new drive
						for ( var fpath in sync.contextDrive.files) {
							if (!drive.files[fpath]) {
								drive.files[fpath] = sync.contextDrive.files[fpath];
							}
						}

						log("DONE: Synchronized " + changed + " changes from Cloud Drive on " + nodeWorkspace + ":"
								+ nodePath);

						if (changed > 0 && sync.contextDrive == contextDrive) {
							// using new drive in the context (only if context wasn't changed)
							contextDrive = drive;
						}

						process.resolve(files, folders, drive);
					});
					sync.fail(function(response, status, err) {
						log("ERROR: synchronization error: " + err + ", " + status + ", " + response);
						if (status == 403 && response.id) {
							updateProvider = response;
						}
						process.reject(response, status);
					});
					sync.always(function() {
						// cleanup
						for ( var i = 0, asize = activeSyncs.length; i < asize; ++i) {
							if (activeSyncs[i] == sync) {
								activeSyncs.splice(i, 1);
								break;
							}
						}
					});
				});

				// start work here (registered done() will be called)
				if (updateProvider) {
					// previous attempt tells us we have t oupdate access keys - reconnect
					var connect = connectDrive(updateProvider.id, updateProvider.authUrl);
					connect.done(function(state) {
						initiator.resolve();
					});
				} else {
					initiator.resolve();
				}
			} else {
				log("WARN Nothing to synchronize!");
				cloudDriveUI.refreshDocuments(); // refresh WCM explorer
			}
		} finally {
			resetContext();
		}
	};

	/**
	 * Connect to Cloud Drive.
	 */
	this.connect = function(providerId, authUrl, userNode, userWorkspace) {
		log("Connecting to Cloud Drive...");

		// set connect node if have an one
		if (userNode && userWorkspace) {
			setConnectNode({
				workspace : userWorkspace,
				path : userNode
			});
		}

		// reset previous drive context
		contextDrive = null;
		excluded = {};

		var process = connectDrive(providerId, authUrl);
		cloudDriveUI.connectProcess(process);
	};

	this.state = function(checkUrl) {
		return connectCheck(checkUrl);
	}

	this.initContext = function(nodeWorkspace, nodePath) {
		// log("Init context node: " + nodeWorkspace + ":" + nodePath
		// + (contextDrive ? ", drive: " + contextDrive.path : "") + " excluded: " + isExcluded(nodePath));

		if (isExcluded(nodePath)) {
			return; // already cached as not in drive
		}

		// XXX do this to support symlinks outside the drive
		var newContext;
		if (contextDrive) {
			var file = contextDrive.files[nodePath];
			if (file) {
				newContext = {
					workspace : nodeWorkspace,
					path : nodePath
				};
			}
		}

		if (newContext) {
			contextNode = newContext;
		} else if (contextDrive && nodePath.indexOf(contextDrive.path) == 0) {
			var file = contextDrive.files[nodePath];
			if (file || nodePath == contextDrive.path) {
				// log(">>> Init context node, cached");
				contextNode = {
					workspace : nodeWorkspace,
					path : nodePath
				};
			} else {
				// get the file from the server and cache it locally
				// log(">>> Init context node, get file");
				getFile(nodeWorkspace, nodePath, {
					fail : function(err, status) {
						log("ERROR: Cloud Drive file " + nodeWorkspace + ":" + nodePath + " cannot be read: " + err
								+ " (" + status + ")");
						CWUtil.showError("Error reading drive file", err);
					},
					done : function(file, status) {
						if (status != 204) {
							contextNode = {
								workspace : nodeWorkspace,
								path : nodePath
							};
							contextDrive.files[nodePath] = file;
						} else {
							// log("Not a cloud file: " + nodePath); // it's not a Cloud Drive file
							addExcluded(nodePath);
							resetContext();
						}
					}
				});
			}
		} else {
			// log(">>> Init context node, get drive");
			// load all files related to this drive
			getDrive(nodeWorkspace, nodePath, {
				fail : function(err, status) {
					log("ERROR: Cloud Drive " + nodeWorkspace + ":" + nodePath + " cannot be read: " + err + " ("
							+ status + ")");
					CWUtil.showError("Error reading drive", err);
				},
				done : function(drive, status) {
					if (status != 204) {
						contextNode = {
							workspace : nodeWorkspace,
							path : nodePath
						};
						if (contextDrive && contextDrive.path == drive.path) {
							// XXX same drive, probably nodePath is a symlink path,
							// use already cached files with new drive
							for ( var fpath in contextDrive.files) {
								if (contextDrive.files.hasOwnProperty(fpath) && !drive.files.hasOwnProperty(fpath)) {
									drive.files[fpath] = contextDrive.files[fpath];
								}
							}
						}
						contextDrive = drive;
					} else {
						// log("Not a cloud drive: " + nodePath); // it's not a Cloud Drive
						addExcluded(nodePath);
						resetContext();
					}
				}
			});
		}
	};

	this.getContextNode = function() {
		return contextNode;
	};

	this.getContextDrive = function() {
		return contextDrive;
	};

	this.getContextFile = function() {
		if (contextNode) {
			var file = contextDrive.files[contextNode.path];
			if (file) {
				return file;
			}
		}
		return null;
	};

	this.isContextSymlink = function() {
		if (contextNode) {
			var file = contextDrive.files[contextNode.path];
			if (file && file.symlink) {
				return true;
			}
		}
		return false;
	};

	this.openFile = function(elem, objectId) {
		var file = cloudDrive.getContextFile();
		try {
			if (file) {
				window.open(file.link);
			} else {
				log("No context path to open as Cloud File");
			}
		} finally {
			resetContext();
		}
	};
}

function CloudDriveUI() {
	var MENU_OPEN_FILE = "OpenCloudFile";
	var MENU_REFRESH_DRIVE = "RefreshCloudDrive";
	var DRIVE_MENU_ACTIONS = [ MENU_OPEN_FILE, MENU_REFRESH_DRIVE ];
	var ALLOWED_DRIVE_MENU_ACTIONS = [ MENU_OPEN_FILE, MENU_REFRESH_DRIVE, "Delete", "AddToFavourite",
			"RemoveFromFavourite", "ViewInfo" ];
	var ALLOWED_FILE_MENU_ACTIONS = [ MENU_OPEN_FILE, MENU_REFRESH_DRIVE, "AddToFavourite",
			"RemoveFromFavourite", "ViewInfo" ];
	var ALLOWED_SYMLINK_MENU_ACTIONS = [ "Delete" ];

	var ALLOWED_DMS_MENU_COMMON_ACTION_CLASSES = [ "ManageRelationsIcon", "ManageActionsIcon",
			"ViewPropertiesIcon", "ManageAuditingIcon", "OverloadThumbnailIcon" ];
	var ALLOWED_DMS_MENU_FILE_ACTION_CLASSES = [ "TaggingDocumentIcon", "WatchDocumentIcon",
			"ViewMetadatasIcon", "VoteIcon", "CommentIcon" ];
	var ALLOWED_DMS_MENU_DRIVE_ACTION_CLASSES = [ "DeleteNodeIcon" ];

	var getAllowedItems = function(menu, items, allowed) {
		var newParams = "";
		$.each(items, function(i, item) {
			if (allowed.indexOf(item) >= 0) {
				newParams = newParams + "," + item;
			}
		});

		if (menu) {
			// remove custom items from the menu
			var file = cloudDrive.getContextFile();
			var link;
			if (file) {
				link = file.link;
			} else {
				link = window.location;
			}

			$(menu).find("div a.MenuItem").each(function(n) {
				var div = $(this).find("div.Permlink16x16Icon");
				if (div) {
					$(this).attr("href", link);
				}

				div = $(this).find("div.GetURL16x16Icon");
				if (div) {
					var a = $(this).parent("a");
					$(a).click("eXo.ecm.ECMUtils.pushToClipboard(event,'" + link + "')");
					$(a).attr("path", link);
					// TODO check with flash zclib
				}

				div = $(this).find("div.WebDAV16x16Icon");
				if (div) {
					// same as OpenCloudFile
					$(this).attr("path", link);
					$(this).attr("href", "javascript:cloudDrive.openFile(this);");
				}
			});
		}
		return newParams;
	};

	var removeCloudItems = function(items) {
		var newParams;
		$.each(items, function(i, item) {
			if (DRIVE_MENU_ACTIONS.indexOf(item) < 0) {
				newParams = (newParams ? newParams + "," + item : item);
			}
		});
		return newParams;
	};

	var cloudMenuActions = function(objId, menu, items, allowedItems) {
		var i = objId.indexOf(":");
		var workspace;
		var path;
		if (i > 0 && i < objId.length - 1) {
			workspace = objId.slice(0, i);
			path = objId.slice(i + 1);
		} else {
			// shouldn't happen
			workspace = "";
			path = objId;
		}

		cloudDrive.initContext(workspace, path);

		var menuItems = items.split(",");

		var drive = cloudDrive.getContextDrive();
		if (drive) {
			var tipText = "Open on " + drive.provider.name;
			$("a[exo\\:attr='" + MENU_OPEN_FILE + "']").attr("title", tipText).attr("alt", tipText);

			// ALLOWED_SYMLINK_MENU_ACTIONS
			if (cloudDrive.isContextSymlink()) {
				allowedItems = allowedItems.concat(ALLOWED_SYMLINK_MENU_ACTIONS);
			}

			// fix menu: keep only allowed items
			return getAllowedItems(menu, menuItems, allowedItems);
		} else {
			// if not cloud file on context path - remove OpenCloudFile from the menu
			return removeCloudItems(menuItems);
		}
	};

	var initDocument = function() {
		var selected = selectedNode();
		// log("selected: " + (selected ? selected.workspace + ":" + selected.path : selected));
		if (selected) {
			var cdActions = "";
			$.each(DRIVE_MENU_ACTIONS, function(i, action) {
				cdActions += (cdActions ? ", " : "") + "a[exo\\:attr='" + action + "']";
			});

			if ($("#ECMContextMenu").find(cdActions).size() > 0) {
				// if have CD actions in context menu... we're have cloud drive in the view
				// XXX this will work until the CD is in root of Personal Docs, otherwise menu might not have CD items
				// if CD node under one of subnodes.

				cloudDrive.initContext(selected.workspace, selected.path);
				var drive = cloudDrive.getContextDrive();
				if (drive) {
					var classes;
					if (drive.path == selected.path) {
						// it's drive in the context
						classes = ALLOWED_DMS_MENU_COMMON_ACTION_CLASSES.concat(ALLOWED_DMS_MENU_DRIVE_ACTION_CLASSES);
					} else if (drive.files[selected.path]) {
						// it's drive's file
						classes = ALLOWED_DMS_MENU_COMMON_ACTION_CLASSES.concat(ALLOWED_DMS_MENU_FILE_ACTION_CLASSES);
					} else {
						// selected node not a cloud drive or its file
						return;
					}

					var allowed = "";
					$.each(classes, function(i, action) {
						allowed += (allowed ? ", ." : ".") + action;
					});

					// Action Bar items (depends on file/folder or the drive itself in the context)
					$("#DMSMenuItemContainer a.SubTabIcon").not(allowed).each(function(i, btn) {
						$(btn).attr("style", "cursor:default; opacity:0.2; filter: alpha(opacity = 20)");
						$(btn).attr("href", "#");
					});

					// File Viewer
					var viewer = $("#CloudFileViewer");
					if (viewer.size() > 0) {
						var file = cloudDrive.getContextFile();
						$(viewer).data("cloudFile", file);

						// <span style="font-weight: normal; font-size: 11px;"><a href="#">(Edit)</a></span>

						var vswitch = $("#ViewerSwitch");
						if (vswitch.size() > 0) {
							var openOnProvider = $(viewer).attr("file-open-on");
							var fileNav = $("div.FileContent div.TopNavContent");
							$(fileNav).find("div.TopTitle").text(file.title);
							if (file.previewLink) {
								// init preview/edit view
								$(viewer).find("iframe").attr("src", file.previewLink);
								$(fileNav).find("div.TopTitle").append(vswitch);
								$(viewer).find("div").show();
							} else {
								// TODO init simple view (p>a)
								// var vp = $(viewer).find("p");
								// $(vp).find("a").each(function() {
								// $(this).attr("title", openOnProvider);
								// $(this).attr("href", file.link);
								// $(this).text(file.title);
								// });
								// $(vp).show();
								// init preview/edit view
								$(viewer).find("iframe").attr("src", file.link);
								$(vswitch).remove();
								$(viewer).find("div").show();
							}

							$(fileNav).find("div.ActionButton a").each(function() {
								$(this).attr("href", file.link);
								$(this).attr("target", "_blank");
								$(this).text(openOnProvider)
								$(this).parent().addClass("OpenOnCloud");
							});
						}
					}
				} // else not a cloud drive or its file
			}
		}
	};

	/**
	 * Update menu items with provider name
	 */
	var addProviderName = function(menuId, selector) {
		var href = $("#" + menuId).find(selector);
		if (!href.attr("exo:text"))
			href.attr("exo:text", href.text());

		var drive = cloudDrive.getContextDrive();
		if (drive) {
			href.text(href.attr("exo:text") + drive.provider.name);
		}
	};

	/**
	 * Find link to open Personal Documents view in WCM. Can return nothing if current page doesn't contain such
	 * element.
	 */
	var personalDocumentsLink = function() {
		var link = $(".BarContent div[title='Personal Documents']");
		if (link.size() > 0) {
			return link.attr("onclick");
		}
	};

	/**
	 * Refresh WCM view.
	 */
	var refresh = function(nodePath) {
		// TODO need refresh but only if user didn't change the view, jQuery selector does this

		// eXo.ecm.ECMUtils.focusCurrentNodeInTree(xxx);
		// eXo.ecm.UIListView.allItems
		// eXo.ecm.WCMUtils.getCurrentNodes()
		// $(".BarContent div[title='Personal Documents']").click();
		$(".BarContent div[title='Personal Documents']").click();
	};

	/**
	 * Return node selected in UI or undefined if nothing selected.
	 */
	var selectedNode = function() {
		var selected = $(".SelectedNode div");
		if (selected.size() > 0) {
			// using Tree Explorer selection
			var objectId = $(selected).attr("objectid");
			var path = decodeURIComponent(objectId).split("+").join(" ");
			var workspace = $(selected).attr("workspacename");
			var element = $(selected).find(".NodeLabel a");
			if (workspace && path) {
				return {
					workspace : workspace,
					path : path,
					element : element
				};
			}
		} else {
			// using Connect Dialog (not used)
			var node = $("#CloudDriveConnectDialog");
			if (node.size() > 0) {
				var path = $(node).attr("user-node");
				var workspace = $(node).attr("user-workspace");
				if (workspace && path) {
					return {
						workspace : workspace,
						path : path
					};
				}
			}
		}
		return undefined;
	};

	this.connectState = function(checkUrl, docsUrl, docsOnclick) {
		var state = cloudDrive.state(checkUrl);
		state.done(function(state) {
			var message;
			if (docsUrl) {
				message = "<div>Find your drive in <a href='" + docsUrl + "'";
				if (docsOnclick) {
					message += " onclick='" + docsOnclick + "'";
				}
				message += "'>Personal Documents</div>";
			} else {
				message = "Find your drive in Personal Documents";
			}
			log("message: " + message);

			$.pnotify({
				title : "Your " + state.drive.provider.name + " connected!",
				type : "success",
				text : message,
				icon : "picon picon-task-complete",
				hide : true,
				closer : true,
				sticker : false,
				opacity : 1,
				shadow : true,
				width : $.pnotify.defaults.width
			});
		});
		state.fail(function(state) {
			var message;
			if (state.drive && state.drive.provider) {
				message = "Error connecting your " + state.drive.provider.name;
			} else {
				message = "Error connecting your drive";
			}
			$.pnotify({
				title : message,
				text : state.error,
				type : "error",
				hide : true,
				closer : true,
				sticker : false,
				icon : 'picon picon-dialog-error',
				opacity : 1,
				shadow : true,
				width : $.pnotify.defaults.width
			});
		});
	};

	/**
	 * UI support for connect deferred process.
	 */
	this.connectProcess = function(process) {
		var driveName = "";
		var progress = 0;
		var task;
		var hideTimeout;

		// pnotify notice
		var notice = $.pnotify({
			title : "Authorizing...",
			type : "info",
			icon : "picon picon-throbber",
			hide : false,
			closer : true,
			sticker : false,
			opacity : .75,
			shadow : false,
			width : $.pnotify.defaults.width,
			nonblock : true,
			nonblock_opacity : .25
		});

		// show close buton in 1min
		var removeNonblock = setTimeout(function() {
			notice.pnotify({
				nonblock : false
			});
		}, 60000);

		var update = function() {
			var options = {
				text : progress + "% complete."
			};
			if (progress >= 75) {
				options.title = "Almost Done...";
			}
			if (progress >= 100) {
				options.title = driveName + " Connected!";
				options.type = "success";
				options.hide = true;
				options.closer = true;
				options.sticker = false;
				options.icon = "picon picon-task-complete";
				options.opacity = 1;
				options.shadow = true;
				options.width = $.pnotify.defaults.width;
				// options.min_height = "300px";
				options.nonblock = false; // remove non-block
			}
			notice.pnotify(options);
		};

		process.progress(function(state) {
			if (!task) {
				// start progress
				var docsUrl = ", \"" + location + "\"";
				var docsOnclick = personalDocumentsLink();
				docsOnclick = docsOnclick ? ", \"" + docsOnclick + "\"" : "";
				task = "CWUtil.loadIfUndefined(\"cloudDriveUI\", \"/cloud-workspaces-drives/js/clouddrive.js\", "
						+ "function() {cloudDriveUI.connectState(\"" + state.serviceUrl + "\"" + docsUrl + docsOnclick
						+ ");}, 500);";
				CWTasks.add(task);

				progress = state.progress;
				driveName = state.drive.provider.name;

				notice.pnotify({
					title : "Connecting Your " + driveName,
					text : progress + "% complete."
				});

				// hide title in 4sec
				hideTimeout = setTimeout(function() {
					notice.pnotify({
						title : false,
						width : "200px"
					});
				}, 4000);
			} else {
				// continue progress
				progress = state.progress < 100 ? state.progress : 99;
			}
			update();
		});

		process.done(function(state) {
			if (hideTimeout) {
				clearTimeout(hideTimeout);
			}

			// wait a bit for JCR/WCM readines
			setTimeout(function() {
				// update progress
				progress = 100;
				update();
				refresh(state.drive.path);
			}, 4000);
		});

		process.always(function() {
			if (task) {
				CWTasks.remove(task);
			}
		});

		process.fail(function(error) {
			if (hideTimeout) {
				clearTimeout(hideTimeout);
			}

			var options = {
				text : error,
				title : "Error connecting " + (driveName ? driveName : "drive") + "!",
				type : "error",
				hide : false,
				delay : 0,
				closer : true,
				sticker : false,
				icon : "picon picon-process-stop",
				opacity : 1,
				shadow : true,
				width : $.pnotify.defaults.width,
				// remove non-block
				nonblock : false
			};
			notice.pnotify(options);
		});
	};

	/**
	 * UI support for synchronization deferred process.
	 */
	this.synchronizeProcess = function(process) {
		process.done(function(files, folders, drive) {
			var ahref = "javascript:cloudDriveUI.openDrive('" + drive.title + "')";
			var driveLink = "<span><a href=\"" + ahref
					+ "\" style=\"curson: pointer; border-bottom: 1px dashed #999; display: inline;\">" + drive.email
					+ "</a></span>"
			var details;
			if (files + folders > 0) {
				// Don't refresh at all, as user can change the view. Istead we show a link on the message.
				// TODO cloudDriveUI.openDrive(drive.title);
				// TODO cloudDriveUI.refreshDocuments();

				var details;
				if (files > 0) {
					details = files + " file" + (files > 1 ? "s" : "");
				}
				if (folders > 0) {
					folders = folders + " folder" + (folders > 1 ? "s" : "");
					details = (details ? details + " and " + folders : folders);
				}
				if (details) {
					details = details + " updated on " + driveLink + " drive.";
				} else {
					details = "Drive " + driveLink + " successfuly updated.";
				}
				var titleLink = "<span><a href=\"" + ahref + "\">" + drive.provider.name
						+ " Synchronized.</a></span>"
				CWUtil.showInfo(titleLink, details);
			} else {
				var titleLink = "<span><a href=\"" + ahref + "\">" + drive.provider.name
						+ " Already Up To Date.</a></span>"
				CWUtil.showInfo(titleLink, "Files on " + driveLink + " are in actual state.");
			}
		});
		process.fail(function(response, status, err) {
			if (status == 403 && response.name) {
				// assuming provider object in response
				CWUtil.showWarn("Error Synchronizing with " + response.name,
						"<span>Access rewoked or outdated. Start <a href='javascript:cloudDrive.synchronize(this)'>"
								+ "Synchronization</a> again to renew access.</span>");
			} else {
				CWUtil.showError("Error Synchronizing Drive", response + " (" + status + ")");
			}
		});
	}

	/**
	 * Refresh WCM explorer documents.
	 */
	this.refreshDocuments = function(currentNodePath) {
		refresh(currentNodePath);
	}

	/**
	 * Open or refresh drive node in WCM explorer.
	 */
	this.openDrive = function(title) {
		// XXX all titles in WCM tree ends with single space
		var selected = $("#UITreeExplorer .Node .NodeLabel a[title='" + title + " ']");
		if (selected.size() > 0) {
			$(selected).click();
		} else {
			log("WARN: drive node '" + title + "' not found");
		}
	};

	this.swicthFileViewer = function() {
		var viewer = $("#CloudFileViewer");
		if (viewer.size() > 0) {
			var file = $(viewer).data("cloudFile");
			var vswitch = $("#ViewerSwitch");
			if (file.previewLink) {
				// init preview/edit view
				var currentLink = viewer.find("iframe").attr("src");
				if (currentLink == file.previewLink) {
					// switch to editor
					viewer.find("iframe").attr("src", file.link);
					var viewerTitle = $(vswitch).attr("view-title");
					vswitch.find("a").text(viewerTitle);
				} else {
					// switch to viewer
					viewer.find("iframe").attr("src", file.previewLink);
					var editTitle = $(vswitch).attr("edit-title");
					vswitch.find("a").text(editTitle);
				}
			}
		}
	};

	/**
	 * Return currently selected node in JCR Explorer. Can be undefined.
	 */
	this.getSelectedNode = function() {
		return selectedNode();
	};

	/**
	 * Open pop-up for Cloud Drive authentication.
	 */
	this.connectDriveWindow = function(authUrl) {
		var w = 850;
		var h = 500;
		var left = (screen.width / 2) - (w / 2);
		var top = (screen.height / 2) - (h / 2);
		return window.open(authUrl, 'contacts', 'width=' + w + ',height=' + h + ',top=' + top + ',left=' + left);
	};

	/**
	 * Init all UI (dialogs, menus, views etc). Called on browser load.
	 */
	this.init = function() {
		// init on each document reload (incl. ajax calls)
		initDocument();

		// tuning of on-file context menu
		if (UIRightClickPopupMenu && typeof UIRightClickPopupMenu.prototype.__cw_overridden == "undefined") {
			var clickRightMouse_orig = UIRightClickPopupMenu.prototype.clickRightMouse;
			UIRightClickPopupMenu.prototype.clickRightMouse = function(event, elemt, menuId, objId, params, opt) {
				if (params.indexOf(MENU_OPEN_FILE) >= 0) {
					params = cloudMenuActions(objId, elemt, params, ALLOWED_FILE_MENU_ACTIONS);
					addProviderName(menuId, "a.OpenCloudFile16x16Icon");
				} else if (params.indexOf(MENU_REFRESH_DRIVE) >= 0) {
					params = cloudMenuActions(objId, elemt, params, ALLOWED_DRIVE_MENU_ACTIONS);
					addProviderName(menuId, "a.RefreshCloudDrive16x16Icon");
				}
				clickRightMouse_orig(event, elemt, menuId, objId, params, opt);
			};

			UIRightClickPopupMenu.prototype.__cw_overridden = true;
		}

		if (ListView && typeof ListView.prototype.__cw_overridden == "undefined") {
			// based on code from UIListView.js
			function selectedFiles(drivePath) {
				var itemsSelected = eXo.ecm.UIListView.itemsSelected;
				if (!itemsSelected || itemsSelected.length == 0) {
					itemsSelected = eXo.ecm.UISimpleView ? eXo.ecm.UISimpleView.itemsSelected : undefined;
				}

				var files = [];

				if (itemsSelected && itemsSelected.length) {
					for ( var i in itemsSelected) {
						if (Array.prototype[i]) {
							continue;
						}

						var currentNode = itemsSelected[i];
						var path = currentNode.getAttribute("objectId");
						// TODO var wsname = currentNode.getAttribute("workspaceName");
						if (path) {
							path = decodeURIComponent(path).split("+").join(" ");
							var drive = cloudDrive.getContextDrive();
							if (drive && path.indexOf(drivePath) == 0) {
								files.push(currentNode);
							}
						}
					}
				}
				return files;
			}

			// don't move files outside the drive but allow to symlink them (drag with ctrl+shift)
			var postGroupAction_orig = ListView.prototype.postGroupAction;
			ListView.prototype.postGroupAction = function(moveActionNode, ext) {
				if (eXo.ecm.UIListView.enableDragAndDrop == "true") {
					var actionEvent = window.event;
					// if not ctrl+shift (create symlink)...
					if (!(actionEvent.ctrlKey && actionEvent.shiftKey)) {
						var leftClick = !((actionEvent.which && actionEvent.which > 1) || (actionEvent.button && actionEvent.button == 2));
						var drive = cloudDrive.getContextDrive();
						if (leftClick && drive) {
							var files = selectedFiles(drive.path);
							if (files.length > 0) {
								// ... and if left click (dragging) with selected cloud files,
								// unselected cloud file elements and cancel this method
								for ( var i = 0, fsize = files.length; i < fsize; ++i) {
									var f = files[i];
									f.isSelect = false;
									f.selected = null;
									f.style.background = "none";
								}
								return;
							}
						}
					}
				}
				postGroupAction_orig(moveActionNode, ext);
			};

			// hide ground-context menu for drive folder
			var showGroundContextMenu_orig = ListView.prototype.showGroundContextMenu;
			ListView.prototype.showGroundContextMenu = function(event, element) {
				var drive = cloudDrive.getContextDrive();
				var selected = selectedNode();
				if (drive && selected && selected.path.indexOf(drive.path) == 0) {
					return;
				} else {
					showGroundContextMenu_orig(event, element);
				}
			};

			ListView.prototype.__cw_overridden = true;
		}

		// connect dialog (not used)
		$("#UIPopupWindow").bind("beforeShow", function() {
			$("#CloudDriveConnectDialogMessage").hide();
		});
		$("#CloudDriveConnectDialog a").each(function(n) {
			this.alt = $(this).text();
		});
	};
}

if (typeof cloudDrive == "undefined") {
	cloudDrive = new CloudDrive();
}

if (typeof cloudDriveUI == "undefined") {
	cloudDriveUI = new CloudDriveUI(cloudDrive);
}

$(function() {
	// workaround to let portal-utils.js load and init
	setTimeout(function() {
		cloudDriveUI.init();
	}, 250);
});
