const vscode = require('vscode');
const { spawn } = require('child_process');
const path = require('path');

function findPowerShell() {
    // 优先使用 PS7（pwsh），回退到 PS5（powershell）
    // 直接用命令名，由系统 PATH 解析，不硬编码路径
    const { execSync } = require('child_process');
    try {
        execSync('pwsh -NoProfile -Command exit', { timeout: 3000, windowsHide: true });
        return 'pwsh';
    } catch {
        return 'powershell';
    }
}

const PS_EXE = findPowerShell();

let daemon = null;
let daemonReady = false;
let daemonStartPromise = null;
let readyResolver = null;
let responseResolver = null;
let outputBuffer = '';

function startDaemon(scriptPath, tempImageDir, maxCachedImages) {
    daemonReady = false;
    outputBuffer = '';

    const args = ['-STA', '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', scriptPath];
    if (tempImageDir) {
        args.push('-Dir', tempImageDir);
    }
    args.push('-MaxImages', String(maxCachedImages));

    daemon = spawn(PS_EXE, args, {
        windowsHide: true
    });

    daemon.stdout.setEncoding('utf8');
    daemon.stdout.on('data', (data) => {
        outputBuffer += data;
        const lines = outputBuffer.split(/\r?\n/);
        outputBuffer = lines.pop();
        for (const line of lines) {
            const msg = line.trim();
            if (!msg) continue;
            if (!daemonReady) {
                // 第一条消息固定是 READY，单独处理，不传给 responseResolver
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

    daemon.on('exit', () => {
        daemon = null;
        daemonReady = false;
        daemonStartPromise = null;
    });

    return new Promise((resolve) => { readyResolver = resolve; });
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
        const terminal = vscode.window.activeTerminal;
        if (!terminal) return;

        const { tempImageDir: dir, maxCachedImages: max } = getConfig();
        try {
            await ensureDaemon(daemonScript, dir, max);
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
