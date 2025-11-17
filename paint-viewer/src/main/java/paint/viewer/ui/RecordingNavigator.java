/*==============================================================================
 *  Class:        RecordingDisplayUpdater.java
 *  Package:      paint.viewer.ui
 *
 *  PURPOSE:
 *    Centralizes all UI updates that must occur when navigating to a new
 *    {@link paint.viewer.model.RecordingEntry}. Ensures that all visible
 *    components in the ViewerFrame—grid, images, labels, and metadata panel—
 *    are fully synchronized with the newly selected recording.
 *
 *  DESCRIPTION:
 *    This utility class performs a coordinated refresh of the viewer UI
 *    whenever the active recording changes. It updates:
 *
 *      • The left grid panel (recording object, squares list, background image)
 *      • The right-side preview image (scaled to viewer constants)
 *      • The experiment and recording labels
 *      • The attribute panel (recording metadata table)
 *
 *    The class is stateless, containing no retained model logic. It operates
 *    solely on the Swing components provided through its constructor.
 *
 *  KEY FEATURES:
 *    • Complete UI refresh in response to navigation actions.
 *    • High-quality deterministic image scaling for the preview panel.
 *    • Reliable metadata updates via {@link paint.shared.config.paintconfig.PaintConfig}.
 *    • No external dependencies—lightweight and side-effect-free.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-viewer
 *
 *  UPDATED:
 *    2025-10-29
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 *==============================================================================*/

package paint.viewer.ui;

import paint.viewer.model.RecordingEntry;

import java.util.List;

/**
 * Handles safe navigation through a list of recordings.
 * ViewerFrame registers as the listener and updates the UI whenever
 * a new index is selected.
 */
public final class RecordingNavigator {

    public interface Listener {
        void onNavigateTo(int newIndex);
    }

    private final Listener listener;

    public RecordingNavigator(Listener listener) {
        this.listener = listener;
    }

    public void first(List<RecordingEntry> entries) {
        if (entries.isEmpty()) return;
        listener.onNavigateTo(0);
    }

    public void last(List<RecordingEntry> entries) {
        if (entries.isEmpty()) return;
        listener.onNavigateTo(entries.size() - 1);
    }

    public void next(List<RecordingEntry> entries, int currentIndex) {
        if (entries.isEmpty()) return;
        int next = Math.min(entries.size() - 1, currentIndex + 1);
        listener.onNavigateTo(next);
    }

    public void prev(List<RecordingEntry> entries, int currentIndex) {
        if (entries.isEmpty()) return;
        int prev = Math.max(0, currentIndex - 1);
        listener.onNavigateTo(prev);
    }
}