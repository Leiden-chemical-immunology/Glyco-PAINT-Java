/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.viewer.ui;

import paint.viewer.model.RecordingEntry;

import java.util.List;

/**
 * Handles safe navigation through a list of recordings.
 * {@link paint.viewer.ui.frames.ViewerFrame} registers as the listener and
 * updates the UI whenever a new index is selected.
 */
public final class RecordingNavigator {

    /**
     * Listener interface for responding to navigation events.
     */
    public interface Listener {
        /**
         * Called when a navigation action requires shifting to a new recording.
         *
         * @param newIndex the target index in the recording list
         */
        void onNavigateTo(int newIndex);
    }

    private final Listener listener;

    /**
     * Constructs a {@code RecordingNavigator} with the specified listener.
     *
     * @param listener the object to notify when navigation occurs
     */
    public RecordingNavigator(Listener listener) {
        this.listener = listener;
    }

    /**
     * Navigates to the first recording in the list.
     *
     * @param entries the list of available recordings
     */
    public void first(List<RecordingEntry> entries) {
        if (entries.isEmpty()) return;
        listener.onNavigateTo(0);
    }

    /**
     * Navigates to the last recording in the list.
     *
     * @param entries the list of available recordings
     */
    public void last(List<RecordingEntry> entries) {
        if (entries.isEmpty()) return;
        listener.onNavigateTo(entries.size() - 1);
    }

    /**
     * Navigates to the next recording in the list.
     *
     * @param entries      the list of available recordings
     * @param currentIndex the index of the currently displayed recording
     */
    public void next(List<RecordingEntry> entries, int currentIndex) {
        if (entries.isEmpty()) return;
        int next = Math.min(entries.size() - 1, currentIndex + 1);
        listener.onNavigateTo(next);
    }

    /**
     * Navigates to the previous recording in the list.
     *
     * @param entries      the list of available recordings
     * @param currentIndex the index of the currently displayed recording
     */
    public void prev(List<RecordingEntry> entries, int currentIndex) {
        if (entries.isEmpty()) return;
        int prev = Math.max(0, currentIndex - 1);
        listener.onNavigateTo(prev);
    }
}