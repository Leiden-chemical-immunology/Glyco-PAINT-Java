# Scripts





### Clean all paint jars

```
#=============================================================================
# clean-all-paint-jars.sh
#
# PURPOSE:
#   Locate all “paint” JAR files (*paint*.jar) in:
#     • Project modules (~/JavaPaintProjects)
#     • Maven local repository (~/.m2)
#     • Fiji installation (/Applications/Fiji.app)
#     • Glyco-PAINT app bundles (~/Applications/Glyco-PAINT)
#   List them to the user with a preview.
#   Prompt for confirmation.
#   If confirmed, delete all listed files.
#
# USE CASE:
#   Run this script before rebuilding, packaging or deploying
#   to ensure no stale “paint” JARs remain.
#
# ACTIONS PERFORMED:
#   1) Search defined directories for matching JAR files.
#   2) Present the found file list to the user.
#   3) Ask “Do you want to delete all these files? (y/N)”.
#   4) If yes → delete files. If no → abort without deletion.
#
# USAGE:
#   ./shell-scripts/clean-all-paint-jars.sh
#=============================================================================
```





### Release manager

```
###############################################################################
# release-manager.sh
#
# PURPOSE:
#   Unified release management script for multi-module Maven + Git + GitHub projects.
#   Handles version bumping, tagging, changelog updates, and GitHub release creation.
#
# USE CASE:
#   Run this when you’re ready to publish a new release version or correct an existing one.
#   It automates Maven version updates, Git commits, tagging, and triggers GitHub Actions
#   for building, packaging, and publishing release artifacts and Maven sites.
#
# ACTIONS PERFORMED:
#   1  Validates that your working tree is clean
#   2  Updates Maven POM versions to the release version
#   3  Commits and tags the release in Git
#   4  Pushes the tag to GitHub (automatically triggers GitHub Actions)
#   5  Optionally deletes or re-creates existing releases and tags
#   6  Bumps project version to the next SNAPSHOT after release
#
# SUBCOMMANDS:
#   create [VERSION]     Create a new release (triggers GitHub Actions build + site deploy)
#   delete <TAG>         Delete a GitHub release and its tags (local + remote)
#   recreate <TAG>       Delete and immediately re-create the specified release
#
# USAGE:
#   ./shell-scripts/release-manager.sh create 1.2.0 --execute
#   ./shell-scripts/release-manager.sh delete 1.2.0 --execute
#   ./shell-scripts/release-manager.sh recreate 1.2.0 --execute
#
# OPTIONS:
#   --execute, -x   Run for real (default is dry-run)
#   --help, -h      Show help
#
# REQUIREMENTS:
#   - Git installed and configured
#   - Maven installed and on PATH
#   - GitHub CLI (`gh`) installed and authenticated (for deleting GitHub releases)
#   - GitHub Actions configured to build and publish on tag push (v*.*.*)
#
# SAFETY FEATURES:
#   - Runs in DRY-RUN mode by default (no file or tag changes)
#   - Confirms before performing destructive actions
#   - Aborts automatically if uncommitted changes are detected
#   - Never pushes or deletes tags without explicit confirmation
#
# RESULT:
#   - New release tag pushed → triggers GitHub Actions to:
#       • Build and attach release JARs
#       • Publish a GitHub Release with notes
#       • Rebuild and deploy the Maven site to GitHub Pages
#   - Local POMs bumped to the next SNAPSHOT version
#
# WHERE TO CHECK RESULTS:
#   🔹 GitHub Releases:
#        https://github.com/jjabakker/JavaPaintProjects/releases
#        → Verify that the new release appears with attached JARs and notes
#
#   🔹 GitHub Actions (CI/CD logs):
#        https://github.com/jjabakker/JavaPaintProjects/actions
#        → Confirm that the “Build, Release, and Publish Site” workflow ran successfully
#
#   🔹 GitHub Pages (Maven site):
#        https://jjabakker.github.io/JavaPaintProjects/
#        → Confirm that the site updated with the new project documentation
###############################################################################
```





### Make Glyco-PAINT installer

```
###############################################################################
# make-glyco-paint-installer.sh
#
# PURPOSE:
#   Creates a cross-platform, self-extracting Glyco-PAINT installer that bundles
#   all desktop applications and the Fiji plugin into a single, portable script.
#
# USE CASE:
#   Run this script after building all Glyco-PAINT modules to produce a complete
#   installer that users can execute on macOS or Windows (via Git Bash).
#   The installer automatically locates or prompts for Fiji.app and installs the
#   appropriate plugin JAR, ensuring a ready-to-run environment.
#
# ACTIONS PERFORMED:
#   1  Detects operating system (macOS or Windows via Git Bash)
#   2  Gathers built .app bundles from:
#        ~/Applications/Glyco-PAINT/  (macOS)
#        ~/AppData/Local/Glyco-PAINT/ (Windows)
#   3  Finds the Fiji fat JAR automatically from:
#        ~/JavaPaintProjects/paint-fiji-plugin/target/
#   4  Packages the applications and JAR into a base64-encoded, self-extracting
#      Bash installer script located in:
#        ~/Downloads/Glyco-PAINT-Installer.sh
#   5  The generated installer:
#        • Installs Glyco-PAINT into the user’s Applications folder
#        • Detects or prompts for Fiji.app location
#        • Copies the Fiji plugin JAR into Fiji’s plugins directory
#
# USAGE:
#   chmod +x make-glyco-paint-installer.sh
#   ./make-glyco-paint-installer.sh
#
# REQUIREMENTS:
#   - macOS or Windows (Git Bash)
#   - tar, base64, and standard Unix utilities available on PATH
#   - Fiji plugin already built (fat JAR must exist in `paint-fiji-plugin/target`)
#
# SAFETY FEATURES:
#   - Exits if required directories or JARs are missing
#   - Creates temporary workspace safely via `mktemp`
#   - Cleans up intermediate files automatically
#
# RESULT:
#   - Self-contained Bash installer at:
#       ~/Downloads/Glyco-PAINT-Installer.sh
#   - When executed, installs:
#       • Glyco-PAINT applications → ~/Applications/Glyco-PAINT/
#       • Fiji plugin → Fiji.app/plugins/
#
# WHERE TO CHECK RESULTS:
#   🔹 Installer Script:
#        ~/Downloads/Glyco-PAINT-Installer.sh
#
#   🔹 Installed Applications:
#        ~/Applications/Glyco-PAINT/              (macOS)
#        ~/AppData/Local/Glyco-PAINT/            (Windows)
#
#   🔹 Installed Fiji Plugin:
#        <path-to-Fiji.app>/plugins/
#
###############################################################################
```





