module com.example.aedusapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires atlantafx.base;
    requires java.sql;
    requires org.postgresql.jdbc;
    requires transitive javafx.graphics;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;

    opens com.example.aedusapp to javafx.fxml;
    opens com.example.aedusapp.controllers.auth to javafx.fxml;
    opens com.example.aedusapp.controllers.incidencias to javafx.fxml;
    opens com.example.aedusapp.controllers.usuarios to javafx.fxml;
    opens com.example.aedusapp.controllers.logs to javafx.fxml;
    opens com.example.aedusapp.controllers.general to javafx.fxml;
    opens com.example.aedusapp.models to javafx.base;

    exports com.example.aedusapp;
}
