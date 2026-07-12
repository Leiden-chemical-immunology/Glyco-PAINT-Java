/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package github;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.time.*;
import java.util.stream.Collectors;

/**
 * Provides an automated interface for managing GitHub releases, tags,
 * workflow runs, and artifacts using the GitHub CLI ({@code gh}).
 */
@SuppressWarnings("unused")
public final class GitHubManager {

    private final String repo; // owner/repo, e.g. "JJBakker/paint"
    private boolean dryRun = false;

    /**
     * Constructs a {@code GitHubManager} for the specified repository.
     *
     * @param repo the GitHub repository identifier (e.g., "owner/repo")
     */
    public GitHubManager(String repo) {
        this.repo = repo;
    }

    /**
     * Enables or disables dry-run mode. When enabled, commands are printed to
     * the console instead of being executed.
     *
     * @param dry {@code true} to enable dry-run mode
     */
    public void setDryRun(boolean dry) {
        this.dryRun = dry;
    }

    // --------------------------------------------------------------------------------------------
    // Internal command runner
    // --------------------------------------------------------------------------------------------

    /**
     * Executes a system command and returns the output as a list of strings.
     *
     * @param cmd the command and its arguments
     * @return the command output lines
     * @throws Exception if the command fails or an I/O error occurs
     */
    private List<String> run(String... cmd) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();

