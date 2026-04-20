#requires -Version 5
# Generates 256x256 GUI panel textures for the reprocessing machines.
# Each panel has the standard Minecraft "vanilla" colour palette (beige panel,
# dark slot sockets) plus machine-specific slot positions baked in.

Add-Type -AssemblyName System.Drawing

$outDir = Join-Path $PSScriptRoot '..\src\main\resources\assets\nuclearpowered\textures\gui\container'
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$BG     = [System.Drawing.Color]::FromArgb(198,198,198)   # #C6C6C6 panel fill
$DARK   = [System.Drawing.Color]::FromArgb(85,85,85)      # #555555 panel bottom/right edge
$LIGHT  = [System.Drawing.Color]::FromArgb(255,255,255)   # panel top/left edge
$SLOT   = [System.Drawing.Color]::FromArgb(139,139,139)   # #8B8B8B slot inner fill
$SLOTD  = [System.Drawing.Color]::FromArgb(55,55,55)      # #373737 slot border
$TRANS  = [System.Drawing.Color]::Transparent

function Fill([System.Drawing.Graphics]$g, [System.Drawing.Color]$c, [int]$x, [int]$y, [int]$w, [int]$h) {
    $b = New-Object System.Drawing.SolidBrush $c
    $g.FillRectangle($b, $x, $y, $w, $h)
    $b.Dispose()
}

function DrawSlot([System.Drawing.Graphics]$g, [int]$sx, [int]$sy) {
    # Slot coord is where the 16x16 icon goes. Socket is 18x18 framed at sx-1,sy-1.
    Fill $g $SLOTD ($sx - 1) ($sy - 1) 18 18
    Fill $g $SLOT  $sx $sy 16 16
}

function DrawInset([System.Drawing.Graphics]$g, [int]$x, [int]$y, [int]$w, [int]$h) {
    Fill $g $SLOTD $x $y $w $h
    Fill $g ([System.Drawing.Color]::FromArgb(34,34,34)) ($x + 1) ($y + 1) ($w - 2) ($h - 2)
}

function DrawArrow([System.Drawing.Graphics]$g, [int]$x, [int]$y) {
    # 24-wide by 17-tall standard progress arrow area, drawn as a dim groove
    # that the Screen overlays with its fill animation.
    Fill $g $SLOTD $x $y 24 17
    Fill $g ([System.Drawing.Color]::FromArgb(100,100,100)) ($x + 1) ($y + 1) 22 15
}

function MakePanel($name, [scriptblock]$extra) {
    $bmp = New-Object System.Drawing.Bitmap 256, 256
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = 'None'
    $g.InterpolationMode = 'NearestNeighbor'
    $g.Clear($TRANS)

    # Panel fill 176x166
    Fill $g $BG 0 0 176 166
    # Edges: top + left white, bottom + right dark
    Fill $g $LIGHT 0 0 176 1
    Fill $g $LIGHT 0 0 1 166
    Fill $g $DARK  0 165 176 1
    Fill $g $DARK  175 0 1 166

    # Player inventory 3x9 at (8,84) + hotbar at (8,142)
    for ($row = 0; $row -lt 3; $row++) {
        for ($col = 0; $col -lt 9; $col++) {
            DrawSlot $g (8 + $col * 18) (84 + $row * 18)
        }
    }
    for ($col = 0; $col -lt 9; $col++) {
        DrawSlot $g (8 + $col * 18) 142
    }

    # Machine-specific overlay
    & $extra $g

    $out = Join-Path $outDir "$name.png"
    $bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose()
    $bmp.Dispose()
    Write-Output $out
}

# Helper for the FE bar groove in the right panel, and progress arrow to its left.
function StdFe {
    param([System.Drawing.Graphics]$g)
    DrawInset $g 151 16 14 54
}

# --- Shearer: input (44,35) + two outputs (104,26) (104,44) + FE bar
MakePanel 'shearer' {
    param($g)
    DrawSlot $g 44 35
    DrawSlot $g 104 26
    DrawSlot $g 104 44
    DrawArrow $g 66 33
    StdFe $g
}

# --- Dissolver: input, two outputs, bucket + tank (8,17) + FE bar
MakePanel 'dissolver' {
    param($g)
    DrawSlot $g 44 35
    DrawSlot $g 104 26
    DrawSlot $g 104 44
    DrawSlot $g 134 35
    DrawInset $g 7 16 14 54
    DrawArrow $g 66 33
    StdFe $g
}

# --- Extraction Column: input, 3 stacked outputs, bucket + tank + FE
MakePanel 'extraction_column' {
    param($g)
    DrawSlot $g 44 35
    DrawSlot $g 104 17
    DrawSlot $g 104 35
    DrawSlot $g 104 53
    DrawSlot $g 134 35
    DrawInset $g 7 16 14 54
    DrawArrow $g 66 33
    StdFe $g
}

# --- Cs Column: two stacked inputs (44,26)(44,44) + two stacked outputs (116,26)(116,44) + FE
MakePanel 'cs_column' {
    param($g)
    DrawSlot $g 44 26
    DrawSlot $g 44 44
    DrawSlot $g 116 26
    DrawSlot $g 116 44
    DrawArrow $g 66 33
    StdFe $g
}

# --- Vitrifier: two stacked inputs + single output (116,35) + FE
MakePanel 'vitrifier' {
    param($g)
    DrawSlot $g 44 26
    DrawSlot $g 44 44
    DrawSlot $g 116 35
    DrawArrow $g 66 33
    StdFe $g
}

# --- Cladding Recycler: single input + single output + FE
MakePanel 'cladding_recycler' {
    param($g)
    DrawSlot $g 56 35
    DrawSlot $g 116 35
    DrawArrow $g 78 33
    StdFe $g
}

# --- Cooling Pond: left input queue + right cooling slot + horizontal
#     progress-bar groove between them (player sees rods flow left to right).
MakePanel 'cooling_pond' {
    param($g)
    DrawSlot $g 44 35
    DrawSlot $g 116 35
    DrawInset $g 61 39 54 6
}
