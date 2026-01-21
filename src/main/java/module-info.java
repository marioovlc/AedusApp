module com.example.aedusapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires atlantafx.base;
    requires java.sql;
    requires mysql.connector.j;
    requires transitive javafx.graphics;

    opens com.example.aedusapp to javafx.fxml;
    opens com.example.aedusapp.controllers to javafx.fxml;
    opens com.example.aedusapp.models to javafx.base;

    exports com.example.aedusapp;
}
