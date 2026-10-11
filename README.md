# Secure Local Data Shredder

A local file-processing prototype that demonstrates overwrite-and-delete workflows, selected metadata handling, folder operations, and dry-run previews. It does not guarantee secure erasure on every storage device.

## Features

- Single-file shred mode
- Directory shred mode
- Safe-path policy to block unapproved locations
- Dry-run mode to preview actions without deleting files
- Metadata sanitization for text and common image types
- Audit logging in JSON format
- Progress summary for folder processing

## Project structure

- `src/Main.java` — main application logic
- `audit-log.json` — generated log file with shred activity

## Browser workflow

The browser interface uses a local Python bridge and the Java CLI. It does not send files to a remote server. The browser uploads the selected file to the bridge at `127.0.0.1`, which stores a staging copy under `src/uploads`.

Important: the browser workflow shreds the staged copy only. The original file selected in the browser remains unchanged. This is not a native desktop file picker.

Requirements:

- A JDK with `java` and `javac` available on `PATH`
- Python 3
- A web browser

From the project root, start the bridge:

```powershell
.\.venv\Scripts\python.exe .\src\local_bridge.py
```

If the workspace virtual environment is unavailable, use an installed Python instead:

```powershell
python .\src\local_bridge.py
```

Open `http://127.0.0.1:8000` in the browser. Keep the bridge terminal open while using the page. The existing `run.ps1` continues to launch the command-line interface.

## Safely test the browser workflow

1. Create a disposable source file:

	```powershell
	Set-Content "$env:TEMP\shredder-test.txt" "Disposable test content"
	```

2. Start the bridge and open the local page.
3. Choose the test file. The page shows that a local staged copy was created.
4. Select a pass count and choose **Preview run**. Confirm that the preview finishes and the file remains available.
5. Check the confirmation box and choose **Shred staged copy**. Watch the progress bar, pass count, and estimated time.
6. Confirm that the page reports the staged copy was removed. The original `$env:TEMP\shredder-test.txt` should still exist.

The bridge accepts destructive operations only for files inside its staging directory and rejects requests from untrusted web origins. Do not expose or forward its local port to another machine.

## Run the Java CLI

From the project root:

```powershell
Set-Location .\src
javac Main.java
java Main
```

## Example usage

The CLI is interactive. Start it from the `src` directory, enter menu
option `1` for a file or `3` for a dry run, then follow its prompts. Use
only disposable test files when trying destructive operations.

Example disposable-file setup:

```powershell
$testFile = Join-Path $env:TEMP "shredder-test.txt"
Set-Content $testFile "Disposable test content"
Write-Output $testFile
Set-Location .\src
java Main
```

Copy the printed path. In the Java menu, choose option `3` for a dry run
or `1` for shredding, then paste the path when prompted. For a real
operation, verify the path and confirmation prompt carefully.

Folder mode creates a backup before shredding; the backup remains a
separate copy until explicitly removed.

## Safety notes

- This is a local prototype for learning and testing.
- It does not guarantee destruction of every backup, snapshot, or remote copy.
- Always test on disposable files before using it on real data.
- Treat shredding as a security workflow, not a casual delete button.

## Audit log

The tool writes entries to `audit-log.json` in the project directory. Each entry records the time, file path, overwrite passes, status, and message.
