<#
.SYNOPSIS
Prints one row per CVE from an OWASP dependency-check JSON report, highest CVSS first.

.DESCRIPTION
Works on Windows PowerShell 5.1 and PowerShell 7. Windows PowerShell's ConvertFrom-Json refuses large
files, so on 5.1 the report is parsed with JavaScriptSerializer and a raised size limit.
#>
param(
    [string] $ReportPath = 'build/reports/dependency-check-report.json'
)

$ErrorActionPreference = 'Stop'

if (-not (Test-Path $ReportPath)) {
    throw "Report not found at $ReportPath. Run ./gradlew dependencyCheckAnalyze first."
}

function Read-DependencyCheckReport([string] $path) {
    $json = Get-Content -Path $path -Raw -Encoding UTF8
    if ($PSVersionTable.PSEdition -eq 'Core') {
        return $json | ConvertFrom-Json
    }
    Add-Type -AssemblyName System.Web.Extensions
    $serializer = New-Object System.Web.Script.Serialization.JavaScriptSerializer
    $serializer.MaxJsonLength = [int]::MaxValue
    return $serializer.DeserializeObject($json)
}

function Get-HighestScore($vulnerability) {
    if ($vulnerability.cvssv3 -and $vulnerability.cvssv3.baseScore) { return [double] $vulnerability.cvssv3.baseScore }
    if ($vulnerability.cvssv4 -and $vulnerability.cvssv4.baseScore) { return [double] $vulnerability.cvssv4.baseScore }
    if ($vulnerability.cvssv2 -and $vulnerability.cvssv2.score) { return [double] $vulnerability.cvssv2.score }
    return $null
}

$report = Read-DependencyCheckReport $ReportPath
$rows = foreach ($dependency in @($report.dependencies)) {
    foreach ($vulnerability in @($dependency.vulnerabilities)) {
        if ($null -eq $vulnerability) { continue }
        $packageIds = @($dependency.packages | ForEach-Object { $_.id }) -join ', '
        [pscustomobject] @{
            Package        = if ($packageIds) { $packageIds } else { $dependency.fileName }
            Cve            = $vulnerability.name
            Severity       = $vulnerability.severity
            Score          = Get-HighestScore $vulnerability
            KnownExploited = [bool] $vulnerability.knownExploitedVulnerability
            Configurations = @($dependency.projectReferences) -join ', '
        }
    }
}

$rows = @($rows)
if ($rows.Count -eq 0) {
    Write-Output "No vulnerabilities reported in $ReportPath."
    return
}

Write-Output "$($rows.Count) vulnerabilities in $(@($rows | Select-Object -ExpandProperty Package -Unique).Count) packages."
$rows | Sort-Object -Property @{ Expression = 'Score'; Descending = $true }, Package, Cve |
    Format-Table -AutoSize -Wrap Package, Cve, Severity, Score, KnownExploited, Configurations
