<template>
  <div class="serverFiles">
    <div class="VuetifyApp">
      <v-app>
        <div v-show="attachmentsComposerActions.length > 0" class="contentHeader">
          <div class="currentDirectory">
            <div class="documents" @click="fetchUserDrives()">
              <i class="uiIconFolder"></i>
              <p class="documents" data-toggle="tooltip" rel="tooltip" data-placement="bottom" data-original-title="Documents">
                Drives
              </p>
            </div>
          </div>
          <div v-for="action in attachmentsComposerActions" :ref="action.key" :key="action.key" :class="`${action.appClass}Action`" class="searchBox">
            <v-icon :class="action.iconClass" @click="executeAction(action)">cloud</v-icon>
            <component :is="action.component.name"></component>
          </div>
        </div>  
      </v-app>
    </div>
    
    <div class="contentHeader">
      <div v-if="!showSearchInput" class="currentDirectory">
        <div class="documents" @click="fetchUserDrives()">
          <i class="uiIconFolder"></i>
          <p class="documents" data-toggle="tooltip" rel="tooltip" data-placement="bottom" data-original-title="Documents">
            {{ $t('attachments.drawer.documents') }}
          </p>
        </div>
        <div v-if="currentDrive.title" class="currentDrive" @click="openDrive(currentDrive)">
          <span class="uiIconArrowRight"></span>
          <a
            :title="currentDrive.title"
            :class="currentDrive.isSelected ? 'active' : ''"
            class="currentDriveTitle"
            data-toggle="tooltip"
            rel="tooltip"
            data-placement="bottom"
          >
            {{ currentDrive.title }}
          </a>
        </div>
        <div class="foldersHistory">
          <div v-if="foldersHistory.length > 2" class="longFolderHistory">
            <span class="uiIconArrowRight"></span>
            <div class="btn-group">
              <button class="btn dropdown-toggle" data-toggle="dropdown">...</button>
              <ul class="dropdown-menu">
                <li v-for="folderHist in foldersHistory.slice(0,foldersHistory.length-2)" :key="folderHist"><a @click="openFolder(folderHist)">{{ folderHist.title }}</a></li>
              </ul>
            </div>
          </div>
          <div
            v-for="folderHis in foldersHistory.slice(foldersHistory.length - 2, foldersHistory.length)"
            :key="folderHis"
            class="folderHistory"
          >
            <span class="uiIconArrowRight"></span>
            <a :title="folderHis.title" :class="folderHis.isSelected? 'active' : ''" class="currentSpaceDirectory" data-toggle="tooltip" rel="tooltip"
               data-placement="bottom" @click="openFolder(folderHis)">
              {{ folderHis.title }}
            </a>
          </div>
        </div>
      </div>
      <div :class="showSearchInput ? 'visible' : ''" class="searchBox">
        <input
          id="searchServerAttachments"
          ref="searchServerAttachments"
          v-model="searchFilesFolders"
          type="text"
          class="searchInput"
        />
        <a
          :class="showSearchInput ? 'uiIconCloseServerAttachments' : 'uiIconSearch'"
          class="uiIconLightGray"
          @click="showSearchDocumentInput()"
        ></a>
      </div>
    </div>

    <div class="contentBody">
      <div class="selectionBox">
        <div v-if="loadingFolders" class="VuetifyApp loader">
          <v-app class="VuetifyApp">
            <v-progress-circular :size="30" :width="3" indeterminate class="loadingRing" color="#578dc9" />
          </v-app>
        </div>
        <div v-if="emptyFolder" class="emptyFolder">
          <i class="uiIconEmptyFolder"></i>
          <p>This folder is empty</p>
        </div>
        <div
          v-for="driver in filteredDrivers"
          :key="driver.name"
          :title="driver.title"
          class="folderSelection"
          @click="openDrive(driver)"
        >
          <a :data-original-title="driver.title" rel="tooltip" data-placement="bottom">
            <i :class="driver.driveTypeCSSClass" class="uiIconEcms24x24DriveGroup uiIconEcmsLightGray selectionIcon center"></i>
            <div class="selectionLabel center">{{ driver.title }}</div>
          </a>
        </div>
        <div v-for="folder in filteredFolders" :key="folder.id" :id="folder.id" :title="folder.name" class="folderSelection"
             @click="openFolder(folder)">
          <a :title="folder.title" href="javascript:void(0);" rel="tooltip" data-placement="bottom">
            <i :class="folder.folderTypeCSSClass" class="uiIcon24x24FolderDefault uiIconEcmsLightGray selectionIcon center"></i>
            <div class="selectionLabel center">{{ folder.title }}</div>
          </a>
        </div>
        <div v-if="emptyFolderForSelectDestination && modeFolderSelection && !emptyFolder" class="emptyFolder">
          <i class="uiIconEmptyFolder"></i>
          <p>{{ $t('attachments.drawer.destination.folder.empty') }}</p>
        </div>
        <div v-for="file in filteredFiles" v-show="!modeFolderSelection" :key="file.id" :id="file.idAttribute" :title="file.idAttribute" :class="file.selected? 'selected' : ''" class="fileSelection" @click="selectFile(file)">
          <exo-attachment-item :file="file"></exo-attachment-item>
        </div>
      </div>
    </div>
    <div class="attachActions">
      <div v-if="!modeFolderSelection" class="limitMessage">
        <span :class="filesCountClass" class="countLimit">
          {{ $t('attachments.drawer.maxFileCountLeft').replace('{0}', filesCountLeft) }}
        </span>
      </div>
      <div v-if="modeFolderSelection" class="buttonActions btnActions">
        <button class="btn btn-primary attach ignore-vuetify-classes btnSelect" type="button" @click="selectDestination()">{{ $t('attachments.drawer.select') }}</button>
        <button class="btn btnCancel" type="button" @click="$emit('cancel')">{{ $t('attachments.drawer.cancel') }}</button>
      </div>
      <div v-if="!modeFolderSelection" class="buttonActions">
        <button class="btn" type="button" @click="$emit('cancel')">{{ $t('attachments.drawer.cancel') }}</button>
        <button
          :disabled="selectedFiles.length === 0"
          class="btn btn-primary attach ignore-vuetify-classes"
          type="button"
          @click="addSelectedFiles()"
        >
          {{ $t('attachments.drawer.select') }}
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import * as attachmentsService from '../attachmentsService.js';
import { getAttachmentsComposerExtensions, executeExtensionAction } from '../extension';

