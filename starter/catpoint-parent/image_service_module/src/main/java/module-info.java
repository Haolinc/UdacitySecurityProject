module com.udacity.catpoint.imageclass {
    exports com.udacity.catpoint.imageclass.service;
    requires java.desktop;
    requires org.slf4j;
    requires software.amazon.awssdk.auth;
    requires software.amazon.awssdk.core;
    requires software.amazon.awssdk.regions;
    requires software.amazon.awssdk.services.rekognition;
}