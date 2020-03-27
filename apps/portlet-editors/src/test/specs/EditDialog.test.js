import { shallowMount } from "@vue/test-utils";
import EditDialog from "../../main/webapp/vue-apps/editorsAdmin/components/EditDialog.vue";
import Vuetify from "vuetify";
import Vue from "vue";

describe("EditDialog.test.js", () => {
  let cmp;

  beforeEach(() => {
    Vue.use(Vuetify);

    cmp = shallowMount(EditDialog, {
      mocks: {
        $t: () => {
          // mocked translate function
        }
      },
      propsData: {
        providerLink: "/rest/documents/editors/onlyoffice",
        providerName: "onlyoffice",
        searchUrl: "portal/rest/identity/search",
        i18n: { te: () => true }
      }
    });
  });

  afterEach(() => cmp.destroy());

  it("should be a Vue instance", () => {
    expect(cmp.isVueInstance).toBeTruthy();
  });

  it("should clear search field and uncheck everybody checkbox after at least one item was selected", () => {
    cmp.vm.search = "val";
    cmp.vm.accessibleToAll = true;
    expect(cmp.contains(".searchPermissions")).toBe(true);
    const autoComplete = cmp.find(".searchPermissions");
    autoComplete.vm.$emit("input", [
      {
        type: "group",
        id: "/platform/web-contributors",
        displayName: "Content Management",
        avatarUrl: "/eXoSkin/skin/images/system/SpaceAvtDefault.png"
      }
    ]);
    expect(autoComplete.emitted().input).toBeTruthy();
    expect(cmp.vm.search.length).toBe(0);
    expect(cmp.vm.accessibleToAll).toBeFalsy();
  });

  it("should fetch searched permissions by value in search property", async () => {
    expect(cmp.contains(".searchPermissions")).toBeTruthy();
    const mockJsonPromise = Promise.resolve({
      identities: [
        {
          type: "group",
          id: "/platform/web-contributors",
          displayName: "Content Management",
          avatarUrl: "/eXoSkin/skin/images/system/SpaceAvtDefault.png"
        },
        {
          type: "group",
          id: "/organization",
          displayName: "Organization",
          avatarUrl: "/eXoSkin/skin/images/system/SpaceAvtDefault.png"
        }
      ]
    });
    const mockFetchPromise = Promise.resolve({
      json: () => mockJsonPromise
    });
    global.fetch = jest.fn().mockImplementation(() => mockFetchPromise);

    const querySelections = jest.spyOn(cmp.vm, "querySelections");
    cmp.vm.search = "pla";
    await Vue.nextTick();
    expect(querySelections).toHaveBeenCalledWith("pla");
    expect(global.fetch).toHaveBeenCalledTimes(1);
    expect(global.fetch).toHaveBeenCalledWith("portal/rest/identity/search/pla", {
      headers: { "Content-Type": "application/json" },
      method: "GET"
    });
    global.fetch.mockClear();
  });

  it("should set checked to everybody-checkbox if existing permissions available to anybody", done => {
    const mockJsonPromise = Promise.resolve({
      permissions: [
        {
          id: "*",
          displayName: null,
          avatarUrl: null
        }
      ],
      provider: "onlyoffice",
      active: true
    });
    const mockFetchPromise = Promise.resolve({
      ok: true,
      json: () => mockJsonPromise
    });
    global.fetch = jest.fn().mockResolvedValue(mockFetchPromise);

    expect(cmp.vm.accessibleToAll).toBeFalsy();
    cmp.vm.showDialog = true;
    process.nextTick(() => {
      expect(global.fetch).toHaveBeenCalledTimes(1);
      expect(cmp.vm.accessibleToAll).toBeTruthy();
      global.fetch.mockClear();
      done();
    });
  });

  it("should set selection to null after cancel button has been clicked", () => {
    const cancelButton = cmp.find("[data-test='cancelButton']");
    const closeDialog = jest.spyOn(cmp.vm, "closeDialog");
    const autoComplete = cmp.find(".searchPermissions");
    autoComplete.vm.$emit("input", [
      {
        type: "group",
        id: "/platform/web-contributors",
        displayName: "Content Management",
        avatarUrl: "/eXoSkin/skin/images/system/SpaceAvtDefault.png"
      }
    ]);
    expect(cmp.vm.selectedItems).toHaveLength(1);
    cancelButton.trigger("click");
    expect(closeDialog).toHaveBeenCalled();
    expect(cmp.vm.selectedItems).toBeNull();
  });

  it(`should merge existing permissions with selected and deleted and then send them to provider link 
  after button save has been clicked`, done => {
    const saveButton = cmp.find("[data-test='saveButton']");
    const saveChanges = jest.spyOn(cmp.vm, "saveChanges");
    const mockJsonPromise = Promise.resolve({});
    const mockFetchPromise = Promise.resolve({
      ok: true,
      json: () => mockJsonPromise
    });
    global.fetch = jest.fn().mockResolvedValue(mockFetchPromise);
    cmp.vm.existingPermissions = [
      {
        id: "/platform",
        displayName: "Platform",
        avatarUrl: "/eXoSkin/skin/images/system/SpaceAvtDefault.png"
      },
      {
        type: "group",
        id: "/organization",
        displayName: "Organization",
        avatarUrl: "/eXoSkin/skin/images/system/SpaceAvtDefault.png"
      }
    ];
    cmp.vm.permissionChanges = [
      {
        type: "group",
        id: "/organization",
        displayName: "Organization",
        avatarUrl: "/eXoSkin/skin/images/system/SpaceAvtDefault.png"
      }
    ];
    const autoComplete = cmp.find(".searchPermissions");
    autoComplete.vm.$emit("input", [
      {
        type: "group",
        id: "/platform/web-contributors",
        displayName: "Content Management",
        avatarUrl: "/eXoSkin/skin/images/system/SpaceAvtDefault.png"
      }
    ]);
    saveButton.trigger("click");
    expect(saveChanges).toHaveBeenCalled();
    process.nextTick(() => {
      expect(global.fetch).toHaveBeenCalled();
      expect(global.fetch).toHaveBeenCalledWith("/rest/documents/editors/onlyoffice", {
        body: JSON.stringify({ permissions: [{ id: "/platform" }, { id: "/platform/web-contributors" }] }),
        headers: { "Content-Type": "application/json" },
        method: "POST"
      });
      global.fetch.mockClear();
      done();
    });
  });

  it("should send everybody permission if everybody checkbox is checked after button save has been clicked", done => {
    const saveButton = cmp.find("[data-test='saveButton']");
    const saveChanges = jest.spyOn(cmp.vm, "saveChanges");
    const mockJsonPromise = Promise.resolve({});
    const mockFetchPromise = Promise.resolve({
      ok: true,
      json: () => mockJsonPromise
    });
    global.fetch = jest.fn().mockResolvedValue(mockFetchPromise);
    cmp.vm.existingPermissions = [
      {
        id: "/platform",
        displayName: "Platform",
        avatarUrl: "/eXoSkin/skin/images/system/SpaceAvtDefault.png"
      },
      {
        type: "group",
        id: "/organization",
        displayName: "Organization",
        avatarUrl: "/eXoSkin/skin/images/system/SpaceAvtDefault.png"
      }
    ];
    expect(cmp.contains("[data-test='everybodyCheckbox']")).toBeTruthy();
    const checkbox = cmp.find("[data-test='everybodyCheckbox']");
    checkbox.vm.$emit("change", true);
    saveButton.trigger("click");
    expect(saveChanges).toHaveBeenCalled();
    process.nextTick(() => {
      expect(global.fetch).toHaveBeenCalled();
      expect(global.fetch).toHaveBeenCalledWith("/rest/documents/editors/onlyoffice", {
        body: JSON.stringify({ permissions: [{ id: "*" }] }),
        headers: { "Content-Type": "application/json" },
        method: "POST"
      });
      global.fetch.mockClear();
      done();
    });
  });
});
