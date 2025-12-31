/*=============================================================================
 *  Class:        VersionInfo.java
 *  Package:      release
 *
 *  PURPOSE:
 *    Encapsulates version information for the Glyco-PAINT software.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-development-utils
 *
 *  UPDATED:
 *    2025-12-31
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 *=============================================================================*/

package release;

final class VersionInfo {
    final String releaseVersion;
    final String nextDevVersion;
    VersionInfo(String release, String next) {
        this.releaseVersion = release;
        this.nextDevVersion = next;
    }
}