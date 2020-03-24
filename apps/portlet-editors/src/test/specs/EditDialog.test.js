import { shallowMount } from "@vue/test-utils";
import EditDialog from "../../main/webapp/vue-apps/editorsAdmin/components/EditDialog.vue";
import Vuetify from "vuetify";
import Vue from "vue";

describe("EditDialog.test.js", () => {
  let cmp;
  const data = {
    provider: {
      permissions: [
        {
          id: "/platform",
          displayName: "Platform",
          avatarUrl: "/eXoSkin/skin/images/system/SpaceAvtDefault.png"
        }
      ],
      provider: "onlyoffice",
      active: true,
      links: [
        {
          rel: "self",
          href: "/rest/documents/editors/onlyoffice"
        },
        {
          rel: "update",
          href: "/rest/documents/editors/onlyoffice"
        }
      ]
    }
  };

  beforeEach(() => {
    Vue.use(Vuetify);

    cmp = shallowMount(EditDialog, {
      mocks: {
        $t: () => {
          // mocked translate function
        }
      },
      propsData: {
        provider: data.provider,
        searchUrl: "portal/rest/identity/search"
      }
    });
  });

  afterEach(() => cmp.destroy());

  it("should be a Vue instance", () => {
    expect(cmp.isVueInstance).toBeTruthy();
  });

  it("should change component edited permissions after selection changes", () => {
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
    expect(cmp.vm.editedPermissions).toEqual([
      {
        id: "/platform",
        displayName: "Platform",
        avatarUrl: "/eXoSkin/skin/images/system/SpaceAvtDefault.png"
      },
      {
        type: "group",
        id: "/platform/web-contributors",
        displayName: "Content Management",
        avatarUrl: "/eXoSkin/skin/images/system/SpaceAvtDefault.png"
      }
    ]);
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

  it("should change permissions to everybody if checkbox checked and clear permissiond after uncheck", () => {
    expect(cmp.contains("[data-test='everybodyCheckbox']")).toBeTruthy();
    const checkbox = cmp.find("[data-test='everybodyCheckbox']");
    checkbox.vm.$emit("change", true);
    expect(cmp.vm.existingPermissions).toEqual([{ id: "*", displayName: null, avatarUrl: null }]);
    checkbox.vm.$emit("change", false);
    expect(cmp.vm.existingPermissions).toEqual([]);
  });

  it("should set checked to everybody-checkbox if existing permissions available to anybody and set unchecked if no", async () => {
    expect(cmp.vm.accessibleToAll).toBeFalsy();
    cmp.setProps({ provider: { ...data.provider, permissions: [{ id: "*", displayName: null, avatarUrl: null }] } });
    await Vue.nextTick();
    expect(cmp.vm.accessibleToAll).toBeTruthy();
  });

  it("should reset edited permissions to initial permissions even after selection changes after cancel button has been clicked", () => {
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
    expect(cmp.vm.editedPermissions).toHaveLength(2);
    cancelButton.trigger("click");
    expect(closeDialog).toHaveBeenCalled();
    expect(cmp.vm.editedPermissions).toHaveLength(1);
  });

  it("should send existing permissiond and selected permissions to provider link after button save has been clicked", () => {
    const saveButton = cmp.find("[data-test='saveButton']");
    const saveChanges = jest.spyOn(cmp.vm, "saveChanges");
    const mockJsonPromise = Promise.resolve({});
    const mockFetchPromise = Promise.resolve({
      json: () => mockJsonPromise
    });
    global.fetch = jest.fn().mockImplementation(() => mockFetchPromise);
    expect(cmp.vm.existingPermissions).toEqual(data.provider.permissions);
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
    expect(global.fetch).toHaveBeenCalled();
    expect(global.fetch).toHaveBeenCalledWith("/rest/documents/editors/onlyoffice", {
      body: JSON.stringify({ permissions: [
        { id: "/platform" }, { id: "/platform/web-contributors"}
      ]}),
      headers: { "Content-Type": "application/json" },
      method: "POST"
    });
    global.fetch.mockClear();
  });
});
