$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    New-Item -ItemType Directory -Force build/classes, dist | Out-Null
    $arquivosFonte = @(Get-ChildItem -LiteralPath src -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
    & javac --release 17 -encoding UTF-8 -d build/classes $arquivosFonte
    if ($LASTEXITCODE -ne 0) { throw 'Falha na compilação' }
    Copy-Item -LiteralPath example.sic -Destination build/classes/example.sic -Force
    # Lê as propriedades do ambiente Java sem transformar o stderr nativo em erro do PowerShell 5.
    $processoJava = New-Object System.Diagnostics.Process
    $processoJava.StartInfo.FileName = (Get-Command java).Source
    $processoJava.StartInfo.Arguments = '-XshowSettings:properties -version'
    $processoJava.StartInfo.UseShellExecute = $false
    $processoJava.StartInfo.CreateNoWindow = $true
    $processoJava.StartInfo.RedirectStandardError = $true
    [void] $processoJava.Start()
    $propriedadesJava = $processoJava.StandardError.ReadToEnd()
    $processoJava.WaitForExit()
    if ($processoJava.ExitCode -ne 0) { throw 'Não foi possível inspecionar o ambiente Java' }
    $linhaDiretorioJava = $propriedadesJava -split "`n" | Select-String 'java.home ='
    $diretorioJdk = ($linhaDiretorioJava.ToString() -split ' = ', 2)[1].Trim()
    $processoJava.Dispose()
    $ferramentaJar = Join-Path $diretorioJdk 'bin/jar.exe'
    if (-not (Test-Path -LiteralPath $ferramentaJar)) { throw 'É necessário um JDK completo 17 ou superior para compilar' }
    & $ferramentaJar --create --file dist/sic-xe-simulator.jar --main-class Main -C build/classes .
    if ($LASTEXITCODE -ne 0) { throw 'Falha na criação do JAR' }
    Write-Host 'Gerado dist/sic-xe-simulator.jar'
} finally { Pop-Location }
