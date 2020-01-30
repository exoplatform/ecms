package org.exoplatform.ecms.mock;

import org.exoplatform.commons.file.model.FileInfo;
import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.commons.file.services.FileStorageException;

import java.io.IOException;

public class MockFileService implements FileService {
  @Override
  public FileInfo getFileInfo(long l) {
    return null;
  }

  @Override
  public FileItem getFile(long l) throws FileStorageException {
    return null;
  }

  @Override
  public FileItem writeFile(FileItem fileItem) throws FileStorageException, IOException {
    return null;
  }

  @Override
  public FileItem updateFile(FileItem fileItem) throws FileStorageException, IOException {
    return null;
  }

  @Override
  public FileInfo deleteFile(long l) {
    return null;
  }
}
