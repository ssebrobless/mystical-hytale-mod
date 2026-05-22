param(
    [ValidateSet("Key", "Text", "LeftClick", "RightClick", "Command", "Jump", "Stomp", "ThirdPerson", "Forward", "Back", "StrafeLeft", "StrafeRight", "ForwardJump", "FaceLeft", "FaceRight")]
    [string]$Action = "Key",
    [string]$Keys,
    [string]$Text,
    [int]$DelayMilliseconds = 150,
    [int]$HoldMilliseconds = 650,
    [int]$MouseDelta = 180
)

$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Windows.Forms

Add-Type @"
using System;
using System.Runtime.InteropServices;

public static class MotmInputWin32 {
    [StructLayout(LayoutKind.Sequential)]
    public struct RECT {
        public int Left;
        public int Top;
        public int Right;
        public int Bottom;
    }

    [DllImport("user32.dll")]
    public static extern bool SetForegroundWindow(IntPtr hWnd);

    [DllImport("user32.dll")]
    public static extern bool GetWindowRect(IntPtr hWnd, out RECT rect);

    [DllImport("user32.dll")]
    public static extern bool SetCursorPos(int X, int Y);

    [DllImport("user32.dll")]
    public static extern void mouse_event(uint dwFlags, int dx, int dy, uint dwData, UIntPtr dwExtraInfo);

    [DllImport("user32.dll")]
    public static extern void keybd_event(byte bVk, byte bScan, uint dwFlags, UIntPtr dwExtraInfo);

    public const uint MOUSEEVENTF_LEFTDOWN = 0x0002;
    public const uint MOUSEEVENTF_LEFTUP = 0x0004;
    public const uint MOUSEEVENTF_RIGHTDOWN = 0x0008;
    public const uint MOUSEEVENTF_RIGHTUP = 0x0010;
    public const uint MOUSEEVENTF_MOVE = 0x0001;
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
    return $window
}

function Click-HytaleCenter($Window) {
    $rect = New-Object MotmInputWin32+RECT
    if (-not [MotmInputWin32]::GetWindowRect($Window.MainWindowHandle, [ref]$rect)) {
        return
    }
    $x = [int](($rect.Left + $rect.Right) / 2)
    $y = [int](($rect.Top + $rect.Bottom) / 2)
    [MotmInputWin32]::SetCursorPos($x, $y) | Out-Null
    Start-Sleep -Milliseconds 80
    [MotmInputWin32]::mouse_event([MotmInputWin32]::MOUSEEVENTF_LEFTDOWN, 0, 0, 0, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds 60
    [MotmInputWin32]::mouse_event([MotmInputWin32]::MOUSEEVENTF_LEFTUP, 0, 0, 0, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds 180
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

function Press-Key([byte]$VirtualKey, [int]$HoldMilliseconds = 80) {
    [MotmInputWin32]::keybd_event($VirtualKey, 0, 0, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds $HoldMilliseconds
    [MotmInputWin32]::keybd_event($VirtualKey, 0, [MotmInputWin32]::KEYEVENTF_KEYUP, [UIntPtr]::Zero)
}

function Hold-Key([byte]$VirtualKey, [int]$Milliseconds) {
    [MotmInputWin32]::keybd_event($VirtualKey, 0, 0, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds $Milliseconds
    [MotmInputWin32]::keybd_event($VirtualKey, 0, [MotmInputWin32]::KEYEVENTF_KEYUP, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds $DelayMilliseconds
}

function Send-ForwardJump {
    [MotmInputWin32]::keybd_event([byte]0x57, 0, 0, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds 120
    [MotmInputWin32]::keybd_event([byte]0x20, 0, 0, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds 120
    [MotmInputWin32]::keybd_event([byte]0x20, 0, [MotmInputWin32]::KEYEVENTF_KEYUP, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds ([Math]::Max(250, $HoldMilliseconds))
    [MotmInputWin32]::keybd_event([byte]0x57, 0, [MotmInputWin32]::KEYEVENTF_KEYUP, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds $DelayMilliseconds
}

function Send-MouseNudge([int]$DeltaX) {
    [MotmInputWin32]::mouse_event([MotmInputWin32]::MOUSEEVENTF_MOVE, $DeltaX, 0, 0, [UIntPtr]::Zero)
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

function Send-GameCommand([string]$Value) {
    if ($null -eq $Value) { $Value = "" }
    if ($Value.StartsWith("/")) {
        $Value = $Value.Substring(1)
    }
    [System.Windows.Forms.Clipboard]::SetText($Value)
    Start-Sleep -Milliseconds 80
    Press-Key ([byte]0xBF) 90
    Start-Sleep -Milliseconds 180
    [MotmInputWin32]::keybd_event([byte]0x11, 0, 0, [UIntPtr]::Zero)
    [MotmInputWin32]::keybd_event([byte]0x56, 0, 0, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds 80
    [MotmInputWin32]::keybd_event([byte]0x56, 0, [MotmInputWin32]::KEYEVENTF_KEYUP, [UIntPtr]::Zero)
    [MotmInputWin32]::keybd_event([byte]0x11, 0, [MotmInputWin32]::KEYEVENTF_KEYUP, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds 160
    Press-Key ([byte]0x0D) 90
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

$hytaleWindow = Focus-Hytale

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
        Click-HytaleCenter $hytaleWindow
        Send-GameCommand $Text
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
    "Forward" {
        Hold-Key ([byte]0x57) $HoldMilliseconds
    }
    "Back" {
        Hold-Key ([byte]0x53) $HoldMilliseconds
    }
    "StrafeLeft" {
        Hold-Key ([byte]0x41) $HoldMilliseconds
    }
    "StrafeRight" {
        Hold-Key ([byte]0x44) $HoldMilliseconds
    }
    "ForwardJump" {
        Send-ForwardJump
    }
    "FaceLeft" {
        Send-MouseNudge (-1 * [Math]::Abs($MouseDelta))
    }
    "FaceRight" {
        Send-MouseNudge ([Math]::Abs($MouseDelta))
    }
}

Write-Host "[send-input] Sent $Action"
