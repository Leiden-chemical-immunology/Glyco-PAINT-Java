package paint.viewer;

import paint.viewer.utils.RecordingEntry;

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