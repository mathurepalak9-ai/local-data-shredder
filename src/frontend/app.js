const filePicker = document.getElementById('filePicker');
const selectedFile = document.getElementById('selectedFile');
const selectedFileName = document.getElementById('selectedFileName');
const selectedFileMeta = document.getElementById('selectedFileMeta');
const stagingState = document.getElementById('stagingState');
const passCount = document.getElementById('passCount');
const confirmStagedCopy = document.getElementById('confirmStagedCopy');
const confirmRow = document.getElementById('confirmRow');
const modePreviewButton = document.getElementById('modePreview');
const modeShredButton = document.getElementById('modeShred');
const runActionButton = document.getElementById('runAction');
const modeHint = document.getElementById('modeHint');
const progressPanel = document.getElementById('progressPanel');
const progressBar = document.getElementById('progressBar');
const progressFill = document.getElementById('progressFill');
const progressPercent = document.getElementById('progressPercent');
const progressDetail = document.getElementById('progressDetail');
const etaText = document.getElementById('etaText');
const operationOutput = document.getElementById('operationOutput');
const activityList = document.getElementById('activityList');
const clearHistoryButton = document.getElementById('clearHistory');
const bridgeStatus = document.getElementById('bridgeStatus');
const bridgeStatusText = document.getElementById('bridgeStatusText');
const proofOutput = document.getElementById('proofOutput');
const proofStatus = document.getElementById('proofStatus');
const themeToggle = document.getElementById('themeToggle');
const fieldNoteText = document.getElementById('fieldNoteText');
const resultPanel = document.getElementById('resultPanel');
const resultStatus = document.getElementById('resultStatus');
const resultOperation = document.getElementById('resultOperation');
const resultFile = document.getElementById('resultFile');
const resultSize = document.getElementById('resultSize');
const resultPasses = document.getElementById('resultPasses');
const resultTarget = document.getElementById('resultTarget');
const resultOriginal = document.getElementById('resultOriginal');
const resultElapsed = document.getElementById('resultElapsed');
const resultNote = document.getElementById('resultNote');

let stagedFilePath = '';
let stagedFileSize = 0;
let operationStartedAt = 0;
let busy = false;
let selectedMode = 'preview';
let currentFieldNote = 0;

const fieldNotes = [
    'Overwriting an SSD file cannot guarantee that every physical block was erased.',
    'This browser workflow removes its local staged copy; the selected original stays in place.',
    'The progress, report, and file processing run through the local service on this computer.'
];

async function requestJson(url, options = {}) {
    const response = await fetch(url, options);
    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
        throw new Error(data.error || 'The local service could not complete the request.');
    }
    return data;
}

function postJson(url, payload) {
    return requestJson(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    });
}

