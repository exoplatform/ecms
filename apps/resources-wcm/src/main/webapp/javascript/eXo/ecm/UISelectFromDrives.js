(function($, base){
  var UISelectFromDrives = {
      personalDriveName : "Personal Documents",
      defaultPrivateLocation : "Public",
      thumbnailUrl : '',
      init : function(imageDialogWindowCnt, spaceGroupId, callback) {
        this.driveDatas = null;
        this.callback = callback;
        this.context = eXo.env.server.context;
        this.restContext = this.context + "/" + eXo.env.portal.rest;
        this.drivesURL = this.restContext + "/wcmDriver/getDrivers?lang=" + eXo.env.portal.language;
        this.foldersAndFilesURL = this.restContext + "/wcmDriver/getFoldersAndFiles?driverName=DRIVE_NAME&workspaceName=WS_NAME&currentFolder=SELECTED_FOLDER&filterBy=SimpleImage&currentPortal=" + this.context;

        this.imageDialogWindowCnt = imageDialogWindowCnt;

        var isPersonalDrive = !spaceGroupId || spaceGroupId.length == 0 || spaceGroupId == "null";

        this.selectExistingDriveCnt = this.imageDialogWindowCnt.find(".selectExistingDriveDialog");
        if (this.selectExistingDriveCnt.length == 0) {
          this.imageDialogWindowCnt.append('<div class="selectExistingDriveDialog" id="UIPopupComposer" >' +
              '<div class="PopupContent popupContent">' +
                '<div class="UIDocActivityPopup" id="UIDocActivityPopup">' +
                  '<div id="UIDocumentSelectorTab">' +
                    '<div class="breadcrumbContainer">' +
                      '<ul class="breadcrumb pull-left" id="BreadcumbsContainer">' +
                        '<li class="backBTN desktop-hidden tablet-hidden mobile-visible">' +
                          '<a class="uiIconGoBack" href="javascript:void(0);" data-toggle="tooltip" rel="tooltip" data-placement="bottom" title="" data-original-title="${CKEditor.image.Back}"></a>' +
                        '</li>' +
                        '<li class="drives">' +
                          '<a class="normal" href="javascript:void(0);" data-toggle="tooltip" rel="tooltip" data-placement="top" title="" data-original-title="${SelectFromDrivesPopup.drives.link.title}">' +
                            '${SelectFromDrivesPopup.drives.link.title}' +
                          '</a>' +
                        '</li>' +
                      '</ul>' +
                      '<div class="searchBox pull-right">' +
                        '<input type="text"> <a href="#"><i ' +
                          'class="uiIconSearch uiIconLightGray"></i></a>' +
                      '</div>' +
                    '</div>' +
                    '<div class="selectionBox">' +
                      '<div class="emptyFolder">' +
                        '<div style="margin-top: 50px;">${SelectFromDrivesPopup.EmptyFolder}</div>' +
                      '</div>' +
                      '<div class="emptyResults">' +
                        '<div style="margin-top: 50px;">${SelectFromDrivesPopup.NoResults}</div>' +
                      '</div>' +
                      '<h4 class="foldersTitle">${SelectFromDrivesPopup.Folders}</h4>' +
                      '<h4 class="filesTitle clearfix">${SelectFromDrivesPopup.Files}</h4>' +
                    '</div>' +
                  '</div>' +
                  '<div class="clearfix uiActionBorder">' +
                    '<div class="uiAction pull-right">' +
                      '<button class="btn selectFileBTN" type="button" disabled>${SelectFromDrivesPopup.Select}</button>' +
                      '<button class="btn cancelBTN" type="button">${SelectFromDrivesPopup.Cancel}</button>' +
                    '</div>' +
                  '</div>' +
                '</div>' +
              '</div>' +
            '</div> ');
          this.selectExistingDriveCnt = this.imageDialogWindowCnt.find(".selectExistingDriveDialog");
          var self = this;
          this.selectExistingDriveCnt.find(".breadcrumb .drives").on("click", function () {
            self.displayFolder();
          });

          this.breadCrumbCnt = this.selectExistingDriveCnt.find(".breadcrumb");
          this.backBTN = this.breadCrumbCnt.find(".backBTN");
          this.backBTN.on("click", function() {
            self.goBack();
          });
          this.selectBTN = this.imageDialogWindowCnt.find(".selectFileBTN");
          this.selectBTN.on("click", function() {
            self.imageDialogWindowCnt.find(" > *").removeClass("hidden");
            self.selectExistingDriveCnt.addClass("hidden");
            if (self.thumbnailUrl && self.thumbnailUrl !== 'undefined') {
              self.callback(self.thumbnailUrl);
            } else {
              self.callback(window.location.origin + self.selectedFileURL);
            }
            self.selectExistingDriveCnt.find(".fileSelection").removeClass("selected");
            self.setSelectedFile();
          });
          this.cancelBTN = this.imageDialogWindowCnt.find(".cancelBTN");
          this.cancelBTN.on("click", function() {
            self.imageDialogWindowCnt.find(">*").removeClass("hidden");
            self.selectExistingDriveCnt.remove();
          });
  
          this.initSearchBox(self.selectExistingDriveCnt);
  
          this.breadCrumbElements = [];
  
          var selectedDriveData = null;
          var driveDatas = this.getDrives();
          if(isPersonalDrive) {
            if(driveDatas.personalDrives && driveDatas.personalDrives.find("Folder").length) {
              selectedDriveData = this.getDriveByName(this.personalDriveName);
              if (!selectedDriveData.length) {
                selectedDriveData = driveDatas.personalDrives.find("Folder").first();
              }
            }
          } else {
            var spaceDriveName = spaceGroupId.replace(new RegExp("/", 'g'), ".");
            if(driveDatas.groupDrives && driveDatas.groupDrives.find("Folder").length) {
              selectedDriveData = this.getDriveByName(spaceDriveName);
              if (!selectedDriveData.length) {
                selectedDriveData = driveDatas.groupDrives.find("Folder").first();
              }
            }
          }
          this.displayFolder(selectedDriveData, isPersonalDrive ? this.defaultPrivateLocation : null);
        } else {
          this.selectExistingDriveCnt.removeClass("hidden");
        }
        $('*[rel="tooltip"]').tooltip();
      },
      setSelectedFile:  function(fileURL) {
        this.selectedFileURL = fileURL;
        if(fileURL) {
          this.selectBTN.removeAttr("disabled");
        } else {
          this.selectBTN.attr("disabled", "disabled");
        }
      },
      goBack : function() {
        if(!this.currentFolderPath || !this.currentFolderPath.length) {
          this.displayFolder();
        } else if (this.currentFolderPath.indexOf("/") < 0) {
          this.displayFolder(this.selectedDriveData);
        } else {
          var lastIndexOfSlash = this.currentFolderPath.lastIndexOf("/");
          var folderPath = this.currentFolderPath.substring(0, lastIndexOfSlash);
          this.displayFolder(this.selectedDriveData, folderPath);
        }
      },
      displayFolder : function(driveData, folderPath) {
        this.currentFolderPath = folderPath;

        folderPath = folderPath ? folderPath : "";

        this.selectExistingDriveCnt.find(".fileSelection").remove();
        this.selectExistingDriveCnt.find(".folderSelection").remove();
        this.selectExistingDriveCnt.find(".driveData").remove();
        this.selectExistingDriveCnt.find(".selectionBox > *").addClass("hidden");
        this.selectExistingDriveCnt.find(".selectionBox > .emptyFolder").hide();
        this.selectExistingDriveCnt.find(".selectionBox > .emptyResults").hide();
        this.breadCrumbCnt.find("li.folder").remove();
        this.breadCrumbCnt.find("li.arrow").remove();
        this.setSelectedFile();

        var self = this;
        if(driveData) {
          this.selectedDriveData = driveData;
          var driveClass = "normal";
          if(!folderPath || !folderPath.trim().length) {
            driveClass = "active";
          }
          this.breadCrumbCnt.append('<li class="arrow">' +
              '<span class="uiIconMiniArrowRight"></span>' +
            '</li>' +
            '<li class="folder">' +
            '<a class="' + driveClass + '" href="javascript:void(0);" data-toggle="tooltip" rel="tooltip" data-placement="top" title="" data-original-title="' + driveData.attr("label") + '">' +
                driveData.attr("label") +
            '</a>' +
          '</li>');
  
          if(folderPath && folderPath.trim().length) {
            var lastIndexOfSlash = -1;
            do {
              var newLastIndexOfSlash = folderPath.indexOf("/", lastIndexOfSlash + 1);
              var lastIndex = newLastIndexOfSlash >= 0 ? newLastIndexOfSlash : (folderPath.length);
              var folderName = folderPath.substring(lastIndexOfSlash + 1, lastIndex);
              var breadCrumbPath = folderPath.substring(0, lastIndex);
              var folderLabel = this.breadCrumbElements[breadCrumbPath];
              if(!folderLabel) {
                folderLabel = folderName;
              }
              var folderClass = newLastIndexOfSlash >= 0 ? "normal" : "active";
              this.breadCrumbCnt.append('<li class="arrow">' +
                    '<span class="uiIconMiniArrowRight"></span>' +
                  '</li>' +
                  '<li class="folder" data-path="' + breadCrumbPath + '">' +
                  '<a class="' + folderClass + '" href="javascript:void(0);" data-toggle="tooltip" rel="tooltip" data-placement="top" title="" data-original-title="' + folderLabel + '">' +
                      folderLabel +
                  '</a>' +
                '</li>');
              lastIndexOfSlash = newLastIndexOfSlash;
            } while (lastIndexOfSlash >= 0 );
          }
          this.selectExistingDriveCnt.find(".breadcrumb .folder").on("click", function () {
            var path = $(this).attr("data-path");
            self.displayFolder(self.selectedDriveData, path);
          });

          var foldersAndFilesURL = this.foldersAndFilesURL.replace("DRIVE_NAME", driveData.attr("name")).replace("WS_NAME",driveData.attr("workspace")).replace("SELECTED_FOLDER", folderPath);
          var foldersAndFilesXML = ajaxAsyncGetRequest(foldersAndFilesURL, false);
  
          var foldersAndFilesDoc = $(foldersAndFilesXML);
          var filesDoc = foldersAndFilesDoc.find("Files");
          var foldersDoc = foldersAndFilesDoc.find("Folders");
  
          var filesArray = filesDoc.find("File");
          var foldersArray = foldersDoc.find("Folder");
          if (!filesArray.length && !foldersArray.length) {
            this.selectExistingDriveCnt.find(".selectionBox > .emptyFolder").removeClass("hidden");
            this.selectExistingDriveCnt.find(".selectionBox > .emptyFolder").show();
          } else {
            if (filesArray.length) {
              this.selectExistingDriveCnt.find(".selectionBox > .filesTitle").removeClass("hidden");
              filesArray.each(function() {
                var relativePath = $(this).attr("path").replace(self.selectedDriveData.attr("path") + "/", "");
                $('<div class="fileSelection">' +
                  '<a href="javascript:void(0);" rel="tooltip" data-placement="bottom" title="" data-original-title="' + $(this).attr("title") + '" data-name="' + $(this).attr("name") + '" data-url="' + $(this).attr("url") + '" thumbnail-url="' + $(this).attr("thumbnailUrl") + '">' +
                    '<div class="' + $(this).attr("nodeTypeCssClass") + ' selectionIcon center"></div>' +
                    '<div class="selectionLabel truncate center">' + $(this).attr("title") + '</div>' +
                  '</a>' +
                '</div>').insertAfter(".selectExistingDriveDialog .filesTitle");
              });
              this.selectExistingDriveCnt.find(".fileSelection a").on("click", function() {
                var fileURL = '';
                self.selectExistingDriveCnt.find(".fileSelection").removeClass("selected");
                if ($(this).attr("thumbnail-url") && $(this).attr("thumbnail-url") !== 'undefined') {
                  self.thumbnailUrl = $(this).attr("thumbnail-url");
                  fileURL = self.thumbnailUrl;
                } else {
                  self.thumbnailUrl = '';
                  fileURL = $(this).attr("data-url");
                }
                self.setSelectedFile(fileURL);
                $(this).parent().addClass("selected");
              });
            }
    
            if (foldersArray.length) {
              this.selectExistingDriveCnt.find(".selectionBox > .foldersTitle").removeClass("hidden");
              foldersArray.each(function() {
                var relativePath = $(this).attr("path").replace(self.selectedDriveData.attr("path") + "/", "");
                $('<div class="folderSelection">' +
                  '<a href="javascript:void(0);" rel="tooltip" data-placement="bottom" title="" data-original-title="' + $(this).attr("title") + '" data-name="' + $(this).attr("name") + '" data-path="' + $(this).attr("currentFolder") + '">' +
                    '<i class="' + $(this).attr("nodeTypeCssClass") + ' uiIconEcmsLightGray selectionIcon center"></i>' +
                    '<div class="selectionLabel truncate center">' +
                        $(this).attr("title") +
                    '</div>' +
                  '</a>' +
                '</div>').insertAfter(".selectExistingDriveDialog .foldersTitle");
              });
              this.selectExistingDriveCnt.find(".folderSelection a").on("click", function() {
                var path = $(this).attr("data-path");
                self.breadCrumbElements[path] = $(this).attr("data-original-title");
                self.displayFolder(self.selectedDriveData, path);
              });
            }
          }
        } else {
          var driveDatas = this.getDrives();
          if(driveDatas.personalDrives) {
            driveDatas.personalDrives.find("Folder").each(function() {
              $('<div class="driveData" rel="tooltip" data-placement="bottom" title="" data-original-title="' + $(this).attr("label") + '" style="width:135px;">' +
                  '<a href="javascript:void(0);" data-name="' + $(this).attr("name") + '">' +
                    '<i class="' + $(this).attr("nodeTypeCssClass") + ' uiIconEcms24x24Drive' + $(this).attr("name").replace(" ", "") + ' uiIconEcms24x24DrivePrivate uiIconEcmsLightGray selectionIcon center"></i>' +
                    '<div class="selectionLabel center">' + $(this).attr("label") + '</div>' +
                  '</a>' +
                '</div>').insertAfter(".selectExistingDriveDialog .filesTitle");
            });
          }
          if(driveDatas.groupDrives) {
            driveDatas.groupDrives.find("Folder").each(function() {
              $('<div class="driveData" rel="tooltip" data-placement="bottom" title="" data-original-title="' + $(this).attr("label") + '" style="width:135px;">' +
                  '<a href="javascript:void(0);" data-name="' + $(this).attr("name") + '">' +
                    '<i class="' + $(this).attr("nodeTypeCssClass") + ' uiIconEcms24x24Drive' + $(this).attr("name").replace(" ", "") + ' uiIconEcms24x24DriveGroup uiIconEcmsLightGray selectionIcon center"></i>' +
                    '<div class="selectionLabel center">' + $(this).attr("label") + '</div>' +
                  '</a>' +
                '</div>').insertAfter(".selectExistingDriveDialog .filesTitle");
            });
          }
          if(driveDatas.generalDrives) {
            driveDatas.generalDrives.find("Folder").each(function() {
              $('<div class="driveData" rel="tooltip" data-placement="bottom" title="" data-original-title="' + $(this).attr("label") + '" style="width: 135px;">' +
                  '<a href="javascript:void(0);" data-name="' + $(this).attr("name") + '">' +
                    '<i class="' + $(this).attr("nodeTypeCssClass") + ' uiIconEcms24x24Drive' + $(this).attr("name").replace(" ", "") + ' uiIconEcms24x24DriveGeneral uiIconEcmsLightGray selectionIcon center"></i>' +
                    '<div class="selectionLabel center">' + $(this).attr("label") + '</div>' +
                  '</a>' +
                '</div>').insertAfter(".selectExistingDriveDialog .filesTitle");
            });
          }
          this.selectExistingDriveCnt.find(".driveData a").on("click", function() {
            var name = $(this).attr("data-name");
            self.displayFolder(self.getDriveByName(name));
          });
        }
        $('*[rel="tooltip"]').tooltip();
        this.selectExistingDriveCnt.find(".searchBox input").val('').trigger("keyup");
      },
      initSearchBox : function($parentElement) {
        if($.expr && $.expr.createPseudo) {
          $.expr[":"].containsCaseInsensitive = $.expr.createPseudo(function(arg) {
              return function( elem ) {
                  return $(elem).text().toUpperCase().indexOf(arg.toUpperCase()) >= 0;
              };
          });
        } else {
          $.expr[':'].containsCaseInsensitive = function(a, i, m) {
            return $(a).text().toUpperCase()
                .indexOf(m[3].toUpperCase()) >= 0;
          };
        }

        $parentElement.find(".searchBox input").on("keyup", function(event) {
          var term = this.value;
          if(!term || !(term.trim())) {
            $parentElement.find(".filesTitle").css("display", "");
            $parentElement.find(".foldersTitle").css("display", "");
            $parentElement.find(".fileSelection").show();
            $parentElement.find(".folderSelection").show();
            $parentElement.find(".driveData").show();
            $parentElement.find(".emptyResults").hide();
          } else {
            var $drivesToShow = $parentElement.find(".driveData:containsCaseInsensitive('" + term + "')");
            $drivesToShow.show();

            var $foldersToShow = $parentElement.find(".folderSelection:containsCaseInsensitive('" + term + "')");
            $foldersToShow.show();

            var $filesToShow = $parentElement.find(".fileSelection:containsCaseInsensitive('" + term + "')");
            $filesToShow.show();

            $parentElement.find(".driveData:not(:containsCaseInsensitive('" + term + "'))").hide();
            $parentElement.find(".folderSelection:not(:containsCaseInsensitive('" + term + "'))").hide();
            $parentElement.find(".fileSelection:not(:containsCaseInsensitive('" + term + "'))").hide();

            if($foldersToShow.length > 0) {
              $parentElement.find(".foldersTitle").css("display", "");
            } else {
              $parentElement.find(".foldersTitle").hide();
            }
            if($filesToShow.length > 0) {
              $parentElement.find(".filesTitle").css("display", "");
            } else {
              $parentElement.find(".filesTitle").hide();
            }
            if(($foldersToShow.length + $filesToShow.length + $drivesToShow.length) == 0) {
              if(!$parentElement.find(".emptyFolder").is(":visible")) {
                $parentElement.find(".emptyResults").removeClass("hidden");
                $parentElement.find(".emptyResults").show();
              }
            } else {
              $(".emptyResults").hide();
            }
          }
        })
      },
      getDriveByName : function(driveName) {
        var selectedDrive = null;
        var driveDatas = this.getDrives();
        if(driveDatas.personalDrives) {
          selectedDrive = $(driveDatas.personalDrives).find("Folder[name='" + driveName + "']");
        }
        if(!selectedDrive.length && driveDatas.groupDrives) {
          selectedDrive = $(driveDatas.groupDrives).find("Folder[name='" + driveName + "']");
        }
        if(!selectedDrive.length && driveDatas.generalDrives) {
          selectedDrive = $(driveDatas.generalDrives).find("Folder[name='" + driveName + "']");
        }
        return selectedDrive;
      },
      getDrives : function() {
          if(this.driveDatas) {
            return this.driveDatas;
          }
          this.driveDatas = {generalDrives : [], personalDrives : [], groupDrives : []};
          var drivesDataXML = ajaxAsyncGetRequest(this.drivesURL, false);
          var drivesDataXMLDoc = $.parseXML(drivesDataXML);
          var drivesDataXMLFolders = $(drivesDataXMLDoc).find("Folders");
          if(drivesDataXMLFolders.length == 3) {
            this.driveDatas.generalDrives = $(drivesDataXMLFolders[0]);
            this.driveDatas.groupDrives = $(drivesDataXMLFolders[1]);
            this.driveDatas.personalDrives = $(drivesDataXMLFolders[2]);
          } else if(drivesDataXMLFolders.length == 2) {
            this.driveDatas.groupDrives = $(drivesDataXMLFolders[0]);
            this.driveDatas.personalDrives = $(drivesDataXMLFolders[1]);
          } else if(drivesDataXMLFolders.length == 1) {
            this.driveDatas.groupDrives = $(drivesDataXMLFolders[0]);
          }
          return this.driveDatas;
      }
  };

  return UISelectFromDrives;
})(jQuery, gtnbase)
