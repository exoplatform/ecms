<template>
  <exo-drawer
    ref="driveExplorerDrawer"
    class="driveExplorerDrawer"
    right>
    <template slot="title">
      <div class="drawerHeader">
        <v-btn icon>
          <v-icon @click="closeAttachmentsDriveExplorerDrawer()">
            mdi-keyboard-backspace
          </v-icon>
        </v-btn>
        <span>{{ driveExplorerDrawerTitle }}</span>
      </div>
    </template>
    <template slot="content">
      <div class="serverFiles pt-0 pa-3" @click="closeFolderActionsMenu">
        <div v-show="connectedMessage" class="alert alert-info attachmentsAlert">
          <b>{{ $t('attachments.alert.connected') }} {{ connectedMessage }}!</b>{{ $t('attachments.alert.pleaseNote') }}
        </div>
        <div class="contentHeader border-bottom-color d-flex align-center pb-2 ma-3">
          <div v-if="!showSearchInput" class="currentDirectory d-flex align-center mr-2">
            <div class="documents clickable d-flex align-center" @click="fetchUserDrives()">
              <i class="uiIconFolder mr-1"></i>
              <span
                class="documents"
                data-toggle="tooltip"
                rel="tooltip"
                data-placement="bottom"
                data-original-title="Documents">
                {{ $t('attachments.drawer.drives') }}
              </span>
            </div>
            <div
              v-if="currentDrive"
              class="currentDrive d-flex clickable align-center"
              @click="openDrive(currentDrive)">
              <span class="uiIconArrowRight"></span>
              <a
                :title="currentDrive.title"
                :class="currentDrive.isSelected? 'active font-weight-bold' : ''"
                class="currentDriveTitle text-truncate"
                data-toggle="tooltip"
                rel="tooltip"
                data-placement="bottom">
                {{ currentDrive.title }}
              </a>
            </div>
            <div v-if="foldersHistory.length > 2" class="longFolderHistory d-flex align-center">
              <span class="uiIconArrowRight"></span>
              <div class="btn-group">
                <button class="btn dropdown-toggle btn px-2 py-1" data-toggle="dropdown">
                  ...
                </button>
                <ul class="dropdown-menu folders-menu">
                  <li v-for="folderHist in foldersHistory.slice(0,foldersHistory.length-2)" :key="folderHist">
                    <a
                      @click="openFolder(folderHist)">{{ folderHist.title }}</a>
                  </li>
                </ul>
              </div>
            </div>
            <div class="foldersHistory  d-flex">
              <div
                v-for="folderHis in foldersHistory.slice(foldersHistory.length-2,foldersHistory.length)"
                :key="folderHis"
                class="folderHistory d-flex align-center text-truncate">
                <span class="uiIconArrowRight"></span>
                <a
                  :title="folderHis.title"
                  :class="folderHis.isSelected? 'active font-weight-bold' : ''"
                  class="currentSpaceDirectory text-truncate clickable"
                  data-toggle="tooltip"
                  rel="tooltip"
                  data-placement="bottom"
                  @click="openFolder(folderHis)">
                  {{ folderHis.title }}
                </a>
              </div>
            </div>
          </div>
          <div :class="showSearchInput? 'visible' : ''" class="selectorActions d-flex align-center">
            <input
              id="searchServerAttachments"
              ref="searchServerAttachments"
              v-model="searchFilesFolders"
              type="text"
              class="searchInput ma-0">
            <a
              :class="showSearchInput ? 'uiIconCloseServerAttachments' : 'uiIconFilter'"
              class="uiIconLightGray mr-1"
              @click="showSearchDocumentInput()"></a>
            <a
              v-if="(modeFolderSelectionForFile || modeFolderSelection) && currentDrive"
              :title="$t('attachments.filesFoldersSelector.button.addNewFOlder.tooltip')"
              rel="tooltip"
              data-placement="bottom"
              class="uiIconLightGray uiIconAddFolder"
              @click="addNewFolder()"></a>
          </div>
          <!-- Action buttons for extensionRegistry extensions -->
          <div
            v-for="action in attachmentsComposerActions"
            v-show="showDriveAction"
            :key="action.key"
            :class="`${action.appClass}Action`"
            class="actionBox ml-1 align-center">
            <div
              v-if="!modeFolderSelection"
              class="actionBoxLogo"
              @click="executeAction(action)">
              <v-icon v-if="action.iconName" class="uiActionIcon pa-2">
                {{ action.iconName }}
              </v-icon>
              <i
                v-else
                :class="action.iconClass"
                class="uiActionIcon"></i>
            </div>
          </div>
          <!-- end of action buttons block -->
        </div>

        <transition name="fade" mode="in-out">
          <div v-show="showErrorMessage" class="alert foldersFilesSelectorAlert alert-error mx-auto">
            <i class="uiIconError"></i>{{ errorMessage }}
          </div>
        </transition>
        <div class="contentBody">
          <div v-if="currentDrive" class="selectionBox px-5 d-flex flex-wrap">
            <div v-if="loadingFolders" class="VuetifyApp loader ma-auto">
              <v-app class="VuetifyApp">
                <v-progress-circular
                  :size="30"
                  :width="3"
                  indeterminate
                  class="loadingRing"
                  color="#578dc9" />
              </v-app>
            </div>
            <div v-if="emptyFolder" class="emptyFolder my-10 mx-auto mx-auto flex-column d-flex align-center">
              <i class="uiIconEmptyFolder"></i>
              <p>{{ $t('attachments.drawer.destination.folder.empty') }}</p>
            </div>
            <div
              v-for="folder in filteredFolders"
              :id="folder.id"
              :key="folder.id"
              :title="folder.name"
              :class="folder.type === 'new_folder' ? 'boxOfFolder d-flex flex-column' : ''"
              class="folderSelection ma-2"
              @click="openFolder(folder)"
              @contextmenu="openFolderActionsMenu(folder, $event)">
              <a
                v-if="folder.type === 'new_folder'"
                href="javascript:void(0);"
                class="closeIcon pt-1 pr-1 align-self-end"
                @mousedown="cancelCreatingNewFolder($event)">
                <span>x</span>
              </a>
              <div :class="folder.type === 'new_folder' ? 'boxOfTitle px-1' :''">
                <a
                  :title="folder.title"
                  href="javascript:void(0);"
                  rel="tooltip"
                  class="folderTitle d-flex flex-column v-messages"
                  data-placement="bottom">
                  <i
                    :class="folder.folderTypeCSSClass"
                    class="uiIcon24x24FolderDefault uiIconEcmsLightGray selectionIcon center"></i>
                  <i
                    v-show="folder.isCloudDrive"
                    :class="getFolderIcon(folder)"
                    class="uiIcon-clouddrive"></i>
                  <input
                    v-if="folder.type === 'new_folder'"
                    :ref="folder.ref"
                    v-model="newFolderName"
                    type="text"
                    class="newFolderInput  ignore-vuetify-classes"
                    @blur="createNewFolder($event)"
                    @keyup.enter="$event.target.blur()"
                    @keyup.esc="cancelCreatingNewFolder($event)">
                  <input
                    v-else-if="renameFolderAction && folder.id === selectedFolder.id"
                    :id="folder.id"
                    ref="rename"
                    v-model="newName"
                    type="text"
                    class="newFolderInput  ignore-vuetify-classes"
                    @blur="saveNewNameFolder()"
                    @keyup.enter="$event.target.blur()"
                    @keyup.esc="cancelRenameNewFolder($event)">
                  <div v-else class="selectionLabel text-truncate text-color center">{{ folder.title }}</div>
                </a>
              </div>
            </div>
            <attachments-folder-actions-menu
              ref="folderActionsMenu"
              :folder-actions-menu-left="folderActionsMenuLeft"
              :folder-actions-menu-top="folderActionsMenuTop"
              :selected-folder="selectedFolder"
              @renameFolder="renameFolder()"
              @deleteFolder="deleteFolder"
              @closeMenu="closeFolderActionsMenu" />
            <div v-if="emptyFolderForSelectDestination && modeFolderSelection && !emptyFolder" class="emptyFolder d-flex flex-column align-center mx-auto mt-10">
              <i class="uiIconEmptyFolder"></i>
              <p>{{ $t('attachments.drawer.destination.subfolder.empty') }}</p>
            </div>
            <div
              v-for="file in filteredFiles"
              v-show="!modeFolderSelection"
              :id="file.idAttribute"
              :key="file.id"
              :title="file.idAttribute"
              :class="file.isSelected? 'selected' : ''"
              class="fileSelection"
              @click="selectFile(file)">
              <attachments-drive-explorer-file-item :file="file" />
            </div>
          </div>
          <div v-else class="categorizedDrives">
            <v-list
              dense
              flat
              class="drivesList">
              <v-list-group
                value="true"
                class="categories"
                eager>
                <v-list-group
                  v-for="(group, name) in filteredDrivers"
                  :key="name"
                  :ripple="false"
                  sub-group
                  no-action
                  value="true"
                  class="category"
                  active-class="categoryActive">
                  <template #activator>
                    <v-list-item-content class="categoryContent">
                      {{ name }}
                    </v-list-item-content>
                  </template>
                  <!-- Drives block -->
                  <div class="selectionBox px-5 d-flex flex-wrap">
                    <div
                      v-for="driver in group.drives"
                      :key="driver.name"
                      :title="driver.title"
                      class="folderSelection ma-2 d-flex flex-column"
                      @click="openDrive(driver, name)">
                      <a
                        :data-original-title="driver.title"
                        rel="tooltip"
                        class="driveTitle d-flex flex-column v-messages"
                        data-placement="bottom">
                        <i
                          v-show="!drivesInProgress[driver.title]"
                          :class="driver.isCloudDrive ? driver.driveTypeCSSClass : `uiIconEcms24x24DriveGroup ${driver.driveTypeCSSClass}`"
                          class="uiIconEcmsLightGray drive-icon selectionIcon center"></i>
                        <div class="text-center connectingDrive">
                          <!-- show circular progress if cloud drive is connecting -->
                          <v-progress-circular
                            v-show="drivesInProgress[driver.title] >= 0 || drivesInProgress[driver.title] <= 100"
                            :indeterminate="false"
                            :rotate="0"
                            :size="40"
                            :value="drivesInProgress[driver.title]"
                            :width="4"
                            color="var(--allPagesPrimaryColor, #578dc9)"
                            class="connectingDriveProgress">{{ drivesInProgress[driver.title] }}<span class="connectingDriveProgressPercent">%</span>
                          </v-progress-circular>
                          <!-- end of progress block -->
                        </div>
                        <div
                          :class="{ 'connectingDriveTitle': drivesInProgress[driver.title] >= 0 || drivesInProgress[driver.title] <= 100}"
                          class="selectionLabel text-truncate text-color center">{{ driver.title }}
                        </div>
                      </a>
                    </div>
                  </div>
                  <!-- end of drives -->
                </v-list-group>
              </v-list-group>
            </v-list>
          </div>
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
        @ok="okConfirmDialog" />
      <!--end -->
    </template>
    <template slot="footer">
      <div class="d-flex">
        <span
          v-if="!modeFolderSelection"
          :class="filesCountClass"
          class="countLimit">
          {{ $t('attachments.drawer.maxFileCountLeft').replace('{0}', filesCountLeft) }}
        </span>
        <v-spacer />
        <v-btn
          class="btn mr-3"
          @click="closeAttachmentsDriveExplorerDrawer()">
          {{ $t('attachments.drawer.cancel') }}
        </v-btn>
        <v-btn
          class="btn btn-primary"
          @click="selectActionDriveExplorerDrawer()">
          {{ $t('attachments.drawer.select') }}
        </v-btn>
      </div>
    </template>
  </exo-drawer>
