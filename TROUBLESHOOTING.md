# Troubleshooting

Problems that are easy to hit and hard to diagnose, with what actually causes them.

## "The recording exists but cannot be read" when playing a recording

You press Play in the Viewer and get a dialog saying the recording exists but cannot be read, or
(in older versions) a bare "Failed to open image file". The file is plainly there — you can see it
in Finder, and `ls` in a terminal lists it happily.

This is macOS, not Glyco-PAINT. Recordings are large, so the Images Root nearly always lives on an
external drive, and macOS gates access to removable volumes per application. The application can
*see* the file — enough for it to pass an existence check — but the actual read is refused with
"Operation not permitted".

**Fix.** Open System Settings → Privacy & Security → Files and Folders, find the application that
launches Glyco-PAINT, and enable **Removable Volumes**. Granting **Full Disk Access** instead also
works and is simpler.

Which application to grant it to is the part people get wrong. macOS attributes file access to the
process that *launched* the JVM, not to `java` itself:

- Running from the installed app → grant it to the Glyco-PAINT app.
- Running from IntelliJ → grant it to **IntelliJ IDEA**, not to `java`.
- Running from a terminal → grant it to the terminal (Terminal, iTerm, …).

**Then restart the application completely.** macOS decides these permissions when a process starts
and never re-checks, so a permission granted while the app is running has no effect on it. Quit it
fully — ⌘Q, not just closing the window — and start it again.

If it is still refused after a genuine restart, the permission entry may be stale: macOS
invalidates a grant when an application is re-signed, which happens on every update, while still
showing the row as enabled. Remove the application from Full Disk Access with the `–` button,
add it back with `+`, and restart it.

To confirm the drive itself is healthy, read the file from a terminal that already has Full Disk
Access:

```bash
head -c 16 "/Volumes/<your drive>/<experiment>/<recording>.nd2" | xxd
```

If that works but the application still cannot, the problem is definitely the permission and not
the disk.

## A long run becomes inexplicably slow, and the console mentions the CodeCache

You may see this on the console, typically after the application has been running for a while:

```
Java HotSpot(TM) 64-Bit Server VM warning: CodeCache is full. Compiler has been disabled.
CodeCache: size=131072Kb used=15065Kb ... compilation: disabled (not enough contiguous free space left)
```

Note that it says the cache is *full* while reporting most of it as free. That is a Java 8 defect,
not a memory shortage. Loading ImageJ, TrackMate and Bio-Formats compiles a very large number of
methods; when the code cache comes under pressure, Java 8 fragments it and can then switch the
JIT compiler off entirely.

The consequence is quiet and easy to miss: from that moment the application runs *interpreted* —
several times slower — with no error, and nothing in the log. A Generate Squares or TrackMate run
simply takes far longer than it should, for no apparent reason.

**Fix.** The packaged applications set `-XX:ReservedCodeCacheSize=512m -XX:-UseCodeCacheFlushing`
in their launcher scripts, which keeps well clear of the problem. If you run from an IDE, add the
same options to the run configuration's VM options. If you see the warning in **Fiji's** console
while running the TrackMate plugin, the setting belongs in Fiji's own `ImageJ.cfg` — that JVM is
Fiji's, not ours.

Using a current Java 8 build (for example Azul Zulu 8, which is what CI uses) also helps, as
several code-cache bugs were fixed in later 8u releases.

## The Viewer refuses to open a project: "not a square grid"

The Viewer needs the number of squares per recording to be a square number — 25 (5×5), 100
(10×10), 625 (25×25) and so on — because it lays the squares out on a grid. If the configured
value is not a perfect square, the Viewer says so and declines to open, rather than displaying a
grid that does not correspond to the data.

Check `Number of Squares in Recording` under `Generate Squares` in the project's
`Paint Configuration.json`.

## Nothing appears to happen, and there is no error

Check the log first: it is written under the project folder, and it records far more than the
console shows. Failures that are reported to the log and not to the screen are the usual reason
for a silent no-op.
