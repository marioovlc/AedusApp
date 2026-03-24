module com.example.aedusapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires atlantafx.base;
    requires java.sql;
    requires java.desktop;
    requires org.postgresql.jdbc;
    requires transitive javafx.graphics;
    requires org.slf4j;
    requires ch.qos.logback.classic;
    requires ch.qos.logback.core;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;
    requires org.kordamp.ikonli.antdesignicons;
    requires org.controlsfx.controls;

    requires java.net.http;
    requires com.google.gson;
    requires spring.security.crypto;
    requires io.github.cdimascio.dotenv.java;
    requires com.zaxxer.hikari;
    requires java.prefs;

    opens com.example.aedusapp to javafx.fxml;
    opens com.example.aedusapp.controllers.auth to javafx.fxml;
    opens com.example.aedusapp.controllers.incidencias to javafx.fxml;
    opens com.example.aedusapp.controllers.usuarios to javafx.fxml;
    opens com.example.aedusapp.controllers.general to javafx.fxml;
    opens com.example.aedusapp.controllers.logs to javafx.fxml;
    opens com.example.aedusapp.components to javafx.fxml;
    opens com.example.aedusapp.models to javafx.base;

    exports com.example.aedusapp;
}
