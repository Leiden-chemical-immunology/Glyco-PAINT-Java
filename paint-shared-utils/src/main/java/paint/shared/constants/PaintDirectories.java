/*=============================================================================
 *  Class:        PaintDirectories.java
 *  Package:      paint.shared.constants
 *
 *  PURPOSE:
 *    Defines standard directory names and folder structures used by the
 *    PAINT application.
 *
 *  DESCRIPTION:
 *    The {@code PaintDirectories} class centralizes constants for core
 *    project folders, such as where experiments, viewer data, and exports
 *    are stored.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-shared-utils
 *
 *  UPDATED:
 *    2025-12-31
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 *============================================================================*/

package paint.shared.constants;

/**
 * Defines standard directory names and folder structures used by the
 * PAINT application.
 */
public final class PaintDirectories {

    private PaintDirectories() {
        // Prevent instantiation
    }

    /** Folder name for images used in or produced by TrackMate. */
    public static final String DIR_TRACKMATE_IMAGES   = "TrackMate Images";
    /** Folder name for corresponding Brightfield images. */
    public static final String DIR_BRIGHTFIELD_IMAGES = "Brightfield Images";
}