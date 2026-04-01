const vscode = require('vscode');
const { spawn, exec } = require('child_process');
const path = require('path');

// 依次尝试候选列表，找到第一个可用的 PowerShell 可执行文件并缓存
// 只缓存成功结果；失败时下次调用会重新检测
let psExeCache = null;

async function findPowerShell() {
    if (psExeCache) return psExeCache;
    for (const exe of ['pwsh', 'powershell']) {
        const found = await new Promise((resolve) => {
            const p = exec(`${exe} -NoProfile -Command exit`, { timeout: 5000, windowsHide: true });
            p.on('close', (code) => resolve(code === 0 ? exe : null));
            p.on('error', () => resolve(null));
        });
        if (found) {
            psExeCache = found;
            return found;
        }
    }
    return null;
}

let daemon = null;
let daemonReady = false;
let daemonStartPromise = null;
let readyResolver = null;
let responseResolver = null;
let outputBuffer = '';

async function startDaemon(scriptPath, tempImageDir, maxCachedImages) {
    const PS_EXE = await findPowerShell();
    if (!PS_EXE) {
        daemonStartPromise = null;
        throw new Error('未找到可用的 PowerShell（pwsh / powershell），请确认已安装');
    }
    daemonReady = false;
    outputBuffer = '';

    const args = ['-STA', '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', scriptPath];
    if (tempImageDir) {
        args.push('-Dir', tempImageDir);
    }
    args.push('-MaxImages', String(maxCachedImages));

    const proc = spawn(PS_EXE, args, {
        windowsHide: true,
        stdio: ['pipe', 'pipe', 'pipe']
    });
    daemon = proc;

    proc.on('error', (err) => {
        if (daemon !== proc) return;
        daemon = null;
        daemonStartPromise = null;
        psExeCache = null; // 清除缓存，下次重新检测
        if (readyResolver) { readyResolver(); readyResolver = null; }
        vscode.window.showErrorMessage('Paste Enhance: 无法启动守护进程 — ' + err.message);
    });

    proc.stdout.setEncoding('utf8');
    proc.stdout.on('data', (data) => {
        if (daemon !== proc) return; // 忽略已被替换的旧进程消息
        outputBuffer += data;
        const lines = outputBuffer.split(/\r?\n/);
        outputBuffer = lines.pop();
        for (const line of lines) {
            const msg = line.trim();
            if (!msg) continue;
            if (!daemonReady) {
                if (msg === 'READY') {
                    daemonReady = true;
                    if (readyResolver) { readyResolver(); readyResolver = null; }
                }
            } else if (responseResolver) {
                responseResolver(msg);
                responseResolver = null;
            }
        }
    });

    proc.stderr.setEncoding('utf8');
    proc.stderr.on('data', (data) => {
        if (daemon !== proc) return;
        vscode.window.showErrorMessage('Paste Enhance daemon 错误: ' + data.trim());
    });

    proc.on('exit', (code) => {
        if (daemon !== proc) return; // 已被替换，忽略
        if (!daemonReady) {
            vscode.window.showErrorMessage(`Paste Enhance: daemon 启动失败，退出码 ${code}`);
        }
        if (readyResolver) { readyResolver(); readyResolver = null; }
        daemon = null;
        daemonReady = false;
        daemonStartPromise = null;
    });

    return new Promise((resolve, reject) => {
        readyResolver = resolve;
        setTimeout(() => {
            if (daemonReady) return; // 已就绪，无需处理
            readyResolver = null;
            daemonStartPromise = null;
            if (daemon === proc) {
                daemon = null;
                proc.kill(); // kill 旧进程，exit 事件因 daemon!==proc 会被忽略
            }
            reject(new Error('守护进程启动超时，请再按一次快捷键重试'));
        }, 25000);
    });
}

async function ensureDaemon(scriptPath, tempImageDir, maxCachedImages) {
    if (daemon && daemonReady) return;
    if (!daemonStartPromise) {
        daemonStartPromise = startDaemon(scriptPath, tempImageDir, maxCachedImages);
    }
    await daemonStartPromise;
}

function sendSave() {
    return new Promise((resolve) => {
        responseResolver = resolve;
        daemon.stdin.write('save\n');
    });
}

const IMAGE_EXTS = /\.(png|jpg|jpeg|gif|bmp|webp|tiff?|heic|heif|avif)$/i;

function isImagePath(p) {
    return IMAGE_EXTS.test(p);
}

function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

function getConfig() {
    const cfg = vscode.workspace.getConfiguration('pasteEnhance');
    return {
        tempImageDir: cfg.get('tempImageDir', ''),
        maxCachedImages: cfg.get('maxCachedImages', 20)
    };
}

function restartDaemon() {
    if (daemon) {
        daemon.kill();
        daemon = null;
    }
    daemonReady = false;
    daemonStartPromise = null;
}

function activate(context) {
    const daemonScript = path.join(context.extensionPath, 'daemon.ps1');
    const { tempImageDir, maxCachedImages } = getConfig();

    ensureDaemon(daemonScript, tempImageDir, maxCachedImages).catch(() => {});

    const configWatcher = vscode.workspace.onDidChangeConfiguration(e => {
        if (e.affectsConfiguration('pasteEnhance')) {
            restartDaemon();
        }
    });
    context.subscriptions.push(configWatcher);

    const disposable = vscode.commands.registerCommand('paste-enhance.paste', async () => {
        const terminal = vscode.window.activeTerminal || vscode.window.terminals[0];
        if (!terminal) {
            vscode.window.showWarningMessage('Paste Enhance: 未找到终端，请先打开一个终端');
            return;
        }

        const { tempImageDir: dir, maxCachedImages: max } = getConfig();
        let statusMsg = null;
        try {
            if (!daemonReady) {
                statusMsg = vscode.window.setStatusBarMessage('$(sync~spin) Paste Enhance 启动中...');
            }
            await ensureDaemon(daemonScript, dir, max);
            statusMsg && statusMsg.dispose(); statusMsg = null;
            const result = await sendSave();
            if (result && result !== 'NONE') {
                const paths = result.split(' ');
                const hasImage = paths.some(isImagePath);
                if (paths.length > 1 && hasImage) {
                    // 含图片时逐个发送并加间隔，让 Claude Code 逐一识别为 [Image #N]
                    for (let i = 0; i < paths.length; i++) {
                        terminal.sendText((i === 0 ? '' : ' ') + paths[i], false);
                        if (i < paths.length - 1 && isImagePath(paths[i])) await sleep(300);
                    }
                } else {
                    terminal.sendText(result, false);
                }
            } else {
                const text = await vscode.env.clipboard.readText();
                if (text) terminal.sendText(text, false);
            }
        } catch (e) {
            statusMsg && statusMsg.dispose();
            vscode.window.showErrorMessage('Paste Enhance: ' + e.message);
        }
    });

    context.subscriptions.push(disposable);
    context.subscriptions.push({ dispose: () => { if (daemon) daemon.kill(); } });
}

function deactivate() {
    if (daemon) daemon.kill();
}

module.exports = { activate, deactivate };
