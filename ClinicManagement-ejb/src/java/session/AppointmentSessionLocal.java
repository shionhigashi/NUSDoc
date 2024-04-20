/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/J2EE/EJB30/SessionLocal.java to edit this template
 */
package session;

import entity.Appointment;
import entity.Patient;
import error.NoResultException;
import java.util.Date;
import java.util.List;
import javax.ejb.Local;

/**
 *
 * @author foozh
 */
@Local
public interface AppointmentSessionLocal {

    public void createAppointment(Appointment appointment);

    public void createGeneralPracAppointment(Appointment newAppointment, Long generalPracId, Long patientId);

    public Appointment getAppointment(Long aId) throws NoResultException;

    public List<Appointment> getAppointmentsByDate(Date date);

    public int getCountOfPatientsBeforeAppointmentWithStaff(Date appointmentTime, Patient patient);

    public List<Appointment> getUpcomingAppointmentsForPatient(Patient patient);

    public List<Appointment> getPastAppointmentsForPatient(Patient patient);

    public void updateAppointment(Appointment appointment) throws NoResultException;

    public void updateStatusCheckIn(Appointment a) throws NoResultException;

    public void updateStatusNoShow(Appointment a) throws NoResultException;

    public void updateMessage(Appointment a) throws NoResultException;

    public void deleteAppointment(Long aId) throws NoResultException;
    
}
