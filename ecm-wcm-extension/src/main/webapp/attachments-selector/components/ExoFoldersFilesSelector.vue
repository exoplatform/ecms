<template>
  <div class="serverFiles">
    <div v-show="connectedMessage" class="alert alert-info attachmentsAlert">
      <b>{{ $t('attachments.alert.connected') }} {{ connectedMessage }}!</b>{{ $t('attachments.alert.pleaseNote') }}
    </div>
    <div class="contentHeader">
      <div v-if="!showSearchInput" class="currentDirectory">
        <div class="documents" @click="fetchUserDrives()">
          <i class="uiIconFolder"></i>
          <p class="documents" data-toggle="tooltip" rel="tooltip" data-placement="bottom"
             data-original-title="Documents">{{ $t('attachments.drawer.drives') }}</p>
        </div>
        <div v-if="currentDrive" class="currentDrive" @click="openDrive(currentDrive)">
          <span class="uiIconArrowRight"></span>
          <a :title="currentDrive.title" :class="currentDrive.isSelected? 'active' : ''" class="currentDriveTitle" data-toggle="tooltip" rel="tooltip"
             data-placement="bottom">
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
          <div v-for="folderHis in foldersHistory.slice(foldersHistory.length-2,foldersHistory.length)" :key="folderHis" class="folderHistory">
            <span class="uiIconArrowRight"></span>
            <a :title="folderHis.title" :class="folderHis.isSelected? 'active' : ''" class="currentSpaceDirectory" data-toggle="tooltip" rel="tooltip"
               data-placement="bottom" @click="openFolder(folderHis)">
              {{ folderHis.title }}
            </a>
          </div>
        </div>
      </div>
      <div :class="showSearchInput? 'visible' : ''" class="selectorActions">
        <input id="searchServerAttachments" ref="searchServerAttachments" v-model="searchFilesFolders" type="text" class="searchInput">
        <a :class="showSearchInput ? 'uiIconCloseServerAttachments' : 'uiIconFilter'" class="uiIconLightGray" @click="showSearchDocumentInput()"></a>
        <a v-if="(modeFolderSelectionForFile || modeFolderSelection) && currentDrive" :title="$t('attachments.filesFoldersSelector.button.addNewFOlder.tooltip')" rel="tooltip" data-placement="bottom" class="uiIconLightGray uiIconAddFolder" @click="addNewFolder()"></a>
      </div>
      <div v-for="action in attachmentsComposerActions" v-show="showDriveAction" :key="action.key" :class="`${action.appClass}Action`" class="actionBox">
        <div v-if="!modeFolderSelection" class="actionBoxLogo" @click="executeAction(action)">
          <v-icon v-if="action.iconName" class="uiActionIcon" >{{ action.iconName }}</v-icon>
          <i v-else :class="action.iconClass" class="uiActionIcon"></i>
        </div>
      </div>
    </div>

    <transition name="fade" mode="in-out">
      <div v-show="showErrorMessage" class="alert foldersFilesSelectorAlert alert-error">
        <i class="uiIconError"></i>{{ errorMessage }}
      </div>
    </transition>
    <div class="contentBody">
      <div v-if="currentDrive" class="selectionBox">
        <div v-if="loadingFolders" class="VuetifyApp loader">
          <v-app class="VuetifyApp">
            <v-progress-circular
              :size="30"
              :width="3"
              indeterminate
              class="loadingRing"
              color="#578dc9" />
          </v-app>
        </div>
        <div v-if="emptyFolder" class="emptyFolder">
          <i class="uiIconEmptyFolder"></i>
          <p>This folder is empty</p>
        </div>
        <div v-for="folder in filteredFolders" :key="folder.id" :id="folder.id" :title="folder.name" :class="folder.type === 'new_folder' ? 'boxOfFolder':''"
             class="folderSelection" @click="openFolder(folder)" @contextmenu="openFolderActionsMenu(folder, $event)">
          <a v-if="folder.type === 'new_folder'" href="javascript:void(0);" class="closeIcon" @mousedown="cancelCreatingNewFolder($event)">
            <p>
              x
            </p>
          </a>
          <div :class="folder.type === 'new_folder' ? 'boxOfTitle' :''">
            <a :title="folder.title" href="javascript:void(0);" rel="tooltip" data-placement="bottom">
              <i :class="folder.folderTypeCSSClass" class="uiIcon24x24FolderDefault uiIconEcmsLightGray selectionIcon center"></i>
              <i v-show="folder.isCloudDrive" :class="getFolderIcon(folder)" class="uiIcon-clouddrive"></i>
              <input v-if="folder.type === 'new_folder'" :ref="folder.ref" v-model="newFolderName" type="text" class="newFolderInput  ignore-vuetify-classes" @blur="createNewFolder($event)" @keyup.enter="$event.target.blur()" @keyup.esc="cancelCreatingNewFolder($event)">
              <input v-else-if="renameFolderAction && folder.id === selectedFolder.id" ref="rename" :id="folder.id" v-model="newName" type="text" class="newFolderInput  ignore-vuetify-classes" @blur="saveNewNameFolder()" @keyup.enter="$event.target.blur()" @keyup.esc="cancelRenameNewFolder($event)">
              <div v-else class="selectionLabel center">{{ folder.title }}</div>
            </a>
          </div>
        </div>
        <exo-dropdown-menu ref="folderActionsMenu" :folder-actions-menu-left="folderActionsMenuLeft" :folder-actions-menu-top="folderActionsMenuTop" :show-dropdown-menu="showFolderActionsMenu" :selected-folder="selectedFolder" @renameFolder="renameFolder()" @deleteFolder="deleteFolder" @closeMenu="closeFolderActionsMenu"></exo-dropdown-menu>
        <div v-if="emptyFolderForSelectDestination && modeFolderSelection && !emptyFolder" class="emptyFolder">
          <i class="uiIconEmptyFolder"></i>
          <p>{{ $t('attachments.drawer.destination.folder.empty') }}</p>
        </div>
        <div v-for="file in filteredFiles" v-show="!modeFolderSelection" :key="file.id" :id="file.idAttribute" :title="file.idAttribute" :class="file.selected? 'selected' : ''" class="fileSelection" @click="selectFile(file)">
          <exo-attachment-item :file="file"></exo-attachment-item>
        </div>
      </div>
      <div v-else class="categorizedDrives">
        <v-list dense flat class="drivesList">
          <v-list-group value="true" class="categories" eager>
            <v-list-group v-for="(group, name) in filteredDrivers" :key="name" :ripple="false" sub-group no-action value="true" class="category" active-class="categoryActive">
              <template v-slot:activator>
                <v-list-item-content class="categoryContent">{{ name }}</v-list-item-content>
              </template>
              <div class="selectionBox">
                <div 
                  v-for="driver in group.drives" 
                  :key="driver.name" 
                  :title="driver.title" 
                  class="folderSelection"
                  @click="openDrive(driver, name)">
                  <a :data-original-title="driver.title" rel="tooltip" data-placement="bottom">
                    <i 
                      v-show="!drivesInProgress[driver.title]"
                      :class="driver.isCloudDrive ? driver.driveTypeCSSClass : `uiIconEcms24x24DriveGroup ${driver.driveTypeCSSClass}`"
                      class="uiIconEcmsLightGray selectionIcon center"></i>
                    <div class="text-center ma-12 connectingDrive">
                      <v-progress-circular
                        v-show="drivesInProgress[driver.title] >= 0 || drivesInProgress[driver.title] <= 100"
                        :indeterminate="false"
                        :rotate="0"
                        :size="40"
                        :value="drivesInProgress[driver.title]"
                        :width="4"
                        color="var(--allPagesPrimaryColor, #578dc9)"
                        class="connectingDriveProgress"
                      >{{ drivesInProgress[driver.title] }}<span class="connectingDriveProgressPercent">%</span></v-progress-circular>
                    </div>
                    <div 
                      :class="{ 'connectingDriveTitle': drivesInProgress[driver.title] >= 0 || drivesInProgress[driver.title] <= 100}"
                      class="selectionLabel center"
                    >{{ driver.title }}</div>
                  </a>
                </div>
              </div>
            </v-list-group>
          </v-list-group>
        </v-list>
      </div>
    </div>
    <div class="attachActions">
      <div v-if="!modeFolderSelection && currentDrive" class="limitMessage">
        <span :class="filesCountClass" class="countLimit">
          {{ $t('attachments.drawer.maxFileCountLeft').replace('{0}', filesCountLeft) }}
        </span>
      </div>
      <div v-if="modeFolderSelection" class="buttonActions btnActions">
        <button class="btn btn-primary attach ignore-vuetify-classes btnSelect" type="button" @click="selectDestination()">{{ $t('attachments.drawer.select') }}</button>
        <button class="btn btnCancel" type="button" @click="$emit('cancel')">{{ $t('attachments.drawer.cancel') }}</button>
      </div>
      <div v-if="!modeFolderSelection && currentDrive" class="buttonActions">
        <button class="btn" type="button" @click="$emit('cancel')">{{ $t('attachments.drawer.cancel') }}</button>
        <button :disabled="selectedFiles.length === 0" class="btn btn-primary attach ignore-vuetify-classes" type="button" @click="addSelectedFiles()">{{ $t('attachments.drawer.select') }}</button>
      </div>
    </div>
    <!-- The following bloc is needed in order to display the confirmation popup -->
    <!--begin -->
    <exo-confirm-dialog
      ref="confirmDialog"
      :title="titleLabel"
      :message="popupBodyMessage"
      :ok-label="okLabel"
      :cancel-label="cancelLabel"
      @ok="okConfirmDialog"/>
      <!--end -->
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
      default: ''
    },
    attachedFiles: {
      type: Array,
      default: () => []
    },
    extensionRefs: {
      type: Array,
      default: () => []
    },
    connectedDrive: {
      type: Object,
      default: () => ({})
    },
    cloudDrivesInProgress: {
      type: Object,
      default: () => ({})
    }
  },
  data() {
    return {
      workspace: 'collaboration',
      currentDrive: null,
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
      attachmentsComposerActions: [],
      creatingNewFolder: false,
      newFolderName: '',
      currentAbsolutePath: '',
      popupBodyMessage: '',
      showFolderActionsMenu: false,
      folderActionsMenuTop: '0px',
      folderActionsMenuLeft: '0px',
      selectedFolder: {},
      windowPositionLimit: 25,
      MESSAGE_TIMEOUT: 5000,
      showErrorMessage: false,
      errorMessage: '',
      renameFolderAction:false,
      newName:'',
      MESSAGES_DISPLAY_TIME: 5000,
      privateDestinationForFile: false,
      fromSpace: {},
      okLabel: '',
      cancelLabel: '',
      titleLabel: '',
      okAction: false
    };
  },
  computed: {
    showDriveAction(){
      return this.currentDrive ? this.currentDrive.name === 'Personal Documents': true;
    },
    filteredFolders() {
      let folders = this.folders.slice();
      if (this.searchFilesFolders && this.searchFilesFolders.trim().length){
        const searchTerm = this.searchFilesFolders.trim().toLowerCase();
        folders = this.folders.filter(folder => folder.name.toLowerCase().indexOf(searchTerm) >= 0 );
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
      if (this.searchFilesFolders && this.searchFilesFolders.trim().length){
        const searchTerm = this.searchFilesFolders.trim().toLowerCase();
        files = this.files.filter(file => file.name.toLowerCase().indexOf(searchTerm) >= 0 );
      }
      return files;
    },
    filteredDrivers() {
      let drivers = this.drivers.slice();
      if (this.searchFilesFolders && this.searchFilesFolders.trim().length){
        const searchTerm = this.searchFilesFolders.trim().toLowerCase();
        drivers = this.drivers.filter(driver => driver.title.toLowerCase().indexOf(searchTerm) >= 0 );
      }
      const drivesByTypes = { 
        'My Drives': { drives: [] }, 
        'My Spaces': { drives: [] }, 
        'Others': { drives: [] } 
      };
      drivers.map(drive => { 
        if (drive.driverType === 'Personal Drives') {
          drivesByTypes['My Drives'].drives.push(drive);
        } else if (drive.path.includes('spaces')) {
          drivesByTypes['My Spaces'].drives.push(drive);
        } else if (!drive.path.includes('Trash')) {
          drivesByTypes['Others'].drives.push(drive);
        }
        return drive;
      });
      return drivesByTypes;
    },
    filesCountLeft(){
      return this.maxFilesCount - this.selectedFiles.length;
    },
    emptyFolder() {
      return this.files.length === 0 && this.folders.length === 0 && this.drivers.length === 0 && !this.loadingFolders;
    },
    emptyFolderForSelectDestination(){
      return this.folders.length === 0 && this.drivers.length === 0 && !this.loadingFolders;
    },
    connectedMessage() {
      let connectedDrive;
      for (const key in this.drivesInProgress) {
        const fullProgress = 100;
        connectedDrive = this.drivesInProgress[key] >= fullProgress ? key : '';
      }
      return connectedDrive;
    },
    drivesInProgress() {
      return { ...this.cloudDrivesInProgress.drives };
    }
  },
  watch: {
    filesCountLeft() {
      this.filesCountClass = this.filesCountLeft === 0 ? 'noFilesLeft' : '';
    },
    showErrorMessage: function(newVal) {
      if(newVal) {
        setTimeout(() => this.showErrorMessage = false, this.MESSAGE_TIMEOUT);
      }
    },
    connectedDrive: function(drive) {
      if (!this.drivers.some(({ title }) => title === drive.title)) {
        this.drivers.push(drive); // display connecting drive in 'My Drives' section
      }
    }
  },
  created() {
    this.selectedFiles = this.attachedFiles.slice();
    const self = this;
    const spaceId = this.getURLQueryParam('spaceId') ? this.getURLQueryParam('spaceId') : `${eXo.env.portal.spaceId}` ? `${eXo.env.portal.spaceId}` : this.spaceId;
    attachmentsService.getSpaceById(spaceId).then( space => {
      if(space.id) {
        self.space = space;
        const spaceGroupId = space.groupId.split('/spaces/')[1];
        self.currentDrive = {
          name: `.spaces.${spaceGroupId}`,
          title: spaceGroupId,
          isSelected: true
        };
        self.fetchChildrenContents('');
      } else {
        self.currentDrive = null;
        this.fetchUserDrives();
      }
    }).catch(() => {
      this.errorMessage= `${this.$t('attachments.fetchFoldersAndFiles.error')}`;
      this.showErrorMessage = true;
    });
    this.attachmentsComposerActions = getAttachmentsComposerExtensions();
  },
  methods: {
    openFolder: function (folder) {
      if (this.selectedFolder.id && this.selectedFolder.canRemove){
        this.$refs.rename[0].focus();
      }
      else if (folder.type === 'new_folder') {
        this.$refs.newFolder[0].focus();
      } else {
        this.currentAbsolutePath = folder.path;
        this.generateHistoryTree(folder);
        this.resetExplorer();
        folder.isSelected = true;
        this.fetchChildrenContents(folder.path);
        this.getFolderPath(folder);
        this.schemaFolder = this.currentDrive.title.concat('/', folder.path);
        this.folderDestinationForFile = folder.name;
      }
      this.schemaFolder = this.currentDrive.title.concat('/', folder.path);
      this.folderDestinationForFile = folder.title;
      this.privateDestinationForFile = folder.isPublic;
    },
    openDrive(drive, group) {
      this.currentAbsolutePath = '';
      this.foldersHistory = [];
      this.resetExplorer();
      this.fromSpace = group === 'My Spaces' ? { title: drive.title, name: drive.name } : {};
      this.currentDrive = {
        name: drive.name,
        title: drive.title,
        isSelected: true
      };
      this.fetchChildrenContents('');
    },
    fetchChildrenContents: function (parentPath) {
      this.loadingFolders = true;
      const self = this;
      attachmentsService.fetchFoldersAndFiles(this.currentDrive.name, this.workspace, parentPath).then(xml => {
        const rootFolder = xml.childNodes[0];
        if (rootFolder.getAttribute('path') === '/') {
          self.driveRootPath = `${rootFolder.getAttribute('path')}`;
        } else if (parentPath === '') {
          self.driveRootPath = `${rootFolder.getAttribute('path')}/`;
        }
        self.setFoldersAndFiles(rootFolder);
        self.loadingFolders = false;
      }).catch(error => {
        this.loadingFolders = false;
        this.errorMessage= `${this.$t('attachments.fetchFoldersAndFiles.error')}. ${error.message ? error.message : ''}`;
        this.showErrorMessage = true;
      });
    },
    fetchUserDrives() {
      this.resetExplorer();
      this.loadingFolders = true;
      this.currentDrive = null;
      this.foldersHistory = [];
      const self = this;
      attachmentsService.getDrivers().then(xml => {
        const drivers = xml.childNodes[0].childNodes;
        self.setDrivers(drivers);
        this.loadingFolders = false;
      }).catch(() => {
        this.loadingFolders = false;
        this.errorMessage= `${this.$t('attachments.getDrivers.error')}`;
        this.showErrorMessage = true;
      });
    },
    resetExplorer() {
      this.drivers = [];
      this.folders = [];
      this.files = [];
    },
    getRelativePath: function (absolutePath) {
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
        if (!this.selectedFiles.find(f => f.id === file.id)) {
          this.selectedFiles.push({ ...file, space: this.fromSpace });
        }
      } else {
        document.getElementById(file.idAttribute).className = 'fileSelection';
        const index = this.selectedFiles.findIndex(f => f.id === file.id);
        if (index !== -1) {
          this.selectedFiles.splice(index, 1);
        }
      }
    },
    generateHistoryTree(folder) {
      if (!this.foldersHistory.find(f => f.name === folder.name) && folder) {
        this.foldersHistory.push({
          name: folder.name,
          title: folder.title,
          path: folder.driverType ? '' : folder.path,
          driverType: folder.driverType ? folder.driverType : ''
        });
      }
      if (!folder.driverType && folder.path) {
        this.foldersHistory = this.foldersHistory.filter(ele =>
          folder.path.split('/').find(f => f === ele.name)
        );
      }
      this.currentDrive.isSelected = false;
      this.foldersHistory.forEach(f => f.isSelected = false);
      this.foldersHistory.find(f => f.name === folder.name).isSelected = true;
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
            const id = fetchedFolders[j].getAttribute('path').split('/').pop();
            this.folders.push({
              id: id,
              name: fetchedFolders[j].getAttribute('name'),
              title: fetchedFolders[j].getAttribute('title'),
              path: fetchedFolders[j].getAttribute('currentFolder'),
              folderTypeCSSClass: folderTypeCSSClass,
              isSelected: false,
              canRemove: fetchedFolders[j].getAttribute('canRemove') === 'true',
              isCloudDrive: fetchedFolders[j].getAttribute('isCloudDrive') === 'true' ? true : false,
              isPublic: fetchedFolders[j].getAttribute('isPublic') === 'true' ? true : false,
              cloudProvider: fetchedFolders[j].getAttribute('cloudProvider')
            });
          }
        } else if (fetchedDocuments[i].tagName === 'Files') {
          const fetchedFiles = fetchedDocuments[i].childNodes;
          for (let j = 0; j < fetchedFiles.length; j++) {
            const idAttribute = fetchedFiles[j].getAttribute('path').split('/').pop();
            const id = fetchedFiles[j].getAttribute('id');
            const selected = this.attachedFiles.some(f => f.id === id);
            this.files.push({
              id: id,
              name: fetchedFiles[j].getAttribute('name'),
              title: fetchedFiles[j].getAttribute('title'),
              path: this.getRelativePath(fetchedFiles[j].getAttribute('path')),
              size: fetchedFiles[j].getAttribute('size'),
              idAttribute: idAttribute,
              selected: selected,
              mimetype: fetchedFiles[j].getAttribute('nodeType'),
              isCloudFile: fetchedFiles[j].getAttribute('isCloudFile') === 'true' ? true : false,
              isPublic: fetchedFiles[j].getAttribute('isPublic') === 'true' ? true : false
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
            const isCloudDrive = fetchedDrivers[j].getAttribute('isCloudDrive') === 'true' ? true : false;
            const driveTypeCSSClass = isCloudDrive 
              ? `uiIconEcmsDrive-${fetchedDrivers[j].getAttribute('cloudProvider')}`
              : `${name.replace(/\s/g, '')} ${driverTypeClass}`;
            this.drivers.push({
              name: name,
              title: fetchedDrivers[j].getAttribute('label'),
              path: fetchedDrivers[j].getAttribute('path'),
              css: fetchedDrivers[j].getAttribute('nodeTypeCssClass'),
              type: 'drive',
              driveTypeCSSClass: driveTypeCSSClass,
              driverType: driverType,
              isCloudDrive: isCloudDrive
            });
          }
        }
      }
    },
    selectDestination() {
      if (!this.selectedFolderPath) {
        this.selectedFolderPath = this.driveRootPath;
        this.schemaFolder = this.currentDrive.title;
        this.folderDestinationForFile = this.currentDrive.title;
      }
      if (this.modeFolderSelectionForFile) {
        this.$emit('itemsSelected', this.selectedFolderPath, this.folderDestinationForFile, this.privateDestinationForFile);
      } else {
        this.$emit('itemsSelected', this.selectedFolderPath, this.schemaFolder);
      }
    },
    executeAction(action) {
      executeExtensionAction(action, this.extensionRefs[action.key][0]);
    },
    addNewFolder() {
      if (!this.creatingNewFolder) {
        this.creatingNewFolder = true;
        this.newFolderName = 'new_folder';
        this.folders.unshift({
          id: 'new_folder',
          type: 'new_folder',
          ref: 'newFolder',
          folderTypeCSSClass: 'uiIcon24x24nt_folder',
          isSelected: false
        });
      }
      this.$nextTick(() => this.$refs.newFolder[0].focus());
    },
    createNewFolder() {
      if (this.creatingNewFolder) {
        if (this.newFolderName) {
          const folderNameExists = this.folders.some(folder => folder.title === this.newFolderName);
          if (folderNameExists) {
            this.$refs.confirmDialog.open();
            this.titleLabel = this.$t('attachments.filesFoldersSelector.popup.title');
            this.okLabel = this.$t('attachments.filesFoldersSelector.popup.button.ok');
            this.popupBodyMessage = `${this.$t('attachments.filesFoldersSelector.popup.folderNameExists')}`;
          } else {
            const self = this;
            attachmentsService.createFolder(this.currentDrive.name, this.workspace, this.currentAbsolutePath, this.newFolderName).then(xml => {
              const createdNewFolder = xml.childNodes[0];
              if (createdNewFolder) {
                const folderType = createdNewFolder.getAttribute('nodeType');
                const folderTypeCSSClass = `uiIcon24x24${folderType.replace(':', '_')}`;
                const id = createdNewFolder.getAttribute('path').split('/').pop();
                const newFolder = {
                  id: id,
                  name: createdNewFolder.getAttribute('name'),
                  title: createdNewFolder.getAttribute('title'),
                  path: createdNewFolder.getAttribute('currentFolder'),
                  folderTypeCSSClass: folderTypeCSSClass,
                  isSelected: false,
                  canRemove: true
                };
                self.folders.shift();
                self.folders.unshift(newFolder);
                self.creatingNewFolder = false;
                self.newFolderName = '';
              } else {
                self.creatingNewFolder = false;
                self.newFolderName = '';
              }
            }).catch(() => {
              this.errorMessage= `${this.$t('attachments.createFolder.error')}`;
              this.showErrorMessage = true;
            });
          }
        } else {
          this.$refs.confirmDialog.open();
          this.titleLabel = this.$t('attachments.filesFoldersSelector.popup.title');
          this.okLabel = this.$t('attachments.filesFoldersSelector.popup.button.ok');
          this.popupBodyMessage = `${this.$t('attachments.filesFoldersSelector.popup.emptyFolderName')}`;
        }
      }
    },
    cancelCreatingNewFolder() {
      this.folders.shift();
      this.creatingNewFolder = false;
      this.newFolderName = '';
    },
    cancelRenameNewFolder() {
      this.renameFolderAction = false;
      this.newName = this.selectedFolder.title;
    },
    openFolderActionsMenu(folder, event) {
      this.selectedFolder = folder;
      this.showFolderActionsMenu = true;
      Vue.nextTick(function() {
        this.$refs.folderActionsMenu.$el.focus();
        this.setFolderActionsMenu(event.y, event.x);
      }.bind(this));
      event.preventDefault();
    },
    setFolderActionsMenu: function(top, left) {
      const largestHeight = window.innerHeight - this.$refs.folderActionsMenu.$el.offsetHeight - this.windowPositionLimit;
      const largestWidth = window.innerWidth - this.$refs.folderActionsMenu.$el.offsetWidth - this.windowPositionLimit;
      if (top > largestHeight) {
        top = largestHeight;
      }
      if (left > largestWidth) {
        left = largestWidth;
      }
      this.folderActionsMenuTop = `${top}px`;
      this.folderActionsMenuLeft = `${left}px`;
    },
    closeFolderActionsMenu: function() {
      this.showFolderActionsMenu = false;
    },
    deleteFolder() {
      if(this.selectedFolder.canRemove) {
        this.$refs.confirmDialog.open();
        this.titleLabel = this.$t('attachments.filesFoldersSelector.action.delete.popup.title');
        this.okLabel = this.$t('attachments.filesFoldersSelector.action.delete.popup.button.ok');
        this.cancelLabel = this.$t('attachments.filesFoldersSelector.action.delete.popup.button.cancel');
        this.okAction = true;
        this.popupBodyMessage = `${this.$t('attachments.filesFoldersSelector.action.delete.popup.bodyMessage')}`;
      }
    },
    okConfirmDialog() {
      if (this.okAction){
        attachmentsService.deleteFolderOrFile(this.currentDrive.name, this.workspace,this.selectedFolder.path).then(() => {
          this.reloadCurrentPath();
        }).catch(() => {
          this.errorMessage= `${this.$t('attachments.deleteFolderOrFile.error')}`;
          this.showErrorMessage = true;
        });
      }else {
        return;
      }
    },
    reloadCurrentPath(){
      this.resetExplorer();
      if(this.currentAbsolutePath) {
        this.fetchChildrenContents(this.currentAbsolutePath);
      } else {
        this.fetchChildrenContents('');
      }
    },
    renameFolder() {
      if (this.selectedFolder.canRemove) {
        if (this.selectedFolder.title) {
          this.newName = this.selectedFolder.title;
          this.renameFolderAction = true;
          this.showFolderActionsMenu = false;
          this.$nextTick(() => {this.$refs.rename[0].focus();});
        }
      }
    },
    saveNewNameFolder() {
      if (this.newName !== this.selectedFolder.title && this.newName !== '') {
        const folderNameExists = this.folders.some(folder => folder.title === this.newName);
        if (folderNameExists) {
          this.$refs.confirmDialog.open();
          this.titleLabel = this.$t('attachments.filesFoldersSelector.popup.title');
          this.okLabel = this.$t('attachments.filesFoldersSelector.popup.button.ok');
          this.popupBodyMessage = `${this.$t('attachments.renameFolder.error')}`;
          this.cancelRenameNewFolder();
        } else {
          this.getFolderPath(this.selectedFolder);
          const path = encodeURIComponent(this.workspace.concat(':', this.selectedFolderPath));
          attachmentsService.renameFolder(path, this.newName).then(response => {
            if (response) {
              this.renameFolderAction = false;
              this.folders.find(folder => {
                if (folder.id === this.selectedFolder.id) {
                  folder.title = this.newName;
                  folder.name = this.newName;
                  folder.id = this.newName;
                  const oldFolder = folder.path.split('/');
                  let newPath = oldFolder[0];
                  for (let i = 0; i < oldFolder.length - 1; i++) {
                    newPath.concat('/', newPath[i]);
                  }
                  if (oldFolder.length === 1) {
                    newPath = this.newName;
                  } else {
                    newPath.concat('/', this.newName);
                  }
                  folder.path = newPath;
                }
              });
              this.selectedFolder = {};
            }
          });
        }
      }else {
        this.selectedFolder = {};
      }
      this.selectedFolderPath = '';
    },
    getFolderPath(folder){
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
    },
    getFolderIcon(folder) {
      return `uiIcon-${folder.cloudProvider}`;
    }
  }
};
</script>