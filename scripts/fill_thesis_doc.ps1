[CmdletBinding()]
param(
    [string]$SourceDoc = "E:\bs\_doc_extract\初稿.docx",
    [string]$TargetDoc = "E:\bs\_doc_extract\初稿-插图完成版.docx",
    [string]$ManifestPath = "E:\bs\_doc_extract\generated_figures\manifest.json"
)

$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Drawing

function Get-FigureMap {
    param([string]$Path)
    $manifest = Get-Content -LiteralPath $Path -Raw -Encoding UTF8 | ConvertFrom-Json
    $map = @{}
    foreach ($item in $manifest.figures) {
        $map[$item.number] = @{
            Caption = $item.caption
            File = $item.file
        }
    }
    foreach ($aliasKey in $manifest.aliases.PSObject.Properties.Name) {
        $alias = $manifest.aliases.$aliasKey
        $map[$aliasKey] = @{
            Caption = $alias.caption
            File = $alias.file
        }
    }
    return $map
}

function Find-TextRange {
    param(
        $Document,
        [string]$Text
    )

    $range = $Document.Content
    $finder = $range.Find
    $finder.ClearFormatting() | Out-Null
    $finder.Text = $Text
    $finder.Forward = $true
    $finder.Wrap = 0
    $found = $finder.Execute()
    if (-not $found) {
        throw "未找到锚点文本: $Text"
    }
    return $range
}

function Try-SetCaptionStyle {
    param($Selection)
    try {
        $Selection.Style = "Caption"
    }
    catch {
    }
    $Selection.Font.NameFarEast = "宋体"
    $Selection.Font.Name = "Times New Roman"
    $Selection.Font.Size = 10.5
}

function Insert-FigureAtSelection {
    param(
        $Document,
        $Selection,
        [string]$ImagePath,
        [string]$Caption,
        [double]$TextWidthPoints
    )

    if (-not (Test-Path -LiteralPath $ImagePath)) {
        throw "图片不存在: $ImagePath"
    }

    $bitmap = [System.Drawing.Image]::FromFile($ImagePath)
    try {
        $ratio = [double]$bitmap.Width / [double]$bitmap.Height
    }
    finally {
        $bitmap.Dispose()
    }

    $pageSetup = $Document.PageSetup
    $textHeightPoints = [double]($pageSetup.PageHeight - $pageSetup.TopMargin - $pageSetup.BottomMargin)
    $captionReserve = 42.0
    $remainingOnPage = $textHeightPoints - [double]$Selection.Information(6)
    if ($remainingOnPage -lt 0) {
        $remainingOnPage = $textHeightPoints
    }

    if ($ratio -ge 2.0) {
        $widthPoints = $TextWidthPoints * 0.98
    }
    elseif ($ratio -ge 1.2) {
        $widthPoints = $TextWidthPoints * 0.90
    }
    elseif ($ratio -ge 0.8) {
        $widthPoints = $TextWidthPoints * 0.84
    }
    else {
        $widthPoints = $TextWidthPoints * 0.72
    }

    $predictedHeight = $widthPoints / $ratio
    if (($predictedHeight + $captionReserve) -gt $remainingOnPage) {
        $Selection.InsertBreak(7)
        $Selection.Collapse(0)
        $remainingOnPage = $textHeightPoints
    }

    $maxHeight = [Math]::Max($remainingOnPage - $captionReserve, $textHeightPoints * 0.42)
    if ($predictedHeight -gt $maxHeight) {
        $widthPoints = $maxHeight * $ratio
    }

    $Selection.ParagraphFormat.Alignment = 1
    $shape = $Selection.InlineShapes.AddPicture($ImagePath, $false, $true)
    $shape.LockAspectRatio = -1
    $shape.Width = [math]::Round($WidthPoints, 0)
    $shape.Range.ParagraphFormat.Alignment = 1

    $Selection.Collapse(0)
    $Selection.TypeParagraph()
    Try-SetCaptionStyle -Selection $Selection
    $Selection.ParagraphFormat.Alignment = 1
    $Selection.TypeText($Caption)
    $Selection.TypeParagraph()
    $Selection.TypeParagraph()
}

