package com.example.aedusapp;

// =============================================
//  CLASE: Launcher
//  Punto de entrada alternativo para arrancar
//  la aplicación sin conflictos de módulos JavaFX.
//  Delega directamente a MainApp.main().
// =============================================
public class Launcher {
    public static void main(String[] args) {
        MainApp.main(args);
    }
}
