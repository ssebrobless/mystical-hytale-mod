param(
    [ValidateSet("Key", "Text", "LeftClick", "RightClick", "Command", "Jump", "Stomp", "ThirdPerson")]
    [string]$Action = "Key",
    [string]$Keys,
    [string]$Text,
    [int]$DelayMilliseconds = 150
)

$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Windows.Forms

Add-Type @"
using System;
using System.Runtime.InteropServices;

public static class MotmInputWin32 {
    [DllImport("user32.dll")]
    public static extern bool SetForegroundWindow(IntPtr hWnd);

    [DllImport("user32.dll")]
    public static extern void mouse_event(uint dwFlags, uint dx, uint dy, uint dwData, UIntPtr dwExtraInfo);

    [DllImport("user32.dll")]
    public static extern void keybd_event(byte bVk, byte bScan, uint dwFlags, UIntPtr dwExtraInfo);

    public const uint MOUSEEVENTF_LEFTDOWN = 0x0002;
    public const uint MOUSEEVENTF_LEFTUP = 0x0004;
    public const uint MOUSEEVENTF_RIGHTDOWN = 0x0008;
    public const uint MOUSEEVENTF_RIGHTUP = 0x0010;
    public const uint KEYEVENTF_KEYUP = 0x0002;
}
"@

function Focus-Hytale {
    $window = Get-Process -ErrorAction SilentlyContinue |
        Where-Object { $_.MainWindowTitle -eq "Hytale" -and $_.MainWindowHandle -ne 0 } |
        Select-Object -First 1
    if (-not $window) {
        throw "No foregroundable Hytale window found. Load Hytale into the world first."
    }

    [MotmInputWin32]::SetForegroundWindow($window.MainWindowHandle) | Out-Null
    Start-Sleep -Milliseconds 250
    Write-Host "[send-input] Focused Hytale PID=$($window.Id)"
}

function Send-KeyChord([string]$Chord) {
    if ([string]::IsNullOrWhiteSpace($Chord)) {
        throw "Keys cannot be empty for Action=Key."
    }
    [System.Windows.Forms.SendKeys]::SendWait($Chord)
    Start-Sleep -Milliseconds $DelayMilliseconds
}

function Send-Jump {
    [MotmInputWin32]::keybd_event([byte]0x20, 0, 0, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds 120
    [MotmInputWin32]::keybd_event([byte]0x20, 0, [MotmInputWin32]::KEYEVENTF_KEYUP, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds $DelayMilliseconds
}

function Send-TextValue([string]$Value) {
    if ($null -eq $Value) { $Value = "" }
    [System.Windows.Forms.SendKeys]::SendWait($Value)
    Start-Sleep -Milliseconds $DelayMilliseconds
}

function Send-ClipboardText([string]$Value) {
    if ($null -eq $Value) { $Value = "" }
    [System.Windows.Forms.Clipboard]::SetText($Value)
    Start-Sleep -Milliseconds 80
    [System.Windows.Forms.SendKeys]::SendWait("^v")
    Start-Sleep -Milliseconds $DelayMilliseconds
}

function Send-Click([string]$Button) {
    if ($Button -eq "Left") {
        [MotmInputWin32]::mouse_event([MotmInputWin32]::MOUSEEVENTF_LEFTDOWN, 0, 0, 0, [UIntPtr]::Zero)
        Start-Sleep -Milliseconds 80
        [MotmInputWin32]::mouse_event([MotmInputWin32]::MOUSEEVENTF_LEFTUP, 0, 0, 0, [UIntPtr]::Zero)
    } else {
        [MotmInputWin32]::mouse_event([MotmInputWin32]::MOUSEEVENTF_RIGHTDOWN, 0, 0, 0, [UIntPtr]::Zero)
        Start-Sleep -Milliseconds 80
        [MotmInputWin32]::mouse_event([MotmInputWin32]::MOUSEEVENTF_RIGHTUP, 0, 0, 0, [UIntPtr]::Zero)
    }
    Start-Sleep -Milliseconds $DelayMilliseconds
}

Focus-Hytale

switch ($Action) {
    "Key" {
        Send-KeyChord $Keys
    }
    "Text" {
        Send-TextValue $Text
    }
    "LeftClick" {
        Send-Click "Left"
    }
    "RightClick" {
        Send-Click "Right"
    }
    "Command" {
        if ([string]::IsNullOrWhiteSpace($Text)) { throw "Text is required for Action=Command." }
        Send-KeyChord "/"
        Send-ClipboardText $Text
        Send-KeyChord "{ENTER}"
    }
    "Jump" {
        Send-Jump
    }
    "Stomp" {
        Send-Click "Left"
        Start-Sleep -Milliseconds 400
        Send-Jump
        Start-Sleep -Milliseconds 1300
    }
    "ThirdPerson" {
        Send-KeyChord "v"
    }
}

Write-Host "[send-input] Sent $Action"
