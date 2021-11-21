package com.udacity.catpoint.securityclass.service;

import com.udacity.catpoint.imageclass.service.ImageService;
import com.udacity.catpoint.securityclass.application.DisplayPanel;
import com.udacity.catpoint.securityclass.application.SensorPanel;
import com.udacity.catpoint.securityclass.application.StatusListener;
import com.udacity.catpoint.securityclass.data.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.awt.image.BufferedImage;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTesting {

    SecurityService securityService;
    Sensor sensor;


    //Mock objects
    @Mock
    SecurityRepository securityRepository;
    @Mock
    ImageService imageService;
    @Mock
    BufferedImage bufferedImage;
    @Mock
    SensorPanel sensorPanel;
    @Mock
    DisplayPanel displayPanel;
    @Mock
    StatusListener statusListener;


    //initialization
    @BeforeEach
    void initializeSecurityServiceAndSensor() {
        securityService = new SecurityService(securityRepository, imageService);
        sensor = new Sensor("Test", SensorType.MOTION);
    }


    //1
    @Test
    void armedSensor_activatedSensor_checkPendingAlarmStatus() {
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME, ArmingStatus.ARMED_AWAY);
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }
    //2
    @Test
    void armedSensor_activatedSensor_pendingAlarmStatus_checkAlarmStatus() {
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME, ArmingStatus.ARMED_AWAY);
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }
    //3
    @Test
    void pendingAlarm_inactiveSensor_returnNoAlarmState(){
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    //4
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void activeAlarm_checkNoChangeOnAlarmState(boolean valSource){
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor, valSource);
        assertEquals(securityRepository.getAlarmStatus(), AlarmStatus.ALARM);
    }
    //5 Implemented in security service
    @Test
    void alreadyActiveSensor_activeSensor_pendingAlarmStatus_returnAlarmState(){
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }
    //6
    @Test
    void alreadyDeactivatedSensor_deactivateSensor_checkNoChangeOnAlarmState(){
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, false);
        assertEquals(securityRepository.getAlarmStatus(), AlarmStatus.NO_ALARM);
    }
    //7
    @Test
    void cameraImageContainCat_armedHomeAlarmState_returnAlarmState() {
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(eq(bufferedImage), anyFloat())).thenReturn(true);
        securityService.processImage(bufferedImage);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);

    }
    //8
    @Test
    void cameraImageNoCat_inactiveSensor_returnNoAlarmStatus(){
        when(imageService.imageContainsCat(eq(bufferedImage), anyFloat())).thenReturn(false);
        securityService.processImage(bufferedImage);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }
    //9
    @Test
    void disarmed_returnNoAlarmStatus(){
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }
    //10   implemented in SecurityRepository and SecurityService
    @Test
    void armed_returnInactiveSensor(){
        securityService.setArmingStatus(ArmingStatus.ARMED_AWAY);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        verify(securityRepository, times(2)).deactivateAllSensor();
    }
    //11
    @Test
    void armedHome_cameraShowsCat_returnAlarmedAlarmStatus(){
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(eq(bufferedImage), anyFloat())).thenReturn(true);
        securityService.processImage(bufferedImage);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }
    // --------additional coverages
    @Test
    void alarmedStatus_deactivatedSensor_returnPendingAlarmStatus(){
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }
    @Test      //addSensor method
    void addSensorInSecurityService_checkIfSensorAdded(){
        securityService.addSensor(sensor);
        sensorPanel = new SensorPanel(securityService);
        verify(securityRepository).addSensor(sensor);
    }
    @Test      //removeSensor method
    void removeSensorInSecurityService_checkIfSensorRemoved(){
        securityService.removeSensor(sensor);
        sensorPanel = new SensorPanel(securityService);
        verify(securityRepository).removeSensor(sensor);
    }
    @Test     //addStatusListener method
    void addListenerInSecurityService_checkIfListenerAdded(){
        securityService.addStatusListener(statusListener);
    }
    @Test    //removeStatusListener method
    void removeListenerInSecurityService_checkIfListenerRemoved(){
        securityService.removeStatusListener(statusListener);
    }


}
