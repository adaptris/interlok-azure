package com.adaptris.interlok.azure.mail;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EmailConstants {

  /**
   * Metadata key that specifies the type of the payload. Either {@link EmailConstants#EMAIL_PAYLOAD_TYPE_PAYLOAD} "payload" or
   * {@link EmailConstants#EMAIL_PAYLOAD_TYPE_ATTACHMENT} "attachment"
   */
  public static final String EMAIL_PAYLOAD_TYPE = "emailpayloadtype";

  /**
   * Possible value for the metadata key {@link EmailConstants#EMAIL_PAYLOAD_TYPE}
   */
  public static final String EMAIL_PAYLOAD_TYPE_PAYLOAD = "payload";

  /**
   * Possible value for the metadata key {@link EmailConstants#EMAIL_PAYLOAD_TYPE}
   */
  public static final String EMAIL_PAYLOAD_TYPE_ATTACHMENT = "attachment";

  /**
   * Metadata key that specifies the name of the attachment.
   *
   */
  public static final String EMAIL_ATTACH_FILENAME = "emailattachmentfilename";

  /**
   * Metadata key that specifies the content-type of the attachment.
   *
   */
  public static final String EMAIL_ATTACH_CONTENT_TYPE = "emailattachmentcontenttype";

  /**
   * Metadata key that specifies the size of the attachment.
   *
   */
  public static final String EMAIL_ATTACH_SIZE = "emailattachmentsize";

}
