package trackMatePaint;

import java.nio.file.Path;

public class TrackMateRecording {

    public TrackMateRecording(Path experimentPath, Path omeroExperimentPath, String recording) {
        System.out.println(String.format("Ready to start TrackMate on a recording: %s.", recording));
    }

}