function formatBytes(bytes) {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
    return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`;
}

function formatDuration(seconds) {
    const rounded = Math.max(0, Math.ceil(seconds));
    if (rounded < 60) return `About ${rounded} sec remaining`;
    const minutes = Math.floor(rounded / 60);
    const remainder = rounded % 60;
    return remainder ? `About ${minutes} min ${remainder} sec remaining` : `About ${minutes} min remaining`;
}

function updateControls() {
    const hasFile = Boolean(stagedFilePath);
    filePicker.disabled = busy;
    passCount.disabled = busy;
    modePreviewButton.disabled = busy;
    modeShredButton.disabled = busy;
    confirmRow.hidden = selectedMode !== 'shred';
    confirmStagedCopy.disabled = busy;
    runActionButton.disabled = busy || !hasFile || (selectedMode === 'shred' && !confirmStagedCopy.checked);
    runActionButton.textContent = selectedMode === 'preview' ? 'Run preview' : 'Shred staged copy';
    runActionButton.className = `button ${selectedMode === 'preview' ? 'button-secondary' : 'button-danger'}`;
    modeHint.textContent = selectedMode === 'preview'
        ? 'No data will be overwritten or deleted.'
        : 'Only the local staged copy will be overwritten and removed.';
}

function setOperationMode(mode) {
    selectedMode = mode;
    modePreviewButton.setAttribute('aria-pressed', String(mode === 'preview'));
    modeShredButton.setAttribute('aria-pressed', String(mode === 'shred'));
    modePreviewButton.classList.toggle('is-selected', mode === 'preview');
    modeShredButton.classList.toggle('is-selected', mode === 'shred');
    updateControls();
}

function setTheme(theme, persist = true) {
    const isDark = theme === 'dark';
    document.body.dataset.theme = isDark ? 'dark' : 'light';
    themeToggle.textContent = isDark ? 'Use light mode' : 'Use dark mode';
    themeToggle.setAttribute('aria-pressed', String(isDark));
    document.querySelector('meta[name="theme-color"]').content = isDark ? '#17231c' : '#f1f3ed';
    if (persist) {
        try {
            localStorage.setItem('local-shredder-theme', isDark ? 'dark' : 'light');
        } catch {
            // The theme still works when browser storage is unavailable.
        }
    }
}

function showResult({ status, operation, fileName, size, passes, target, elapsed, note }) {
    resultPanel.hidden = false;
    resultStatus.textContent = status;
    resultStatus.dataset.state = status === 'Complete' ? 'complete' : 'failed';
    resultOperation.textContent = operation;
    resultFile.textContent = fileName;
    resultSize.textContent = formatBytes(size);
    resultPasses.textContent = `${passes} ${passes === 1 ? 'pass' : 'passes'}`;
    resultTarget.textContent = target;
    resultOriginal.textContent = 'Unchanged';
    resultElapsed.textContent = elapsed;
    resultNote.textContent = note;
}

function formatElapsed(startTime) {
    const durationMs = Math.max(0, Date.now() - startTime);
    return durationMs < 1000 ? `${durationMs} ms` : `${(durationMs / 1000).toFixed(1)} sec`;
}

function addActivity(message) {
    const emptyMessage = activityList.querySelector('.empty-activity');
    if (emptyMessage) emptyMessage.remove();

    const entry = document.createElement('li');
    const timestamp = document.createElement('time');
    timestamp.dateTime = new Date().toISOString();
    timestamp.textContent = new Intl.DateTimeFormat(undefined, {
        hour: '2-digit',
        minute: '2-digit'
    }).format(new Date());
    const description = document.createElement('span');
    description.textContent = message;
    entry.append(timestamp, description);
    activityList.prepend(entry);
    clearHistoryButton.disabled = false;

    while (activityList.children.length > 8) {
        activityList.lastElementChild.remove();
    }
}

function clearSessionHistory() {
    const emptyMessage = document.createElement('li');
    emptyMessage.className = 'empty-activity';
    emptyMessage.textContent = 'Session history cleared.';
    activityList.replaceChildren(emptyMessage);
    clearHistoryButton.disabled = true;
}

function setBridgeState(state, message) {
    bridgeStatus.dataset.state = state;
    bridgeStatusText.textContent = message;
}

function resetProgress() {
    progressPanel.hidden = true;
    progressFill.style.width = '0%';
    progressBar.setAttribute('aria-valuenow', '0');
    progressPercent.textContent = '0%';
    progressDetail.textContent = 'Starting local process';
    etaText.textContent = 'Estimating remaining time';
    operationOutput.hidden = true;
    operationOutput.textContent = '';
}

function displayOutput(text) {
    operationOutput.textContent = text.trim() || 'The operation returned no additional details.';
    operationOutput.hidden = false;
}

function displayJobProgress(job) {
    const percent = Math.max(0, Math.min(100, Number(job.percent) || 0));
    progressPanel.hidden = false;
    progressFill.style.width = `${percent}%`;
    progressBar.setAttribute('aria-valuenow', String(percent));
    progressPercent.textContent = `${percent}%`;

    if (job.status === 'complete') {
        progressDetail.textContent = 'Staged copy shredded and removed';
        etaText.textContent = 'Complete';
        return;
    }
    if (job.status === 'failed') {
        progressDetail.textContent = 'Operation stopped with an error';
        etaText.textContent = 'Not completed';
        return;
    }

    progressDetail.textContent = job.currentPass > 0
        ? `Pass ${job.currentPass} of ${job.totalPasses}`
        : 'Preparing file';
    if (percent > 0) {
        const elapsed = (Date.now() - operationStartedAt) / 1000;
        etaText.textContent = formatDuration(elapsed * (100 - percent) / percent);
    } else {
        etaText.textContent = 'Estimating remaining time';
    }
}

async function watchJob(jobId, fileName) {
    try {
        while (true) {
            const job = await requestJson(`/api/jobs/${encodeURIComponent(jobId)}`);
            displayJobProgress(job);

            if (job.status === 'complete') {
                displayOutput(`Shred complete. The staged copy of ${fileName} was removed. The original remains unchanged.`);
                showResult({
                    status: 'Complete',
                    operation: 'Shred staged copy',
                    fileName,
                    size: stagedFileSize,
                    passes: Number(passCount.value),
                    target: 'Overwritten and removed',
                    elapsed: formatElapsed(operationStartedAt),
                    note: 'The original selected in the browser was not modified.'
                });
                addActivity(`Shredded staged copy: ${fileName}`);
                return;
            }
            if (job.status === 'failed') {
                displayOutput(job.error || 'The operation failed.');
                showResult({
                    status: 'Failed',
                    operation: 'Shred staged copy',
                    fileName,
                    size: stagedFileSize,
                    passes: Number(passCount.value),
                    target: 'Copy retained; inspect error',
                    elapsed: formatElapsed(operationStartedAt),
                    note: job.error || 'The operation did not complete.'
                });
                addActivity(`Shred failed: ${fileName}`);
                throw new Error(job.error || 'Shred operation failed.');
            }

            await new Promise(resolve => setTimeout(resolve, 350));
        }
    } finally {
        busy = false;
        updateControls();
    }
}

filePicker.addEventListener('change', async () => {
    const file = filePicker.files[0];
    stagedFilePath = '';
    stagedFileSize = file ? file.size : 0;
    resultPanel.hidden = true;
    resetProgress();

    if (!file) {
        selectedFile.hidden = true;
        updateControls();
        return;
    }

    selectedFile.hidden = false;
    selectedFileName.textContent = file.name;
    selectedFileMeta.textContent = `${formatBytes(file.size)} selected; copying to local staging`;
    stagingState.textContent = 'Staging';
    stagingState.dataset.state = 'staging';
    busy = true;
    updateControls();

    try {
        const formData = new FormData();
        formData.append('file', file);
        const data = await requestJson('/api/upload', { method: 'POST', body: formData });
        stagedFilePath = data.file;
        selectedFileMeta.textContent = `${formatBytes(file.size)} staged locally; original remains unchanged`;
        stagingState.textContent = 'Ready';
        stagingState.dataset.state = 'staged';
        addActivity(`Staged local copy: ${file.name}`);
    } catch (error) {
        selectedFileMeta.textContent = error.message;
        stagingState.textContent = 'Failed';
        stagingState.dataset.state = 'failed';
    } finally {
        busy = false;
        updateControls();
    }
});

modePreviewButton.addEventListener('click', () => setOperationMode('preview'));
modeShredButton.addEventListener('click', () => setOperationMode('shred'));
confirmStagedCopy.addEventListener('change', updateControls);
clearHistoryButton.addEventListener('click', clearSessionHistory);
document.getElementById('nextFieldNote').addEventListener('click', () => {
    currentFieldNote = (currentFieldNote + 1) % fieldNotes.length;
    fieldNoteText.textContent = fieldNotes[currentFieldNote];
});
themeToggle.addEventListener('click', () => {
    setTheme(document.body.dataset.theme === 'dark' ? 'light' : 'dark');
});

runActionButton.addEventListener('click', async () => {
    if (!stagedFilePath || busy) return;
    const fileName = selectedFileName.textContent;
    const passes = Number(passCount.value);
    operationStartedAt = Date.now();
    resultPanel.hidden = true;

    if (selectedMode === 'preview') {
        busy = true;
        updateControls();
        try {
            const data = await postJson('/api/dry-run', { filePath: stagedFilePath, passes });
            if (data.status !== 'complete') {
                throw new Error(data.error || data.stderr || 'Preview did not complete.');
            }
            displayOutput(`Preview complete for ${fileName}.\nPasses selected: ${passes}.\nNo file data was overwritten or deleted.`);
            progressPanel.hidden = false;
            progressPercent.textContent = 'Preview';
            progressDetail.textContent = 'Preview finished; file kept';
            etaText.textContent = 'Dry run does not overwrite or delete the staged copy';
            showResult({
                status: 'Complete',
                operation: 'Preview',
                fileName,
                size: stagedFileSize,
                passes,
                target: 'Staged copy retained',
                elapsed: formatElapsed(operationStartedAt),
                note: 'No bytes were overwritten or deleted. The original remains unchanged.'
            });
            addActivity(`Completed dry run: ${fileName}`);
        } catch (error) {
            progressPanel.hidden = false;
            progressDetail.textContent = error.message;
            etaText.textContent = 'Preview failed';
            showResult({
                status: 'Failed',
                operation: 'Preview',
                fileName,
                size: stagedFileSize,
                passes,
                target: 'Staged copy retained',
                elapsed: formatElapsed(operationStartedAt),
                note: error.message
            });
            addActivity(`Dry run failed: ${fileName}`);
        } finally {
            busy = false;
            updateControls();
        }
        return;
    }

    if (!confirmStagedCopy.checked) return;
    busy = true;
    progressPanel.hidden = false;
    progressPercent.textContent = '0%';
    progressDetail.textContent = 'Starting local process';
    etaText.textContent = 'Estimating remaining time';
    updateControls();

    try {
        const data = await postJson('/api/shred', { filePath: stagedFilePath, passes, confirm: true });
        await watchJob(data.jobId, fileName);
        stagedFilePath = '';
        stagingState.textContent = 'Removed';
        stagingState.dataset.state = 'removed';
        selectedFileMeta.textContent = 'Staged copy removed; original file remains unchanged';
        confirmStagedCopy.checked = false;
    } catch (error) {
        progressPanel.hidden = false;
        progressDetail.textContent = error.message;
        etaText.textContent = 'Not completed';
        if (resultPanel.hidden) {
            showResult({
                status: 'Failed',
                operation: 'Shred staged copy',
                fileName,
                size: stagedFileSize,
                passes,
                target: 'Staged copy retained',
                elapsed: formatElapsed(operationStartedAt),
                note: error.message
            });
        }
        addActivity(`Shred failed: ${fileName}`);
        busy = false;
    } finally {
        updateControls();
    }
});

document.getElementById('generateProof').addEventListener('click', async () => {
    try {
        const data = await requestJson('/api/proof');
        proofOutput.textContent = data.content || 'No report content available.';
        proofOutput.hidden = false;
        proofStatus.textContent = 'Local validation report loaded.';
        addActivity('Viewed validation report');
    } catch (error) {
        proofStatus.textContent = error.message;
    }
});

document.getElementById('exportProof').addEventListener('click', async () => {
    try {
        const response = await fetch('/api/proof-download');
        if (!response.ok) throw new Error('Report export failed.');
        const blob = await response.blob();
        const objectUrl = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = objectUrl;
        link.download = 'validation-proof-report.txt';
        link.click();
        URL.revokeObjectURL(objectUrl);
        proofStatus.textContent = 'Report exported to this device.';
        addActivity('Exported validation report');
    } catch (error) {
        proofStatus.textContent = error.message;
    }
});

requestJson('/api/health')
    .then(() => setBridgeState('ready', 'Local service ready'))
    .catch(() => setBridgeState('offline', 'Local service unavailable'));

let savedTheme = 'light';
try {
    savedTheme = localStorage.getItem('local-shredder-theme') || 'light';
} catch {
    savedTheme = 'light';
}
setTheme(savedTheme, false);
setOperationMode('preview');
updateControls();