function Insert-AfterHeading {
    param(
        $Document,
        $Selection,
        [string]$HeadingText,
        [string]$ImagePath,
        [string]$Caption,
        [double]$TextWidthPoints
    )

    $range = Find-TextRange -Document $Document -Text $HeadingText
    $paragraph = $range.Paragraphs.Item(1).Range
    $insertAt = [Math]::Max($paragraph.End - 1, $paragraph.Start)
    $Selection.SetRange($insertAt, $insertAt)
    $Selection.TypeParagraph()
    Insert-FigureAtSelection -Document $Document -Selection $Selection -ImagePath $ImagePath -Caption $Caption -TextWidthPoints $TextWidthPoints
}

function Replace-ParagraphWithFigure {
    param(
        $Document,
        $Selection,
        [string]$PlaceholderText,
        [string]$ImagePath,
        [string]$Caption,
        [double]$TextWidthPoints
    )

    $range = Find-TextRange -Document $Document -Text $PlaceholderText
    $paragraph = $range.Paragraphs.Item(1).Range
    $start = $paragraph.Start
    $paragraph.Delete()
    $Selection.SetRange($start, $start)
    Insert-FigureAtSelection -Document $Document -Selection $Selection -ImagePath $ImagePath -Caption $Caption -TextWidthPoints $TextWidthPoints
}

function Get-TextWidthPoints {
    param($Document)
    $setup = $Document.PageSetup
    return [double]($setup.PageWidth - $setup.LeftMargin - $setup.RightMargin)
}

function Convert-AllShapesToInline {
    param($Document)
    while ($Document.Shapes.Count -gt 0) {
        $Document.Shapes.Item(1).ConvertToInlineShape() | Out-Null
    }
}

function Update-DocumentFields {
    param($Document)
    foreach ($toc in $Document.TablesOfContents) {
        $toc.Update()
    }
    $Document.Fields.Update() | Out-Null
}

if (-not (Test-Path -LiteralPath $SourceDoc)) {
    throw "源文档不存在: $SourceDoc"
}
if (-not (Test-Path -LiteralPath $ManifestPath)) {
    throw "图稿清单不存在: $ManifestPath"
}

$figureMap = Get-FigureMap -Path $ManifestPath

Copy-Item -LiteralPath $SourceDoc -Destination $TargetDoc -Force

$document = $null
$selection = $null
$word = New-Object -ComObject Word.Application
$word.Visible = $false
$word.DisplayAlerts = 0

