# PowerShell script to find Firebase Cloud Functions in your local repositories
# This script searches for common Cloud Functions patterns

Write-Host "Searching for Firebase Cloud Functions..." -ForegroundColor Cyan
Write-Host ""

# Define search paths - adjust these to your local repository locations
$searchPaths = @(
    "$env:USERPROFILE\Documents\GitHub",
    "$env:USERPROFILE\Projects",
    "$env:USERPROFILE\source",
    "C:\Projects",
    "C:\Users\$env:USERNAME\Documents"
)

# Function to search for Cloud Functions
function Find-CloudFunctions {
    param([string]$basePath)
    
    if (-not (Test-Path $basePath)) {
        return
    }
    
    Write-Host "Searching in: $basePath" -ForegroundColor Yellow
    
    # Search for functions directories
    $functionsDirs = Get-ChildItem -Path $basePath -Recurse -Directory -ErrorAction SilentlyContinue | 
        Where-Object { $_.Name -eq "functions" -or $_.Name -eq "cloud-functions" }
    
    foreach ($dir in $functionsDirs) {
        # Check if it contains index.js or index.ts (common Cloud Functions entry points)
        $indexFiles = Get-ChildItem -Path $dir.FullName -Filter "index.*" -File
        
        if ($indexFiles) {
            Write-Host "✓ Found Cloud Functions at: $($dir.FullName)" -ForegroundColor Green
            
            # Check for package.json
            $packageJson = Join-Path $dir.FullName "package.json"
            if (Test-Path $packageJson) {
                Write-Host "  - Contains package.json" -ForegroundColor Gray
                
                # Try to read and check for firebase-functions
                try {
                    $content = Get-Content $packageJson -Raw | ConvertFrom-Json
                    if ($content.dependencies.'firebase-functions') {
                        Write-Host "  - Uses firebase-functions package ✓" -ForegroundColor Green
                    }
                } catch {
                    # Ignore JSON parse errors
                }
            }
            
            # List key files
            $keyFiles = Get-ChildItem -Path $dir.FullName -File | 
                Where-Object { $_.Name -match "\.(js|ts)$" } | 
                Select-Object -First 5
            
            if ($keyFiles) {
                Write-Host "  - Key files:" -ForegroundColor Gray
                foreach ($file in $keyFiles) {
                    Write-Host "    • $($file.Name)" -ForegroundColor DarkGray
                }
            }
            
            Write-Host ""
        }
    }
}

# Search in all defined paths
foreach ($path in $searchPaths) {
    Find-CloudFunctions -basePath $path
}

# Also search for files containing "createBookingPaymentIntent"
Write-Host "Searching for createBookingPaymentIntent function..." -ForegroundColor Cyan
Write-Host ""

foreach ($path in $searchPaths) {
    if (-not (Test-Path $path)) {
        continue
    }
    
    $files = Get-ChildItem -Path $path -Recurse -File -Include "*.js","*.ts" -ErrorAction SilentlyContinue |
        Select-String -Pattern "createBookingPaymentIntent" -List
    
    foreach ($match in $files) {
        Write-Host "✓ Found createBookingPaymentIntent in: $($match.Path)" -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "Search complete!" -ForegroundColor Cyan
Write-Host ""
Write-Host "If you found the Cloud Functions directory, please provide the full path." -ForegroundColor Yellow