        List<String> out = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) out.add(line);
        }
        if (p.waitFor() != 0) {
            throw new RuntimeException("Command failed: " + String.join(" ", cmd));
        }
        return out;
    }

    /**
     * Executes a command or prints it if dry-run mode is enabled.
     *
     * @param cmd the command and its arguments
     * @throws Exception if the command fails
     */
    private void runOrDry(String... cmd) throws Exception {
        if (dryRun) {
            System.out.println("[DRY RUN] " + String.join(" ", cmd));
        } else {
            run(cmd);
        }
    }

    // --------------------------------------------------------------------------------------------
    // Releases
    // --------------------------------------------------------------------------------------------

    /**
     * Lists all releases for the repository.
     *
     * @return a list of strings representing the releases
     * @throws Exception if the GitHub CLI call fails
     */
    public List<String> listReleases() throws Exception {
        return run("gh", "release", "list", "--repo", repo);
    }

    /**
     * Deletes a release identified by its tag.
     *
     * @param tag the tag of the release to delete
     * @throws Exception if the deletion fails
     */
    public void deleteRelease(String tag) throws Exception {
        runOrDry("gh", "release", "delete", tag, "--repo", repo, "--yes");
    }

    /**
     * Deletes all releases for the repository.
     *
     * @throws Exception if any deletion fails
     */
    public void deleteAllReleases() throws Exception {
        for (String line : listReleases()) {
            String tag = line.split("\\s+")[0];
            deleteRelease(tag);
        }
    }

    /**
     * Retrieves a list of all release tags.
     *
     * @return a list of release tag strings
     * @throws Exception if the GitHub CLI call fails
     */
    public List<String> listReleaseTagsOnly() throws Exception {
        List<String> out = new ArrayList<>();
        for (String line : listReleases()) {
            out.add(line.split("\\s+")[0]);
        }
        return out;
    }

    /**
     * Deletes all prereleases for the repository.
     *
     * @throws Exception if any deletion fails
     */
    public void deletePrereleases() throws Exception {
        List<Map<String,String>> releases = run(
                "gh", "release", "list", "--repo", repo, "--json", "tagName,isPrerelease"
        ).stream()
         .map(s -> parseLine(s))
         .collect(Collectors.toList());

        for (Map<String,String> r : releases) {
            if ("true".equals(r.get("isPrerelease"))) {
                deleteRelease(r.get("tagName"));
            }
        }
    }

    // Delete all releases except last N
    public void deleteAllReleasesExceptLatest(int keep) throws Exception {
        List<String> tags = listReleaseTagsOnly();
        if (tags.size() <= keep) return;

        for (int i = keep; i < tags.size(); i++) {
            deleteRelease(tags.get(i));
        }
    }

    // --------------------------------------------------------------------------------------------
    // Tags
    // --------------------------------------------------------------------------------------------
    public List<String> listTags() throws Exception {
        return run("git", "ls-remote", "--tags", "https://github.com/" + repo + ".git");
    }

    private String extractTagName(String lsRemoteLine) {
        String[] parts = lsRemoteLine.split("/");
        return parts[parts.length - 1].trim();
    }

    public void deleteTag(String tag) throws Exception {
        runOrDry("git", "tag", "-d", tag);
        runOrDry("git", "push", "origin", ":refs/tags/" + tag);
    }

    public void deleteAllTags() throws Exception {
        for (String line : listTags()) {
            deleteTag(extractTagName(line));
        }
    }

    // Delete tags older than X days
    public void deleteTagsOlderThan(int days) throws Exception {
        Instant cutoff = Instant.now().minus(Duration.ofDays(days));

        List<Map<String,String>> tags = run(
                "gh", "release", "list", "--repo", repo, "--limit", "200",
                "--json", "tagName,publishedAt"
        ).stream()
         .map(s -> parseLine(s))
         .collect(Collectors.toList());

        for (Map<String,String> t : tags) {
            Instant published = Instant.parse(t.get("publishedAt"));
            if (published.isBefore(cutoff)) {
                deleteTag(t.get("tagName"));
            }
        }
    }

    // --------------------------------------------------------------------------------------------
    // Workflow runs
    // --------------------------------------------------------------------------------------------
    public List<String> listWorkflowRunsRaw() throws Exception {
        return run("gh", "run", "list", "--repo", repo, "--limit", "200");
    }

    public List<String> listWorkflowRunIds() throws Exception {
        return run("gh", "run", "list", "--repo", repo, "--limit", "200",
                   "--json", "databaseId", "--jq", ".[].databaseId");
    }

    public void deleteWorkflowRun(String id) throws Exception {
        runOrDry("gh", "run", "delete", id, "--repo", repo, "--yes");
    }

    public void deleteAllWorkflowRuns() throws Exception {
        for (String id : listWorkflowRunIds()) {
            deleteWorkflowRun(id.trim());
        }
    }

    // Delete workflow runs older than X days
    public void deleteWorkflowRunsOlderThan(int days) throws Exception {
        Instant cutoff = Instant.now().minus(Duration.ofDays(days));

        List<String> lines = run("gh", "run", "list",
                                 "--repo", repo, "--limit", "1000",
                                 "--json", "databaseId,createdAt",
                                 "--jq", ".[] | (.databaseId|tostring) + \" \" + .createdAt"
        );

        for (String l : lines) {
            String[] parts = l.split(" ");
            String id = parts[0];
            Instant created = Instant.parse(parts[1]);

            if (created.isBefore(cutoff)) {
                deleteWorkflowRun(id);
            }
        }
    }

    // Delete all workflow runs except last N
    public void deleteWorkflowRunsExceptLatest(int keep) throws Exception {
        List<String> ids = listWorkflowRunIds();
        if (ids.size() <= keep) return;

        for (int i = keep; i < ids.size(); i++) {
            deleteWorkflowRun(ids.get(i).trim());
        }
    }

    // --------------------------------------------------------------------------------------------
    // Artifacts
    // --------------------------------------------------------------------------------------------
    public List<Map<String, String>> listArtifacts() throws Exception {
        List<String> out = run(
                "gh", "api",
                "/repos/" + repo + "/actions/artifacts",
                "--jq", ".artifacts[] | {id: .id, name: .name, size: .size_in_bytes, created_at: .created_at}"
        );

        List<Map<String, String>> list = new ArrayList<>();
        for (String line : out) list.add(parseLine(line));
        return list;
    }

    public void deleteArtifact(String id) throws Exception {
        runOrDry(
                "gh", "api",
                "/repos/" + repo + "/actions/artifacts/" + id,
                "-X", "DELETE"
        );
    }

    public void deleteAllArtifacts() throws Exception {
        List<Map<String,String>> arts = listArtifacts();
        for (Map<String,String> a : arts) {
            deleteArtifact(a.get("id"));
        }
    }

    public void deleteArtifactsOlderThan(int days) throws Exception {
        Instant cutoff = Instant.now().minus(Duration.ofDays(days));

        for (Map<String,String> a : listArtifacts()) {
            Instant created = Instant.parse(a.get("created_at"));
            if (created.isBefore(cutoff)) {
                deleteArtifact(a.get("id"));
            }
        }
    }

    public void deleteArtifactsExceptLatest(int keep) throws Exception {
        List<Map<String,String>> artifacts = listArtifacts();

        artifacts.sort(new Comparator<Map<String,String>>() {
            public int compare(Map<String,String> a, Map<String,String> b) {
                return b.get("created_at").compareTo(a.get("created_at"));
            }
        });

        if (artifacts.size() <= keep) return;

        for (int i = keep; i < artifacts.size(); i++) {
            deleteArtifact(artifacts.get(i).get("id"));
        }
    }

    public void deleteArtifactsMatching(String regex) throws Exception {
        for (Map<String,String> a : listArtifacts()) {
            if (a.get("name").matches(regex)) {
                deleteArtifact(a.get("id"));
            }
        }
    }

    // --------------------------------------------------------------------------------------------
    // JSON-ish parsing helper
    // --------------------------------------------------------------------------------------------
    private Map<String,String> parseLine(String jsonLine) {
        Map<String,String> map = new HashMap<>();
        jsonLine = jsonLine.replace("{", "").replace("}", "").trim();
        String[] parts = jsonLine.split(",");
        for (String p : parts) {
            String[] kv = p.split(":");
            if (kv.length == 2) {
                String k = kv[0].replaceAll("[\" ]", "");
                String v = kv[1].replaceAll("[\" ]", "");
                map.put(k, v);
            }
        }
        return map;
    }

    // --------------------------------------------------------------------------------------------
    // Printers
    // --------------------------------------------------------------------------------------------
    public static void print(List<String> list) {
        for (String s : list) System.out.println("  " + s);
    }

    public static void printArtifacts(List<Map<String,String>> list) {
        for (Map<String,String> a : list) {
            System.out.println("  id=" + a.get("id")
                                       + "  name=" + a.get("name")
                                       + "  created_at=" + a.get("created_at"));
        }
    }

    // --------------------------------------------------------------------------------------------
    // Demo main
    // --------------------------------------------------------------------------------------------
    public static void main(String[] args) throws Exception {
        GitHubManager gh = new GitHubManager("Leiden-chemical-immunology/Glyco-PAINT-Java");

        gh.setDryRun(true);

        System.out.println("== Releases ==");
        print(gh.listReleases());

        System.out.println("== Tags ==");
        print(gh.listTags());

        System.out.println("== Workflow Runs ==");
        print(gh.listWorkflowRunsRaw());

        System.out.println("== Artifacts ==");
        printArtifacts(gh.listArtifacts());
    }
}