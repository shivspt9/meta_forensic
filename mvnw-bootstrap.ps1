#!/usr/bin/env pwsh
<#
Bootstrap script to download the Maven wrapper jar and run it.
Usage: .\mvnw-bootstrap.ps1 -Args '--version'  (powershell)
#>
param(
    [Parameter(ValueFromRemainingArguments=$true)]
    [String[]] $Args
)

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Definition
$wrapperDir = Join-Path $repoRoot ".mvn\wrapper"
if (-not (Test-Path $wrapperDir)) {
    New-Item -ItemType Directory -Path $wrapperDir -Force | Out-Null
}

$jarPath = Join-Path $wrapperDir "maven-wrapper.jar"
if (-not (Test-Path $jarPath)) {
    Write-Output "Downloading maven-wrapper.jar..."
    $url = "https://repo1.maven.org/maven2/io/takari/maven-wrapper/0.5.6/maven-wrapper-0.5.6.jar"
    try {
        Invoke-WebRequest -Uri $url -OutFile $jarPath -UseBasicParsing
    } catch {
        Write-Error "Failed to download maven-wrapper.jar: $_"
        exit 1
    }
}

# Run the wrapper jar with passed arguments
$java = "java"
& $java -jar $jarPath @Args
