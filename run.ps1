Set-Location $PSScriptRoot
Set-Location "src"
Write-Host "Compiling Java shredder..."
javac Main.java
Write-Host "Running shredder..."
java Main
