$ErrorActionPreference = 'Stop'
Push-Location (Split-Path -Parent $PSScriptRoot)
try {
    if (-not $env:MAVEN_OPTS) { $env:MAVEN_OPTS = '-Xmx256m -XX:ReservedCodeCacheSize=32m -XX:TieredStopAtLevel=1 -XX:ActiveProcessorCount=2' }
    $endpoint = docker context inspect --format '{{.Endpoints.docker.Host}}'
    if ($LASTEXITCODE -ne 0) { throw 'Docker Desktop must be running.' }
    $env:DOCKER_HOST = $endpoint.Trim()
    $env:DOCKER_API_VERSION = '1.44'
    & .\mvnw.cmd '-Dapi.version=1.44' '-DforkCount=0' test
    if ($LASTEXITCODE -ne 0) { throw 'Backend tests failed.' }
    Push-Location frontend
    try {
        npm run build
        if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed.' }
        npm audit --audit-level=high
        if ($LASTEXITCODE -ne 0) { throw 'Dependency audit failed.' }
    } finally { Pop-Location }
} finally { Pop-Location }
