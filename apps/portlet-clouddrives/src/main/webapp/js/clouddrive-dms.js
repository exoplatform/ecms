(function($, utils, cloudDrives, tasks, uiRightClickPopupMenu, uiSimpleView, uiFileView) {
    // Error constants
	var ACCESS_DENIED = "access-denied";
	var DRIVE_REMOVED = "drive-removed";
	var NODE_NOT_FOUND = "node-not-found";

	function CloudDrivesDms() {
		// Node workspace and path currently open in ECMS explorer view
		var currentNode;

		var getDocument = function(workspace, path) {
			var request = $.ajax({
				async : false,
				type : "GET",
				url : prefixUrl + "/portal/rest/clouddrive/document/file",
				dataType : "json",
				data : {
					workspace : workspace,
					path : path
				}
			});
			return cloudDrives.initRequest(request);
		};

		/**
		 * Initialize provider for later operations.
		 */
		this.initProvider = function(id, provider) {
			if (window == top) {
				try {
					// load provider styles
					utils.loadStyle("/clouddrives-" + id + "/skin/clouddrive.css");
					// XXX load Enterprise Skin if required
					if (eXo.env && eXo.env.client && eXo.env.client.skin == "Enterprise") {
						utils.loadStyle("/clouddrives-" + id + "/skin/clouddrive-enterprise.css");
					}
				} catch(e) {
					utils.log("Error loading provider (" + id + ") style.", e);
				}
			}
			cloudDrives.initProvider(id, provider);
		};

		this.synchronize = function(elem, objectId) {
			return cloudDrives.synchronize(elem, objectId, cloudDriveUI.refreshDocuments, currentNode, cloudDriveUI.synchronizeProcess);
		};

		this.connect = function(providerId) {
			return cloudDrives.connect(providerId, cloudDriveUI.connectProcess);
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
		 * Initialize context and UI.
		 */
		this.init = function(nodeWorkspace, nodePath) {
			try {
				// currently open node (or last open node, e.g. in activity stream)
				if (nodeWorkspace && nodePath) {
					currentNode = {
						workspace : nodeWorkspace,
						path : nodePath
					};
				} else {
					currentNode = null;
				}
				cloudDrives.initContext(nodeWorkspace, nodePath, true);

				$(function() {
					try {
						if (nodeWorkspace && nodePath) {
							// and on-page-ready initialization of Cloud Drive UI
							cloudDriveUI.init();
						} else {
							// on-page-ready initialization of global Cloud Drive UI only
							cloudDriveUI.initGlobal();
						}
					} catch(e) {
						utils.log("Error initializing Cloud Drive UI " + e, e);
					}
				});
			} catch(e) {
				utils.log("Error initializing Cloud Drive " + e, e);
			}
		};

		this.getFile = function(path) {
			if (cloudDrives.contextDrive) {
				var file = cloudDrives.contextDrive.files[path];
				if (!file || cloudDrives.isUpdating(path) || cloudDrives.contextNode.local) {
					// file not yet cached, file is syncing or local - read file from the server
					cloudDrives.readFile(path);
					file = cloudDrives.contextDrive.files[path];
				}
				return file;
			}
			return null;
		};

		this.getDocument = getDocument;

		this.getCurrentNode = function() {
			return currentNode;
		};

		this.isContextSymlink = function() {
			if (cloudDrives.contextNode && cloudDrives.contextDrive) {
				var file = cloudDrives.contextDrive.files[cloudDrives.contextNode.path];
				return file && file.symlink;
			}
			return false;
		};

		this.openFile = function(elem, objectId) {
			var file = cloudDrives.getContextFile();
			if (file) {
				window.open(file.link);
			} else {
				utils.log("No context path to open as Cloud File");
			}
		};

		this.showInfo = function(title, text) {
			cloudDriveUI.showInfo(title, text);
		};

		this.getContextFile = function() {
			return cloudDrives.getContextFile();
		}
	}

	/**
	 * Cloud Drive WebUI integration.
	 */
	function CloudDriveUI() {
		var self = this;

		var NOTICE_WIDTH = "380px";

		// Menu items managed via uiRightClickPopupMenu menu interception
		var MENU_OPEN_FILE = "OpenCloudFile";
		var MENU_PUSH_FILE = "PushCloudFile";
		var MENU_REFRESH_DRIVE = "RefreshCloudDrive";
		var DRIVE_MENU_ACTIONS = [MENU_OPEN_FILE, MENU_REFRESH_DRIVE];
		var ALLOWED_DRIVE_MENU_ACTIONS = [MENU_OPEN_FILE, MENU_PUSH_FILE, MENU_REFRESH_DRIVE, "Delete", "Paste", "AddToFavourite", "RemoveFromFavourite", "ViewInfo"];
		var ALLOWED_FILE_MENU_ACTIONS = [MENU_OPEN_FILE, MENU_PUSH_FILE, MENU_REFRESH_DRIVE, "Delete", "Rename", "Copy", "Cut", "Paste", "AddToFavourite", "RemoveFromFavourite", "ViewInfo", "ShareDocuments" ]; // "ViewSharing"
		var ALLOWED_SYMLINK_MENU_ACTIONS = ["Delete"];
		var ALLOWED_LOCAL_FILE_MENU_ACTIONS = [MENU_PUSH_FILE, "Delete", "Cut", "RemoveFromFavourite", "ViewInfo"];

		// Menu items managed via view's showItemContextMenu() method (multi-selection)
		// 21.05.2014 "uiIconEcmsOverloadThumbnail" removed from allowed
		var ALLOWED_DMS_MENU_COMMON_ACTION_CLASSES = ["uiIconEcmsUpload", "uiIconEcmsAddFolder", "uiIconEcmsAddToFavourite", "uiIconEcmsRemoveFromFavourite", "uiIconEcmsManageActions", "uiIconEcmsManageRelations", "uiIconEcmsViewProperties", "uiIconEcmsManageAuditing"];
		var ALLOWED_DMS_MENU_FILE_ACTION_CLASSES = ["uiIconEcmsOpenCloudFile", "uiIconEcmsPushCloudFile", "uiIconEcmsTaggingDocument", "uiIconEcmsWatchDocument", "uiIconEcmsViewMetadatas", "uiIconEcmsVote", "uiIconEcmsComment", "uiIconEcmsCopy", "uiIconEcmsPaste", "uiIconEcmsCut", "uiIconEcmsDelete", "uiIconEcmsRename", "uiIconEcmsShareDocuments" ]; // "uiIconEcmsViewSharing"
		var ALLOWED_DMS_MENU_DRIVE_ACTION_CLASSES = ["uiIconEcmsRefreshCloudDrive", "DeleteNodeIcon"];
		var ALLOWED_DMS_MENU_LOCAL_FILE_ACTION_CLASSES = ["uiIconEcmsPushCloudFile", "uiIconEcmsViewMetadatas", "uiIconEcmsCut", "uiIconEcmsDelete", "uiIconEcmsRemoveFromFavourite", "uiIconEcmsViewProperties"];

		var initLock = null;

		var syncingUpdater = null;

		var getIEVersion = function()
		// Returns the version of Windows Internet Explorer or a -1
		// (indicating the use of another browser).
		{
			var rv = -1;
			// Return value assumes failure.
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
					newParams = ( newParams ? newParams + "," + item : item);
				}
			});
			return newParams;
		};

		var removeCloudItems = function(items) {
			var newParams;
			$.each(items, function(i, item) {
				if (DRIVE_MENU_ACTIONS.indexOf(item) < 0) {
					newParams = ( newParams ? newParams + "," + item : item);
				}
			});
			return newParams;
		};

		var initContextMenu = function(menu, items, allowedItems) {
			var menuItems = items.split(",");
			var drive = cloudDrives.getContextDrive();
			if (drive) {
				// branded icons in context menu
				$("i.uiIconEcmsRefreshCloudDrive, i.uiIconEcmsOpenCloudFile, i.uiIconEcmsPushCloudFile").each(function() {
					var classPrefix = "uiIcon16x16CloudFile-";
					var classPatt = new RegExp(classPrefix, "g");
					var brandClass = classPrefix + drive.provider.id;
					var newClasses = [];
					var updateClasses = false;
					var addBrandClass = true;
					// remove other brand classes and add/leave current drive icon
					$.each($(this).attr("class").split(" "), function(i, className) {
						if (className.indexOf(classPrefix) >= 0) {
							if (className === brandClass) {
								addBrandClass = false;
							} else {
								updateClasses = true;
							}
						} else {
							newClasses.push(className);
						}
					});
					if (addBrandClass) {
						newClasses.push(brandClass);
						updateClasses = true;
					}
					if (updateClasses) {
						$(this).attr("class", newClasses.join(" "));
					}
				});

				// Common context menu: add links to CD actions
				$("#ECMContextMenu a[exo\\:attr='" + MENU_OPEN_FILE + "']").each(function() {
					var text = $(this).data("cd-action-prefix");
					if (!text) {
						text = $(this).text();
						$(this).data("cd-action-prefix", text).click(function() {
							cloudDrivesDms.openFile();
							uiFileView.UIFileView.clearCheckboxes();
						});
					}
					var $i = $(this).find("i");
					text = text + drive.provider.name;
					$(this).text(text);
					$(this).prepend($i);
					if (cloudDrives.isContextUpdating()) {
						$(this).addClass("cloudFileDisabled");
					} else {
						$(this).removeClass("cloudFileDisabled");
					}
				});
				$("#ECMContextMenu a[exo\\:attr='" + MENU_PUSH_FILE + "']").each(function() {
					var text = $(this).data("cd-action-prefix");
					if (!text) {
						text = $(this).text();
						$(this).data("cd-action-prefix", text);
					}
					var $i = $(this).find("i");
					text = text + drive.provider.name;
					$(this).text(text);
					$(this).prepend($i);
				});
				$("#ECMContextMenu a[exo\\:attr='" + MENU_REFRESH_DRIVE + "']").each(function() {
					var text = $(this).data("cd-action-prefix");
					if (!text) {
						$(this).click(function() {
							cloudDrivesDms.synchronize();
							uiFileView.UIFileView.clearCheckboxes();
						});
						text = $(this).text();
						$(this).data("cd-action-prefix", text);
					}
					var $i = $(this).find("i");
					text = text + drive.provider.name;
					$(this).text(text);
					$(this).prepend($i);
				});

				if (cloudDrivesDms.isContextSymlink()) {
					allowedItems = allowedItems.concat(ALLOWED_SYMLINK_MENU_ACTIONS);
				}

				// Custom context menu links
				if (menu) {
					var file = cloudDrives.getContextFile();
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
			var drive = cloudDrives.getContextDrive();
			if (drive) {
				// Fix group Context Menu items using CSS
				var classes;
				if (cloudDrives.isContextFile()) {
					// it's drive's file
					classes = ALLOWED_DMS_MENU_COMMON_ACTION_CLASSES.concat(ALLOWED_DMS_MENU_FILE_ACTION_CLASSES);
				} else if (cloudDrives.isContextDrive()) {
					// it's drive in the context
					classes = ALLOWED_DMS_MENU_COMMON_ACTION_CLASSES.concat(ALLOWED_DMS_MENU_DRIVE_ACTION_CLASSES);
				} else if (cloudDrives.isContextLocal()) {
					// it's local node in the drive context
					classes = ALLOWED_DMS_MENU_LOCAL_FILE_ACTION_CLASSES;
				} else {
					// selected node not a cloud drive or its file
					classes = null;
				}

				if (classes) {
					var allowed = "";
					$.each(classes, function(i, action) {
						allowed += ( allowed ? ", ." : ".") + action;
					});
					// filter Context Menu common items: JCRContextMenu located in action bar
					var $items = $("#JCRContextMenu li.menuItem a i");
					var itemsCount = $items.length;
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
			}
			if (byteSize > 0) {
				return byteSize + " B";
			} else {
				return "";
				// return empty not 1 KB as ECMS does
			}
		};

		var updateDate = function(text, newDate) {
      		return text.replace(/\d{1,2}\.\d{1,2}\.\d{2,4}/g, newDate);
		};
		/**
		 * Init file list according the actions set for each file item.
		 */
		var initFileList = function() {
			var syncingPaths = [];

			// List/Admin view
			var $listView = $("div.fileViewRowView");
			if ($listView.length > 0) {
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
			if ($listView.length > 0) {
				$listView.each(function() {
					var objectId = decodeString($(this).attr("objectid"));
					// find all info lines with 1K size and replace the size with real value
          $(this).find("p.fileInfoBottom").each(function() {
            var file = cloudDrivesDms.getFile(objectId);
            if (file) {
              var orig = $(this).text();
              if(orig.includes("- 1 KB")){
                var str = sizeString(file.size);
                var withUpdatedSize = orig.replace("- 1 KB", str.length > 0 ? "- " + str : str);
                orig = withUpdatedSize;
              }
              // TODO better use if(file.connected) for this check.
              if (file.modifiedRemote && file.modifiedLocal) {
                var modifiedRemoteDate = file.modifiedRemote;
                var modifiedLocalDate = file.modifiedLocal;
                // var withUpdatedDate = updateDate(orig,  modifiedDate);
                var withUpdatedDate = orig.replace(modifiedLocalDate,modifiedRemoteDate);
                $(this).text(withUpdatedDate);                
              }
            }
          });
				});
			}

			// Icon view
			var $iconView = $("div.actionIconBox");
			if ($iconView.length > 0) {
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
			if ($iconView.length > 0) {
				$("#UIPopupContainer").on("DOMSubtreeModified propertychange", function() {
					var $info = $("#UIViewInfoManager");
					if ($info.length > 0 && !$info.data("cd-data-sizefixed")) {// avoid loops caused by text modification below
						$info.find("td").filter("td:contains(' Byte(s)'), td:contains(' KB'), td:contains(' MB'), td:contains(' GB')").each(function() {
							var file = cloudDrives.getContextFile();
							if (file) {
								var str = sizeString(file.size);
								$info.data("cd-data-sizefixed", true);
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
						var stateProcess = cloudDrives.getState();
						if (stateProcess) {
							stateProcess.done(function(state) {
								var updated = 0;
								var paths = syncingUpdater.paths;
								if (paths && paths.length > 0) {
									// compare remote state's updating with paths stored in the updater
									// if have difference - cancel the interval and run UI refresh
									var currentNode = cloudDrivesDms.getCurrentNode();
									if (currentNode) {
										var currentPath = currentNode.path;
										var contextPathsNumber = 0;
										next:
										for (var li = 0; li < paths.length; li++) {
											var path = paths[li];
											if (path.indexOf(currentPath) == 0) {// starts with current path in ECMS explorer UI
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
											return;
											// no changes or not everything changed in syncing list - wait for next interval
										}
									}
								}
								clearInterval(syncingUpdater.interval);
								syncingUpdater = null;
								if (updated > 0) {
									refresh();
									// refresh after the cancellation!
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

		var initFileViewer = function() {
			var drive = cloudDrives.getContextDrive();
			var file = cloudDrives.getContextFile();
			if (drive && file) {
				var $viewer = $("#CloudFileViewer");
				if ($viewer.length > 0 && !$viewer.data("cd-init")) {
					$viewer.data("cd-init", true);
					var $vswitch = $("#ViewerSwitch");
					var openOnProvider = $viewer.attr("file-open-on");

					var iconColor;
					// Open On Provider button (made from Download button)
					var openOn = [];
					// Open On Provider button icon
					var openOnIcon = [];
					// fix Document explorer's title, Download icon, text and link
					var $title = $("div.fileContent .title");
					if ($title.is(":visible")) {
						var $titleText = $title.find("div.topTitle");
						$titleText.text(file.title);
						iconColor = "uiIconLightGray";
						openOnIcon.push($title.find("i.uiIconDownload"));
						openOn.push($title.find("a.dowload-link"));
					}

					// fix activity file preview: Download icon, text and link
					var $activityDownload = $("#uiDocumentPreview .downloadBtn>a");
					if ($activityDownload.length > 0) {
						iconColor = "uiIconWhite";
						openOnIcon.push($activityDownload.find("i.uiIconDownload"));
						openOn.push($activityDownload);
						$vswitch.removeClass("pull-right");
						$vswitch.removeClass("btn");
						$vswitch.addClass("pull-left");
					}

					$.each(openOn, function(index, $a) {
						// this will erase all children including the icon
						$a.text(" " + openOnProvider);
						// add icon back to the button
						var $i = openOnIcon[index];
						var providerClass = "uiIcon16x16CloudFile-" + drive.provider.id;
						if (!$i.hasClass(providerClass)) {
							$i.addClass(providerClass);
						}
						$a.prepend($i);
						$a.attr("href", file.link);
						$a.attr("target", "_blank");
						$a.css("font-weight", "normal");
					});

					var $viewIcon = $("<i class='uiIconWatch'></i>");
					$viewIcon.addClass(iconColor);
					var $editIcon = $("<i class='uiIconEdit'></i>");
					$editIcon.addClass(iconColor);

					var $iframe = $viewer.find("iframe");
					// file link as edit link
					if ($vswitch.length > 0 && file.editLink && file.previewLink && file.editLink != file.previewLink) {
						// init Edit/View mode
						$iframe.attr("src", file.previewLink);
						$vswitch.click(function() {
							var currentLink = $iframe.attr("src");
							if (currentLink == file.previewLink) {
								// switch to editor
								$iframe.attr("src", file.editLink);
								var viewerTitle = $vswitch.attr("view-title");
								$(this).text(viewerTitle);
								$(this).prepend($viewIcon);
							} else {
								// switch to viewer
								$iframe.attr("src", file.previewLink);
								var editTitle = $vswitch.attr("edit-title");
								$(this).text(editTitle);
								$(this).prepend($editIcon);
							}
						});
						$vswitch.prepend($editIcon);
						$.each(openOn, function(index, $a) {
							$a.after($vswitch);
						});
					} else {
						$viewer.find("iframe").attr("src", file.previewLink ? file.previewLink : file.link);
						$vswitch.remove();
					}
					$viewer.find(".file-content").show();
				}
			}
		};

		var initDocument = function() {
			var drive = cloudDrives.getContextDrive();
			if (drive) {
				// Fix Action Bar items
				var classes;
				if (cloudDrives.isContextFile()) {
					// it's drive's file
					classes = ALLOWED_DMS_MENU_COMMON_ACTION_CLASSES.concat(ALLOWED_DMS_MENU_FILE_ACTION_CLASSES);
				} else if (cloudDrives.isContextDrive()) {
					// it's drive in the context
					classes = ALLOWED_DMS_MENU_COMMON_ACTION_CLASSES.concat(ALLOWED_DMS_MENU_DRIVE_ACTION_CLASSES);
				} else {
					// selected node not a cloud drive or its file
					return;
				}

				var allowed = "";
				$.each(classes, function(i, action) {
					allowed += ( allowed ? ", ." : ".") + action;
				});

				var $actionBar = $("#uiActionsBarContainer ul");
				// filter Action Bar items (depends on file/folder or the drive itself in the context)
				$actionBar.find("li a.actionIcon i").not(allowed).each(function() {// div ul li
					$(this).parent().css("display", "none");
				});
				if ($actionBar.find("li a:visible").length == 0) {
					// hack to prevent empty menu bar
					$actionBar.append("<li style='display: block;'><a class='actionIcon' style='height: 18px;'><i></i> </a></li>");
				}

				// add sync call to Refresh action
				$("a.refreshIcon").click(function() {
					var $refreshChanges = $("span.uiCloudDriveChanges");
					if ($refreshChanges.length > 0) {
						var currentDate = new Date();
						var syncDate = $refreshChanges.data("timestamp");
						if (syncDate && (currentDate.getMilliseconds() - syncDate.getMilliseconds() <= 60000)) {
							return true;
							// don't invoke sync if it was less a min ago
						}
					}
					cloudDrivesDms.synchronize();
				});

				// File Viewer
				initFileViewer();

				// init file listing (special handling for not cloud's and currently syncing files)
				initFileList();

				eXo.ecm.ECMUtils.loadContainerWidth();
			} // else not a cloud drive or its file
		};

		/**
		 * Init text file viewer (based on Codemirror).
		 */
		var initTextViewer = function() {
			// Add an action to "show plain text" tab to load the frame content to the code viewer
			var $viewer = $("#WebContent");
			var $code = $viewer.find("#TabCode");

			if ($viewer.length > 0) {
				// load Codemirror styles only if text editor tab exists
				// and only in window (not in iframe as gadgets may do)
				if (window == top) {
					try {
						utils.loadStyle("/clouddrives/skin/codemirror.css");
					} catch(e) {
						utils.log("Error intializing text viewer.", e);
					}
				}
			}

			function createViewer(codeURL) {
				var cursorCss = $viewer.css("cursor");
				$viewer.css("cursor", "wait");

				var contextFile = cloudDrives.getContextFile();
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
						window.require(["../../clouddrives/js/codemirror-bundle.min"], function() {
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
				if ($code.length > 0 && $code.children().length == 0) {
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

		var initActivity = function() {
			// Remove Download link in cloud file activities, remove Preview button and such link on the icon
			// If description empty - remove this element to do not eat space by empty bar.
			try {
				$("i.uiCloudFileActivity").each(function() {
					var $elem = $(this);
					// five parents higher in DOM we have ActivityContextBox div
					var $media = $elem.parent().parent();
					var $text = $media.siblings(".text");
					var isMediaContent = $media.is(".mediaContent");
					// Logic similar with initSearch()
					var $link, onClick;
					var jsClick = $media.attr("onclick");
					if (!jsClick || jsClick.indexOf("javascript:void(") == 0) {
						$link = $media.children("a");
						jsClick = $link.attr("href");
						onClick = false;
					} else {
						$link = $media;
						onClick = true;
					}
					if (jsClick) {
						var item = findItemInfo(jsClick);
						if (item) {
							$link.click(function() {
								initFileViewerWait(item.workspace, item.path);
							});
						}
					}
					$link.off("mouseenter"); // disabale hover stuff on cloud files
					$media.find("button.btn.doc-preview-thumbnail-footer").hide();
					var $description = $text.find(".descriptionText");
					if ($description.text().length == 0) {
						$description.remove();
					}
					var $i = $media.parent().parent().parent().find(".actionBar>.statusAction i.uiIconDownload");
					$i.parent().parent().hide();
					// ensure icon shown not a thumbnail (as it's not supported by the core)
					if (isMediaContent) {
						$media.children("span").children("img").parent().hide();
						$media.children("span.fallbackImage").show();
					}
				});
			} catch(e) {
				utils.log("Error initializing activity stream " + e, e);
			}
		};

		var findMappedText = function(key, text) {
			var regex = new RegExp(key + "[ ]*:[ ]*'([^']*)'", "g");
			var res = regex.exec(text);
			return res && res.length > 0 ? res[1] : null;
		};

		var findItemInfo = function(jsCode) {
			if (jsCode.indexOf("javascript") == 0) {
				var path = findMappedText("path", jsCode);
				var openUrl = findMappedText("openUrl", jsCode);
				var workspace = findMappedText("workspace", jsCode);
				var downloadUrl = findMappedText("downloadUrl", jsCode);
				if (workspace && path && openUrl && downloadUrl) {
					return {
						workspace : workspace,
						path : path,
						openUrl : openUrl,
						downloadUrl : downloadUrl
					};
				}
			}
			return null;
		};

		var initFileViewerWait = function(workspace, path) {
			var attempts = 120; // wait 30sec
			function tryInit() {
				var $viewer = $("#CloudFileViewer");
				if ($viewer.length == 0 || !$viewer.is(":visible")) {
					// wait for viewer
					if (attempts > 0) {
						attempts--;
						setTimeout(tryInit, 250);
					} else {
						utils.log("Cannot initialize cloud file viewer (timeout): " + path);
					}
				} else {
					utils.log("Initialize cloud file viewer: " + path);
					cloudDrives.initContext(workspace, path);
					initFileViewer();
				}
			}
			tryInit();
		};

		var initSearch = function() {
			function initRes($res) {
				if (!$res.data("cd-init")) {
					$res.data("cd-init", true);
					var $link = $res.children("a");
					var jsClick = $link.attr("href");
					var onClick = false;
					if (!jsClick || jsClick.indexOf("javascript:void(") == 0) {
						jsClick = $link.attr("onclick");
						onClick = true;
					}
					if (jsClick) {
						var item = findItemInfo(jsClick);
						if (item) {
							cloudDrivesDms.getDocument(item.workspace, item.path).done(function(file) {
								if (file) {
									// XXX fix the CSS class (PLF 5.0.0 case)
									$link.children("i[class*='uiIcon']").each(function(i, elem) {
										var $i = $(elem);
										var iclass = $i.attr("class");
										iclass = iclass.replace(/[\/.]/g, "");
										$i.attr("class", iclass);
									});
									jsClick = jsClick.replace("path:'" + item.path + "'", "path:'" + file.path + "'");
									jsClick = jsClick.replace("downloadUrl:'" + item.downloadUrl + "'", "downloadUrl:'" + file.link + "'");
									if (file.openLink) {
										jsClick = jsClick.replace("openUrl:'" + item.openUrl + "'", "openUrl:'" + file.openLink + "'");
										// content div exists on Unified Search page
										var $content = $res.children(".content");
										if ($content.length > 0) {
											$content.find("a").attr("href", file.openLink);
										}
									}
									if (onClick) {
										$link.attr("onclick", jsClick);
									} else {
										$link.attr("href", jsClick);
									}
									$link.click(function() {
										initFileViewerWait(item.workspace, file.path);
									});
								}
							}).fail(function(err, status) {
								if (status != 404) {
									$link.attr("href", "#");
									utils.log("ERROR getting cloud file: " + item.workspace + ":" + item.path + ". " + err.message);
								} // otherwise it's not cloud file
							});
						}
					}
				}
			}

			var MutationObserver = window.MutationObserver || window.WebKitMutationObserver || window.MozMutationObserver;

			// init Quick Search
			var $searchToolbar = $("#ToolBarSearch");
			var $quickSearchResult = $searchToolbar.find(".uiQuickSearchResult");
			if ($quickSearchResult.length > 0) {
				// run DOM listener to know when results will be populated to fix the urls
				var observer = new MutationObserver(function(mutations) {
					$quickSearchResult.find(".quickSearchResult").each(function() {
						initRes($(this));
					});
				});
				observer.observe($quickSearchResult.get(0), {
					subtree : false,
					childList : true,
					attributes : false,
					characterData : false
				});
			}
			// init Unified Search
			var $searchPortlet = $("#searchPortlet");
			var $result = $searchPortlet.find("#resultPage #result");
			if ($result.length > 0) {
				function initSearchPage() {
					$result.children(".resultBox").each(function() {
						initRes($(this));
					});
				}
				initSearchPage();
				// run DOM listener to know when results will be populated to fix the urls
				var observer = new MutationObserver(function(mutations) {
					initSearchPage();
				});
				observer.observe($result.get(0), {
					subtree : false,
					childList : true,
					attributes : false,
					characterData : false
				});
			}
		};

		/**
		 * Find link to open Personal Documents view in WCM. Can return nothing if current page doesn't
		 * contain such element.
		 */
		var personalDocumentsLink = function() {
			var $link = $("a.refreshIcon");
			if ($link.length > 0) {
				return $link.attr("href");
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
				if ($("div#UIDocumentInfo:visible").length > 0) {
					if ($("div#UIPopupWindow:visible, div#UIRenameWindowPopup, span.loading").length == 0) {
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
				for (var i = 0; i < drive.messages.length; i++) {
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

			var state = cloudDrives.state(checkUrl);
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
				driveMessage(state.drive);
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
			var stack_topright = {"dir1": "down", "dir2": "left", "firstpos1": 5, "firstpos2": 5};

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
					options.nonblock = false;
					// remove non-block
				}
				notice.pnotify(options);
			};

			process.progress(function(state) {
				if (!task) {
					// start progress
					progress = state.progress;
					if (progress > 0) {
						driveName = state.drive.provider.name;

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
					driveName = state.drive.provider.name;
					// need update drive name
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
						cloudDrivesDms.synchronize();
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
						title : title ? title : "Error connecting " + ( driveName ? driveName : "drive") + "!",
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
					for (var i = 0; i < drive.messages.length; i++) {
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
					cloudDriveUI.showWarn("Renew access to your " + response.name, "Start <a class='cdSynchronizeProcessAction' href='javascript:void(0);'" + " style='cursor: pointer; border-bottom: 1px dashed #999; display: inline;'>" + " synchronization</a> to update access permissions.</div>", function(pnotify) {
						$(pnotify.text_container).find("a.cdSynchronizeProcessAction").click(function() {
							synchronize(this);
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
				}
				// if status == 0 we go silently - it's server or network down
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
			var w = 740;
			var h = 540;
			var left = (screen.width / 2) - (w / 2);
			var top = (screen.height / 2) - (h / 2);
			return window.open(authURL, 'contacts', 'width=' + w + ',height=' + h + ',top=' + top + ',left=' + left);
		};

		/**
		 * Init only global UI (search etc).
		 */
		var initGlobal = true;
		this.initGlobal = function() {
			if (initGlobal) {
				initGlobal = false;
				initSearch();
			}
		};

		this.getState = function(workspace, path) {
			return cloudDrives.getState(workspace, path);
		};

		/**
		 * Init all UI (dialogs, menus, views etc).
		 */
		this.init = function() {
			// Global things first
			self.initGlobal();

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
								var process = cloudDrivesDms.connect(providerId);
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
									cloudDrivesDms.connect(providerId);
								});
							}
						}
					}
				}
			});

			// init doc view (list or file view)
			initDocument();

			// init file view for text
			initTextViewer();

			// init activity stream
			initActivity();

			// init menus below

			// XXX using deprecated DOMNodeInserted and the explorer panes selector
			// choose better selector to get less events here for DOM, now it's tens of events
			// reloading during the navigation
			var ieVersion = getIEVersion();
			var domEvent = ieVersion > 0 && ieVersion < 9.0 ? "onpropertychange" : "DOMNodeInserted";
			// DOMSubtreeModified
			$(".PORTLET-FRAGMENT").on(domEvent, ".LeftCotainer, .RightCotainer", function(event) {// #UIJCRExplorerPortlet
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
					cloudDrives.initContext(workspace, path);

					var drive = cloudDrives.getContextDrive();
					if (drive) {
						if (cloudDrives.isContextFile()) {
							// it's drive's file
							return initContextMenu(menu, params, ALLOWED_FILE_MENU_ACTIONS);
						} else if (cloudDrives.isContextDrive()) {
							// it's drive in the context
							return initContextMenu(menu, params, ALLOWED_DRIVE_MENU_ACTIONS);
						} else if (cloudDrives.isContextLocal()) {
							// it's local node in the drive context
							return initContextMenu(menu, params, ALLOWED_LOCAL_FILE_MENU_ACTIONS);
						}
						// selected node not a cloud drive or its file
					}
				}
				return params;
			}

			// tuning of single-selection context menu (used in Simple/Icon view)
			if ( typeof uiRightClickPopupMenu.__cw_overridden == "undefined") {
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

			if ( typeof fileView.__cw_overridden == "undefined") {
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
				if (event.clientX + contentMenu.offsetWidth > portWidth)
					X -= contentMenu.offsetWidth;
				if (event.clientY + contentMenu.offsetHeight > portHeight)
					Y -= contentMenu.offsetHeight + 5;
				contextMenu.css("top", Y + 5 + "px");
				contextMenu.css("left", X + 5 + "px");
			}

			if ( typeof simpleView.__cw_overridden == "undefined") {
				// tune multi-selection menu
				// showItemContextMenu will be invoked on multi-selection in Simple/Icon view
				simpleView.showItemContextMenu_orig = simpleView.showItemContextMenu;
				simpleView.showItemContextMenu = function(event, element) {
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
					if (cloudDrives.isContextDrive() || cloudDrives.isContextFile()) {
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
			var $target, $tree;
			var $files = $("#UIDocumentInfo");
			var $activeViewBtn = $("#UIAddressBar .detailViewIcon .btn.active");
			if ($activeViewBtn.find(".uiIconEcmsViewIcons").length > 0) {
				// Icon view
				styleSize = "uiIcon64x64";
				$target = $files.find(".actionIconBox");
				$tree = $("#UITreeExplorer .node");
			} else {
				// List or Admin view
				styleSize = "uiIcon24x24";
				$target = $files.find(".rowView");
			}
			for (name in map) {
				if (map.hasOwnProperty(name)) {
					var providerId = map[name];
					var cname = styleSize + "CloudDrive-" + providerId;
					$target.each(function(i, item) {
						if ($(item).find("span.nodeName:contains('" + name + "')").length > 0) {
							$(item).find("div." + styleSize + "nt_folder:not(:has(div." + cname + "))").each(function() {
								$("<div class='" + cname + "'></div>").appendTo(this);
							});
						}
					});
					if ($tree) {
						cname = "uiIcon16x16CloudDrive-" + providerId;
						$tree.each(function() {
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
			if ($permission.length > 0) {
				$permission.find("label.checkbox").hide();
				if ($permission.find("input[name='userOrGroup']").val().length > 0) {
					$permission.find("input[name='read']").prop("checked", true);
				}
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
				icon : "picon " + ( options ? options.icon : ""),
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

    var cloudDrivesDms = new CloudDrivesDms();
    var cloudDriveUI = new CloudDriveUI();

    // Load CloudDrive dependencies only in top window (not in iframes of gadgets).
	if (window == top) {
		try {
			// load required styles
			utils.loadStyle("/clouddrives/skin/jquery-ui.css");
			utils.loadStyle("/clouddrives/skin/jquery.pnotify.default.css");
			utils.loadStyle("/clouddrives/skin/jquery.pnotify.default.icons.css");
			utils.loadStyle("/clouddrives/skin/clouddrive.css");

			// configure Pnotify
			// use jQuery UI css
			$.pnotify.defaults.styling = "jqueryui";
			// no history roller in the right corner
			$.pnotify.defaults.history = false;
		} catch(e) {
			utils.log("Error configuring Cloud Drive style.", e);
		}
    }
    
    return cloudDrivesDms;
})($, cloudDriveUtils, cloudDrives, cloudDriveTasks, uiRightClickPopupMenu, uiSimpleView, uiFileView);