package release;

final class VersionInfo {
    final String releaseVersion;
    final String nextDevVersion;
    VersionInfo(String release, String next) {
        this.releaseVersion = release;
        this.nextDevVersion = next;
    }
}