/**
 * CloudDrive WebUI integration.
 */
(function($, utils, tasks, uiRightClickPopupMenu, uiListView, uiSimpleView, uiFileView) {

	/**
	 * Connector class.
	 */
	function CloudDrive() {
		var prefixUrl = utils.pageBaseUrl(location);

		var contextNode; // for Node's workspace and path
		var connectProvider = {}; // for Provider's id and authUrl
		var contextDrive;
		var excluded = {};
		var updateProvider;
		var activeSyncs = []; // array of drives doing synchronization
		var autoSyncs = {}; // active auto-synchronization jobs

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
					} catch (e) {
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
					} catch (e) {
						// not JSON
						data = jqXHR.responseText;
					}
					// in err - textual portion of the HTTP status, such as "Not
					// Found" or "Internal Server Error."
					process.reject(data, jqXHR.status, err);
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
		}

		var connectInit = function(providerId, callbacks) {
			var request = $.ajax({
			  type : "GET",
			  url : prefixUrl + "/portal/rest/clouddrive/connect/init/" + providerId,
			  dataType : "json"
			});

			initRequestDefaults(request, callbacks);
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
		}
		
		var changesPost = function(workspace, path) {
			var request = $.ajax({
			  async : true, 
			  type : "POST",
			  url : prefixUrl + "/portal/rest/clouddrive/changes",
			  dataType : "json",
			  data : {
			    workspace : workspace,
			    path : path
			  }
			});

			return initRequest(request);
		}
		
		var changesLinkGet = function(workspace, path) {
			var request = $.ajax({
			  async : true, 
			  type : "GET",
			  url : prefixUrl + "/portal/rest/clouddrive/changes/link",
			  dataType : "json",
			  data : {
			    workspace : workspace,
			    path : path
			  }
			});

			return initRequest(request);
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

		// TODO deprecated
		var connectDriveOld = function(providerId, authUrl) {
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
				  utils.log(provider.name + " connect initialized.");
				  if (auth) {
					  auth(provider.authUrl);
				  }
				  // 2 wait for authentication
				  waitAuth(provider, {
				    done : function() {
					    utils.log(provider.name + " user authenticated.");
					    // 3 and finally connect the drive
					    var userNode = contextNode;
					    if (userNode) {
						    utils.log("Connecting Cloud Drive to node " + userNode.path + " in "
						        + userNode.workspace);

						    var post = connectPost(userNode.workspace, userNode.path);
						    post.done(function(state, status) {
							    utils.log("Connect requested: " + status + ". ");
							    if (state) {
								    if (status == 201) {
									    utils.log("DONE: " + provider.name + " successfully connected.");
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
								    utils.log("ERROR: " + provider.name + " connect return null state.");
								    process.reject("Cannot connect " + provider.name
								        + ". Server return empty response.");
							    }
						    });
						    post.fail(function(state, error, errorText) {
							    utils.log("ERROR: " + provider.name + " connect failed: " + error + ". ");
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
					    utils.log("ERROR: " + provider.name + " authentication error: " + error);
					    process.reject(error);
				    },
				    timeout : function() {
					    utils.log("ERROR: " + provider.name + " user not authenticated in 2 minutes.");
					    process.reject("Authentication timeout.");
				    }
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

		// TODO deprecated
		var waitAuthOld = function(provider, callbacks) {
			var i = 0;
			var intervalId = setInterval(function() {
				var connectId = utils.getCookie("cloud-drive-connect-id");
				if (connectId) {
					intervalId = clearInterval(intervalId);
					callbacks.done();
				} else {
					var error = utils.getCookie("cloud-drive-error");
					if (error) {
						intervalId = clearInterval(intervalId);
						callbacks.error(error);
					} else if (i > 120) {
						// if open more 2min - close it and treat as not authenticated/allowed
						intervalId = clearInterval(intervalId);
						callbacks.timeout();
					}
				}
				i++;
			}, 1000);
		}

		var connectDrive = function(providerId, authUrl) {
			var authWindow;
			var authService;
			if (authUrl) {
				// use user interaction for authentication
				authWindow = cloudDriveUI.connectDriveWindow(authUrl);
			} else {
				// function to call for auth using authUrl from provider
				authService = serviceGet;
			}

			// 1 initialize connect workflow
			var process = $.Deferred();
			connectInit(providerId, {
			  done : function(provider) {
				  utils.log(provider.name + " connect initialized.");
				  if (authService) {
				  	authService(provider.authUrl);
				  }
				  // 2 wait for authentication
				  var auth = waitAuth(authWindow);
				  auth.done(function() {
				  	utils.log(provider.name + " user authenticated.");
				    // 3 and finally connect the drive
				    var userNode = contextNode;
				    if (userNode) {
					    utils.log("Connecting Cloud Drive to node " + userNode.path + " in "
					        + userNode.workspace);

					    var post = connectPost(userNode.workspace, userNode.path);
					    post.done(function(state, status) {
						    utils.log("Connect requested: " + status + ". ");
						    if (state) {
							    if (status == 201) {
								    utils.log("DONE: " + provider.name + " successfully connected.");
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
							    utils.log("ERROR: " + provider.name + " connect return null state.");
							    process.reject("Cannot connect " + provider.name
							        + ". Server return empty response.");
						    }
					    });
					    post.fail(function(state, error, errorText) {
						    utils.log("ERROR: " + provider.name + " connect failed: " + error + ". ");
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
				  });
				  auth.fail(function(error) {
				  	utils.log("ERROR: " + provider.name + " authentication error: " + error);
				    process.reject(error);
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
					intervalId = clearInterval(intervalId);
					process.resolve();
				} else {
					var error = utils.getCookie("cloud-drive-error");
					if (error) {
						intervalId = clearInterval(intervalId);
						process.reject(error);
					} else if (authWindow && authWindow.closed) {
						intervalId = clearInterval(intervalId);
						process.reject("Authentication canceled.");
					} else if (i > 120) {
						// if open more 2min - close it and treat as not authenticated/allowed
						intervalId = clearInterval(intervalId);
						process.reject("Authentication timeout.");
					}
				}
				i++;
			}, 1000);
			return process.promise();
		}

		var connectCheck = function(checkUrl) {
			var process = $.Deferred();
			var serviceUrl = checkUrl;
			// if Accepted start Interval to wait for Created
			var intervalId = setInterval(function() {
				// use serviceUrl to check until 201/200 will be
				// returned or an error
				var check = serviceGet(serviceUrl);
				check.done(function(state, status) {
					if (status == "204") {
						// No content - not a cloud drive or
						// drive not connected, or not to this
						// user.
						// This also might mean an error as
						// connect not active but the drive not
						// connected.
						process
						    .reject("Drive not connected. Check if no other connection active and try again.");
					} else if (state && state.serviceUrl) {
						serviceUrl = state.serviceUrl;
						if (status == "201" || status == "200") {
							// created or ok - drive
							// successfully connected
							// or appears as already connected
							// (by another request)
							process.resolve(state);
							utils.log("DONE: " + status + " " + state.drive.provider.name
							    + " connected successfully.");
						} else if (status == "202") {
							// else inform progress and continue
							// to wait created
							process.notify(state);
							utils.log("PROGRESS: " + status + " " + state.drive.provider.name
							    + " connectCheck progress " + state.progress);
						} else {
							// unexpected status, wait for
							// created
							utils.log("WARN: unexpected status in connectCheck:" + status);
						}
					} else {
						utils.log("ERROR: " + status + " connectCheck return wrong state.");
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
					utils.log("ERROR: Connect check error: " + error + ". " + JSON.stringify(state));
					if (typeof state === "string") {
						process.reject(state);
					} else {
						process.reject("Internal error: " + (state && state.error ? state.error : error + " " + errorText) + ". Try again later.");
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

		var getFileLink = function(nodePath) {
			var file = contextDrive.files[nodePath];
			return file ? file.link : null;
		}

		var addExcluded = function(path) {
			excluded[path] = true;
		};

		var isExcluded = function(path) {
			return excluded[path] === true;
		};
		
		var autoSynchronize = function() {
			if (contextNode && contextDrive) {
				var driveWorkspace = contextNode.workspace;
				var drive = contextDrive;
				var drivePath = drive.path;
				var syncName = driveWorkspace + ":" + drivePath;
				
				if (!autoSyncs[syncName]) {
					/** Synchronization job */
					if (drive.changesLink) {
						// use long-polling from cloud provider
						function scheduleSync() {
							autoSyncs[syncName] = setTimeout(changesPoll, 100);
						}
						function changesPoll() {
							var changes = serviceGet(drive.changesLink);
							changes.done(function(info) {
								if (info.message) {
									// XXX hardcode for Box only now
									// http://developers.box.com/using-long-polling-to-monitor-events/
									if (info.message == "new_change") {
										var syncProcess = synchronize(driveWorkspace, drivePath); // drive needs synchronization
										syncProcess.done(function() {
											scheduleSync(); // re-schedule
										});
										syncProcess.fail(function() {
											delete autoSyncs[syncName]; // cancel and cleanup
										});
									} else if (info.message == "reconnect") {
										// update change link and re-schedule
										var newLink = changesLinkGet(driveWorkspace, drivePath);
										newLink.done(function(res) {
											utils.log("New changes link: " + res.changesLink);
											drive.changesLink = res.changesLink;
											scheduleSync();
										});	
										newLink.fail(function(response, status, err) {
											utils.log("ERROR: error getting new changes link: " + err + ", " + status + ", " + response);
										});
									}
								}
							});
							changes.fail(function(response, status, err) {
								utils.log("ERROR: changes long-polling error: " + err + ", " + status + ", " + response);
								/*if (status >= 400 && status < 500) {
									// need new changes link?
								}
								*/
							});	
						}
						scheduleSync();
						utils.log("Long-polling synchronization enabled for Cloud Drive on " + syncName);
					} else {
						// run sync periodically
						var syncJob = setInterval(function() {
							utils.log("Running auto synchronization for Cloud Drive on " + syncName);
							var sync = changesPost(driveWorkspace, drivePath);
							sync.done(function(status) {
								if (status.syncDone) {
									utils.log("Auto synchronization done for Cloud Drive on " + syncName);
								} else {
									utils.log("Auto synchronization in progress for Cloud Drive on " + syncName);
								}
							});	
							sync.fail(function(response, status, err) {
								utils.log("ERROR: auto synchronization error: " + err + ", " + status + ", " + response);
								clearInterval(syncJob);
								delete autoSyncs[syncName];
							});
						}, 15000);
						autoSyncs[syncName] = syncJob;
						utils.log("Periodical synchronization enabled for Cloud Drive on " + syncName);
					}
				}
			}
		}

		var synchronize = function(nodeWorkspace, nodePath) {
			var process = $.Deferred();
			cloudDriveUI.synchronizeProcess(process.promise());

			var initiator = $.Deferred();
			initiator.done(function() {
				// sync and load all files related to this drive
				var sync = synchronizePost(nodeWorkspace, nodePath);
				sync.contextDrive = contextDrive;
				activeSyncs.push(sync);
				sync.done(function(drive, status) {
					if (status == 204) {
						// drive was removed 
						process.reject("Cloud Drive removed", status);
					} else {
						var files = 0;
						var folders = 0;
						var changed = 0;
						for (var fpath in drive.files) {
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
						for (var fpath in sync.contextDrive.files) {
							if (!drive.files[fpath]) {
								drive.files[fpath] = sync.contextDrive.files[fpath];
							}
						}
	
						utils.log("DONE: Synchronized " + changed + " changes from Cloud Drive on "
						    + nodeWorkspace + ":" + nodePath);
	
						if (changed > 0 && sync.contextDrive == contextDrive) {
							// using new drive in the context (only if context wasn't changed)
							contextDrive = drive;
						}
	
						process.resolve(files, folders, drive);
					}
				});
				sync.fail(function(response, status, err) {
					utils.log("ERROR: synchronization error: " + err + ", " + status + ", " + response);
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
				// previous attempt tells us we have to update access keys - reconnect
				var connect = connectDrive(updateProvider.id, updateProvider.authUrl);
				connect.done(function(state) {
					initiator.resolve();
				});
			} else {
				initiator.resolve();
			}
			
			return process.promise();
		};

		
		/**
		 * Synchronize documents view.
		 */
		this.synchronize = function(elem, objectId) {
			if (contextNode) {
				var nodePath = contextNode.path;
				var nodeWorkspace = contextNode.workspace;
				utils.log("Synchronizing Cloud Drive on " + nodeWorkspace + ":" + nodePath);
				synchronize(nodeWorkspace, nodePath);
			} else {
				utils.log("WARN Nothing to synchronize!");
				cloudDriveUI.refreshDocuments(); // refresh WCM explorer
			}
		};

		/**
		 * Connect to Cloud Drive.
		 */
		this.connect = function(providerId, authUrl, userNode, userWorkspace) {
			utils.log("Connecting Cloud Drive...");

			if (!authUrl) {
				// TODO cleanup // providerId = connectProvider.id;
				authUrl = connectProvider[providerId];
				if (!authUrl) {
					utils.log("ERROR: Authentication URL not found for " + providerId);
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

			var process = connectDrive(providerId, authUrl);
			cloudDriveUI.connectProcess(process);
		};

		this.state = function(checkUrl) {
			return connectCheck(checkUrl);
		};

		/**
		 * Initialize provider for connect operation.
		 */
		this.initProvider = function(id, authUrl) {
			// connectProvider = {
			// id : id,
			// authUrl : authUrl
			// };
			connectProvider[id] = authUrl;
		};

		/**
		 * Auth URL for given Id of a provider.
		 */
		this.getAuthUrl = function(id) {
			return connectProvider[id];
		};

		/**
		 * Initialize context and UI.
		 */
		this.init = function(nodeWorkspace, nodePath) {
			try {
				cloudDrive.initContext(nodeWorkspace, nodePath);
				// and on-page-ready initialization of Cloud Drive UI
				$(function() {
					try {
						cloudDriveUI.init();
					} catch (e) {
						utils.log("Error initializing Cloud Drive UI " + e, e);
					}
				});
			} catch (e) {
				utils.log("Error initializing Cloud Drive " + e, e);
			}
		};

		/**
		 * Initialize context node and optionaly drive.
		 */
		this.initContext = function(nodeWorkspace, nodePath) {
			utils.log("Init context node: " + nodeWorkspace + ":" + nodePath
			    + (contextDrive ? ", drive: " + contextDrive.path : "") + " excluded: "
			    + isExcluded(nodePath));

			contextNode = {
			  workspace : nodeWorkspace,
			  path : nodePath
			};

			if (!isExcluded(nodePath)) {
				// XXX do this to support symlinks outside the drive
				if (contextDrive && nodePath.indexOf(contextDrive.path) == 0
				    && nodePath != contextDrive.path) {
					var file = contextDrive.files[nodePath];
					if (!file) {
						// file not cached, get the file from the server and
						// cache it locally
						getFile(nodeWorkspace, nodePath, {
						  fail : function(err, status) {
							  utils.log("ERROR: Cloud Drive file " + nodeWorkspace + ":" + nodePath
							      + " cannot be read: " + err + " (" + status + ")");
							  cloudDriveUI.showError("Error reading drive file", err ? err : "Internal error. Try again later.");
						  },
						  done : function(file, status) {
							  if (status != 204) {
								  contextDrive.files[nodePath] = file;
							  } else {
								  addExcluded(nodePath);
							  }
						  }
						});
					}
				} else {
					// load all files related to this drive
					getDrive(nodeWorkspace, nodePath, {
					  fail : function(err, status) {
						  utils.log("ERROR: Cloud Drive " + nodeWorkspace + ":" + nodePath
						      + " cannot be read: " + err + " (" + status + ")");
						  cloudDriveUI.showError("Error reading drive", err ? err : "Internal error. Try again later.");
					  },
					  done : function(drive, status) {
						  if (status != 204) {
							  if (contextDrive && contextDrive.path == drive.path) {
								  // XXX same drive, probably nodePath is a symlink path,
								  // use already cached files with new drive
								  for ( var fpath in contextDrive.files) {
									  if (contextDrive.files.hasOwnProperty(fpath)
									      && !drive.files.hasOwnProperty(fpath)) {
										  drive.files[fpath] = contextDrive.files[fpath];
									  }
								  }
							  }
							  contextDrive = drive;
						  } else {
							  // utils.log("Not a cloud drive: " + nodePath); // it's not a Cloud Drive
							  addExcluded(nodePath);
						  }
					  }
					});
				}
				if (contextDrive) {
					autoSynchronize();
				}
			} // else already cached as not in drive
		};

		this.getContextDrive = function() {
			return contextDrive;
		};

		this.getContextFile = function() {
			if (contextNode && contextDrive) {
				return contextDrive.files[contextNode.path];
			}
			return null;
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

		this.openFile = function(elem, objectId) {
			var file = cloudDrive.getContextFile();
			if (file) {
				window.open(file.link);
			} else {
				utils.log("No context path to open as Cloud File");
			}
		};
	}

	/**
	 * Integration class.
	 */
	/**
	 * WebUI integration.
	 */
	function CloudDriveUI() {
		var NOTICE_WIDTH = "320px"

		var MENU_OPEN_FILE = "OpenCloudFile";
		var MENU_REFRESH_DRIVE = "RefreshCloudDrive";
		var DRIVE_MENU_ACTIONS = [ MENU_OPEN_FILE, MENU_REFRESH_DRIVE ];
		var ALLOWED_DRIVE_MENU_ACTIONS = [ MENU_OPEN_FILE, MENU_REFRESH_DRIVE, "Delete",
		    "AddToFavourite", "RemoveFromFavourite", "ViewInfo" ];
		var ALLOWED_FILE_MENU_ACTIONS = [ MENU_OPEN_FILE, MENU_REFRESH_DRIVE, "AddToFavourite",
		    "RemoveFromFavourite", "ViewInfo" ];
		var ALLOWED_SYMLINK_MENU_ACTIONS = [ "Delete" ];

		var ALLOWED_DMS_MENU_COMMON_ACTION_CLASSES = [ "uiIconEcmsAddToFavourite",
		    "uiIconEcmsRemoveFromFavourite", "uiIconEcmsManageActions", "uiIconEcmsManageRelations",
		    "uiIconEcmsViewProperties", "uiIconEcmsManageAuditing", "uiIconEcmsOverloadThumbnail" ];
		var ALLOWED_DMS_MENU_FILE_ACTION_CLASSES = [ "uiIconEcmsOpenCloudFile",
		    "uiIconEcmsTaggingDocument", "uiIconEcmsWatchDocument", "uiIconEcmsViewMetadatas",
		    "uiIconEcmsVote", "uiIconEcmsComment" ];
		var ALLOWED_DMS_MENU_DRIVE_ACTION_CLASSES = [ "uiIconEcmsRefreshCloudDrive", "DeleteNodeIcon" ];

		var initLock = null;

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
				$("i.uiIconEcmsRefreshCloudDrive, i.uiIconEcmsOpenCloudFile").each(function() {
					$(this).attr("class", $(this).attr("class") + " uiIcon16x16CloudFile-" + drive.provider.id);
				});
				
				// Common context menu: add links to CD actions
				$("#ECMContextMenu a[exo\\:attr='" + MENU_OPEN_FILE + "']").each(function() {
					var text = $(this).data("cd_action_prefix");
					if (!text) {
						$(this).click(function() {
							cloudDrive.openFile();
							uiFileView.UIFileView.clearCheckboxes();
						});
						text = $(this).text();
						$(this).data("cd_action_prefix", text);
					}
					var i = $(this).find("i");
					text = text + drive.provider.name;
					$(this).text(text);
					$(this).prepend(i);
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
					var i = $(this).find("i");
					text = text + drive.provider.name;
					$(this).text(text);
					$(this).prepend(i);
				});

				if (cloudDrive.isContextSymlink()) {
					allowedItems = allowedItems.concat(ALLOWED_SYMLINK_MENU_ACTIONS);
				}

				// Custom context menu links
				if (menu) {
					var file = cloudDrive.getContextFile();
					var link;
					if (file) {
						link = file.link;
					} else {
						link = window.location; // TODO use drive's link
					}

					$(menu)
					    .find("li.menuItem")
					    .each(
					        function() {
						        $(this)
						            .find("i.uiIconDownload")
						            .each(
						                function() {
							                $(this).parent().attr("target", "_new");
							                $(this).parent().attr("href", link);
							                $(this)
							                    .parent()
							                    .attr("onclick",
							                        "eXo.ecm.WCMUtils.hideContextMenu(this);eXo.ecm.UIFileView.clearCheckboxes();");
						                });
						        $(this).find("i.uiIconEcmsViewDocument").each(function() {
						        });
						        $(this).find("i.uiIconEcmsCopyUrlToClipboard").each(function() {
							        $(this).parent().attr("path", link);
							        $(this).parent().click(function() {
								        eXo.ecm.ECMUtils.pushToClipboard(event, link); // TODO
								        // use
								        // require
								        uiFileView.UIFileView.clearCheckboxes();
							        });
						        });
					        });
				}

				// fix menu: keep only allowed items
				return getAllowedItems(menuItems, allowedItems);
			} else {
				// if not cloud file on context path - remove OpenCloudFile from
				// the menu
				return removeCloudItems(menuItems);
			}
		};

		var initDocument = function() {
			var drive = cloudDrive.getContextDrive();
			if (drive) {
				// Fix Action Bar items
				var classes;
				if (cloudDrive.isContextFile()) {
					// it's drive's file
					classes = ALLOWED_DMS_MENU_COMMON_ACTION_CLASSES
					    .concat(ALLOWED_DMS_MENU_FILE_ACTION_CLASSES);
				} else if (cloudDrive.isContextDrive()) {
					// it's drive in the context
					classes = ALLOWED_DMS_MENU_COMMON_ACTION_CLASSES
					    .concat(ALLOWED_DMS_MENU_DRIVE_ACTION_CLASSES);
				} else {
					// selected node not a cloud drive or its file
					return;
				}

				var allowed = "";
				$.each(classes, function(i, action) {
					allowed += (allowed ? ", ." : ".") + action;
				});

				// filter Action Bar items (depends on file/folder or the drive
				// itself in the context)
				$("#uiActionsBarContainer li a.actionIcon i").not(allowed).each(function() { // div ul li
					$(this).parent().css("display", "none");
				});
				// hack to prevent empty menu bar
				$("#uiActionsBarContainer ul")
				    .append(
				        "<li style='display: block;'><a class='actionIcon' style='height: 18px;'><i></i> </a></li>");
				
				// File Viewer
				var viewer = $("#CloudFileViewer");
				if (viewer.size() > 0) {
					var file = cloudDrive.getContextFile();
					var vswitch = $("#ViewerSwitch");
					if (vswitch.size() > 0) {
						var openOnProvider = viewer.attr("file-open-on");

						// fix title
						var title = $("div.fileContent .title");
						var titleText = title.find("div.topTitle");
						titleText.text(file.title);

						// fix Download icon, text and link
						var i = title.find("i.uiIconDownload");
						// TODO cleanup i.attr("class", "uiIconEcmsOpenCloudFile");
						i.attr("class", "uiIcon16x16CloudFile-" + drive.provider.id);
						var a = title.find("a");
						a.text(" " + openOnProvider);
						a.prepend(i);
						a.attr("href", file.link);
						a.attr("target", "_blank");
						a.css("font-weight", "normal");

						var iframe = viewer.find("iframe");
						if (file.previewLink) {
							// init Edit/View mode
							iframe.attr("src", file.previewLink);
							vswitch.find("a").click(function() {
								var currentLink = iframe.attr("src");
								if (currentLink == file.previewLink) {
									// switch to editor
									iframe.attr("src", file.link);
									var viewerTitle = vswitch.attr("view-title");
									$(this).text(viewerTitle);
								} else {
									// switch to viewer
									iframe.attr("src", file.previewLink);
									var editTitle = vswitch.attr("edit-title");
									$(this).text(editTitle);
								}
							});
							titleText.append(vswitch);
							viewer.find("div").show();
						} else {
							viewer.find("iframe").attr("src", file.link);
							vswitch.remove();
							viewer.find("div").show();
						}
					}
				}
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
		var refresh = function(allowRefresh) {
			if (allowRefresh) {
  			// try reuse Refresh action but w/o popup
				$("a.refreshIcon i.uiIconRefresh").click();
			} else {
				var personalDocs = $("div.breadcrumbLink a.nodeLabel:first");
				if (personalDocs.size() > 0) {
					// in List view
					personalDocs.click();
				} else {
					personalDocs = $("#UISideBar .title");
					if (personalDocs.size() > 0) {
						// in Icon view with tree sidebar
						personalDocs.click();
					} else {
						// try click on address bar view icon
						$(".detailViewIcon a.active").click();
						// click RefreshSession
						//$("a.refreshIcon i.uiIconRefresh").click();
					}
				}
			}
		};

		this.connectState = function(checkUrl, docsUrl, docsOnclick) {
			var task;
			if (tasks) {
				// add check task to get user notified in case of leaving this
				// page
				task = "cloudDriveUI.connectState(\"" + checkUrl + "\", \"" + docsUrl + "\", \""
				    + docsOnclick + "\");"
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

			// show close buton in 20s
			var removeNonblock = setTimeout(function() {
				notice.pnotify({
					nonblock : false
				});
			}, 20000);

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

					// add as tasks also
					if (tasks) {
						var docsUrl = ", \"" + location + "\"";
						var docsOnclick = personalDocumentsLink();
						docsOnclick = docsOnclick ? ", \"" + docsOnclick + "\"" : "";
						// TODO this doesn't work in CW4
						task = "cloudDriveUI.connectState(\"" + state.serviceUrl + "\"" + docsUrl + docsOnclick
						    + ");";
						tasks.add(task);
					} else {
						utils.log("Tasks not defined");
					}
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
					refresh();
				}, 4000);
			});

			process.always(function() {
				if (task) {
					tasks.remove(task);
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
				  width : NOTICE_WIDTH,
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
				function doneAction(pnotify) {
					$(pnotify.text_container).find("a.cdSynchronizeProcessAction").click(function() {
						//cloudDriveUI.openDrive(drive.title);
						refresh(true);
					});
				}
				var alink = "<a class='cdSynchronizeProcessAction' href='javascript:void(0);'";
				var driveLink = "<span>" + alink
				    + " style=\"curson: pointer; border-bottom: 1px dashed #999; display: inline;\">"
				    + drive.email + "</a></span>"
				var details;
				if (files + folders > 0) {
					// Don't refresh at all, as user can change the
					// view. Istead we show a link on the message.
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
					var titleLink = "<span>" + alink + ">" + drive.provider.name
					    + " Synchronized.</a></span>"
					cloudDriveUI.showInfo(titleLink, details, doneAction);
				} else {
					var titleLink = "<span>" + alink + ">" + drive.provider.name
					    + " Already Up To Date.</a></span>"
					cloudDriveUI.showInfo(titleLink, "Files on " + driveLink + " are in actual state.",
					    doneAction);
				}
			});
			process.fail(function(response, status, err) {
		    if (status == 403 && response.name) {
			    // assuming provider object in response
			    cloudDriveUI.showWarn(
            "Error Synchronizing with "
                + response.name
                + "<span>Access rewoked or outdated. Start <a class='cdSynchronizeProcessAction' href='javascript:void(0);'>"
                + "Synchronization</a> again to renew access.</span>", function(pnotify) {
	            $(pnotify.text_container).find("a.cdSynchronizeProcessAction").click(
	                function() {
		                cloudDrive.synchronize(this);
	                });
            });
		    } else if (status == 204) {
		    	// do nothing for no content - drive was removed
		    } else {
			    cloudDriveUI.showError("Error Synchronizing Drive", response + " (" + status + ")");
		    }
	    });
		};

		/**
		 * Refresh WCM explorer documents.
		 */
		this.refreshDocuments = function(currentNodePath) {
			refresh();
		};

		/**
		 * Open or refresh drive node in WCM explorer.
		 * TODO deprecated
		 */
		this.openDrive = function(title) {
			var selected = $("a.nodeName:contains('" + title + "')");
			if (selected.size() > 0) {
				// in List view
				selected.mousedown();
			} else {
				// in Icon view
				selected = $("div.actionIconBox .nodeName:contains('" + title + "')");
				if (selected.size() > 0) {
					selected.parent().parent().parent().dblclick(); // TODO
					// .parent()
				} else {
					// in Icon view - tree in side bar
					// XXX all titles in WCM tree ends with single space
					selected = $("a[data-original-title='" + title + " ']");
					if (selected.size() > 0) {
						selected.click();
					}
				}
			}

			if (selected.size() == 0) {
				utils.log("WARN: drive node '" + title + "' not found");
			}
		};

		/**
		 * Open pop-up for Cloud Drive authentication.
		 */
		this.connectDriveWindow = function(authUrl) {
			var w = 850;
			var h = 500;
			var left = (screen.width / 2) - (w / 2);
			var top = (screen.height / 2) - (h / 2);
			return window.open(authUrl, 'contacts', 'width=' + w + ',height=' + h + ',top=' + top
			    + ',left=' + left);
		};

		/**
		 * Init all UI (dialogs, menus, views etc).
		 */
		this.init = function() {
			// Add Connect Drive action
			// TODO need transparent way of adding new providers, w/o exact naming
			$("i.uiIconEcmsConnectGoogleDrive, i.uiIconEcmsConnectBox").each(function() {
				var t = $(this).parent().parent().attr("onclick");
				if (t) {
					var c = t.split("//");
					if (c.length >= 3) {
						var providerId = c[1];
						$(this).parent().parent().click(function() {
							cloudDrive.connect(providerId);
						});
					}
				}
			});

			// init doc view (list of file view)
			initDocument();

			// TODO PLF4 init on each document reload (incl. ajax calls)
			// XXX using deprecated DOMNodeInserted and the explorer panes selector
			// choose better selector to get less events here for DOM, now it's tens of events
			// reloading during the navigation
			var ieVersion = getIEVersion();
			var domEvent = ieVersion > 0 && ieVersion < 9.0 ? "onpropertychange" : "DOMNodeInserted"; // DOMSubtreeModified
			$(".PORTLET-FRAGMENT").on(domEvent, ".LeftCotainer, .RightCotainer", function(event) { // #UIJCRExplorerPortlet
				if (!initLock) {
					initLock = setTimeout(function() {
						utils.log(">>>>>>>>> initDocument");
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
						} // selected node not a cloud drive or its file
					}
				}
				return params;
			}

			// tuning of on-file context menu
			if (typeof uiRightClickPopupMenu.__cw_overridden == "undefined") {
				uiRightClickPopupMenu.clickRightMouse_orig = uiRightClickPopupMenu.clickRightMouse;
				// event, elemt, menuId, objId, whiteList, opt
				uiRightClickPopupMenu.clickRightMouse = function(event, elemt, menuId, objId, params, opt) {
					uiRightClickPopupMenu.clickRightMouse_orig(event, elemt, menuId, objId, filterActions(
					    objId, elemt, params), opt);
				};

				uiRightClickPopupMenu.__cw_overridden = true;
			}

			var fileView = uiFileView.UIFileView;
			var listView = uiListView.UIListView;
			var simpleView = uiSimpleView.UISimpleView;

			if (typeof fileView.__cw_overridden == "undefined") {
				fileView.clickRightMouse_orig = fileView.clickRightMouse;
				fileView.clickRightMouse = function(event, elemt, menuId, objId, whiteList, opt) {
					fileView.clickRightMouse_orig(event, elemt, menuId, objId, filterActions(objId, elemt,
					    whiteList), opt);
				};

				fileView.showItemContextMenu_orig = fileView.showItemContextMenu;
				fileView.showItemContextMenu = function(event, element) {
					// run original
					fileView.showItemContextMenu_orig(event, element);

					var drive = cloudDrive.getContextDrive();
					if (drive) {
						// Fix group Context Menu items in Action Bar
						var classes;
						if (cloudDrive.isContextFile()) {
							// it's drive's file
							classes = ALLOWED_DMS_MENU_COMMON_ACTION_CLASSES
							    .concat(ALLOWED_DMS_MENU_FILE_ACTION_CLASSES);
						} else if (cloudDrive.isContextDrive()) {
							// it's drive in the context
							classes = ALLOWED_DMS_MENU_COMMON_ACTION_CLASSES
							    .concat(ALLOWED_DMS_MENU_DRIVE_ACTION_CLASSES);
						} else {
							// selected node not a cloud drive or its file
							classes = null;
						}

						if (classes) {
							var allowed = "";
							$.each(classes, function(i, action) {
								allowed += (allowed ? ", ." : ".") + action;
							});
							// filter Context Menu common items: JCRContextMenu
							// located in action bar
							$("#JCRContextMenu li.menuItem a i").not(allowed).each(function() {
								$(this).parent().css("display", "none");
							});
							// seems we need this
							eXo.ecm.ECMUtils.loadContainerWidth();
						}
					}
				};

				fileView.__cw_overridden = true;
			}

			// TODO Deprectaed. Gather currently selected files
			function selectedFiles(drivePath, view) {
				var itemsSelected = view.itemsSelected;
				// this check based on code from UIListView.js
				if (!itemsSelected || itemsSelected.length == 0) {
					itemsSelected = simpleView ? simpleView.itemsSelected : undefined;
				}

				var files = [];
				if (itemsSelected && itemsSelected.length) {
					for ( var i in itemsSelected) {
						if (Array.prototype[i]) {
							continue;
						}
						var currentNode = itemsSelected[i];
						var path = currentNode.getAttribute("objectId");
						if (path) {
							path = decodeURIComponent(path).split("+").join(" ");
							if (drive && path.indexOf(drivePath) == 0) { 
								// add file if its path starts with the drive root
								files.push(currentNode);
							}
						}
					}
				}
				return files;
			}

			function actionAllowed(view) {
				var allowed = true;
				var actionEvent = window.event;
				// if not ctrl+shift (create symlink)...
				if (!(actionEvent.ctrlKey && actionEvent.shiftKey)) {
					var leftClick = !((actionEvent.which && actionEvent.which > 1) || (actionEvent.button && actionEvent.button == 2));
					var drive = cloudDrive.getContextDrive();
					if (leftClick && drive) {
						// var files = selectedFiles(drive.path, view);
						var itemsSelected = view.itemsSelected;
						// this check based on code from UIListView.js
						if (!itemsSelected || itemsSelected.length == 0) {
							itemsSelected = simpleView ? simpleView.itemsSelected : undefined;
						}

						if (itemsSelected && itemsSelected.length) {
							for ( var i in itemsSelected) {
								if (Array.prototype[i]) {
									continue;
								}
								var currentNode = itemsSelected[i];
								var path = currentNode.getAttribute("objectId");
								if (path) {
									path = decodeURIComponent(path).split("+").join(" ");
									if (path.indexOf(drive.path) == 0) { 
										// if node path starts with the drive root if left click (dragging) with
										// selected cloud files, unselected cloud file elements
										currentNode.isSelect = false;
										currentNode.selected = null;
										currentNode.style.background = "none";
										allowed = false; // ...and cancel
										// action
									}
								}
							}
						}
					}
				}
				return allowed;
			}

			if (typeof listView.__cw_overridden == "undefined") {
				// don't move files outside the drive but allow to symlink them
				// (drag with ctrl+shift)
				listView.postGroupAction_orig = listView.postGroupAction;
				listView.postGroupAction = function(moveActionNode, ext) {
					// utils.log("listView.postGroupAction: " + moveActionNode +
					// ", " + ext);
					if (listView.enableDragAndDrop && actionAllowed(listView)) {
						listView.postGroupAction_orig(moveActionNode, ext);
					}
				};

				// XXX ListView doesn't have context menu
				// hide ground-context menu for drive folder
				listView.showGroundContextMenu_orig = listView.showGroundContextMenu;
				listView.showGroundContextMenu = function(event, element) {
					if (!(cloudDrive.isContextDrive() || cloudDrive.isContextFile())) {
						listView.showGroundContextMenu_orig(event, element);
					} // else don't show ground context for drive's stuff
				};

				listView.__cw_overridden = true;
			}

			if (typeof simpleView.__cw_overridden == "undefined") {
				// don't move files outside the drive but allow to symlink them
				// (drag with ctrl+shift)
				simpleView.postGroupAction_orig = simpleView.postGroupAction;
				simpleView.postGroupAction = function(moveActionNode, ext) {
					// utils.log("simpleView.postGroupAction: " + moveActionNode
					// + ", " + ext);
					if (simpleView.enableDragAndDrop && actionAllowed(simpleView)) {
						simpleView.postGroupAction_orig(moveActionNode, ext);
					}
				};

				// hide ground-context menu for drive folder
				simpleView.showGroundContextMenu_orig = simpleView.showGroundContextMenu;
				simpleView.showGroundContextMenu = function(event, element) {
					if (!(cloudDrive.isContextDrive() || cloudDrive.isContextFile())) {
						simpleView.showGroundContextMenu_orig(event, element);
					} // else don't show ground context for drive's stuff
				};

				simpleView.__cw_overridden = true;
			}
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
			  // TODO width : options && options.width ? options.width :
			  // $.pnotify.defaults.width,
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
		 * Show info notice to user. Info will be show for 8sec and hided then.
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
		 * Show warning notice to user. Info will be show for 8sec and hided then.
		 */
		this.showWarn = function(title, text, onInit) {
			return cloudDriveUI.showNotice("info", title, text, {
			  hide : true,
			  delay : 8000,
			  icon : "picon-dialog-warning",
			  onInit : onInit
			});
		};
	}

	var cloudDrive = new CloudDrive();
	var cloudDriveUI = new CloudDriveUI();

	// Load CloudDrive dependencies only in top window (not in iframes of
	// gadgets).
	if (window == top) {
		try {
			// load required styles
			utils.loadStyle("/cloud-drive/skin/jquery-ui.css");
			utils.loadStyle("/cloud-drive/skin/jquery.pnotify.default.css");
			utils.loadStyle("/cloud-drive/skin/jquery.pnotify.default.icons.css");
			utils.loadStyle("/cloud-drive/skin/cloud-drive.css");

			// configure Pnotify
			$.pnotify.defaults.styling = "jqueryui"; // use jQuery UI css
			$.pnotify.defaults.history = false; // no history roller in the
			// right corner
		} catch (e) {
			utils.log("Error configuring Cloud Drive style.", e);
		}
	}

	return cloudDrive;
})
    ($, cloudDriveUtils, cloudDriveTasks, uiRightClickPopupMenu, uiListView, uiSimpleView,
        uiFileView);