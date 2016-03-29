package org.exoplatform.services.pdfviewer;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cms.jodconverter.JodConverterService;
import org.exoplatform.services.jcr.RepositoryService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class TestPDFViewerService {

  @Mock
  private RepositoryService repositoryService;

  @Mock
  private CacheService cacheService;

  @Mock
  private JodConverterService jodConverterService;

  @Test
  public void shouldGetDefaultValuesWhenNoInitParams() throws Exception {
    InitParams initParams = new InitParams();

    PDFViewerService pdfViewerService = new PDFViewerService(repositoryService, cacheService, jodConverterService, initParams);

    assertEquals(PDFViewerService.DEFAULT_MAX_FILE_SIZE, pdfViewerService.getMaxFileSize());
    assertEquals(PDFViewerService.DEFAULT_MAX_PAGES, pdfViewerService.getMaxPages());
  }

  @Test
  public void shouldGetParamValuesWhenValuesAreValidNumbers() throws Exception {
    InitParams initParams = new InitParams();
    ValueParam maxFileSizeValueParam = new ValueParam();
    maxFileSizeValueParam.setName(PDFViewerService.MAX_FILE_SIZE_PARAM_NAME);
    maxFileSizeValueParam.setValue("5");
    initParams.addParam(maxFileSizeValueParam);
    ValueParam maxPagesValueParam = new ValueParam();
    maxPagesValueParam.setName(PDFViewerService.MAX_PAGES_PARAM_NAME);
    maxPagesValueParam.setValue("10");
    initParams.addParam(maxPagesValueParam);

    PDFViewerService pdfViewerService = new PDFViewerService(repositoryService, cacheService, jodConverterService, initParams);

    assertEquals(5 * 1024 * 1024, pdfViewerService.getMaxFileSize());
    assertEquals(10, pdfViewerService.getMaxPages());
  }

  @Test
  public void shouldGetParamValuesWhenValuesAreNotValidNumbers() throws Exception {
    InitParams initParams = new InitParams();
    ValueParam maxFileSizeValueParam = new ValueParam();
    maxFileSizeValueParam.setName(PDFViewerService.MAX_FILE_SIZE_PARAM_NAME);
    maxFileSizeValueParam.setValue("abc");
    initParams.addParam(maxFileSizeValueParam);
    ValueParam maxPagesValueParam = new ValueParam();
    maxPagesValueParam.setName(PDFViewerService.MAX_PAGES_PARAM_NAME);
    maxPagesValueParam.setValue("0.5");
    initParams.addParam(maxPagesValueParam);

    PDFViewerService pdfViewerService = new PDFViewerService(repositoryService, cacheService, jodConverterService, initParams);

    assertEquals(PDFViewerService.DEFAULT_MAX_FILE_SIZE, pdfViewerService.getMaxFileSize());
    assertEquals(PDFViewerService.DEFAULT_MAX_PAGES, pdfViewerService.getMaxPages());
  }
}