try {
    $document = $word.Documents.Open($TargetDoc)
    $selection = $word.Selection
    $widthPoints = Get-TextWidthPoints -Document $document

    Insert-AfterHeading -Document $document -Selection $selection -HeadingText "2.1.1 买家用户需求分析" -ImagePath $figureMap["2-1"].File -Caption $figureMap["2-1"].Caption -TextWidthPoints $widthPoints
    Insert-AfterHeading -Document $document -Selection $selection -HeadingText "2.1.3 管理员需求分析" -ImagePath $figureMap["2-2"].File -Caption $figureMap["2-2"].Caption -TextWidthPoints $widthPoints

    Replace-ParagraphWithFigure -Document $document -Selection $selection -PlaceholderText "图3-1 系统总体架构图" -ImagePath $figureMap["3-1"].File -Caption $figureMap["3-1"].Caption -TextWidthPoints $widthPoints
    Replace-ParagraphWithFigure -Document $document -Selection $selection -PlaceholderText "图3-2 系统功能模块图" -ImagePath $figureMap["3-2"].File -Caption $figureMap["3-2"].Caption -TextWidthPoints $widthPoints

    Insert-AfterHeading -Document $document -Selection $selection -HeadingText "3.3 买家用户功能设计" -ImagePath $figureMap["3-3"].File -Caption $figureMap["3-3"].Caption -TextWidthPoints $widthPoints
    Insert-AfterHeading -Document $document -Selection $selection -HeadingText "3.4.1 Steam 绑定与库存同步设计" -ImagePath $figureMap["3-4"].File -Caption $figureMap["3-4"].Caption -TextWidthPoints $widthPoints
    Insert-AfterHeading -Document $document -Selection $selection -HeadingText "3.3.2 饰品详情与订单创建设计" -ImagePath $figureMap["3-5"].File -Caption $figureMap["3-5"].Caption -TextWidthPoints $widthPoints
    Insert-AfterHeading -Document $document -Selection $selection -HeadingText "3.4.2 出售订单与发货流程设计" -ImagePath $figureMap["3-6"].File -Caption $figureMap["3-6"].Caption -TextWidthPoints $widthPoints
    Insert-AfterHeading -Document $document -Selection $selection -HeadingText "3.4.3 收益管理设计" -ImagePath $figureMap["3-7"].File -Caption $figureMap["3-7"].Caption -TextWidthPoints $widthPoints
    Insert-AfterHeading -Document $document -Selection $selection -HeadingText "3.5.2 内容审核与统计设计" -ImagePath $figureMap["3-8"].File -Caption $figureMap["3-8"].Caption -TextWidthPoints $widthPoints
    Insert-AfterHeading -Document $document -Selection $selection -HeadingText "3.5.1 平台运营管理设计" -ImagePath $figureMap["3-9"].File -Caption $figureMap["3-9"].Caption -TextWidthPoints $widthPoints

    Replace-ParagraphWithFigure -Document $document -Selection $selection -PlaceholderText "图3-3 数据库 E-R 关系图" -ImagePath $figureMap["3-10"].File -Caption $figureMap["3-10"].Caption -TextWidthPoints $widthPoints
    Insert-AfterHeading -Document $document -Selection $selection -HeadingText "3.6.2 用户实体设计" -ImagePath $figureMap["3-11"].File -Caption $figureMap["3-11"].Caption -TextWidthPoints $widthPoints
    Insert-AfterHeading -Document $document -Selection $selection -HeadingText "3.6.3 饰品实体设计" -ImagePath $figureMap["3-12"].File -Caption $figureMap["3-12"].Caption -TextWidthPoints $widthPoints
    Insert-AfterHeading -Document $document -Selection $selection -HeadingText "3.6.5 出售订单与交易订单实体设计" -ImagePath $figureMap["3-13"].File -Caption $figureMap["3-13"].Caption -TextWidthPoints $widthPoints
    Insert-AfterHeading -Document $document -Selection $selection -HeadingText "3.6.6 钱包与辅助实体设计" -ImagePath $figureMap["3-14"].File -Caption $figureMap["3-14"].Caption -TextWidthPoints $widthPoints
    Insert-AfterHeading -Document $document -Selection $selection -HeadingText $figureMap["3-14"].Caption -ImagePath $figureMap["3-15"].File -Caption $figureMap["3-15"].Caption -TextWidthPoints $widthPoints

    Replace-ParagraphWithFigure -Document $document -Selection $selection -PlaceholderText "图4-1 交易订单处理流程图" -ImagePath $figureMap["4-1"].File -Caption $figureMap["4-1"].Caption -TextWidthPoints $widthPoints

    Convert-AllShapesToInline -Document $document
    Update-DocumentFields -Document $document
    $document.Save()
    $document.Close()
}
finally {
    if ($null -ne $word) {
        $word.Quit()
    }
    if ($null -ne $selection) {
        [System.Runtime.InteropServices.Marshal]::ReleaseComObject($selection) | Out-Null
    }
    if ($null -ne $document) {
        [System.Runtime.InteropServices.Marshal]::ReleaseComObject($document) | Out-Null
    }
    if ($null -ne $word) {
        [System.Runtime.InteropServices.Marshal]::ReleaseComObject($word) | Out-Null
    }
    [GC]::Collect()
    [GC]::WaitForPendingFinalizers()
}

Write-Output "已生成文档: $TargetDoc"
