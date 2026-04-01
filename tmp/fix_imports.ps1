# Inyectar import de DBConnection en DAOs
$daoFiles = Get-ChildItem -Path "src/main/java/com/example/aedusapp/database/daos/*.java"
foreach ($file in $daoFiles) {
    $content = Get-Content $file.FullName -Raw
    if ($content -notlike "*import com.example.aedusapp.database.config.DBConnection;*") {
        $content = $content -replace "package com.example.aedusapp.database.daos;", "package com.example.aedusapp.database.daos;`n`nimport com.example.aedusapp.database.config.DBConnection;"
        $content | Set-Content $file.FullName -NoNewline
    }
}

# Inyectar imports en DatabaseSetup
$setupFile = "src/main/java/com/example/aedusapp/database/config/DatabaseSetup.java"
if (Test-Path $setupFile) {
    $content = Get-Content $setupFile -Raw
    if ($content -notlike "*import com.example.aedusapp.database.daos.*;*") {
        $content = $content -replace "package com.example.aedusapp.database.config;", "package com.example.aedusapp.database.config;`n`nimport com.example.aedusapp.database.daos.*;"
        $content | Set-Content $setupFile -NoNewline
    }
}

# Inyectar import en AlertUtils
$alertUtilsFile = "src/main/java/com/example/aedusapp/utils/ui/AlertUtils.java"
if (Test-Path $alertUtilsFile) {
    $content = Get-Content $alertUtilsFile -Raw
    if ($content -notlike "*import com.example.aedusapp.utils.config.ThemeManager;*") {
        $content = $content -replace "package com.example.aedusapp.utils.ui;", "package com.example.aedusapp.utils.ui;`n`nimport com.example.aedusapp.utils.config.ThemeManager;"
        $content | Set-Content $alertUtilsFile -NoNewline
    }
}
