package androidx.media3.common.util;

import android.net.Uri;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Helpers for local (license-server-less) ClearKey decryption. A ClearKey description is either a
 * raw W3C ClearKey JSON body or a {@code data:} URI embedding one; when the key description can
 * be parsed locally the media item is marked with the ClearKey scheme so the extractors can
 * decrypt samples without a network round trip.
 */
@UnstableApi
public final class LocalClearKeyLicense {

  private LocalClearKeyLicense() {}

  /**
   * Returns the license JSON carried by the given key description, or an empty string when the
   * description is not a locally parseable ClearKey body.
   */
  public static String parse(@Nullable String keyDescription) {
    if (keyDescription == null) return "";
    String json = keyDescription.trim();
    if (json.startsWith("data:")) {
      int comma = json.indexOf(',');
      if (comma < 0) return "";
      json = Uri.decode(json.substring(comma + 1)).trim();
    }
    if (!json.startsWith("{")) return "";
    try {
      JSONObject object = new JSONObject(json);
      JSONArray keys = object.optJSONArray("keys");
      if (keys == null || keys.length() == 0) return "";
      return json;
    } catch (Exception error) {
      return "";
    }
  }

  /**
   * Convenience overload returning the license JSON of a media item's DRM configuration, or an
   * empty string when the configuration is absent or not locally parseable.
   */
  public static String parse(@Nullable MediaItem.DrmConfiguration drmConfiguration) {
    if (drmConfiguration == null) return "";
    Uri licenseUri = drmConfiguration.licenseUri;
    return parse(licenseUri == null ? null : licenseUri.toString());
  }
}
