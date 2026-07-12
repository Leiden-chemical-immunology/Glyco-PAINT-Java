/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package release.support;

/**
 * Encapsulates version information for the Glyco-PAINT software.
 */
public final class VersionInfo {
    public final String releaseVersion;
    public final String nextDevVersion;
    public VersionInfo(String release, String next) {
        this.releaseVersion = release;
        this.nextDevVersion = next;
    }
}