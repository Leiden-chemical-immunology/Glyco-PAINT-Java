/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.shared.constants;

/**
 * Defines standard directory names and folder structures used by the PAINT application.
 * <p>
 * The {@code PaintDirectories} class centralizes constants for core project folders, such as
 * where experiments, viewer data, and exports are stored.
 * </p>
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