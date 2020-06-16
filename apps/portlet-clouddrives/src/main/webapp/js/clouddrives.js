(function($, utils) {
    // Error constants
    var NOT_CLOUD_DRIVE = "not-cloud-drive";
    var NOT_CLOUD_FILE = "not-cloud-file";

    /**
     * Connector core class.
     */
    function CloudDrives() {
        var prefixUrl = utils.pageBaseUrl(location);

        // Node workspace and path currently selected in ECMS explorer (currently open or clicked in context menu)
        var contextNode;
        // for Provider's id and authURL
        var providers = {};
        // Cloud Drive associated with current context node
        var contextDrive;
        var excluded = {};
        // array of drives doing synchronization
        var activeSyncs = [];
        // active auto-synchronization jobs
        var autoSyncs = {};
        // i18n resources (lazy loaded), see getResource()
        var resource;
        // true if cloud drive methods called from explorer
        // var explorerOpened;
        var updateProvider;

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
                    } catch (e) {
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
                    status = 200;
                    // what else we could to do
                }
                process.always(status, textStatus);
            });

            // custom Promise target to provide an access to jqXHR object
            var processTarget = {
                request: request,
            };
            return process.promise(processTarget);
        };

        var connectPost = function(workspace, path) {
            var request = $.ajax({
                type: "POST",
                url: prefixUrl + "/portal/rest/clouddrive/connect",
                dataType: "json",
                data: {
                    workspace: workspace,
                    path: path,
                },
                xhrFields: {
                    withCredentials: true,
                },
            });

            return initRequest(request);
        };

        var connectInit = function(providerId, callbacks) {
            var request = $.ajax({
                type: "GET",
                url: prefixUrl + "/portal/rest/clouddrive/connect/init/" + providerId,
                dataType: "json",
            });

            initRequestDefaults(request, callbacks);
        };

        var getDrive = function(workspace, path) {
            var request = $.ajax({
                async: false,
                type: "GET",
                url: prefixUrl + "/portal/rest/clouddrive/drive",
                dataType: "json",
                data: {
                    workspace: workspace,
                    path: path,
                },
            });

            return initRequest(request);
        };

        var getFile = function(workspace, path) {
            var request = $.ajax({
                async: false,
                type: "GET",
                url: prefixUrl + "/portal/rest/clouddrive/drive/file",
                dataType: "json",
                data: {
                    workspace: workspace,
                    path: path,
                },
            });
            return initRequest(request);
        };

        var getState = function(workspace, path) {
            var request = $.ajax({
                async: true,
                type: "GET",
                url: prefixUrl + "/portal/rest/clouddrive/drive/state",
                dataType: "json",
                data: {
                    workspace: workspace,
                    path: path,
                },
            });

            return initRequest(request);
        };

        var synchronizePost = function(workspace, path) {
            var request = $.ajax({
                async: true, // use false for avoid the popup blocker
                type: "POST",
                url: prefixUrl + "/portal/rest/clouddrive/drive/synchronize",
                dataType: "json",
                data: {
                    workspace: workspace,
                    path: path,
                },
            });

            return initRequest(request);
        };

        var serviceGet = function(url, data, contentType) {
            var request = $.ajax({
                async: true,
                type: "GET",
                url: url,
                dataType: "json",
                contentType: contentType ? contentType : undefined,
                data: data ? data : {},
            });
            return initRequest(request);
        };

        var servicePost = function(url, data, contentType) {
            var request = $.ajax({
                async: true,
                type: "POST",
                url: url,
                dataType: "json",
                contentType: contentType ? contentType : undefined,
                data: data ? data : {},
            });
            return initRequest(request);
        };

        var featuresIsAutosync = function(workspace, path) {
            var request = $.ajax({
                async: true,
                type: "GET",
                url: prefixUrl + "/portal/rest/clouddrive/features/is-autosync-enabled",
                dataType: "json",
                data: {
                    workspace: workspace,
                    path: path,
                },
            });

            return initRequest(request);
        };

        var getResourceBundle = function() {
            var request = $.ajax({
                async: false,
                type: "GET",
                url: prefixUrl + "/portal/rest/clouddrive/resource/bundle",
                dataType: "json",
            });

            return initRequest(request);
        };

        var connectDrive = function(providerId, authURL) {
            var authWindow;
            var authService;

            if (authURL) {
                // use user interaction for authentication
                authWindow = window.open(authURL, "_blank");
            } else {
                // function to call for auth using authURL from provider
                authService = serviceGet;
            }

            // 1 initialize connect workflow
            var process = $.Deferred();
            connectInit(providerId, {
                done: function(provider) {
                    utils.log(provider.name + " connect initialized.");
                    if (authService) {
                        authService(provider.authURL);
                    }
                    // 2 wait for authentication
                    var auth = waitAuth(authWindow);
                    auth.done(function() {
                        utils.log(provider.name + " user authenticated.");
                        // 3 and finally connect the drive
                        // set initial progress	with dummy state object
                        process.notify({
                            progress: 0,
                            drive: {
                                provider: provider,
                            },
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
                                    process.reject("Cannot connect " + provider.name + ". Server return empty response.");
                                }
                            });
                            post.fail(function(state, error, errorText) {
                                utils.log("ERROR: " + provider.name + " connect failed: " + error + ". ");
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
                    auth.fail(function(message) {
                        if (message) {
                            utils.log("ERROR: " + provider.name + " authentication error: " + message);
                        }
                        process.reject(message);
                    });
                },
                fail: function(error) {
                    utils.log("ERROR: Connect to Cloud Drive cannot be initiated. " + error);
                    if (authWindow && !authWindow.closed) {
                        authWindow.close();
                    }
                    process.reject(error);
                },
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
                        // reject w/o UI message
                        process.reject(null);
                    } else if (i > 310) {
                        // +10sec to ConnectService.INIT_COOKIE_EXPIRE
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
                            utils.log("DONE: " + status + " " + state.drive.provider.name + " connected successfully.");
                        } else if (status == "202") {
                            // else inform progress and continue
                            process.notify(state);
                            utils.log(
                                "PROGRESS: " +
                                    status +
                                    " " +
                                    state.drive.provider.name +
                                    " connectCheck progress " +
                                    state.progress
                            );
                        } else {
                            // unexpected status, wait for created
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

        var addExcluded = function(path) {
            excluded[path] = true;
        };

        var isExcluded = function(path) {
            return excluded[path] === true;
        };

        var loadClientModule = function(provider) {
            var loader = $.Deferred();
            // try load provider client
            var moduleId = "SHARED/cloudDrive_" + provider.id;
            if (window.require.s.contexts._.config.paths[moduleId]) {
                try {
                    // load client module and work with it asynchronously
                    window.require(
                        [moduleId],
                        function(client) {
                            // FYI client module's initialization (if provided) will be invoked in initContext()
                            loader.resolve(client);
                        },
                        function(err) {
                            utils.log(
                                "ERROR: Cannot load client module for Cloud Drive provider " +
                                    provider.name +
                                    "(" +
                                    provider.id +
                                    "). " +
                                    err.message +
                                    ": " +
                                    JSON.stringify(err),
                                err
                            );
                            loader.reject();
                        }
                    );
                } catch (e) {
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
                            contextNode.local = true;
                            // not cloud file marker
                        } else if (err.error === NOT_CLOUD_DRIVE) {
                            addExcluded(path);
                        }
                    } else {
                        utils.log(
                            "ERROR: Cloud Drive file " +
                                workspace +
                                ":" +
                                path +
                                " cannot be read: " +
                                err.message +
                                " (" +
                                status +
                                ")"
                        );
                    }
                });
            }
        };

        var readContextFile = function() {
            readFile(null);
            // path will be read from context node
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
                        utils.log(
                            "ERROR: Cloud Drive " +
                                workspace +
                                ":" +
                                path +
                                " cannot be read: " +
                                err.message +
                                " (" +
                                status +
                                ")"
                        );
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
                    } catch (e) {
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
                        autoSyncs[syncName] = setTimeout(function() {
                            syncFunc()
                                .done(function() {
                                    if (autoSyncs[syncName]) {
                                        // re-schedule only if enabled
                                        scheduleSync();
                                    }
                                })
                                .fail(function(e) {
                                    delete autoSyncs[syncName];
                                    // cancel and cleanup
                                    utils.log(
                                        "ERROR: " + (e && e.message ? e.message : e) + ". Auto-sync canceled for " + syncName
                                    );
                                });
                        }, syncTimeout);
                    }

                    // sync function
                    var doSync = function() {
                        return synchronize(drive.workspace, drive.path);
                    };
                    // default algorithm
                    var defaultSync = function() {
                        // sync each 20sec
                        syncTimeout = 20000;
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
                                // sync in 10sec
                                var defaultSyncTimeout = 10000;
                                syncTimeout = defaultSyncTimeout;
                                syncFunc = function() {
                                    var process = $.Deferred();
                                    // We chain actual sync to the sync initiator from client.
                                    // The initiator should return jQuery Promise: it will be resolved if changes appear and rejected on error.
                                    // We use jQuery.when() to deal if not Promise returned (it's bad case - sync will run each 10sec forever).
                                    var initiator = client.onChange(drive);
                                    $.when(initiator)
                                        .done(function(nextTimeout) {
                                            // nextTimeout - is optional
                                            if (nextTimeout && typeof nextTimeout === "number") {
                                                syncTimeout = nextTimeout;
                                            } else {
                                                syncTimeout = defaultSyncTimeout;
                                            }
                                            // changes happen remotely - it's time to sync
                                            doSync()
                                                .done(function() {
                                                    process.resolve();
                                                })
                                                .fail(function(e) {
                                                    process.reject(e);
                                                });
                                        })
                                        .fail(function(e) {
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
            // Check auto-sync only on Document Explorer pages, otherwise sync will not be started automatically.
            // FYI By doing this we don't let sync start from activity stream where shared drive files may appear
            // and where many drives may have its files (thus several drives in the context - what isn't supported).
            if (eXo.ecm && eXo.ecm.UIJCRExplorer && contextDrive) {
                var syncName = contextDrive.workspace + ":" + contextDrive.path;
                if (!autoSyncs[syncName]) {
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
            }
            return null;
        };

        var synchronize = function(nodeWorkspace, nodePath, currentNode, syncProcessFn) {
            var process = $.Deferred();

            var initiator = $.Deferred();
            if (syncProcessFn) {
                syncProcessFn(process.promise());
            }
            initiator.done(function() {
                // sync only if drive connected
                if (contextDrive.connected) {
                    // sync and load all files related to this drive
                    var sync = synchronizePost(nodeWorkspace, nodePath);
                    sync.contextDrive = contextDrive;
                    activeSyncs.push(sync);
                    var currentPath = currentNode ? currentNode.path : nodePath;
                    sync.done(function(drive, status) {
                        try {
                            var changed = 0;
                            var updated = 0;
                            // updated in context node

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

                            utils.log(
                                "DONE: Synchronized " +
                                    changed +
                                    " changes from Cloud Drive associated with " +
                                    nodeWorkspace +
                                    ":" +
                                    nodePath +
                                    ". " +
                                    updated +
                                    " updated in current folder."
                            );

                            if (sync.contextDrive == contextDrive) {
                                // using new drive in the context (only if context wasn't changed)
                                contextDrive = drive;
                            }

                            checkAutoSynchronize();

                            process.resolve(updated, drive);
                        } catch (err) {
                            utils.log("ERROR: synchronization error: " + (err.message ? err.message : err), err);
                            process.reject(err, status);
                        }
                    });
                    sync.fail(function(response, status, err) {
                        utils.log("ERROR: synchronization failed: " + err + ", " + status + ", " + JSON.stringify(response));
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
                // mark as active
                updateProvider.process = process.promise();
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

        var initClientContext = function() {
            // invoke custom initialization of all registered providers
            for (var pid in providers) {
                if (providers.hasOwnProperty(pid)) {
                    var provider = providers[pid];
                    if (provider) {
                        provider.clientModule.done(function(client) {
                            if (client && client.initContext && client.hasOwnProperty("initContext")) {
                                client.initContext(provider);
                            }
                        });
                    }
                }
            }
        };

        this.getContextFile = function() {
			return getContextFile();
		}

        this.getContextDrive = function() {
			return contextDrive;
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

        /**
         * Synchronize documents view.
         */
        this.synchronize = function(elem, objectId, refreshFn, currentNode, syncProcessFn) {
            if (!currentNode) {
                currentNode = contextNode;
            }
            if (contextDrive) {
                var nodePath = contextDrive.path;
                var nodeWorkspace = contextDrive.workspace;
                utils.log("Synchronizing Cloud Drive on " + nodeWorkspace + ":" + nodePath);
                return synchronize(nodeWorkspace, nodePath, currentNode, syncProcessFn);
            } else {
                utils.log("WARN Nothing to synchronize!");
                if (refreshFn) {
                    refreshFn();
                }
            }
        };

        /**
         * Connect to Cloud Drive.
         */
        this.connect = function(providerId, connectProcessFn) {
            utils.log("Connecting Cloud Drive...");

            var failure = $.Deferred();
            var provider = providers[providerId];
            if (provider) {
                var authURL = provider.authURL;
                if (authURL) {
                    if (authURL.indexOf(prefixUrl) == 0) {
                        // XXX warm-up the portal with its ajax request :)
                        serviceGet(authURL + "&ajaxRequest=true");
                    }

                    // reset previous drive context
                    contextDrive = null;
                    excluded = {};

                    var process = connectDrive(providerId, authURL);
                    if (connectProcessFn) {
                        connectProcessFn(process);
                    }
                    return process;
                } else {
                    utils.log("ERROR: Provider has no authURL " + providerId);
                    failure.reject("Provider has no authentication URL");
                }
            } else {
                utils.log("ERROR: Provider not found " + providerId);
                failure.reject("Provider not found");
            }
            return failure.promise();
        };

        this.state = function(checkUrl) {
            return connectCheck(checkUrl);
        };

        /**
         * Initialize provider for later operations.
         */
        this.initProvider = function(id, provider) {
            providers[id] = provider;
            // load client module
            provider.clientModule = loadClientModule(provider);
        };

        /**
         * Initialize context and UI.
         */
        this.init = function(nodeWorkspace, nodePath) {
            try {
                if (nodeWorkspace && nodePath) {
                    contextNode = {
                        workspace: nodeWorkspace,
                        path: nodePath,
                    };
                } else {
                    contextNode = null;
                }
            } catch (e) {
                utils.log("Error initializing Cloud Drive " + e, e);
            }
        };

        /**
         * Initialize context node and optionally a drive. Method works synchronously and when complete the context is
         * initialized. If given workspace and path are undefined (or null or empty) then context will be reset.
         */
        this.initContext = function(workspace, path, fromExplorer) {
            //utils.log("Init context node: " + workspace + ":" + path
            //    + (contextDrive ? " (current drive: " + contextDrive.path + ")" : "") + " excluded: " + isExcluded(nodePath));
            explorerOpened = fromExplorer;
            if (workspace && path) {
                contextNode = {
                    workspace: workspace,
                    path: path,
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
                        checkAutoSynchronize();
                    } else {
                        readContextDrive();
                    }
                }
            } else {
                contextNode = null;
            }

            // invoke custom initialization of all registered providers
            initClientContext();
        };

        this.getProviders = function() {
            return providers;
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

        /**
         * Helper for AJAX GET requests.
         * */
        this.ajaxGet = serviceGet;

        /**
         * Helper for AJAX POST requests.
         * */
        this.ajaxPost = servicePost;

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
                                continue nextUpdating;
                                // still updating
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

        /**
         * Return internationalization resource by a key from lazy-loaded bundle.
         * If key not found or null, or if bundle cannot be loaded, then the key will be returned as a value.
         */
        this.getResource = function(key) {
            if (!resource) {
                var get = getResourceBundle();
                get.done(function(bundle) {
                    resource = bundle;
                });
                get.fail(function(response, status, err) {
                    utils.log("ERROR: resource error: " + err + ", " + status + ", " + JSON.stringify(response));
                });
                if (!resource) {
                    return key;
                }
            }
            var val = resource.data[key];
            return typeof val != "undefined" && val !== null ? val : key;
        };
    }

    var cloudDrives = new CloudDrives();

    return cloudDrives;
})($, cloudDriveUtils);