### Make Icons


```
###############################################################################
# make-icons.sh
#
# PURPOSE:
#   Unified icon generation script for all Glyco-PAINT desktop applications.
#   Converts high-resolution PNG source images into platform-specific icons
#   (.icns for macOS, .ico for Windows) and installs them into each module’s
#   resource directory for packaging.
#
# USE CASE:
#   Run this script whenever PNG icon artwork changes. It rebuilds all required
#   app icons for macOS and Windows automatically and places them in the proper
#   module resource locations.
#
# ACTIONS PERFORMED:
#   1  Scans the source directory:
#        ~/JavaPaintProjects/paint-icons-generation/
#   2  For each 1024×1024 PNG file found:
#        • Creates a macOS .iconset directory
#        • Uses `sips` and `iconutil` to generate a .icns bundle
#        • Optionally uses ImageMagick `convert` to generate a .ico file
#   3  Copies both icon types into each module’s resource directory:
#        ~/JavaPaintProjects/<module>/src/main/resources/icons/
#
# SUPPORTED MODULES:
#   • paint-generate-squares
#   • paint-create-experiment
#   • paint-get-omero
#   • paint-viewer
#
# REQUIREMENTS:
#   - macOS system (for `sips` and `iconutil`)
#   - ImageMagick (optional, for .ico generation)
#       → install via: brew install imagemagick
#
# USAGE:
#   chmod +x make-icons.sh
#   ./make-icons.sh
#
# RESULT:
#   Each module’s `src/main/resources/icons/` directory will contain:
#     • paint-<app>.icns  — used for macOS .app bundles
#     • paint-<app>.ico   — used for Windows .exe packaging (if ImageMagick found)
#
# SAFETY FEATURES:
#   - Exits early if source directory not found
#   - Skips copy for unknown base names
#   - Gracefully handles missing ImageMagick dependency
#
# WHERE TO CHECK RESULTS:
#   🔹 macOS icon bundles:
#        ~/JavaPaintProjects/<module>/src/main/resources/icons/*.icns
#   🔹 Windows icon files:
#        ~/JavaPaintProjects/<module>/src/main/resources/icons/*.ico
#   🔹 PNG source files:
#        ~/JavaPaintProjects/paint-icons-generation/
#
###############################################################################
```





### Publish site


```
###############################################################################
# publish-site.sh
#
# PURPOSE:
#   Builds and publishes the Maven-generated documentation site (target/site)
#   to the `gh-pages` branch of this repository, which serves GitHub Pages.
#
# USE CASE:
#   Run this script after tagging or releasing a new version. It safely rebuilds
#   and deploys the Maven site without disturbing your main working tree or
#   local branches.
#
# ACTIONS PERFORMED:
#   1  Detects project root automatically (can run from anywhere)
#   2  Removes any stale `gh-pages` worktree or branch
#   3  Builds Maven site if missing (`mvn clean site`)
#   4  Creates a temporary worktree for the `gh-pages` branch
#   5  Copies site files from `target/site` into the worktree
#   6  Commits and pushes changes to GitHub Pages
#   7  Cleans up all temporary files and worktrees
#
# FEATURES:
#   • Fully self-contained — no external dependencies beyond Git + Maven
#   • Safe — never modifies your main working tree
#   • Idempotent — does nothing if no site changes are detected
#
# USAGE:
#   chmod +x publish-site.sh
#   ./shell-scripts/publish-site.sh
#
# REQUIREMENTS:
#   - Maven installed and available on PATH
#   - Git configured with push access to the repository
#
# SAFETY FEATURES:
#   - Uses temporary directory for `gh-pages` updates
#   - Automatically deletes stale branches and worktrees
#   - Commits only when differences are detected
#
# RESULT:
#   - The site is deployed to the `gh-pages` branch on GitHub.
#   - Accessible at:
#       🔹 https://jjabakker.github.io/JavaPaintProjects/
#
# WHERE TO CHECK RESULTS:
#   🔹 GitHub Pages site:
#        https://jjabakker.github.io/JavaPaintProjects/
#        → Confirm that site reflects latest Maven documentation
#
#   🔹 GitHub Actions (optional):
#        https://github.com/jjabakker/JavaPaintProjects/actions
#        → Verify that Pages deployment was successful (if using CI)
#
###############################################################################
```
