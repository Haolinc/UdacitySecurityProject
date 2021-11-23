package com.udacity.catpoint.securityclass.service;

import com.udacity.catpoint.imageclass.service.ImageService;
import com.udacity.catpoint.securityclass.application.DisplayPanel;
import com.udacity.catpoint.securityclass.application.SensorPanel;
import com.udacity.catpoint.securityclass.application.StatusListener;
import com.udacity.catpoint.securityclass.data.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTesting {

    SecurityService securityService;
    Sensor motionSensor, doorSensor, windowSensor;


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
    StatusListener statusListener;


    //initialization
    @BeforeEach
    void initializeSecurityServiceAndSensor() {
        securityService = new SecurityService(securityRepository, imageService);
        motionSensor = new Sensor("Motion Sensor", SensorType.MOTION);
        doorSensor = new Sensor("Door Sensor", SensorType.DOOR);
        windowSensor = new Sensor("Window Sensor", SensorType.WINDOW);

    }


    //1
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void armedSensor_activatedSensor_checkPendingAlarmStatus(ArmingStatus armingStatus) {
        when(securityService.getArmingStatus()).thenReturn(armingStatus);
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(motionSensor, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }
    //2
    @Test
    void armedSensor_activatedSensor_pendingAlarmStatus_checkAlarmStatus() {
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME, ArmingStatus.ARMED_AWAY);
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(motionSensor, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }
    //3
    @Test
    void pendingAlarm_inactiveSensor_returnNoAlarmState(){
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        motionSensor.setActive(true);
        securityService.changeSensorActivationStatus(motionSensor, false);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    //4 Implemented in security service
    @Test
    void activeAlarm_checkNoChangeOnAlarmState(){
        InOrder inOrder = Mockito.inOrder(securityRepository);
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME, ArmingStatus.ARMED_AWAY);
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(motionSensor, true);  //Test for first sensor activation

        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(doorSensor, true);    //Test for second sensor activation

        Set<Sensor> sensors = new HashSet<>();
        sensors.add(motionSensor);
        sensors.add(motionSensor);
        when(securityService.getSensors()).thenReturn(sensors);                //Add sample sensor to test

        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(doorSensor, false);   //Test for closing one sensor activation


        inOrder.verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
        inOrder.verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
        inOrder.verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));

    }
    //5 Implemented in security service
    @Test
    void alreadyActiveSensor_activeSensor_pendingAlarmStatus_returnAlarmState(){
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        motionSensor.setActive(true);
        securityService.changeSensorActivationStatus(motionSensor, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }
    //6
    @Test
    void alreadyDeactivatedSensor_deactivateSensor_checkNoChangeOnAlarmState(){
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        motionSensor.setActive(false);
        securityService.changeSensorActivationStatus(motionSensor, false);
        assertEquals(AlarmStatus.NO_ALARM, securityRepository.getAlarmStatus());
    }
    //7
    @Test
    void cameraImageContainCat_armedHomeAlarmState_returnAlarmState() {
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(eq(bufferedImage), anyFloat())).thenReturn(true);
        securityService.processImage(bufferedImage);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);

    }
    //8 Implemented in security service
    @Test
    void cameraImageNoCat_inactiveSensor_returnNoAlarmStatus(){
        Set<Sensor> sensors = new HashSet<>();
        motionSensor.setActive(false);
        doorSensor.setActive(false);
        windowSensor.setActive(false);
        sensors.add(motionSensor);
        sensors.add(doorSensor);
        sensors.add(windowSensor);
        when(securityService.getSensors()).thenReturn(sensors);
        when(imageService.imageContainsCat(eq(bufferedImage), anyFloat())).thenReturn(false);
        securityService.processImage(bufferedImage);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }
    //9
    @Test
    void disarmed_returnNoAlarmStatus(){
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }
    //10   implemented in SecurityService and SensorPanel
    @Test
    void armed_returnInactiveSensor(){
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        Set<Sensor> sensors = new HashSet<>();
        motionSensor.setActive(true);
        doorSensor.setActive(true);
        windowSensor.setActive(true);
        sensors.add(motionSensor);
        sensors.add(doorSensor);
        sensors.add(windowSensor);
        when(securityService.getSensors()).thenReturn(sensors);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        assertFalse(motionSensor.getActive());
        assertFalse(doorSensor.getActive());
        assertFalse(windowSensor.getActive());
    }
    //11  implemented in SecurityService
    @Test
    void armedHome_cameraShowsCat_returnAlarmedAlarmStatus(){
        when(imageService.imageContainsCat(eq(bufferedImage), anyFloat())).thenReturn(true);
        securityService.processImage(bufferedImage);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }
    // --------additional coverages
    @Test
    void alarmedStatus_deactivatedSensor_returnPendingAlarmStatus(){
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        motionSensor.setActive(true);
        securityService.changeSensorActivationStatus(motionSensor, false);
        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }
    @Test      //addSensor method
    void addSensorInSecurityService_checkIfSensorAdded(){
        securityService.addSensor(motionSensor);
        sensorPanel = new SensorPanel(securityService);
        verify(securityRepository).addSensor(motionSensor);
    }
    @Test      //removeSensor method
    void removeSensorInSecurityService_checkIfSensorRemoved(){
        securityService.removeSensor(motionSensor);
        sensorPanel = new SensorPanel(securityService);
        verify(securityRepository).removeSensor(motionSensor);
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
