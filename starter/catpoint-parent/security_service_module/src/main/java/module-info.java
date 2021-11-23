module com.udacity.catpoint.securityclass {
    requires com.udacity.catpoint.imageclass;
    requires java.desktop;
    requires com.google.gson;
    requires com.miglayout.swing;
    requires java.prefs;
    requires com.google.common;
    opens com.udacity.catpoint.securityclass.data to com.google.gson;
}