export default {
  props: {
    modeFolderSelectionForFile: {
      type: Boolean,
      default: false
    },
    modeFolderSelection: {
      type: Boolean,
      default:false,
    },
    spaceId: {
      type: String,
      default: '',
    },
    attachedFiles: {
      type: Array,
      default: () => [],
    },
  },
  data() {
    return {
      workspace: 'collaboration',
      currentDrive: {
        name: '',
        title: '',
        isSelected: false,
      },
      driveRootPath: '',
      drivers: [],
      folders: [],
      files: [],
      space: {},
      selectedFiles: [],
      maxFilesCount: 20,
      foldersHistory: [],
      showSearchInput: false,
      searchFilesFolders: '',
      loadingFolders: true,
      filesCountClass: '',
      selectedFolderPath : '',
      schemaFolder: '',
      folderDestinationForFile:'',
      attachmentsComposerActions: []
    };
  },
  computed: {
    filteredFolders() {
      let folders = this.folders.slice();
      if (this.searchFilesFolders && this.searchFilesFolders.trim().length) {
        const searchTerm = this.searchFilesFolders.trim().toLowerCase();
        folders = this.folders.filter((folder) => folder.name.toLowerCase().indexOf(searchTerm) >= 0);
      }
      const txt = document.createElement('textarea');
      folders.forEach((folder) => {
        txt.innerHTML = folder.title;
        folder.title = txt.value;
      });
      return folders;
    },
    filteredFiles() {
      let files = this.files.slice();
      if (this.searchFilesFolders && this.searchFilesFolders.trim().length) {
        const searchTerm = this.searchFilesFolders.trim().toLowerCase();
        files = this.files.filter((file) => file.name.toLowerCase().indexOf(searchTerm) >= 0);
      }
      return files;
    },
    filteredDrivers() {
      let drivers = this.drivers.slice();
      if (this.searchFilesFolders && this.searchFilesFolders.trim().length) {
        const searchTerm = this.searchFilesFolders.trim().toLowerCase();
        drivers = this.drivers.filter((driver) => driver.title.toLowerCase().indexOf(searchTerm) >= 0);
      }
      return drivers;
    },
    filesCountLeft() {
      return this.maxFilesCount - this.selectedFiles.length;
    },
    emptyFolder() {
      return this.files.length === 0 && this.folders.length === 0 && this.drivers.length === 0 && !this.loadingFolders;
    },
    emptyFolderForSelectDestination(){
      return this.folders.length === 0 && this.drivers.length === 0 && !this.loadingFolders;
    }
  },
  watch: {
    filesCountLeft() {
      this.filesCountClass = this.filesCountLeft === 0 ? 'noFilesLeft' : '';
    },
  },
  created() {
    this.selectedFiles = this.attachedFiles.slice();
    const self = this;
    const spaceId = this.getURLQueryParam('spaceId')
      ? this.getURLQueryParam('spaceId')
      : `${eXo.env.portal.spaceId}`
        ? `${eXo.env.portal.spaceId}`
        : this.spaceId;
    attachmentsService.getSpaceById(spaceId).then((space) => {
      if (space.id) {
        self.space = space;
        const spaceGroupId = space.groupId.split('/spaces/')[1];
        self.currentDrive = {
          name: `.spaces.${spaceGroupId}`,
          title: spaceGroupId,
          isSelected: true,
        };
      } else {
        self.currentDrive = {
          name: 'Personal Documents',
          title: 'Personal Documents',
          isSelected: true,
        };
      }
      self.fetchChildrenContents('');
    });
    this.attachmentsComposerActions = getAttachmentsComposerExtensions();
  },
  methods: {
    openFolder: function(folder) {
      this.generateHistoryTree(folder);
      this.resetExplorer();
      folder.isSelected = true;
      this.fetchChildrenContents(folder.path);
      if (folder.path === 'Public') {
        const driverPath = this.driveRootPath.split('/');
        let localDrive = driverPath[0];
        const secondPartPath = 2;
        for (let i = 1; i < driverPath.length - secondPartPath; i++) {
          localDrive = localDrive.concat('/', driverPath[i]);
        }
        this.selectedFolderPath = localDrive.concat('/', folder.path);
      } else {
        this.selectedFolderPath = this.driveRootPath.concat(folder.path);
      }
      this.schemaFolder = this.currentDrive.name.concat('/', folder.title);
      this.folderDestinationForFile = folder.title;
    },
    openDrive(drive) {
      this.foldersHistory = [];
      this.resetExplorer();
      this.currentDrive = {
        name: drive.name,
        title: drive.title,
        isSelected: true,
      };
      this.fetchChildrenContents('');
    },
    fetchChildrenContents: function(parentPath) {
      this.loadingFolders = true;
      const self = this;
      attachmentsService
        .fetchFoldersAndFiles(this.currentDrive.name, this.workspace, parentPath)
        .then((xml) => {
          const rootFolder = xml.childNodes[0];
          if (rootFolder.getAttribute('path') === '/') {
            self.driveRootPath = `${rootFolder.getAttribute('path')}`;
          } else if (parentPath === '') {
            self.driveRootPath = `${rootFolder.getAttribute('path')}/`;
          }
          self.setFoldersAndFiles(rootFolder);
          self.loadingFolders = false;
        })
        .catch(() => this.loadingFolders = false);
    },
    fetchUserDrives() {
      this.resetExplorer();
      this.loadingFolders = true;
      this.currentDrive = {};
      this.foldersHistory = [];
      const self = this;
      attachmentsService
        .getDrivers()
        .then((xml) => {
          const drivers = xml.childNodes[0].childNodes;
          self.setDrivers(drivers);
          this.loadingFolders = false;
        })
        .catch(() => this.loadingFolders = false);
    },
    resetExplorer() {
      this.drivers = [];
      this.folders = [];
      this.files = [];
    },
    getRelativePath: function(absolutePath) {
      if (absolutePath && absolutePath.startsWith(this.driveRootPath)) {
        return absolutePath.substr(this.driveRootPath.length);
      }
    },
    getURLQueryParam(paramName) {
      const urlParams = new URLSearchParams(window.location.search);
      if (urlParams.has(paramName)) {
        return urlParams.get(paramName);
      }
    },
    selectFile(file) {
      if (document.getElementById(file.idAttribute).className === 'fileSelection' && this.filesCountLeft > 0) {
        document.getElementById(file.idAttribute).className = 'fileSelection selected';
        if (!this.selectedFiles.find((f) => f.id === file.id)) {
          this.selectedFiles.push(file);
        }
      } else {
        document.getElementById(file.idAttribute).className = 'fileSelection';
        const index = this.selectedFiles.findIndex((f) => f.id === file.id);
        if (index !== -1) {
          this.selectedFiles.splice(index, 1);
        }
      }
    },
    generateHistoryTree(folder) {
      if (!this.foldersHistory.find((f) => f.name === folder.name) && folder) {
        this.foldersHistory.push({
          name: folder.name,
          title: folder.title,
          path: folder.driverType ? '' : folder.path,
          driverType: folder.driverType ? folder.driverType : '',
        });
      }
      if (!folder.driverType && folder.path) {
        this.foldersHistory = this.foldersHistory.filter((ele) => folder.path.split('/').find((f) => f === ele.name));
      }
      this.currentDrive.isSelected = false;
      this.foldersHistory.forEach((f) => f.isSelected = false);
      this.foldersHistory.find((f) => f.name === folder.name).isSelected = true;
    },
    addSelectedFiles() {
      this.$emit('itemsSelected', this.selectedFiles);
    },
    showSearchDocumentInput() {
      this.showSearchInput = !this.showSearchInput;
      document.getElementById('searchServerAttachments').style.display = this.showSearchInput ? 'block' : 'none';
      this.$refs.searchServerAttachments.focus();
      this.searchFilesFolders = '';
    },
    setFoldersAndFiles(rootFolder) {
      const fetchedDocuments = rootFolder.childNodes;
      for (let i = 0; i < fetchedDocuments.length; i++) {
        if (fetchedDocuments[i].tagName === 'Folders') {
          const fetchedFolders = fetchedDocuments[i].childNodes;
          for (let j = 0; j < fetchedFolders.length; j++) {
            const folderType = fetchedFolders[j].getAttribute('nodeType');
            const folderTypeCSSClass = `uiIcon24x24${folderType.replace(':', '_')}`;
            const id = fetchedFolders[j]
              .getAttribute('path')
              .split('/')
              .pop();
            this.folders.push({
              id: id,
              name: fetchedFolders[j].getAttribute('name'),
              title: fetchedFolders[j].getAttribute('title'),
              path: fetchedFolders[j].getAttribute('currentFolder'),
              folderTypeCSSClass: folderTypeCSSClass,
              isSelected: false,
            });
          }
        } else if (fetchedDocuments[i].tagName === 'Files') {
          const fetchedFiles = fetchedDocuments[i].childNodes;
          for (let j = 0; j < fetchedFiles.length; j++) {
            const fileExtension = `${fetchedFiles[j]
              .getAttribute('name')
              .split('.')[1]
              .charAt(0)
              .toUpperCase()}${fetchedFiles[j]
              .getAttribute('name')
              .split('.')[1]
              .substring(1)}`;
            const fileTypeCSSClass = `uiBgd64x64File${fileExtension}`;
            const idAttribute = fetchedFiles[j]
              .getAttribute('path')
              .split('/')
              .pop();
            const id = fetchedFiles[j].getAttribute('id');
            const selected = this.attachedFiles.some((f) => f.id === id);
            this.files.push({
              id: id,
              name: fetchedFiles[j].getAttribute('name'),
              title: fetchedFiles[j].getAttribute('title'),
              path: this.getRelativePath(fetchedFiles[j].getAttribute('path')),
              size: fetchedFiles[j].getAttribute('size'),
              fileTypeCSSClass: fileTypeCSSClass,
              idAttribute: idAttribute,
              selected: selected,
              mimetype: fetchedFiles[j].getAttribute('nodeType')
            });
          }
        }
      }
    },
    setDrivers(drivers) {
      for (let i = 0; i < drivers.length; i++) {
        if (drivers[i].tagName === 'Folders') {
          const fetchedDrivers = drivers[i].childNodes;
          let driverTypeClass;
          const driverType = drivers[i].getAttribute('name');
          if (driverType === 'Personal Drives') {
            driverTypeClass = 'uiIconEcms24x24DrivePrivate';
          } else {
            driverTypeClass = `uiIconEcms24x24Drive${driverType.split(' ')[0]}`;
          }
          for (let j = 0; j < fetchedDrivers.length; j++) {
            const name = fetchedDrivers[j].getAttribute('name');
            const driveTypeCSSClass = `uiIconEcms24x24Drive${name.replace(/\s/g, '')} ${driverTypeClass}`;
            this.drivers.push({
              name: name,
              title: fetchedDrivers[j].getAttribute('label'),
              path: fetchedDrivers[j].getAttribute('path'),
              css: fetchedDrivers[j].getAttribute('nodeTypeCssClass'),
              type: 'drive',
              driveTypeCSSClass: driveTypeCSSClass,
              driverType: driverType,
            });
          }
        }
      }
    },
    selectDestination(){
      if(!this.selectedFolderPath){
        this.selectedFolderPath = this.driveRootPath;
        this.schemaFolder = this.currentDrive.name ;
        this.folderDestinationForFile = this.currentDrive.name;
      }
      if (this.modeFolderSelectionForFile) {
        this.$emit('itemsSelected', this.selectedFolderPath, this.folderDestinationForFile);
      } else {
        this.$emit('itemsSelected', this.selectedFolderPath, this.schemaFolder);
      }
    },
    executeAction(action) {
      executeExtensionAction(action, this.$refs[action.key]);
    },
  },
};
</script>
