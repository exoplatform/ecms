/**
 * Cloud Drive Connector's client.
 */
(function($, utils, tasks, uiRightClickPopupMenu, uiListView, uiSimpleView, uiFileView) {

	// Error constants
	var ACCESS_DENIED = "access-denied";
	var NOT_CLOUD_DRIVE = "not-cloud-drive";
	var DRIVE_REMOVED = "drive-removed";
	var NODE_NOT_FOUND = "node-not-found";
	var NOT_CLOUD_DRIVE = "not-cloud-drive";
	var NOT_CLOUD_FILE = "not-cloud-file";
	var NOT_YET_CLOUD_FILE = "not-yet_cloud-file"; 

	/**
	 * Connector core class.
	 */
	function CloudDrive() {
		
		var prefixUrl = utils.pageBaseUrl(location);

		// Node workspace and path currently open in ECMS explorer view
		var currentNode; 
		// Node workspace and path currently selected in ECMS explorer (currently open or clicked in context menu)
		var contextNode; 
		var providers = {}; // for Provider's id and authURL
		// Cloud Drive associated with current context node
		var contextDrive;
		var excluded = {};
		var updateProvider;
		var activeSyncs = []; // array of drives doing synchronization
		var autoSyncs = {}; // active auto-synchronization jobs

		/** 
		 * Deprecated initialization of ajax request. Use initRequest() instead.
		 * */
		var initRequestDefaults = function(request, callbacks) {
			// stuff in textStatus is less interesting: it can be "timeout",
			// "error", "abort", and "parsererror",
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
					} catch(e) {
						// not JSON
						data = jqXHR.responseText;
					}
					// in err - textual portion of the HTTP status, such as "Not
					// Found" or "Internal Server Error."
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
					};
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

			// stuff in textStatus is less interesting: it can be "timeout",
			// "error", "abort", and "parsererror",
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
					} catch(e) {
						// not JSON
						data = jqXHR.responseText;
					}
					// in err - textual portion of the HTTP status, such as "Not
					// Found" or "Internal Server Error."
					process.reject(data, jqXHR.status, err, jqXHR);
				}
			});
			// hacking jQuery for statusCode handling
			var jQueryStatusCode = request.statusCode;
			request.statusCode = function(map) {
				var user502 = map[502];
				if (!user502) {
					map[502] = function() {
						// treat 502 as request error also
						process.fail("Bad gateway", 502, "error");
					};
				}
				return jQueryStatusCode(map);
			};

			request.done(function(data, textStatus, jqXHR) {
				process.resolve(data, jqXHR.status, textStatus, jqXHR);
			});

			request.always(function(data, textStatus, errorThrown) {
				var status;
				if (data && data.status) {
					status = data.status;
				} else if (errorThrown && errorThrown.status) {
					status = errorThrown.status;
				} else {
					status = 200; // what else we could to do
				}
				process.always(status, textStatus);
			});
			
			// custom Promise target to provide an access to jqXHR object 
			var processTarget = {
				request : request
			};
			return process.promise(processTarget);
		};

		// not used currently
		var getProvider = function(providerId, callbacks) {
			var request = $.ajax({
			  async : true,
			  type : "GET",
			  url : prefixUrl + "/portal/rest/clouddrive/provider/" + providerId,
			  dataType : "json"
			});

			initRequestDefaults(request, callbacks);
		};

		var connectPost = function(workspace, path) {
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

			return initRequest(request);
		};

		var connectInit = function(providerId, callbacks) {
			var request = $.ajax({
			  type : "GET",
			  url : prefixUrl + "/portal/rest/clouddrive/connect/init/" + providerId,
			  dataType : "json"
			});

			initRequestDefaults(request, callbacks);
		};

		var getDrive = function(workspace, path) {
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

			return initRequest(request);
		};

		var getFile = function(workspace, path) {
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
			return initRequest(request);
		};
		
		var getState = function(workspace, path) {
			var request = $.ajax({
			  async : true,
			  type : "GET",
			  url : prefixUrl + "/portal/rest/clouddrive/drive/state",
			  dataType : "json",
			  data : {
			    workspace : workspace,
			    path : path
			  }
			});

			return initRequest(request);
		};

		var synchronizePost = function(workspace, path) {
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

			return initRequest(request);
		};

		var serviceGet = function(url, data) {
			var request = $.ajax({
			  async : true,
			  type : "GET",
			  url : url,
			  dataType : "json",
			  data : data ? data : {}
			});
			return initRequest(request);
		};

		var featuresIsAutosync = function(workspace, path) {
			var request = $.ajax({
			  async : true,
			  type : "GET",
			  url : prefixUrl + "/portal/rest/clouddrive/features/is-autosync-enabled",
			  dataType : "json",
			  data : {
			    workspace : workspace,
			    path : path
			  }
			});

			return initRequest(request);
		};

		var connectDrive = function(providerId, authURL) {
			var authWindow;
			var authService;

			if (authURL) {
				// use user interaction for authentication
				authWindow = cloudDriveUI.connectDriveWindow(authURL);
			} else {
				// function to call for auth using authURL from provider
				authService = serviceGet;
			}
			
			// 1 initialize connect workflow
			var process = $.Deferred();
			connectInit(providerId, {
			  done : function(provider) {
				  utils.log(provider.name + " connect initialized.");
				  if (authService) {
					  authService(provider.authURL);
				  }
				  // 2 wait for authentication
				  var auth = waitAuth(authWindow);
				  auth.done(function() {
					  utils.log(provider.serviceName + " user authenticated.");
					  // 3 and finally connect the drive
					  // set initial progress	with dummy state object
						process.notify({
							progress : 0,
							drive : {
								provider : provider
							}
						});
					  // XXX if it is a re-connect (via providerUpdate), context node may point to a file inside the existing drive
					  // Connect service will care about it and apply correct drive path.
					  var userNode = contextNode;
					  if (userNode) {
						  utils.log("Connecting Cloud Drive to node " + userNode.path + " in " + userNode.workspace);
						  var post = connectPost(userNode.workspace, userNode.path);
						  post.done(function(state, status) {
							  utils.log("Connect requested: " + status + ". ");
							  if (state) {
								  if (status == 201) {
									  utils.log("DONE: " + provider.serviceName + " successfully connected.");
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
									  utils.log("WARN: unexpected state returned from connect service " + status);
								  }
							  } else {
								  utils.log("ERROR: " + provider.serviceName + " connect return null state.");
								  process.reject("Cannot connect " + provider.serviceName + ". Server return empty response.");
							  }
						  });
						  post.fail(function(state, error, errorText) {
							  utils.log("ERROR: " + provider.serviceName + " connect failed: " + error + ". ");
							  if (typeof state === "string") {
								  process.reject(state);
							  } else {
								  process.reject(state && state.error ? state.error : error + " " + errorText);
							  }
						  });
					  } else {
						  process.reject("Connect to " + provider.serviceName + " canceled.");
					  }
				  });
				  auth.fail(function(message) {
				  	if (message) {
					  	utils.log("ERROR: " + provider.serviceName + " authentication error: " + message);
					  }
					  process.reject(message);
				  });
			  },
			  fail : function(error) {
				  utils.log("ERROR: Connect to Cloud Drive cannot be initiated. " + error);
				  if (authWindow && !authWindow.closed) {
					  authWindow.close();
				  }
				  process.reject(error);
			  }
			});
			return process.promise();
		};

		var waitAuth = function(authWindow) {
			var process = $.Deferred();
			var i = 0;
			var intervalId = setInterval(function() {
				var connectId = utils.getCookie("cloud-drive-connect-id");
				if (connectId) {
					// user authenticated and connect allowed 
					intervalId = clearInterval(intervalId);
					process.resolve();
				} else {
					var error = utils.getCookie("cloud-drive-error");
					if (error) {
						intervalId = clearInterval(intervalId);
						// XXX workaround for Google Drive's  access cancellation
						if (error === "Access denied to Google Drive") {
							process.reject(null);
						} else {
							process.reject(error);
						}
					} else if (authWindow && authWindow.closed) {
						intervalId = clearInterval(intervalId);
						utils.log("Authentication canceled.");
						process.reject(null); // reject w/o UI message
					} else if (i > 310) { // +10sec to ConnectService.INIT_COOKIE_EXPIRE
						// if open more 5min - close it and treat as not authenticated/allowed
						intervalId = clearInterval(intervalId);
						process.reject("Authentication timeout.");
					}
				}
				i++;
			}, 1000);
			return process.promise();
		};

		var connectCheck = function(checkUrl) {
			var process = $.Deferred();
			var serviceUrl = checkUrl;
			// if Accepted start Interval to wait for Created
			var intervalId = setInterval(function() {
				// use serviceUrl to check until 201/200 will be returned or an error
				var check = serviceGet(serviceUrl);
				check.done(function(state, status) {
					if (status == "204") {
						// No content - not a cloud drive or drive not connected, or not to this
						// user. This also might mean an error as connect not active but the drive not
						// connected.
						process.reject("Drive not connected. Check if no other connection active and try again.");
					} else if (state && state.serviceUrl) {
						serviceUrl = state.serviceUrl;
						if (status == "201" || status == "200") {
							// created or ok - drive successfully connected or appears as already connected (by another request)
							process.resolve(state);
							utils.log("DONE: " + status + " " + state.drive.provider.serviceName + " connected successfully.");
						} else if (status == "202") {
							// else inform progress and continue 
							process.notify(state);
							utils.log("PROGRESS: " + status + " " + state.drive.provider.serviceName + " connectCheck progress " + state.progress);
						} else {
							// unexpected status, wait for created
							utils.log("WARN: unexpected status in connectCheck:" + status);
						}
					} else {
						utils.log("ERROR: " + status + " connectCheck return wrong state.");
						var driveName;
						if (state.drive && state.drive.provider && state.drive.provider.serviceName) {
							driveName = state.drive.provider.serviceName;
						} else {
							driveName = "Cloud Drive";
						}
						process.reject("Cannot connect " + driveName + ". Server return wrong state.");
					}
				});
				check.fail(function(state, error, errorText) {
					utils.log("ERROR: Connect check error: " + error + ". " + JSON.stringify(state));
					if (typeof state === "string") {
						process.reject(state);
					} else {
						process.reject("Internal error: " + (state && state.error ? state.error : error + " " + errorText));
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
		};

		var getFileLink = function(nodePath) {
			var file = contextDrive.files[nodePath];
			return file ? file.link : null;
		};

		var addExcluded = function(path) {
			excluded[path] = true;
		};

		var isExcluded = function(path) {
			return excluded[path] === true;
		};

		var loadClientModule = function(provider) {
			var loader = $.Deferred();
			// try load provider client
			var moduleId = "SHARED/cloudDrive." + provider.id;
			if (window.require.s.contexts._.config.paths[moduleId]) {
				try {
					// load client module and work with it asynchronously
					window.require([moduleId], function(client) {
						// init client module if it requires that (has a method onLoad())
						if (client && client.onLoad && client.hasOwnProperty("onLoad")) {
							client.onLoad(provider);
						}
						loader.resolve(client); 
					}, function(err) {
						utils.log("ERROR: Cannot load client module for Cloud Drive provider " + provider.name + "(" + provider.id + "). " 
								+ err.message + ": " + JSON.stringify(err), err);
						loader.reject(); 
					});
				} catch(e) {
					// cannot load the module - default behaviour
					utils.log("ERROR: " + e, e);
					loader.reject(); 
				}
			} else {
				loader.reject(); 
			}
			return loader.promise();
		};
		
		var isUpdating = function(path) {
			if (contextDrive) {
				for (var ui = 0; ui < contextDrive.state.updating.length; ui++) {
					if (path === contextDrive.state.updating[ui]) {
						return true;
					}		
				}				
			}
			return false;
		};
		
		var readFile = function(path) {
			if (contextNode) {
				var workspace = contextNode.workspace;
				if (!path) {
					path = contextNode.path;
				}
				var process = getFile(workspace, path);
				process.done(function(file, status) {
			  	// 200 - file exists,
			  	// 202 - file accepted to be a cloud file, but not yet created in cloud- ignore it
			  	if (contextDrive && status == 200) {
			  		initFile(file);
				  	contextDrive.files[path] = file;
				  }
				});
				process.fail(function(err, status) {
			  	if (status == 404) { 
			  		if (err.error === NOT_CLOUD_FILE) {
			  			// cloud file not fond, or node not a cloud file - do nothing
			  			utils.log("WARN: " + err.message + " (" + status + ")");
			  			contextNode.local = true; // not cloud file marker
			  		} else if (err.error === NOT_CLOUD_DRIVE) {
			  			addExcluded(path);
			  		}
			  	} else {
			  		utils.log("ERROR: Cloud Drive file " + workspace + ":" + path + " cannot be read: " 
			  				+ err.message + " (" + status + ")");
					  //cloudDriveUI.showError("Error reading drive file", err.message ? err.message : "Internal error. Try again later.");
				  }
			  });
			}
		};
		
		var readContextFile = function() {
			readFile(null); // path will be read from context node 
		};
		
		var readContextDrive = function() {
			if (contextNode) {
				var workspace = contextNode.workspace;
				var path = contextNode.path;
				var process = getDrive(workspace, path);
				process.done(function(drive, status) {
					var provider = providers[drive.provider.id]; 
					if (provider) {
						provider.clientModule.done(function(client) {
							if (client) {
								// init context drive within the provider client
								if (client.initDrive && client.hasOwnProperty("initDrive")) {
									client.initDrive(drive);
								}
								// init files by the client if applicable
								if (client.initFile && client.hasOwnProperty("initFile")) {
									for (fpath in drive.files) {
										client.initFile(drive.files[fpath]);										
									}
								}
							}
						});
					}
					// use already cached files with new drive
					if (contextDrive && contextDrive.path == drive.path) {
						for (fpath in contextDrive.files) {
							if (contextDrive.files.hasOwnProperty(fpath) && !drive.files.hasOwnProperty(fpath)) {
								drive.files[fpath] = contextDrive.files[fpath];
							}
						}
					}
					contextDrive = drive;
					checkAutoSynchronize();
				});
				process.fail(function(err, status) {
					if (status == 404 && err.error === NOT_CLOUD_DRIVE) {
						addExcluded(path);
						stopAutoSynchronize();
					} else {
						utils.log("ERROR: Cloud Drive " + workspace + ":" + path + " cannot be read: " + err.message + " (" + status + ")");
						cloudDriveUI.showError("Error reading drive", err.message ? err.message : "Internal error. Try again later.");
					}
				});
			}
		};

		var stopAutoSynchronize = function() {
			for (job in autoSyncs) {
				if (autoSyncs.hasOwnProperty(job)) {
					try {
						clearTimeout(autoSyncs[job]);
						clearInterval(autoSyncs[job]);
						delete autoSyncs[job];
					} catch(e) {
						utils.log("Error stopping auto sync job: " + e);
					}
				}
			}
		};

		var autoSynchronize = function() {
			if (contextDrive) {
				var drive = contextDrive;
				var syncName = drive.workspace + ":" + drive.path;

				if (!autoSyncs[syncName]) {
					// by default we do periodic sync, but the provider connector can offer own auto-sync function
					
					var syncFunc;
					var syncTimeout;
					// sync scheduler
					function scheduleSync() {
						// if no sync already scheduled - we run it immediately and then schedule a next one in syncTimeout
						if (!autoSyncs[syncName]) {
							var syncProcess = doSync();
							syncProcess.fail(function(e) {
								delete autoSyncs[syncName]; // cancel and cleanup
								utils.log("ERROR: " + (e.message ? e.message : e)  + ". Auto-sync canceled for " + syncName + ". (1)");
							});
						}
						autoSyncs[syncName] = setTimeout(function() {
							var syncProcess = syncFunc();
							syncProcess.done(function() {
								if (autoSyncs[syncName]) {
									scheduleSync(); // re-schedule only if enabled
								}
							});
							syncProcess.fail(function(e) {
								delete autoSyncs[syncName]; // cancel and cleanup
								utils.log("ERROR: " + (e.message ? e.message : e)  + ". Auto-sync canceled for " + syncName + ". (2)");
							});
						}, syncTimeout);
					}
					// sync function
					var doSync = function() {
						return synchronize(drive.workspace, drive.path);
					};
					// default algorithm
					var defaultSync = function() {
						syncTimeout = 20000; // sync each 20sec
						// use default sync function
						syncFunc = doSync; 
						scheduleSync();
						utils.log("Periodical synchronization enabled for Cloud Drive on " + syncName);
						
						// run periodical sync for some period (30min)
						var syncPeriod = 60000 * 30;
						// ... increase timeout after a 1/3 of a period
						setTimeout(function() {
							syncTimeout = 40000;
						}, Math.round(syncPeriod / 3));
						// ... and stop sync after some period, user can enable it again by page refreshing/navigation
						setTimeout(function() {
							stopAutoSynchronize();
							utils.log("Periodical synchronization stopped for Cloud Drive on " + syncName);
						}, syncPeriod);
					};
					
					// try use loaded provider client (see initProvider())
					var provider = providers[drive.provider.id];
					if (provider) {
						provider.clientModule.done(function(client) {
							if (client && client.onChange && client.hasOwnProperty("onChange")) {
								// apply custom client algorithm
								syncTimeout = 7000; // sync in 7sec
								syncFunc = function() { 
									// We chain actual sync to the sync initiator from client.
									// The initiator should return jQuery Promise: it will be resolved if changes appear and rejected on error. 
									// We use jQuery.when() to deal if not Promise returned (it's bad case - sync will run each 5sec forever).
									var process = $.Deferred(); 
									var initiator = client.onChange(drive);
									$.when(initiator).done(function() {
										// it's time to sync
										var sync = doSync(); 
										sync.done(function() {
											process.resolve();	
										});
										sync.fail(function(e) {
											process.reject(e);	
										});
									});
									$.when(initiator).fail(function(e) {
										process.reject(e);
									});
									return process.promise();
								};
								scheduleSync();
								utils.log("Client synchronization enabled for Cloud Drive on " + syncName);
							} else {
								// client doesn't provide onChange() method - apply default algorithm (in this async callback)
								defaultSync();
							}
						});
						provider.clientModule.fail(function() {
							// module not available - run default periodic auto-sync
							defaultSync();
						});
					} else {
						utils.log("WARN: provider not initialized " + drive.provider.id + " - run default periodic auto-sync");
						defaultSync();
					}
				}
			}
		};
		
		var checkAutoSynchronize = function() {
			if (contextDrive) {
				var autosync = featuresIsAutosync(contextDrive.workspace, contextDrive.path);
				autosync.done(function(check) {
					if (check && check.result) {
						autoSynchronize();
					} else {
						stopAutoSynchronize();
					}
				});
				autosync.fail(function(response, status, err) {
					// in case of error: don't enable/disable autosync
					utils.log("ERROR: features autosync: " + err + ", " + status + ", " + response);
				});
				return autosync.promise();
			}
			return null;
		};

		var synchronize = function(nodeWorkspace, nodePath) {
			var process = $.Deferred();
			cloudDriveUI.synchronizeProcess(process.promise());

			var initiator = $.Deferred();
			initiator.done(function() {
				// sync only if drive connected
				if (contextDrive.connected) {
					// sync and load all files related to this drive
					var sync = synchronizePost(nodeWorkspace, nodePath);
					sync.contextDrive = contextDrive;
					activeSyncs.push(sync);
					var currentPath = currentNode ? currentNode.path : nodePath;
					sync.done(function(drive, status) {
						var changed = 0;
						var updated = 0; // updated in context node
						
						// init files by the client if applicable
						var provider = providers[contextDrive.provider.id]; 
						if (provider) {
							provider.clientModule.done(function(client) {
								if (client && client.initFile && client.hasOwnProperty("initFile")) {
									for (fpath in drive.files) {
										client.initFile(drive.files[fpath]);										
									}
								}
							});
						}
						
						// calculate the whole drive changes and updated in current folder
						for (fpath in drive.files) {
							if (drive.files.hasOwnProperty(fpath)) {
								changed++;
								if (currentPath && fpath.indexOf(currentPath) == 0) {
									updated++;
								}
							}
						}
						
						// count removed as changed
						changed += drive.removed.length; 
						for (var i = 0; i < drive.removed.length; i++) {
							if (currentPath && drive.removed[i].indexOf(currentPath) == 0) {
								updated++;
							}
						}

						// copy already cached but not synced files to the new drive
						nextCached: for (fpath in sync.contextDrive.files) {
							if (!drive.files[fpath]) {
								for (var fi = 0; fi < drive.removed.length; fi++) {
									if (fpath.indexOf(drive.removed[fi]) == 0) {
										// skip already removed, including subtree files
										continue nextCached; 
									}
								}
								for (var ui = 0; ui < drive.state.updating.length; ui++) {
									if (fpath === drive.state.updating[ui]) {
										// skip currently syncing to let them be requested from the server later
										continue nextCached; 
									}
								}
								if (!drive.files.hasOwnProperty(fpath)) {
									drive.files[fpath] = sync.contextDrive.files[fpath];
								}
							}
						}
						
						utils.log("DONE: Synchronized " + changed + " changes from Cloud Drive associated with " + nodeWorkspace + ":"
						    + nodePath + ". " + updated + " updated in current folder.");

						if (sync.contextDrive == contextDrive) {
							// using new drive in the context (only if context wasn't changed)
							contextDrive = drive;
						}
						
						checkAutoSynchronize();

						process.resolve(updated, drive);
					});
					sync.fail(function(response, status, err) {
						utils.log("ERROR: synchronization error: " + err + ", " + status + ", " + JSON.stringify(response));
						if (status == 403) {
							if (response.id) {
								updateProvider = response;
							}
						}
						process.reject(response, status);
					});
					sync.always(function() {
						// cleanup
						for (var i = 0, asize = activeSyncs.length; i < asize; ++i) {
							if (activeSyncs[i] == sync) {
								activeSyncs.splice(i, 1);
								break;
							}
						}
					});
				} else {
					// not yet created: we initiate auto-sync if it is available, it will call this method later
					checkAutoSynchronize();
				}
			});

			// start work here (registered done() will be called)
			if (updateProvider) {
				// previous attempt tells us we have to update access keys - reconnect
				if (updateProvider.process) {
					// previous sync already updating the keys - return it here
					return updateProvider.process;
				}
				var connect = connectDrive(updateProvider.id, updateProvider.authURL);
				updateProvider.process = process.promise(); // mark as active
				connect.done(function(state) {
					updateProvider = null;
					initiator.resolve();
				});
				connect.fail(function(error) {
					updateProvider.process = null;
					process.reject(error);
				});
			} else {
				initiator.resolve();
			}

			return process.promise();
		};
		
		/**
		 * Initialize given file using a client module if it provides initFile() function.
		 */
		var initFile = function(file) {
			if (contextDrive) {
				var provider = providers[contextDrive.provider.id]; 
				if (provider) {
					provider.clientModule.done(function(client) {
						if (client && client.initFile && client.hasOwnProperty("initFile")) {
							client.initFile(file);
						}
					});
				}
			}
		};

		/**
		 * Synchronize documents view.
		 */
		this.synchronize = function(elem, objectId) {
			if (contextDrive) {
				var nodePath = contextDrive.path;
				var nodeWorkspace = contextDrive.workspace;
				utils.log("Synchronizing Cloud Drive on " + nodeWorkspace + ":" + nodePath);
				return synchronize(nodeWorkspace, nodePath);
			} else {
				utils.log("WARN Nothing to synchronize!");
				cloudDriveUI.refreshDocuments(); // refresh WCM explorer
			}
		};

		/**
		 * Connect to Cloud Drive.
		 */
		this.connect = function(providerId, authURL, userNode, userWorkspace) {
			utils.log("Connecting Cloud Drive...");

			if (!authURL) {
				var provider = providers[providerId];
				if (provider) {
					authURL = provider.authURL;
					if (authURL.indexOf(prefixUrl) == 0) {
						// XXX warm-up the portal with its ajax request :)
						serviceGet(authURL + "&ajaxRequest=true");
					}
				} else {
					utils.log("ERROR: Provider cannot be for id " + providerId);
					return;
				}
			}

			// set connect node explicitly
			if (userNode && userWorkspace) {
				contextNode = {
				  workspace : userWorkspace,
				  path : userNode
				};
			}

			// reset previous drive context
			contextDrive = null;
			excluded = {};

			var process = connectDrive(providerId, authURL);
			cloudDriveUI.connectProcess(process);
			return process;
		};

		this.state = function(checkUrl) {
			return connectCheck(checkUrl);
		};

		/**
		 * Initialize provider for later operations.
		 */
		this.initProvider = function(id, provider) {
			providers[id] = provider;
			
			if (window == top) {
				try {
					// load provider styles
					utils.loadStyle("/cloud-drive-" + id + "/skin/clouddrive.css");
				} catch(e) {
					utils.log("Error loading provider (" + id + ") style.", e);
				}
			}
			
			// load client module
			provider.clientModule = loadClientModule(provider);
			
			// TODO cleanup, this op will be performed in init()->initContext()->readContextDrive()
			/*if (contextDrive && contextDrive.provider.id == provider.id) {
				// init context drive within the provider client
				provider.clientModule.done(function(client) {
					if (client) {
						if (client.initDrive && client.hasOwnProperty("initDrive")) {
							client.initDrive(contextDrive);
						}
					}
				});
			}*/
		};

		/**
		 * Initialize connected drive nodes for UI rendering.
		 */
		this.initConnected = function(map) {
			cloudDriveUI.initConnected(map);
		};

		/**
		 * Initialize sharing manager UI.
		 */
		this.initSharing = function() {
			cloudDriveUI.initSharing();
		};
		
		/**
		 * Initialize text file viewer UI (based on Codemirror).
		 */
		this.initTextViewer = function() {
			cloudDriveUI.initTextViewer();
		};
		
		/**
		 * Initialize context and UI.
		 */
		this.init = function(nodeWorkspace, nodePath) {
			try {
				// currently open node in ECMS explorer
				currentNode = {
				  workspace : nodeWorkspace,
				  path : nodePath
				};
			 	cloudDrive.initContext(nodeWorkspace, nodePath);
				// and on-page-ready initialization of Cloud Drive UI
				$(function() {
					try {
						cloudDriveUI.init();
					} catch(e) {
						utils.log("Error initializing Cloud Drive UI " + e, e);
					}
				});
			} catch(e) {
				utils.log("Error initializing Cloud Drive " + e, e);
			}
		};

		/**
		 * Initialize context node and optionally a drive. Method works synchronously and when complete the context is initialized.  
		 */
		this.initContext = function(workspace, path) {
			//utils.log("Init context node: " + workspace + ":" + path
			//    + (contextDrive ? " (current drive: " + contextDrive.path + ")" : "") + " excluded: " + isExcluded(nodePath));

			contextNode = {
			  workspace : workspace,
			  path : path
			};

			if (isExcluded(path)) {
				// already cached as not in drive
				stopAutoSynchronize();
			} else {
				// XXX do this to support symlinks outside the drive
				if (contextDrive && path.indexOf(contextDrive.path) == 0 && path != contextDrive.path) {
					var file = contextDrive.files[path];
					if (!file || isUpdating(path)) {
						// file not cached or was syncing (updating), get the file from the server and cache it locally
						readContextFile();
					}
				} else {
					readContextDrive();
				}
			}
		};

		this.getContextDrive = function() {
			return contextDrive;
		};

		this.getContextFile = function() {
			if (contextNode && contextDrive) {
				var path = contextNode.path;
				if (isUpdating(path) || contextNode.local) {
					// file is syncing or local, its URL may be not yet existing or already changed -
					// read file from the server for a fresh state
					readContextFile();
				}
				return contextDrive.files[path];
			}
			return null;
		};
		
		this.getFile = function(path) {
			if (contextDrive) {
				var file = contextDrive.files[path];
				if (!file || isUpdating(path) || contextNode.local) {
					// file not yet cached, file is syncing or local - read file from the server
					readFile(path);
					file = contextDrive.files[path];
				}
				return file;
			}
			return null;
		};
		
		this.getCurrentNode = function() {
			return currentNode;
		};

		this.isContextSymlink = function() {
			if (contextNode && contextDrive) {
				var file = contextDrive.files[contextNode.path];
				return file && file.symlink;
			}
			return false;
		};

		this.isContextFile = function() {
			return contextNode && contextDrive && contextDrive.files[contextNode.path] != null;
		};

		this.isContextDrive = function() {
			return contextNode && contextDrive && contextDrive.path == contextNode.path;
		};
		
		this.isContextLocal = function() {
			return contextNode && contextDrive && contextNode.local;
		};
		
		this.isContextUpdating = function() {
			return contextNode && isUpdating(contextNode.path);
		};

		this.openFile = function(elem, objectId) {
			var file = cloudDrive.getContextFile();
			if (file) {
				window.open(file.link);
			} else {
				utils.log("No context path to open as Cloud File");
			}
		};
		
		this.showInfo = function(title, text) {
			cloudDriveUI.showInfo(title, text);
		};
		
		/** 
		 * Helper for AJAX GET requests.
		 * */
		this.ajaxGet = function(url, data) {
			return serviceGet(url, data);
		};
		
		/** 
		 * Request current state of given drive and return jQuery promise to the request. 
		 * This method also does update the drive object state. 
		 * If given drive is null/undefined then context drive will be used and updated accordingly, if no context drive 
		 * found then null will be returned.
		 */
		this.getState = function(drive) {
			if (!drive) {
				drive = contextDrive;
			}
			var stateProcess = null;
			if (drive) {
				stateProcess = getState(drive.workspace, drive.path);
				stateProcess.done(function(state) {
					// find files not upgrading anymore and remove them from the drive files:
					nextUpdating: for (var dui = 0; dui < drive.state.updating.length; dui++) {
						var fpath = drive.state.updating[dui];
						for (var sui = 0; sui < state.updating.length; sui++) {
							if (fpath === state.updating[sui]) {
								continue nextUpdating; // still updating		
							}
						}
						// remove from cached files
						delete drive.files[fpath];
					}
					// and set fresh state at the end
					drive.state = state;
				});
			}
			return stateProcess;
		};
	}

	/**
	 * Cloud Drive WebUI integration.
	 */
	function CloudDriveUI() {
		var NOTICE_WIDTH = "380px";

		// Menu items managed via uiRightClickPopupMenu menu interception
		var MENU_OPEN_FILE = "OpenCloudFile";
		var MENU_PUSH_FILE = "PushCloudFile";
		var MENU_REFRESH_DRIVE = "RefreshCloudDrive";
		var DRIVE_MENU_ACTIONS = [ MENU_OPEN_FILE, MENU_REFRESH_DRIVE ];
		var ALLOWED_DRIVE_MENU_ACTIONS = [ MENU_OPEN_FILE, MENU_PUSH_FILE, MENU_REFRESH_DRIVE, "Delete", "Paste",  
				"AddToFavourite", "RemoveFromFavourite", "ViewInfo" ];
		var ALLOWED_FILE_MENU_ACTIONS = [ MENU_OPEN_FILE, MENU_PUSH_FILE, MENU_REFRESH_DRIVE, "Delete", "Rename",  
				"Copy", "Cut", "Paste", "AddToFavourite", "RemoveFromFavourite", "ViewInfo", "ViewSharing" ];
		var ALLOWED_SYMLINK_MENU_ACTIONS = [ "Delete" ];
		var ALLOWED_LOCAL_FILE_MENU_ACTIONS = [ MENU_PUSH_FILE, "Delete", "Cut", "RemoveFromFavourite", "ViewInfo" ];

		// Menu items managed via view's showItemContextMenu() method (multi-selection)
		// 21.05.2014 "uiIconEcmsOverloadThumbnail" removed from allowed
		var ALLOWED_DMS_MENU_COMMON_ACTION_CLASSES = [ "uiIconEcmsUpload", "uiIconEcmsAddFolder", "uiIconEcmsAddToFavourite", 
				"uiIconEcmsRemoveFromFavourite", "uiIconEcmsManageActions", "uiIconEcmsManageRelations", "uiIconEcmsViewProperties", 
				"uiIconEcmsManageAuditing" ];
		var ALLOWED_DMS_MENU_FILE_ACTION_CLASSES = [ "uiIconEcmsOpenCloudFile", "uiIconEcmsPushCloudFile", 
				"uiIconEcmsTaggingDocument", "uiIconEcmsWatchDocument", "uiIconEcmsViewMetadatas", "uiIconEcmsVote", 
				"uiIconEcmsComment", "uiIconEcmsCopy", "uiIconEcmsPaste", "uiIconEcmsCut", "uiIconEcmsDelete", "uiIconEcmsRename",
				"uiIconEcmsViewSharing" ];
		var ALLOWED_DMS_MENU_DRIVE_ACTION_CLASSES = [ "uiIconEcmsRefreshCloudDrive", "DeleteNodeIcon" ];
		var ALLOWED_DMS_MENU_LOCAL_FILE_ACTION_CLASSES = [ "uiIconEcmsPushCloudFile", "uiIconEcmsViewMetadatas", 
				"uiIconEcmsCut", "uiIconEcmsDelete", "uiIconEcmsRemoveFromFavourite", "uiIconEcmsViewProperties" ];

		var initLock = null;
		
		var syncingUpdater = null;

		var getIEVersion = function()
		// Returns the version of Windows Internet Explorer or a -1
		// (indicating the use of another browser).
		{
			var rv = -1; // Return value assumes failure.
			if (navigator.appName == "Microsoft Internet Explorer") {
				var ua = navigator.userAgent;
				var re = new RegExp("MSIE ([0-9]{1,}[\.0-9]{0,})");
				if (re.exec(ua) != null)
					rv = parseFloat(RegExp.$1);
			}
			return rv;
		};

		var getAllowedItems = function(items, allowed) {
			var newParams = "";
			$.each(items, function(i, item) {
				if (allowed.indexOf(item) >= 0) {
					newParams = (newParams ? newParams + "," + item : item);
				}
			});
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

		var initContextMenu = function(menu, items, allowedItems) {
			var menuItems = items.split(",");
			var drive = cloudDrive.getContextDrive();
			if (drive) {
				// branded icons in context menu
				$("i.uiIconEcmsRefreshCloudDrive, i.uiIconEcmsOpenCloudFile, i.uiIconEcmsPushCloudFile").each(function() {
					var brandClass = "uiIcon16x16CloudFile-" + drive.provider.id;
					var currentClass = $(this).attr("class");
					if (currentClass.indexOf(brandClass) < 0) {
						$(this).attr("class", currentClass + " " + brandClass);
					}
				});

				// Common context menu: add links to CD actions
				$("#ECMContextMenu a[exo\\:attr='" + MENU_OPEN_FILE + "']").each(function() {
					var text = $(this).data("cd_action_prefix");
					if (!text) {
						text = $(this).text();
						$(this).data("cd_action_prefix", text).click(function() {
							cloudDrive.openFile();
							uiFileView.UIFileView.clearCheckboxes();
						});
					}
					var $i = $(this).find("i");
					text = text + drive.provider.serviceName;
					$(this).text(text);
					$(this).prepend($i);
					if (cloudDrive.isContextUpdating()) {
						$(this).addClass("cloudFileDisabled");
					} else {
						$(this).removeClass("cloudFileDisabled");
					}
				});
				$("#ECMContextMenu a[exo\\:attr='" + MENU_PUSH_FILE + "']").each(function() {
					var text = $(this).data("cd_action_prefix");
					if (!text) {
						text = $(this).text();
						$(this).data("cd_action_prefix", text);
					}
					var $i = $(this).find("i");
					text = text + drive.provider.serviceName;
					$(this).text(text);
					$(this).prepend($i);
				});
				$("#ECMContextMenu a[exo\\:attr='" + MENU_REFRESH_DRIVE + "']").each(function() {
					var text = $(this).data("cd_action_prefix");
					if (!text) {
						$(this).click(function() {
							cloudDrive.synchronize();
							uiFileView.UIFileView.clearCheckboxes();
						});
						text = $(this).text();
						$(this).data("cd_action_prefix", text);
					}
					var $i = $(this).find("i");
					text = text + drive.provider.serviceName;
					$(this).text(text);
					$(this).prepend($i);
				});

				if (cloudDrive.isContextSymlink()) {
					allowedItems = allowedItems.concat(ALLOWED_SYMLINK_MENU_ACTIONS);
				}

				// Custom context menu links
				if (menu) {
					var file = cloudDrive.getContextFile();
					if (file) {
						var link = file.link;
						$(menu).find("li.menuItem").each(function() {
							$(this).find("i.uiIconDownload").each(function() {
								$(this).parent().attr("target", "_new");
								// XXX need # at the end to deal with ECMS's objectId added on click
								$(this).parent().attr("href", link + "#"); 
								// May 29 2014 was also eXo.ecm.WCMUtils.hideContextMenu(this);
								$(this).parent().attr("onclick", "eXo.ecm.UIFileView.clearCheckboxes();");
							});
							$(this).find("i.uiIconEcmsCopyUrlToClipboard").each(function() {
								$(this).parent().attr("path", link);
								$(this).parent().click(function() {
									eXo.ecm.ECMUtils.pushToClipboard(event, link);
									uiFileView.UIFileView.clearCheckboxes();
								});
							});
						});
					}
				}

				// fix menu: keep only allowed items
				return getAllowedItems(menuItems, allowedItems);
			} else {
				// if not cloud file on context path - remove OpenCloudFile from the menu
				return removeCloudItems(menuItems);
			}
		};

		var initMultiContextMenu = function() {
			var drive = cloudDrive.getContextDrive();
			if (drive) {
				// Fix group Context Menu items using CSS
				var classes;
				if (cloudDrive.isContextFile()) {
					// it's drive's file
					classes = ALLOWED_DMS_MENU_COMMON_ACTION_CLASSES.concat(ALLOWED_DMS_MENU_FILE_ACTION_CLASSES);
				} else if (cloudDrive.isContextDrive()) {
					// it's drive in the context
					classes = ALLOWED_DMS_MENU_COMMON_ACTION_CLASSES.concat(ALLOWED_DMS_MENU_DRIVE_ACTION_CLASSES);
				} else if (cloudDrive.isContextLocal()) {
					// it's local node in the drive context
					classes = ALLOWED_DMS_MENU_LOCAL_FILE_ACTION_CLASSES;
				} else {
					// selected node not a cloud drive or its file
					classes = null;
				}

				if (classes) {
					var allowed = "";
					$.each(classes, function(i, action) {
						allowed += (allowed ? ", ." : ".") + action;
					});
					// filter Context Menu common items: JCRContextMenu located in action bar
					var $items = $("#JCRContextMenu li.menuItem a i");
					var itemsCount = $items.size();
					$items.not(allowed).each(function() {
						$(this).parent().css("display", "none");
						itemsCount--;
					});
					return itemsCount;
				}
			}
			return -1;
		};

		var decodeString = function(str) {
			if (str) {
				try {
					str = str.replace(/\+/g, " ");
					str = decodeURIComponent(str);
					return str;
				} catch(e) {
					utils.log("WARN: error decoding string " + str + ". " + e, e);
				}
			}
			return null;
		};
		
		var encodeString = function(str) {
			if (str) {
				try {
					str = encodeURIComponent(str);
					return str;
				} catch(e) {
					utils.log("WARN: error decoding string " + str + ". " + e, e);
				}
			}
			return null;
		};

		/**
		 * Method adapted from org.exoplatform.services.cms.impl.Utils.refine().
		 */
		var refineSize = function(size) {
	    if (!size || size == 0) {
	      return "";
	    }
	    var strSize = size.toFixed(2);
	    return "," + Math.round(parseInt(strSize) / 100.0);
		};
		
		/**
		 * Method adapted from org.exoplatform.services.cms.impl.Utils.fileSize().
		 */
		var sizeString = function(size) {
			var byteSize = size % 1024;
			var kbSize = (size % 1048576) / 1024;
			var mbSize = (size % 1073741824) / 1048576;
			var gbSize = size / 1073741824;
			
			if (gbSize >= 1) {
			  return gbSize.toFixed(2) + " GB";
			} else if (mbSize >= 1) {
			  return mbSize.toFixed(2) + " MB";
			} else if (kbSize > 1) {
			  return kbSize.toFixed(2) + " KB";
			} if (byteSize > 0) {
				return byteSize + " B";
			} else {
			  return ""; // return empty not 1 KB as ECMS does
			}
		};

		/**
		 * Init file list according the actions set for each file item.
		 */
		var initFileList = function() {
			var syncingPaths = [];

			// List/Admin view
			var $listView = $("div.fileViewRowView");
			if ($listView.size() > 0) {
				$listView.each(function() {
					$(this).removeClass("notCloudFile cloudFileDisabled");
					$(this).find("span.syncingListView").remove();
				});
				$listView.filter("div[onmousedown*='PushCloudFile'], div[mousedown*='PushCloudFile']").each(function() {
					$(this).addClass("notCloudFile");
				});
				$listView.filter("div[onmousedown*='SyncingFile'], div[mousedown*='SyncingFile']").each(function() {
					var objectId = decodeString($(this).attr("objectid"));
					if (objectId) {
						syncingPaths.push(objectId);
						$(this).addClass("notCloudFile cloudFileDisabled");
						$(this).find(".nodeLabel").append("<span class='syncingListView'>&nbsp</span>");
					}
				});
			}
			// List/Admin view - fix file size
			if ($listView.size() > 0) {
				$listView.each(function() {
					var objectId = decodeString($(this).attr("objectid"));
					// find all info lines with 1K size and replace the size with real value
					$(this).find("p.fileInfoBottom:contains('- 1 KB')").each(function() {
						var file = cloudDrive.getFile(objectId);
						if (file) {
							var orig = $(this).text();
							var str = sizeString(file.size);
							$(this).text(orig.replace("- 1 KB", str.length > 0 ? "- " + str : str));
						}
					});
				});
			}
			
			// Icon view
			var $iconView = $("div.actionIconBox");
			if ($iconView.size() > 0) {
				$iconView.each(function() {
					$(this).removeClass("notCloudFile cloudFileDisabled");
					$(this).find("div.syncingIconView").remove();
				});
				$iconView.filter("div[onmousedown*='PushCloudFile'], div[mousedown*='PushCloudFile']").each(function() {
					$(this).addClass("notCloudFile");
				});
				$iconView.filter("div[onmousedown*='SyncingFile'], div[mousedown*='SyncingFile']").each(function() {
					var objectId = decodeString($(this).attr("objectid"));
					if (objectId) {
						syncingPaths.push(objectId);
						$(this).addClass("notCloudFile cloudFileDisabled");
						$(this).find(".nodeLabel").before("<div class='syncingIconView'></div>");
					}
				});
			}
			
			// Icon view - fix file size (nasty way)
			if ($iconView.size() > 0) {
				$("#UIPopupContainer").on("DOMSubtreeModified propertychange", function() {
					var $info = $("#UIViewInfoManager");
					if ($info.size() > 0 && !$info.data("cd_data_sizefixed")) { // avoid loops caused by text modification below
						$info.find("td").filter("td:contains(' Byte(s)'), td:contains(' KB'), td:contains(' MB'), td:contains(' GB')").each(function() {
							var file = cloudDrive.getContextFile();
							if (file) {
								var str = sizeString(file.size);
								$info.data("cd_data_sizefixed", true);
								$(this).text(str);
							}
						});
					}
				});
			}
			
			if (syncingPaths.length > 0) {
				// initiate/update syncingUpdater
				
				if (!syncingUpdater) {
					// create periodic updated, check each 5sec
					// TODO replace with long-poling request w/o periodic task
					syncingUpdater = {};
					syncingUpdater.interval = setInterval(function() {
						var stateProcess = cloudDrive.getState();
						if (stateProcess) {
							stateProcess.done(function(state) {
								var updated = 0;
								var paths = syncingUpdater.paths;
								if (paths && paths.length > 0) {
									// compare remote state's updating with paths stored in the updater
									// if have difference - cancel the interval and run UI refresh
									var currentNode = cloudDrive.getCurrentNode();
									if (currentNode) {
										var currentPath = currentNode.path;
										var contextPathsNumber = 0;
										next: for (var li = 0; li < paths.length; li++) {
											var path = paths[li];
											if (path.indexOf(currentPath) == 0) { // starts with current path in ECMS explorer UI
												contextPathsNumber++;
												for (var ri = 0; ri < state.updating.length; ri++) {
													if (path === state.updating[ri]) {
														continue next;
													}		
												}
												updated++;
											}
										}
										if (updated == 0 || updated != contextPathsNumber) {
											return; // no changes or not everything changed in syncing list - wait for next interval
										}
									}
								}
								clearInterval(syncingUpdater.interval);
								syncingUpdater = null;
								if (updated > 0) {
									refresh(); // refresh after the cancellation!
								}
							});
							stateProcess.fail(function(e) {
								// stop updated on error
								clearInterval(syncingUpdater.interval);
								syncingUpdater = null;
								utils.log("ERROR: syncing updater failed with error " + e, e);
							});
						}
					}, 5000);
				} 
				syncingUpdater.paths = syncingPaths;
			}
			
			return syncingPaths.length;
		};

		var initDocument = function() {
			var drive = cloudDrive.getContextDrive();
			if (drive) {
				// Fix Action Bar items
				var classes;
				if (cloudDrive.isContextFile()) {
					// it's drive's file
					classes = ALLOWED_DMS_MENU_COMMON_ACTION_CLASSES.concat(ALLOWED_DMS_MENU_FILE_ACTION_CLASSES);
				} else if (cloudDrive.isContextDrive()) {
					// it's drive in the context
					classes = ALLOWED_DMS_MENU_COMMON_ACTION_CLASSES.concat(ALLOWED_DMS_MENU_DRIVE_ACTION_CLASSES);
				} else {
					// selected node not a cloud drive or its file
					return;
				}

				var allowed = "";
				$.each(classes, function(i, action) {
					allowed += (allowed ? ", ." : ".") + action;
				});
				
				var $actionBar = $("#uiActionsBarContainer ul");
				// filter Action Bar items (depends on file/folder or the drive itself in the context)
				$actionBar.find("li a.actionIcon i").not(allowed).each(function() {// div ul li
					$(this).parent().css("display", "none");
				});
				if ($actionBar.find("li a:visible").size() == 0) {
					// hack to prevent empty menu bar
					$actionBar.append("<li style='display: block;'><a class='actionIcon' style='height: 18px;'><i></i> </a></li>");
				}

				// add sync call to Refresh action
				$("a.refreshIcon").click(function() {
					var $refreshChanges = $("span.uiCloudDriveChanges");
					if ($refreshChanges.size() > 0) {
						var currentDate = new Date();
						var syncDate = $refreshChanges.data("timestamp");
						if (syncDate && (currentDate.getMilliseconds() - syncDate.getMilliseconds() <= 60000)) {
							return true; // don't invoke sync if it was less a min ago
						}
					}
					cloudDrive.synchronize();
				});

				// File Viewer
				var $viewer = $("#CloudFileViewer");
				if ($viewer.size() > 0) {
					var file = cloudDrive.getContextFile();
					var $vswitch = $("#ViewerSwitch");
					var openOnProvider = $viewer.attr("file-open-on");

					// fix title
					var $title = $("div.fileContent .title");
					var $titleText = $title.find("div.topTitle");
					$titleText.text(file.title);

					// fix Download icon, text and link
					var $i = $title.find("i.uiIconDownload");
					$i.attr("class", "uiIcon16x16CloudFile-" + drive.provider.id);
					var $a = $title.find("a");
					$a.text(" " + openOnProvider);
					$a.prepend($i);
					$a.attr("href", file.link);
					$a.attr("target", "_blank");
					$a.css("font-weight", "normal");
					
					var viewIcon = $("<i></i>").attr("class", "uiIconWatch uiIconLightGray");
					var editIcon = $("<i></i>").attr("class", "uiIconEdit uiIconLightGray");

					var $iframe = $viewer.find("iframe");
					// file link as edit link
					if ($vswitch.size() > 0 && file.editLink && file.previewLink && file.editLink != file.previewLink) {
						// init Edit/View mode
						$iframe.attr("src", file.previewLink);
						$vswitch.click(function() { // TODO cleanup .find("a")
							var currentLink = $iframe.attr("src");
							if (currentLink == file.previewLink) {
								// switch to editor
								$iframe.attr("src", file.editLink);
								var viewerTitle = $vswitch.attr("view-title");
								$(this).text(viewerTitle);
								$(this).prepend(viewIcon);
							} else {
								// switch to viewer
								$iframe.attr("src", file.previewLink);
								var editTitle = $vswitch.attr("edit-title");
								$(this).text(editTitle);
								$(this).prepend(editIcon);
							}
						});
						$vswitch.prepend(editIcon);
						$a.after($vswitch);
					} else {
						$viewer.find("iframe").attr("src", file.previewLink ? file.previewLink : file.link);
						$vswitch.remove();
					}
					$viewer.find(".file-content").show();
				}
				
				// init file listing (special handling for not cloud's and currently syncing files)
				initFileList();
				
				eXo.ecm.ECMUtils.loadContainerWidth();
			} // else not a cloud drive or its file
		};
		
		/**
		 * Find link to open Personal Documents view in WCM. Can return nothing if current page doesn't
		 * contain such element.
		 */
		var personalDocumentsLink = function() {
			var link = $("a.refreshIcon");
			if (link.size() > 0) {
				return link.attr("href");
			}
		};

		/**
		 * Refresh WCM view.
		 */
		var refresh = function(forceRefresh) {
			if (forceRefresh) {
				// refresh view w/ popup
				$("a.refreshIcon i.uiIconRefresh").click();
			} else {
				// don't refresh if user actions active or if file view active
				if ($("div#UIDocumentInfo:visible").size() > 0) {
					if ($("div#UIPopupWindow:visible, div#UIRenameWindowPopup, span.loading").size() == 0) {
						// refresh view w/o popup
						$("#ECMContextMenu a[exo\\:attr='RefreshView'] i").click();
					}
				}
			}
		};
		
		/**
		 * Show messages from drive info (if exists).
		 */
		var driveMessage = function(drive) {
			if (drive.messages.length > 0) {
				for (var i=0; i < drive.messages.length; i++) {
					var message = drive.messages[i];
					if (message.type == "ERROR") {
						cloudDriveUI.showError("Synchronization error", message.text);
					} else if (message.type == "WARN") {
						cloudDriveUI.showWarn("Warning", message.text);
					} else if (message.type == "INFO") {
						cloudDriveUI.showInfo("Information", message.text);
					}
				}	
			}
		};

		this.connectState = function(checkUrl, docsUrl, docsOnclick) {
			var task;
			if (tasks) {
				// add check task to get user notified in case of leaving this
				// page
				task = "cloudDriveUI.connectState(\"" + checkUrl + "\", \"" + docsUrl + "\", \"" + docsOnclick + "\");";
				tasks.add(task);
			} else {
				utils.log("Tasks not defined");
			}

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
				$.pnotify({
				  title : "Your " + state.drive.provider.serviceName + " connected!",
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
				driveMessage(state.drive);
			});
			state.fail(function(state) {
				var message;
				if (state.drive && state.drive.provider) {
					message = "Error connecting your " + state.drive.provider.serviceName;
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
			state.always(function() {
				if (task) {
					tasks.remove(task);
				}
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
			  nonblock : true,
			  nonblock_opacity : .25,
			  width : NOTICE_WIDTH
			});

			// show close button in 20s
			var removeNonblock = setTimeout(function() {
				notice.pnotify({
					nonblock : false
				});
			}, 20000);

			var update = function() {
				var options = {
				};
				if (progress > 0) {
					options.text = progress + "% complete.";
				}
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
					options.width = NOTICE_WIDTH;
					// options.min_height = "300px";
					options.nonblock = false; // remove non-block
				}
				notice.pnotify(options);
			};
			
			process.progress(function(state) {
				if (!task) {
					// start progress
					progress = state.progress;
					if (progress > 0) {
						driveName = state.drive.provider.serviceName;
						
						notice.pnotify({
						  title : "Connecting Your " + driveName,
						  text : progress + "% complete."
						});
	
						// hide title in 5sec
						hideTimeout = setTimeout(function() {
							notice.pnotify({
							  title : false,
							  width : "200px"
							});
						}, 5000);
	
						// add as tasks also
						if (tasks) {
							var docsUrl = ", \"" + location + "\"";
							var docsOnclick = personalDocumentsLink();
							docsOnclick = docsOnclick ? ", \"" + docsOnclick + "\"" : "";
							// TODO this doesn't work in CW4
							task = "cloudDriveUI.connectState(\"" + state.serviceUrl + "\"" + docsUrl + docsOnclick + ");";
							tasks.add(task);
						} else {
							utils.log("Tasks not defined");
						}
					}
				} else {
					// continue progress
					driveName = state.drive.provider.serviceName; // need update drive name
					progress = state.progress;
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
					refresh();
					driveMessage(state.drive);
					setTimeout(function() {
						// start sync automatically but a bit later
						cloudDrive.synchronize();
					}, 10000);		
				}, 3000);
			});

			process.always(function() {
				if (task) {
					tasks.remove(task);
				}
			});

			process.fail(function(message, title) {
				if (hideTimeout) {
					clearTimeout(hideTimeout);
				}
				
				// when message undefined/null then process failure silently
				if (message) {
					var options = {
					  text : message,
					  title : title ? title : "Error connecting " + (driveName ? driveName : "drive") + "!",
					  type : "error",
					  hide : false,
					  delay : 0,
					  closer : true,
					  sticker : false,
					  icon : "picon picon-process-stop",
					  opacity : 1,
					  shadow : true,
					  width : NOTICE_WIDTH,
					  // remove non-block
					  nonblock : false
					};
					notice.pnotify(options);
					refresh();
				} else {
					var options = {
					  title : "Canceled",
					  type : "information",
						hide : true,
					  delay : 1500,
					  closer : true,
					  sticker : false,
			  		icon : "picon-dialog-information",
					  opacity : 1,
					  shadow : true,
					  width : NOTICE_WIDTH,
					  nonblock : true
					};
					notice.pnotify(options);
				}
			});
		};

		/**
		 * UI support for synchronization deferred process.
		 */
		this.synchronizeProcess = function(process) {
			process.done(function(updated, drive) {
				if (drive.messages.length > 0) {
					for (var i=0; i < drive.messages.length; i++) {
						var message = drive.messages[i];
						if (message.type == "ERROR") {
							cloudDriveUI.showError("Synchronization error", message.text);
						} else if (message.type == "WARN") {
							cloudDriveUI.showWarn("Warning", message.text);
						} else if (message.type == "INFO") {
							cloudDriveUI.showInfo("Information", message.text);
						}
					}	
				}
				if (updated > 0 || drive.messages.length > 0) {
					// refresh on success
					refresh();
				} else {
					// file list will be updated by special periodic job (interval)
					initFileList();
				}
			});
			process.fail(function(response, status, err) {
				if (status == 403 && response.name) {
					// assuming provider object in response
					cloudDriveUI.showWarn("Renew access to your " + response.name,
					    "Start <a class='cdSynchronizeProcessAction' href='javascript:void(0);'" 
					    + " style='cursor: pointer; border-bottom: 1px dashed #999; display: inline;'>"
					    + " synchronization</a> to update access permissions.</div>", 
					    function(pnotify) {
						$(pnotify.text_container).find("a.cdSynchronizeProcessAction").click(function() {
							cloudDrive.synchronize(this);
						});
					});
				} else if (status == 403 && response.error === ACCESS_DENIED) {
					// ignore silently
				} else if (status == 404) {
					if (response.error === NODE_NOT_FOUND) {
						// context file not found, warn user
						cloudDriveUI.showInfo("Your session updated", response.message ? response.message : response);
					} else if (response.error === DRIVE_REMOVED) {
						// do nothing
					}
				} else if (status != 0) {
					var message;
					if (response) { 
						if (response.message) {
							message = response.message + " ";
						} else {
							message = response + " ";
						}
					} else {
						message = "";
					}
					if (status) {
						message += "(" + status + ")";
					}
					cloudDriveUI.showError("Error Synchronizing Drive", message);
				} // if status == 0 we go silently - it's server or network down
			});
		};

		/**
		 * Refresh WCM explorer documents.
		 */
		this.refreshDocuments = function(currentNodePath) {
			refresh();
		};

		/**
		 * Open pop-up for Cloud Drive authentication.
		 */
		this.connectDriveWindow = function(authURL) {
			var w = 850;
			var h = 600;
			var left = (screen.width / 2) - (w / 2);
			var top = (screen.height / 2) - (h / 2);
			return window.open(authURL, 'contacts', 'width=' + w + ',height=' + h + ',top=' + top + ',left=' + left);
		};

		/**
		 * Init all UI (dialogs, menus, views etc).
		 */
		this.init = function() {
			// Add Connect Drive action
			// init CloudDriveConnectDialog popup
			$("i[class*='uiIconEcmsConnect']").each(function() {
				if (!$(this).data("cd-connect")) {
					var providerId = $(this).attr("provider-id");
					if (providerId) {
						// in Connect Cloud Documents popup
						$(this).data("cd-connect", true);
						$(this).parent().parent().click(function() {
							var formId = $("div.UIForm.ConnectCloudDriveForm").attr("form-id");
							if (formId) {
								var submited = false;
								var process = cloudDrive.connect(providerId);
								process.progress(function() {
									if (!submited) {
										submited = true;
										eXo.webui.UIForm.submitForm(formId, 'Connect', true);
									}
								});
								process.fail(function(e) {
									//eXo.webui.UIForm.submitForm(formId, 'Cancel', true);
								});
							} else {
								utils.log("ERROR: Attribute form-id not found on ConnectCloudDriveForm");
							}
						});
					} else {
						// in Action bar
						var t = $(this).parent().parent().attr("onclick");
						if (t) {
							var c = t.split("//");
							if (c.length >= 3) {
								var providerId = c[1];
								$(this).data("cd-connect", true);
								$(this).parent().parent().click(function() {
									cloudDrive.connect(providerId);
								});
							}
						}
					}
				}
			});

			// init doc view (list or file view)
			initDocument();
			
			// init menus below
			
			// XXX using deprecated DOMNodeInserted and the explorer panes selector
			// choose better selector to get less events here for DOM, now it's tens of events
			// reloading during the navigation
			var ieVersion = getIEVersion();
			var domEvent = ieVersion > 0 && ieVersion < 9.0 ? "onpropertychange" : "DOMNodeInserted"; // DOMSubtreeModified
			$(".PORTLET-FRAGMENT").on(domEvent, ".LeftCotainer, .RightCotainer", function(event) { // #UIJCRExplorerPortlet
				if (!initLock) {
					initLock = setTimeout(function() {
						initDocument();
						setTimeout(function() {
							initLock = null;
						}, 1000);
					}, 200);
				}
				return true;
			});

			function filterActions(objId, menu, params) {
				if (params) {
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

					var drive = cloudDrive.getContextDrive();
					if (drive) {
						if (cloudDrive.isContextFile()) {
							// it's drive's file
							return initContextMenu(menu, params, ALLOWED_FILE_MENU_ACTIONS);
						} else if (cloudDrive.isContextDrive()) {
							// it's drive in the context
							return initContextMenu(menu, params, ALLOWED_DRIVE_MENU_ACTIONS);
						} else if (cloudDrive.isContextLocal()) {
							// it's local node in the drive context
							return initContextMenu(menu, params, ALLOWED_LOCAL_FILE_MENU_ACTIONS);
						} 
						// selected node not a cloud drive or its file
					}
				}
				return params;
			}

			// tuning of single-selection context menu (used in Simple/Icon view)
			if (typeof uiRightClickPopupMenu.__cw_overridden == "undefined") {
				uiRightClickPopupMenu.clickRightMouse_orig = uiRightClickPopupMenu.clickRightMouse;
				uiRightClickPopupMenu.clickRightMouse = function(event, elemt, menuId, objId, params, opt) {
					var filteredParams = filterActions(objId, elemt, params);
					if (filteredParams.length > 0) {
						uiRightClickPopupMenu.clickRightMouse_orig(event, elemt, menuId, objId, filteredParams, opt);
					}
				};

				uiRightClickPopupMenu.__cw_overridden = true;
			}

			var fileView = uiFileView.UIFileView;
			//var listView = uiListView.UIListView; // TODO not used in clouddrive.js
			var simpleView = uiSimpleView.UISimpleView;

			if (typeof fileView.__cw_overridden == "undefined") {
				// clickRightMouse will be invoked on single-selection in List/Admin view
				fileView.clickRightMouse_orig = fileView.clickRightMouse;
				fileView.clickRightMouse = function(event, elemt, menuId, objId, whiteList, opt) {
					fileView.clickRightMouse_orig(event, elemt, menuId, objId, filterActions(objId, elemt, whiteList), opt);
				};

				// showItemContextMenu will be invoked on multi-selection in List/Admin view
				fileView.showItemContextMenu_orig = fileView.showItemContextMenu;
				fileView.showItemContextMenu = function(event, element) {
					// run original
					fileView.showItemContextMenu_orig(event, element);
					// and hide all not allowed
					initMultiContextMenu();
					// seems we need this
					eXo.ecm.ECMUtils.loadContainerWidth();
				};

				fileView.__cw_overridden = true;
			}

			function fixContextMenuPosition() {
				// code adopted from original showItemContextMenu() in UISimpleView.js
				var X = event.pageX || event.clientX;
				var Y = event.pageY || event.clientY;
				var portWidth = $(window).width();
				var portHeight = $(window).height();
				var contextMenu = $("#JCRContextMenu");
				var contentMenu = contextMenu.children("div.uiRightClickPopupMenu:first")[0];
				if (event.clientX + contentMenu.offsetWidth > portWidth) X -= contentMenu.offsetWidth;
				if (event.clientY + contentMenu.offsetHeight > portHeight) Y -= contentMenu.offsetHeight + 5;
				contextMenu.css("top", Y + 5 + "px");
				contextMenu.css("left", X + 5 + "px");
			}

			if (typeof simpleView.__cw_overridden == "undefined") {
				// tune multi-selection menu
				// showItemContextMenu will be invoked on multi-selection in Simple/Icon view
				simpleView.showItemContextMenu_orig = simpleView.showItemContextMenu;
				simpleView.showItemContextMenu = function (event, element) {
					// run original
					simpleView.showItemContextMenu_orig(event, element);
					// and hide all not allowed
					if (initMultiContextMenu() > 0) {
						// and fix menu position
						fixContextMenuPosition();
					} else {
						// if nothing shown, hide the menu
						simpleView.hideContextMenu();
					}
				};
				
				// hide ground-context menu for drive folder
				simpleView.showGroundContextMenu_orig = simpleView.showGroundContextMenu;
				simpleView.showGroundContextMenu = function(event, element) {
					simpleView.showGroundContextMenu_orig(event, element);
					if (cloudDrive.isContextDrive() || cloudDrive.isContextFile()) {
						// hide all not allowed for cloud drive
						if (initMultiContextMenu() > 0) {
							// and fix menu position
							fixContextMenuPosition();
						} else {
							// if nothing shown, hide the menu
							simpleView.hideContextMenu();
						}
					}
				};

				simpleView.__cw_overridden = true;
			}
		};

		/**
		 * Render given connected drive nodes in ECM documents view with branded styles of drive
		 * providers.
		 */
		this.initConnected = function(map) {
			// map: name = providerId
			var files = [];
			var styleSize;
			var target = $("div.actionIconBox");
			var tree;
			if (target.size() > 0) {
				styleSize = "uiIcon64x64"; // Icon view
				tree = $("#UITreeExplorer li.node");
			} else {
				styleSize = "uiIcon24x24"; // List or Admin view
				target = $("div.rowView");
			}
			for (name in map) {
				if (map.hasOwnProperty(name)) {
					var providerId = map[name];
					var cname = styleSize + "CloudDrive-" + providerId;
					$(target).each(function(i, item) {
						if ($(item).find("span.nodeName:contains('" + name + "')").size() > 0) {
							$(item).find("div." + styleSize + "nt_folder:not(:has(div." + cname + "))").each(function() {
								$("<div class='" + cname + "'></div>").appendTo(this);
							});
						}
					});
					if (tree) {
						cname = "uiIcon16x16CloudDrive-" + providerId;
						$(tree).each(function() {
							$(this).find("span.nodeName:contains('" + name + "')").each(function() {
								$(this).siblings("i.uiIcon16x16nt_folder:not(:has(div." + cname + "))").each(function() {
									$("<div class='" + cname + "'></div>").appendTo(this);
								});
							});
						});
					}
				}
			}
		};
		
		this.initSharing = function() {
			var $permission = $("#UISharingManager .permission");
			if ($permission.size() > 0) {
				$permission.find("label.checkbox").hide();
				if ($permission.find("input[name='userOrGroup']").val().length > 0) {
					$permission.find("input[name='read']").prop("checked", true);
				}
			}
		};
		
		/**
		 * Init text file viewer (based on Codemirror).
		 */
		this.initTextViewer = function() {
			// Add an action to "show plain text" tab to load the frame content to the code viewer
			var $viewer = $("#WebContent");
			var $code = $viewer.find("#TabCode");
			
			if ($viewer.size() > 0) {
				// load Codemirror styles only if text editor tab exists 
				// and only in window (not in iframe as gadgets may do) 
				if (window == top) {
					try {
						utils.loadStyle("/cloud-drive/skin/codemirror.css");
					} catch(e) {
						utils.log("Error intializing text viewer.", e);
					}
				}
			}

			function createViewer(codeURL) {
				var cursorCss = $viewer.css("cursor");
				$viewer.css("cursor", "wait");

				var contextFile = cloudDrive.getContextFile();
				utils.log("contextFile: " + contextFile.path + " " + contextFile.typeMode);
				if (!codeURL && contextFile) {
					codeURL = contextFile.previewLink;
				}

				$.get(codeURL, function(data, status, jqXHR) {
					try {
						var code = jqXHR.responseText;
						var codeMode = jqXHR.getResponseHeader("x-type-mode");
						if (!codeMode) {
							var contentType = jqXHR.getResponseHeader("Content-Type");
							if (contentType) {
								codeMode = contentType;
							} else {
								codeMode = "htmlmixed";
							}
						}

						// XXX CodeMirror script already minified and cannot be loaded via PLF AMD mechanism
						// load it be direct path from the server
						// FYI PLF's RequireJS baseUrl is /portal/intranet, thus we need relative moduleId to reach the WAR location
						// after all this CodeMirror will be available globally as 'require' inside the module wrapper doesn't have amd
						// function.
						window.require(["../../cloud-drive/js/codemirror-bundle.min"], function() {
							CodeMirror($code.get(0), {
								value : code,
								lineNumbers : true,
								readOnly : true,
								mode : codeMode
							});
						});
					} catch(e) {
						utils.log("ERROR: CodeMirror creation error " + provider.name + "(" + provider.id + "). " + e.message + ": " + JSON.stringify(e));
					} finally {
						$viewer.css("cursor", cursorCss);
					}
				});
			}

			var $codeSwitch = $("#FileCodeSwitch");
			$codeSwitch.click(function() {
				if ($code.size() > 0 && $code.children().size() == 0) {
					var codeURL = $viewer.find("iframe").attr("src");
					createViewer(codeURL);
				}
			});

			if ($code.is(".active")) {
				// it is XML document, click to load its content
				$codeSwitch.click();
				$viewer.find("#TabHTML").css("display", "");
			} else if ($code.is(":visible")) {
				createViewer(null);
			}

			// XXX remove display: block, it appears for unknown reason
			$code.css("display", "");
		};
		
		/**
		 * Show notice to user. Options support "icon" class, "hide", "closer" and "nonblock" features.
		 */
		this.showNotice = function(type, title, text, options) {
			var noticeOptions = {
			  title : title,
			  text : text,
			  type : type,
			  icon : "picon " + (options ? options.icon : ""),
			  hide : options && typeof options.hide != "undefined" ? options.hide : false,
			  closer : options && typeof options.closer != "undefined" ? options.closer : true,
			  sticker : false,
			  opacity : .75,
			  shadow : true,
			  width : options && options.width ? options.width : NOTICE_WIDTH,
			  nonblock : options && typeof options.nonblock != "undefined" ? options.nonblock : false,
			  nonblock_opacity : .25,
			  after_init : function(pnotify) {
				  if (options && typeof options.onInit == "function") {
					  options.onInit(pnotify);
				  }
			  }
			};

			return $.pnotify(noticeOptions);
		};

		/**
		 * Show error notice to user. Error will stick until an user close it.
		 */
		this.showError = function(title, text, onInit) {
			return cloudDriveUI.showNotice("error", title, text, {
			  icon : "picon-dialog-error",
			  hide : false,
			  delay : 0,
			  onInit : onInit
			});
		};

		/**
		 * Show info notice to user. Info will be shown for 8sec and hidden then.
		 */
		this.showInfo = function(title, text, onInit) {
			return cloudDriveUI.showNotice("info", title, text, {
			  hide : true,
			  delay : 8000,
			  icon : "picon-dialog-information",
			  onInit : onInit
			});
		};

		/**
		 * Show warning notice to user. Info will be shown for 8sec and hidden then.
		 */
		this.showWarn = function(title, text, onInit) {
			return cloudDriveUI.showNotice("exclamation", title, text, {
			  hide : false,
			  delay : 30000,
			  icon : "picon-dialog-warning",
			  onInit : onInit
			});
		};
	}

	var cloudDrive = new CloudDrive();
	var cloudDriveUI = new CloudDriveUI();

	// Load CloudDrive dependencies only in top window (not in iframes of gadgets).
	if (window == top) {
		try {
			// load required styles
			utils.loadStyle("/cloud-drive/skin/jquery-ui.css");
			utils.loadStyle("/cloud-drive/skin/jquery.pnotify.default.css");
			utils.loadStyle("/cloud-drive/skin/jquery.pnotify.default.icons.css");
			utils.loadStyle("/cloud-drive/skin/clouddrive.css");

			// configure Pnotify
			$.pnotify.defaults.styling = "jqueryui"; // use jQuery UI css
			$.pnotify.defaults.history = false; // no history roller in the
			// right corner
		} catch(e) {
			utils.log("Error configuring Cloud Drive style.", e);
		}
	}

	return cloudDrive;

})($, cloudDriveUtils, cloudDriveTasks, uiRightClickPopupMenu, uiListView, uiSimpleView, uiFileView);

