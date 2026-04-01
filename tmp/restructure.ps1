$replacements = [ordered]@{
    "com.example.aedusapp.database.AulaDAO" = "com.example.aedusapp.database.daos.AulaDAO"
    "com.example.aedusapp.database.IncidenciaDAO" = "com.example.aedusapp.database.daos.IncidenciaDAO"
    "com.example.aedusapp.database.LogDAO" = "com.example.aedusapp.database.daos.LogDAO"
    "com.example.aedusapp.database.MensajeDAO" = "com.example.aedusapp.database.daos.MensajeDAO"
    "com.example.aedusapp.database.TiendaDAO" = "com.example.aedusapp.database.daos.TiendaDAO"
    "com.example.aedusapp.database.TransaccionAeduDAO" = "com.example.aedusapp.database.daos.TransaccionAeduDAO"
    "com.example.aedusapp.database.UsuarioDAO" = "com.example.aedusapp.database.daos.UsuarioDAO"
    "com.example.aedusapp.database.DBConnection" = "com.example.aedusapp.database.config.DBConnection"
    "com.example.aedusapp.database.DatabaseSetup" = "com.example.aedusapp.database.config.DatabaseSetup"
    "com.example.aedusapp.database.CheckDB" = "com.example.aedusapp.database.config.CheckDB"
    "com.example.aedusapp.database.CheckAudioPaths" = "com.example.aedusapp.database.config.CheckAudioPaths"
    "com.example.aedusapp.services.AIService" = "com.example.aedusapp.services.ai.AIService"
    "com.example.aedusapp.services.AudioRecorderService" = "com.example.aedusapp.services.audio.AudioRecorderService"
    "com.example.aedusapp.services.LogService" = "com.example.aedusapp.services.logging.LogService"
    "com.example.aedusapp.services.PostImagesService" = "com.example.aedusapp.services.media.PostImagesService"
    "com.example.aedusapp.utils.AlertUtils" = "com.example.aedusapp.utils.ui.AlertUtils"
    "com.example.aedusapp.utils.ToastNotification" = "com.example.aedusapp.utils.ui.ToastNotification"
    "com.example.aedusapp.utils.SessionManager" = "com.example.aedusapp.utils.config.SessionManager"
    "com.example.aedusapp.utils.ThemeManager" = "com.example.aedusapp.utils.config.ThemeManager"
}

$files = Get-ChildItem -Path "src/main" -Include *.java, *.fxml -Recurse

foreach ($file in $files) {
    Write-Host "Processing $($file.FullName)..."
    $content = Get-Content $file.FullName -Raw
    $changed = $false
    foreach ($key in $replacements.Keys) {
        if ($content -like "*$key*") {
            $content = $content.Replace($key, $replacements[$key])
            $changed = $true
        }
    }
    if ($changed) {
        Write-Host "Saving changes to $($file.FullName)"
        $content | Set-Content $file.FullName -NoNewline
    }
}
