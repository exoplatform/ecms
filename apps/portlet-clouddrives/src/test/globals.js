global.eXo = {
  env: {
    portal: {
      context: "portal",
      rest: "rest",
      language: "en"
    },
  },
};

global.$ = {
  pnotify: () => {
    // mocked pnotify function
  }
};

global.cloudDriveUtils = {
  pageBaseUrl: () => "mockedBaseUrl",
  log: () => {
    // mocked utils log function
  }
};

global.cloudDrives = {
  init: () => {
    // mocked cloudDrives module initialization
  },
  getProviders: () => [{ id: "gDrive", name: "Google Drive" }, { id: "oneDrive", name: "One Drive" }],
  connect: () => Promise.resolve()
};
