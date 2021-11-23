package com.udacity.catpoint.securityclass.service;

import com.udacity.catpoint.imageclass.service.ImageService;
import com.udacity.catpoint.securityclass.application.StatusListener;
import com.udacity.catpoint.securityclass.data.AlarmStatus;
import com.udacity.catpoint.securityclass.data.ArmingStatus;
import com.udacity.catpoint.securityclass.data.SecurityRepository;
import com.udacity.catpoint.securityclass.data.Sensor;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

/**
 * Service that receives information about changes to the security system. Responsible for
 * forwarding updates to the repository and making any decisions about changing the system state.
 *
 * This is the class that should contain most of the business logic for our system, and it is the
 * class you will be writing unit tests for.
 */
public class SecurityService {

    private ImageService imageService;
    private SecurityRepository securityRepository;
    private Set<StatusListener> statusListeners = new HashSet<>();
    private boolean hasCat = false;

    public SecurityService(SecurityRepository securityRepository, ImageService imageService) {
        this.securityRepository = securityRepository;
        this.imageService = imageService;
    }

    /**
     * Sets the current arming status for the system. Changing the arming status
     * may update both the alarm status.
     * @param armingStatus
     */
    public void setArmingStatus(ArmingStatus armingStatus) {
        if (hasCat && armingStatus == ArmingStatus.ARMED_HOME){
            setAlarmStatus(AlarmStatus.ALARM);
        }
        if(armingStatus == ArmingStatus.DISARMED) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        }
        else if (getArmingStatus() == ArmingStatus.DISARMED) {
            if (armingStatus == ArmingStatus.ARMED_AWAY || armingStatus == ArmingStatus.ARMED_HOME) {   //added for requirement 10
                deactivateAllSensor();
            }
        }
        securityRepository.setArmingStatus(armingStatus);
    }

    private void deactivateAllSensor() {     //requirement 10
        for (Sensor s: securityRepository.getSensors()){
            s.setActive(false);
        }
        statusListeners.forEach(StatusListener::sensorStatusChanged);
    }


    /**
     * Internal method that handles alarm status changes based on whether
     * the camera currently shows a cat.
     * @param cat True if a cat is detected, otherwise false.
     */
    private void catDetected(Boolean cat) {
        hasCat = cat;      //requirement 11
        if(cat && getArmingStatus() == ArmingStatus.ARMED_HOME) {
            setAlarmStatus(AlarmStatus.ALARM);
        }
        else {
            if (allSensorDeactivated())         //requirement 8
                setAlarmStatus(AlarmStatus.NO_ALARM);
        }

        statusListeners.forEach(sl -> sl.catDetected(cat));
    }

    private boolean allSensorDeactivated(){    //added for requirement 8
        for(Sensor s : getSensors()){
            if (s.getActive()){
                return false;
            }
        }
        return true;
    }

    /**
     * Register the StatusListener for alarm system updates from within the SecurityService.
     * @param statusListener
     */
    public void addStatusListener(StatusListener statusListener) {
        statusListeners.add(statusListener);
    }

    public void removeStatusListener(StatusListener statusListener) {
        statusListeners.remove(statusListener);
    }

    /**
     * Change the alarm status of the system and notify all listeners.
     * @param status
     */
    public void setAlarmStatus(AlarmStatus status) {
        securityRepository.setAlarmStatus(status);
        statusListeners.forEach(sl -> sl.notify(status));
    }

    /**
     * Internal method for updating the alarm status when a sensor has been activated.
     */
    private void handleSensorActivated() {
        if(securityRepository.getArmingStatus() == ArmingStatus.DISARMED) {
            return; //no problem if the system is disarmed
        }
        switch(securityRepository.getAlarmStatus()) {
            case NO_ALARM -> setAlarmStatus(AlarmStatus.PENDING_ALARM);
            case PENDING_ALARM -> setAlarmStatus(AlarmStatus.ALARM);
        }
    }

    /**
     * Internal method for updating the alarm status when a sensor has been deactivated
     */
    private void handleSensorDeactivated() {
        switch (securityRepository.getAlarmStatus()) {
            case PENDING_ALARM -> setAlarmStatus(AlarmStatus.NO_ALARM);
            case ALARM -> setAlarmStatus(AlarmStatus.PENDING_ALARM);
        }
    }

    /**
     * Change the activation status for the specified sensor and update alarm status if necessary.
     * @param sensor
     * @param active
     */
    public void changeSensorActivationStatus(Sensor sensor, Boolean active) {
        if(!sensor.getActive() && active) {
            handleSensorActivated();
        } else if (sensor.getActive() && !active) {
            if (getAlarmStatus().equals(AlarmStatus.ALARM)) {            //added for requirement 4
                if (allSensorDeactivated(sensor))
                    handleSensorDeactivated();
            }
            else {
                handleSensorDeactivated();
            }
        } else if (sensor.getActive() && active) {                    //added for requirement 5
            handleSensorActivated();
        }
        sensor.setActive(active);
        securityRepository.updateSensor(sensor);

    }

    private boolean allSensorDeactivated(Sensor sensor){
        for (Sensor s: securityRepository.getSensors()){
            if (sensor.compareTo(s)!=0) {
                if (s.getActive()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Send an image to the SecurityService for processing. The securityService will use its provided
     * ImageService to analyze the image for cats and update the alarm status accordingly.
     * @param currentCameraImage
     */
    public void processImage(BufferedImage currentCameraImage) {
        catDetected(imageService.imageContainsCat(currentCameraImage, 50.0f));
    }

    public AlarmStatus getAlarmStatus() {
        return securityRepository.getAlarmStatus();
    }

    public Set<Sensor> getSensors() {
        return securityRepository.getSensors();
    }

    public void addSensor(Sensor sensor) {
        securityRepository.addSensor(sensor);
    }

    public void removeSensor(Sensor sensor) {
        securityRepository.removeSensor(sensor);
    }

    public ArmingStatus getArmingStatus() {
        return securityRepository.getArmingStatus();
    }
}
