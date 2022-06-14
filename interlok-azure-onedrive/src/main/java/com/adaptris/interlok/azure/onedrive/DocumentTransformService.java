package com.adaptris.interlok.azure.onedrive;

import java.util.LinkedList;

import javax.validation.constraints.NotNull;

import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.annotation.InputFieldHint;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.ServiceException;
import com.adaptris.util.KeyValuePair;
import com.adaptris.util.KeyValuePairList;
import com.microsoft.graph.options.QueryOption;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import lombok.Getter;
import lombok.Setter;

/**
 * Retrieve the contents of an item in a specific format. Not all files can be converted into all formats.
 *
 * Supported formats and their original document type:
 *
 * glb Converts the item into GLB format from: cool, fbx, obj, ply, stl, 3mf html Converts the item into HTML format from: eml, md, msg jpg
 * Converts the item into JPG format from: 3g2, 3gp, 3gp2, 3gpp, 3mf, ai, arw, asf, avi, bas, bash, bat, bmp, c, cbl, cmd, cool, cpp, cr2,
 * crw, cs, css, csv, cur, dcm, dcm30, dic, dicm, dicom, dng, doc, docx, dwg, eml, epi, eps, epsf, epsi, epub, erf, fbx, fppx, gif, glb, h,
 * hcp, heic, heif, htm, html, ico, icon, java, jfif, jpeg, jpg, js, json, key, log, m2ts, m4a, m4v, markdown, md, mef, mov, movie, mp3,
 * mp4, mp4v, mrw, msg, mts, nef, nrw, numbers, obj, odp, odt, ogg, orf, pages, pano, pdf, pef, php, pict, pl, ply, png, pot, potm, potx,
 * pps, ppsx, ppsxm, ppt, pptm, pptx, ps, ps1, psb, psd, py, raw, rb, rtf, rw1, rw2, sh, sketch, sql, sr2, stl, tif, tiff, ts, txt, vb,
 * webm, wma, wmv, xaml, xbm, xcf, xd, xml, xpm, yaml, yml pdf Converts the item into PDF format from: doc, docx, epub, eml, htm, html, md,
 * msg, odp, ods, odt, pps, ppsx, ppt, pptx, rtf, tif, tiff, xls, xlsm, xlsx
 *
 * @config azure-one-drive-document-transform-service
 */
@XStreamAlias("azure-one-drive-document-transform-service")
@AdapterComponent
@ComponentProfile(summary = "Retrieve the contents of an item in a specific format. Not all files can be converted into all formats.", tag = "file,o365,microsoft,office,365,one drive,transform")
@DisplayOrder(order = { "username", "filename" })
public class DocumentTransformService extends DocumentDownloadService {
  /**
   * The format of the file to be downloaded.
   */
  @Getter
  @Setter
  @NotNull
  @InputFieldHint(friendly = "The desired format of the file to be downloaded")
  private Format format;

  @Getter
  @Setter
  @AdvancedConfig
  @InputFieldHint(friendly = "Additional request options; for instance converting to JPG requires values for width & height")
  private KeyValuePairList additionalRequestOptions;

  /**
   * Retrieve the contents of an item in a specific format.
   *
   * @param adaptrisMessage
   *          the <code>AdaptrisMessage</code> to process
   * @throws ServiceException
   *           wrapping any underlying <code>Exception</code>s
   */
  @Override
  public void doService(AdaptrisMessage adaptrisMessage) throws ServiceException {
    requestOptions = new LinkedList<>();
    requestOptions.add(new QueryOption("format", format));

    if (additionalRequestOptions != null) {
      for (KeyValuePair option : additionalRequestOptions) {
        requestOptions.add(new QueryOption(option.getKey(), option.getValue()));
      }
    }

    super.doService(adaptrisMessage);
  }

  public enum Format {
    GLB, HTML, JPG, PDF;
  }
}
