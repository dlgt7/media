package androidx.media3.exoplayer.libass;

import androidx.media3.common.util.UnstableApi;

/**
 * Configuration for the libass subtitle renderer: font locations, limits and the default font
 * family. See {@link Builder}.
 */
@UnstableApi
public final class LibassConfiguration {

  /** Builder for {@link LibassConfiguration}. */
  public static final class Builder {

    @androidx.annotation.Nullable private String fontConfig;
    @androidx.annotation.Nullable private String fontsDirectory;
    @androidx.annotation.Nullable private String defaultFontFamily;
    private int maximumRenderPixels;
    private int maximumGlyphCount;
    private int maximumBitmapCacheSizeMb;

    /** Sets the path of the fontconfig file describing system font discovery. */
    public Builder setFontConfig(@androidx.annotation.Nullable String fontConfig) {
      this.fontConfig = fontConfig;
      return this;
    }

    /** Sets the directory scanned for user-provided fonts. */
    public Builder setFontsDirectory(@androidx.annotation.Nullable String fontsDirectory) {
      this.fontsDirectory = fontsDirectory;
      return this;
    }

    /** Sets the font family used when a style does not name an available family. */
    public Builder setDefaultFontFamily(@androidx.annotation.Nullable String defaultFontFamily) {
      this.defaultFontFamily = defaultFontFamily;
      return this;
    }

    /** Sets the maximum number of pixels the renderer may rasterize per frame. */
    public Builder setMaximumRenderPixels(int maximumRenderPixels) {
      this.maximumRenderPixels = maximumRenderPixels;
      return this;
    }

    /** Sets the maximum number of glyphs kept alive per subtitle event. */
    public Builder setMaximumGlyphCount(int maximumGlyphCount) {
      this.maximumGlyphCount = maximumGlyphCount;
      return this;
    }

    /** Sets the bitmap cache size in megabytes. */
    public Builder setMaximumBitmapCacheSizeMb(int maximumBitmapCacheSizeMb) {
      this.maximumBitmapCacheSizeMb = maximumBitmapCacheSizeMb;
      return this;
    }

    public LibassConfiguration build() {
      return new LibassConfiguration(this);
    }
  }

  @androidx.annotation.Nullable public final String fontConfig;
  @androidx.annotation.Nullable public final String fontsDirectory;
  @androidx.annotation.Nullable public final String defaultFontFamily;
  public final int maximumRenderPixels;
  public final int maximumGlyphCount;
  public final int maximumBitmapCacheSizeMb;

  private LibassConfiguration(Builder builder) {
    this.fontConfig = builder.fontConfig;
    this.fontsDirectory = builder.fontsDirectory;
    this.defaultFontFamily = builder.defaultFontFamily;
    this.maximumRenderPixels = builder.maximumRenderPixels;
    this.maximumGlyphCount = builder.maximumGlyphCount;
    this.maximumBitmapCacheSizeMb = builder.maximumBitmapCacheSizeMb;
  }
}
