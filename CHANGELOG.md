# Changelog

## 0.0.2

- Fixed: Plugin did not work on machines where PowerShell 5 execution policy was `Restricted` (default on new Windows installations). The daemon now starts with `-ExecutionPolicy Bypass`, requiring no manual configuration from the user.
- Fixed: Hardcoded PowerShell 5 path replaced with dynamic detection — the plugin now prefers PowerShell 7 (`pwsh`) if available, falling back to PowerShell 5 (`powershell`).

## 0.0.1

- Initial release
