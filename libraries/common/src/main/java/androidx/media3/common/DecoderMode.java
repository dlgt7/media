package androidx.media3.common;

/**
 * User-selectable decoder preference shared by all playback engines.
 *
 * <p>The engine maps these coarse preferences onto its own decoder selection strategy; {@code
 * AUTO} keeps the engine default (usually hardware first with software fallback).
 */
public enum DecoderMode {
  AUTO,
  HARDWARE,
  SOFTWARE,
  FFMPEG
}