</template>

<script>
import {getAttachmentsComposerExtensions, executeExtensionAction} from '../../../../js/extension';

export default {
  props: {
    modeFolderSelectionForFile: {
      type: Boolean,
      default: false
    },
    isCloudEnabled: {
      type: Boolean,
      default: false,
    },
    spaceId: {
      type: String,
      default: ''
    },
    attachedFiles: {
      type: Array,
      default: () => []
    },
    extensionRefs: { // references to extension dynamic components
      type: Array,
      default: () => []
    },
    connectedDrive: { // cloud drive that is in connecting progress recieved from parent
      type: Object,
      default: () => ({})
    },
    cloudDrivesInProgress: { // all cloud drives that is in progress with their progress values recieved from parent
      type: Object,
      default: () => ({})
    },
    entityId: {
      type: String,
      default: ''
    },
    entityType: {
      type: String,
      default: ''
    },
    defaultDrive: {
      type: Object,
      default: () => null
    },
    defaultFolder: {
      type: String,
      default: ''
    },
  },
  data() {
    return {
      workspace: 'collaboration',
      driveRootPath: '',
      drivers: [],
      folders: [],
      files: [],
      space: {},
      currentDrive: {},
      selectedFiles: [],
      maxFilesCount: 20,
      foldersHistory: [],
      showSearchInput: false,
      searchFilesFolders: '',
      loadingFolders: true,
      filesCountClass: '',
      selectedFolderPath: '',
      schemaFolder: '',
      folderDestinationForFile: '',
      attachmentsComposerActions: [], // extensions from extensionRegistry
      creatingNewFolder: false,
      newFolderName: '',
      currentAbsolutePath: '',
      popupBodyMessage: '',
      folderActionsMenuTop: '0px',
      folderActionsMenuLeft: '0px',
      selectedFolder: {},
      windowPositionLimit: 25,
      MESSAGE_TIMEOUT: 5000,
      showErrorMessage: false,
      errorMessage: '',
      renameFolderAction: false,
      newName: '',
      MESSAGES_DISPLAY_TIME: 5000,
      privateDestinationForFile: false,
      fromSpace: {},
      okLabel: '',
      cancelLabel: '',
      titleLabel: '',
      okAction: false,
      modeFolderSelection: true,
    };
  },
  computed: {
    showDriveAction() { // show drivers extension buttons only if it's root path
      return this.isCloudEnabled && (this.currentDrive ? this.currentDrive.name === 'Personal Documents' : true);
    },
    filteredFolders() {
      let folders = this.folders.slice();
      if (this.searchFilesFolders && this.searchFilesFolders.trim().length) {
        const searchTerm = this.searchFilesFolders.trim().toLowerCase();
        folders = this.folders.filter(folder => folder.name.toLowerCase().indexOf(searchTerm) >= 0);
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
        files = this.files.filter(file => file.name.toLowerCase().indexOf(searchTerm) >= 0);
      }
      files.forEach(file => {
        file.isSelected = this.attachedFiles.some(f => f.id === file.id);
      });
      return files;
    },
    filteredDrivers() {
      let drivers = this.drivers.slice();
      if (this.searchFilesFolders && this.searchFilesFolders.trim().length) {
        const searchTerm = this.searchFilesFolders.trim().toLowerCase();
        drivers = this.drivers.filter(driver => driver.title.toLowerCase().indexOf(searchTerm) >= 0);
      }
      const drivesByTypes = {
        'My Drives': {drives: []},
        'My Spaces': {drives: []},
        'Others': {drives: []}
      };
      // map through founded drives and fill in drivesByTypes
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
    filesCountLeft() {
      return this.maxFilesCount - this.selectedFiles.length;
    },
    emptyFolder() {
      return this.files.length === 0 && this.folders.length === 0 && this.drivers.length === 0 && !this.loadingFolders;
    },
    emptyFolderForSelectDestination() {
      return this.folders.length === 0 && this.drivers.length === 0 && !this.loadingFolders;
    },
    connectedMessage() { // returns name of the drive which finished connecting
      let connectedDrive;
      for (const key in this.drivesInProgress) {
        const fullProgress = 100;
        connectedDrive = this.drivesInProgress[key] >= fullProgress ? key : '';
      }
      return connectedDrive;
    },
    drivesInProgress() { // returns the copy of cloudDrivesInProgress.drives
      return {...this.cloudDrivesInProgress.drives};
    },
    driveExplorerDrawerTitle() {
      return this.modeFolderSelection ? this.$t('attachments.drawer.destination.folder') : this.$t('attachments.drawer.existingUploads');
    }
  },
  watch: {
    filesCountLeft() {
      this.filesCountClass = this.filesCountLeft === 0 ? 'noFilesLeft' : '';
    },
    showErrorMessage: function (newVal) {
      if (newVal) {
        setTimeout(() => this.showErrorMessage = false, this.MESSAGE_TIMEOUT);
      }
    },
    connectedDrive: function (drive) {
      if (!this.drivers.some(({title}) => title === drive.title)) {
        this.drivers.push(drive); // display connecting drive in 'My Drives' section
      }
    },
    entityId() {
      this.initDestinationFolderPath();
    },
    entityType() {
      this.initDestinationFolderPath();
    },
    spaceId() {
      this.initDestinationFolderPath();
    },
    defaultDrive() {
      this.initDestinationFolderPath();
    },
    defaultFolder() {
      this.initDestinationFolderPath();
    },
    attachedFiles() {
      this.selectedFiles = this.attachedFiles.slice();
    },
  },
  created() {
    this.selectedFiles = this.attachedFiles.slice();
    this.initDestinationFolderPath();
    document.addEventListener('extension-AttachmentsComposer-attachments-composer-action-updated', () => this.attachmentsComposerActions = getAttachmentsComposerExtensions());
    this.attachmentsComposerActions = getAttachmentsComposerExtensions();
    this.$root.$on('open-drive-explorer-drawer', () => this.openAttachmentsDriveExplorerDrawer());
    this.$root.$on('open-select-from-drives-drawer', () => this.openSelectFromDrivesDrawer());
  },
  methods: {
    openAttachmentsDriveExplorerDrawer() {
      this.modeFolderSelection = true;
      this.$refs.driveExplorerDrawer.open();
    },
    closeAttachmentsDriveExplorerDrawer() {
      this.$refs.driveExplorerDrawer.close();
    },
    initDestinationFolderPath: function () {
      //if default drive exist
      if (this.defaultDrive && this.defaultDrive.name) {
        const self = this;
        //open it to generate the path
        this.openDrive(this.defaultDrive).then(() => {
          const defaultFolder = self.folders.find(folder => folder.title === self.defaultFolder);
          if (self.entityType && self.entityId) {
            if (defaultFolder) {
              this.openFolder(defaultFolder).then(() => {
                this.createEntityTypeAndIdFolders(defaultFolder);
              });
            } else {
              this.createEntityTypeAndIdFolders(defaultFolder);
            }
          }
          //if both default drive and default folder exist
          if (defaultFolder) {
            this.openFolder(defaultFolder).then(() => {
              this.$root.$emit('attachments-default-folder-path-initialized', this.getRelativePath(self.selectedFolderPath), this.schemaFolder);
            });
            //else if no default folder
          } else {
            this.$root.$emit('attachments-default-folder-path-initialized', '', this.currentDrive.title);
          }
        });
      } else {
        this.currentDrive = null;
        this.fetchUserDrives();
      }
    },
    openFolder: function (folder) {
      if (this.selectedFolder.id && this.selectedFolder.canRemove) {
        this.$refs.rename[0].focus();
      } else if (folder.type === 'new_folder') {
        this.$refs.newFolder[0].focus();
      } else {
        this.currentAbsolutePath = folder.path;
        this.generateHistoryTree(folder);
        this.resetExplorer();
        folder.isSelected = true;
        this.selectedFolderPath = this.driveRootPath.concat(folder.path);
        this.schemaFolder = this.currentDrive.title.concat('/', folder.path);
        this.folderDestinationForFile = folder.name;
        return this.fetchChildrenContents(folder.path);
      }
      this.schemaFolder = this.currentDrive.title.concat('/', folder.path);
      this.folderDestinationForFile = folder.title;
      this.privateDestinationForFile = folder.isPublic;
    },
    openDrive(drive, group) {
      this.currentAbsolutePath = '';
      this.selectedFolderPath = '';
      this.folderDestinationForFile = '';
      this.foldersHistory = [];
      this.resetExplorer();
      this.fromSpace = group === 'My Spaces' ? {title: drive.title, name: drive.name} : {};
      const driveTitle = drive.title.replace('.', '').replace(' ', '');
      drive.title = drive.name.includes('space') ? drive.title : this.$t(`Drives.label.${driveTitle}`);
      this.currentDrive = {
        name: drive.name,
        title: drive.title,
        isSelected: true
      };
      return this.fetchChildrenContents('');
    },
    fetchChildrenContents: function (parentPath) {
      this.loadingFolders = true;
      const self = this;
      return this.$attachmentService.fetchFoldersAndFiles(this.currentDrive.name, this.workspace, parentPath).then(xml => {
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
        this.errorMessage = `${this.$t('attachments.fetchFoldersAndFiles.error')}. ${error.message ? error.message : ''}`;
        this.showErrorMessage = true;
      });
    },
    fetchUserDrives() {
      this.resetExplorer();
      this.loadingFolders = true;
      this.currentDrive = null;
      this.foldersHistory = [];
      const self = this;
      this.$attachmentService.getDrivers().then(xml => {
        const drivers = xml.childNodes[0].childNodes;
        self.setDrivers(drivers);
        this.loadingFolders = false;
      }).catch(() => {
        this.loadingFolders = false;
        this.errorMessage = `${this.$t('attachments.getDrivers.error')}`;
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
      if (!file.isSelected && this.filesCountLeft > 0) {
        file.isSelected = true;
        file.isSelectedFromDrives = true;
        if (!this.selectedFiles.find(f => f.id === file.id)) {
          this.selectedFiles.push({...file, space: this.fromSpace});
        }
      } else {
        const index = this.selectedFiles.findIndex(f => f.id === file.id);
        file.isSelected = false;
        file.isSelectedFromDrives = false;
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
      this.$root.$emit('attachments-changed-from-drives', this.selectedFiles);
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
            const isSelected = this.attachedFiles.some(f => f.id === id);
            this.files.push({
              id: id,
              name: fetchedFiles[j].getAttribute('name'),
              title: fetchedFiles[j].getAttribute('title'),
              path: this.getRelativePath(fetchedFiles[j].getAttribute('path')),
              size: fetchedFiles[j].getAttribute('size'),
              idAttribute: idAttribute,
              isSelected: isSelected,
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
    selectActionDriveExplorerDrawer() {
      if (this.modeFolderSelection) {
        if (!this.selectedFolderPath) {
          this.selectedFolderPath = this.driveRootPath;
          this.schemaFolder = this.currentDrive.title;
          this.folderDestinationForFile = this.currentDrive.title;
        }
        if (this.modeFolderSelectionForFile) {
          this.$root.$emit('select-destination-path-for-file', this.getRelativePath(this.selectedFolderPath), this.folderDestinationForFile, this.privateDestinationForFile, this.currentDrive);
        } else {
          this.$root.$emit('select-destination-path-for-all', this.getRelativePath(this.selectedFolderPath), this.schemaFolder, this.currentDrive);
        }
      } else {
        this.addSelectedFiles();
      }

      this.closeAttachmentsDriveExplorerDrawer();
    },
    executeAction(action) { // will execute code inside 'onExecute' extension method
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
            this.okLabel = this.$t('attachments.ok');
            this.popupBodyMessage = `${this.$t('attachments.filesFoldersSelector.popup.folderNameExists')}`;
          } else {
            const self = this;
            return this.$attachmentService.createFolder(this.currentDrive.name, this.workspace, this.currentAbsolutePath, this.newFolderName).then(xml => {
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
                return newFolder;
              } else {
                self.creatingNewFolder = false;
                self.newFolderName = '';
              }
            }).catch(() => {
              this.errorMessage = `${this.$t('attachments.createFolder.error')}`;
              this.showErrorMessage = true;
            });
          }
        } else {
          this.$refs.confirmDialog.open();
          this.titleLabel = this.$t('attachments.filesFoldersSelector.popup.title');
          this.okLabel = this.$t('attachments.ok');
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
      event.preventDefault();
      this.selectedFolder = folder;
      this.folderActionsMenuTop = event.clientY;
      this.folderActionsMenuLeft = event.clientX;
      this.$nextTick(() => {
        this.$refs.folderActionsMenu.openMenu();
      });
    },
    closeFolderActionsMenu: function () {
      this.$refs.folderActionsMenu.closeMenu();
    },
    deleteFolder() {
      if (this.selectedFolder.canRemove) {
        this.closeFolderActionsMenu();
        this.$refs.confirmDialog.open();
        this.titleLabel = this.$t('attachments.filesFoldersSelector.action.delete.popup.title');
        this.okLabel = this.$t('attachments.filesFoldersSelector.action.delete.popup.button.ok');
        this.cancelLabel = this.$t('attachments.cancel');
        this.okAction = true;
        this.popupBodyMessage = `${this.$t('attachments.filesFoldersSelector.action.delete.popup.bodyMessage')}`;
      }
    },
    okConfirmDialog() {
      if (this.okAction) {
        this.$attachmentService.deleteFolderOrFile(this.currentDrive.name, this.workspace, this.selectedFolder.path).then(() => {
          this.reloadCurrentPath();
        }).catch(() => {
          this.errorMessage = `${this.$t('attachments.deleteFolderOrFile.error')}`;
          this.showErrorMessage = true;
        });
      } else {
        return;
      }
    },
    reloadCurrentPath() {
      this.resetExplorer();
      if (this.currentAbsolutePath) {
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
          this.$refs.folderActionsMenu.closeMenu();
          this.$nextTick(() => {
            this.$refs.rename[0].focus();
          });
        }
      }
    },
    saveNewNameFolder() {
      if (this.newName !== this.selectedFolder.title && this.newName !== '') {
        const folderNameExists = this.folders.some(folder => folder.title === this.newName);
        if (folderNameExists) {
          this.$refs.confirmDialog.open();
          this.titleLabel = this.$t('attachments.filesFoldersSelector.popup.title');
          this.okLabel = this.$t('attachments.ok');
          this.popupBodyMessage = `${this.$t('attachments.renameFolder.error')}`;
          this.cancelRenameNewFolder();
        } else {
          this.selectedFolderPath = this.driveRootPath.concat(this.selectedFolder.path);
          const path = encodeURIComponent(this.workspace.concat(':', this.selectedFolderPath));
          this.$attachmentService.renameFolder(path, this.newName).then(response => {
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
      } else {
        this.selectedFolder = {};
      }
      this.selectedFolderPath = '';
    },
    getFolderIcon(folder) {
      return `uiIcon-${folder.cloudProvider}`;
    },
    createEntityTypeAndIdFolders(defaultFolder) {
      defaultFolder = this.folders.find(folder => folder.title === this.entityType);
      //if entityType (tasks, event, ..) folder not found
      if (!defaultFolder) {
        this.newFolderName = this.entityType;
        this.creatingNewFolder = true;
        this.createNewFolder().then((newFolder) => {
          this.openFolder(newFolder).then(() => {
            this.newFolderName = this.entityId;
            this.creatingNewFolder = true;
            this.createNewFolder().then((newFolder) => {
              this.openFolder(newFolder).then(() => {
                this.$root.$emit('attachments-default-folder-path-initialized', this.getRelativePath(this.selectedFolderPath), this.schemaFolder);
              });
            });
          });
        }).finally(() => {
          this.creatingNewFolder = false;
        });
      } else { //if entityType (tasks, event, ..) folder exist, we create directly entityId folder
        this.openFolder(defaultFolder).then(() => {
          defaultFolder = this.folders.find(folder => parseInt(folder.title) === this.entityId);
          if (!defaultFolder) {
            this.newFolderName = this.entityId;
            this.creatingNewFolder = true;
            this.createNewFolder().then((newFolder) => {
              this.openFolder(newFolder).then(() => {
                this.$root.$emit('attachments-default-folder-path-initialized', this.getRelativePath(this.selectedFolderPath), this.schemaFolder);
              });
            });
          } else {
            this.openFolder(defaultFolder).then(() => {
              this.$root.$emit('attachments-default-folder-path-initialized', this.getRelativePath(this.selectedFolderPath), this.schemaFolder);
            });
          }
        });
      }
    },
    openSelectFromDrivesDrawer() {
      this.modeFolderSelection = false;
      this.$refs.driveExplorerDrawer.open();
    }
  }
};
</script>