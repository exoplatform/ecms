package org.exoplatform.social.addons.rdbms.listener;

import java.util.HashMap;
import java.util.Map;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PortalContainerInfo;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.upload.UploadService;

public class MockUploadService extends UploadService {
  Map<String, UploadResource> uploadResources = new HashMap<>();

  public MockUploadService(PortalContainerInfo pinfo, InitParams params) throws Exception {
    super(pinfo, params);
  }

  public void createUploadResource(String uploadId, String filePath, String fileName, String mimeType) throws Exception {
    UploadResource uploadResource = new UploadResource(uploadId, fileName);
    uploadResource.setMimeType(mimeType);
    uploadResource.setStatus(UploadResource.UPLOADED_STATUS);
    uploadResource.setStoreLocation(filePath);
    uploadResources.put(uploadId, uploadResource);
  }

  @Override
  public UploadResource getUploadResource(String uploadId) {
    return uploadResources.get(uploadId);
  }

  @Override
  public void removeUploadResource(String uploadId) {
    uploadResources.remove(uploadId);
  }

  public void removeUpload(String uploadId) {
    uploadResources.remove(uploadId);
  }

  public Map<String, UploadResource> getUploadResources() {
    return uploadResources;
  }
}
