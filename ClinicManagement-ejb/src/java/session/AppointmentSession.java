
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/J2EE/EJB30/StatelessEjbClass.java to edit this template
 */
package session;

import entity.Appointment;
import entity.GeneralPrac;
import entity.Patient;
import entity.Staff;
import enumeration.AppointmentStatusEnum;
import error.NoResultException;
import java.util.Date;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TemporalType;

/**
 *
 * @author foozh
 */
@Stateless
public class AppointmentSession implements AppointmentSessionLocal {

    @PersistenceContext
    private EntityManager em;

    @Override
    public void createAppointment(Appointment appointment) {
        em.persist(appointment);
    } //end createAppointment
    
    @Override
    public void createGeneralPracAppointment(Appointment newAppointment, Long generalPracId, Long patientId) {
        GeneralPrac generalPrac = em.find(GeneralPrac.class, generalPracId);
        Patient patient = em.find(Patient.class, patientId);
        
        newAppointment.setStaff(generalPrac);
        newAppointment.setPatient(patient);

        // Add the appointment to the GeneralPrac and Patient's lists of appointments
        generalPrac.getAppointments().add(newAppointment);
        patient.getAppointments().add(newAppointment);

        // Persist the new appointment
        em.persist(newAppointment);
        
        System.out.println("[AppointmentSession] Appointment created with GeneralPrac: " + generalPrac.getLastName() + " (ID: " + generalPracId + ")");
    } // end createGeneralPracAppointment

    @Override
    public Appointment getAppointment(Long aId) throws NoResultException {
        Appointment appointment = em.find(Appointment.class, aId);

        if (appointment != null) {
            return appointment;
        } else {
            throw new NoResultException("[AppointmentSession] Appointment not found");
        }
    } //end getAppointment
    
    @Override
    public List<Appointment> getAppointmentsByDate(Date date) {
        Query query = em.createQuery("SELECT a FROM Appointment a WHERE FUNCTION('DATE', a.appointDateTime) = :date");
        query.setParameter("date", date, TemporalType.DATE);
        
        return query.getResultList();
    } // end getAppointmentsByDate
    
    @Override
    public int getCountOfPatientsBeforeAppointmentWithStaff(Date appointmentTime, Patient patient) {
        // Query to fetch the appointment with the matching criteria
        Query appointmentQuery = em.createQuery(
                "SELECT a FROM Appointment a WHERE a.patient = :patient AND FUNCTION('DATE', a.appointDateTime) = FUNCTION('DATE', :appointmentTime)",
                Appointment.class);
        appointmentQuery.setParameter("patient", patient);
        appointmentQuery.setParameter("appointmentTime", appointmentTime);

        // Retrieve the result as a list
        List<Appointment> appointments = appointmentQuery.getResultList();
        if (appointments.isEmpty()) {
            System.out.println("[AppointmentSession] No appointment matches the given criteria.");
            return 0;
        }
        Appointment app = appointments.get(0);  // assuming you still want to handle only the first result if there are multiple

        // Prepare the count query using the details from the first appointment
        Query countQuery = em.createQuery(
                "SELECT COUNT(a) FROM Appointment a WHERE FUNCTION('DATE', a.appointDateTime) = FUNCTION('DATE', :appointmentTime) AND a.appointDateTime < :appointmentTime AND a.staff.staffId = :staffId AND a.status = :status AND CAST(a.appointmentType AS CHAR(1)) = :type AND a.patient <> :currentPatient",
                Long.class);
        countQuery.setParameter("appointmentTime", appointmentTime);
        countQuery.setParameter("staffId", app.getStaff().getStaffId());
        countQuery.setParameter("status", AppointmentStatusEnum.NOTCHECKEDIN);
        countQuery.setParameter("type", app.getAppointmentType());
        countQuery.setParameter("currentPatient", patient);

        // Execute the count query
        Long count = (Long) countQuery.getSingleResult();
        return count != null ? count.intValue() : 0;
    } // end getCountOfPatientsBeforeAppointmentWithStaff

    @Override
    public List<Appointment> getUpcomingAppointmentsForPatient(Patient patient) {
        Query query = em.createQuery("SELECT a FROM Appointment a WHERE a.patient.patientId = :patientId AND a.status = :status");
        query.setParameter("patientId", patient.getPatientId());
        query.setParameter("status", AppointmentStatusEnum.NOTCHECKEDIN);
        
        return query.getResultList();
    } // end getUpcomingAppointmentsForPatient

    @Override
    public List<Appointment> getPastAppointmentsForPatient(Patient patient) {
        Query query = em.createQuery("SELECT a FROM Appointment a WHERE a.patient.patientId = :patientId AND  (a.status = :completedStatus OR a.status = :noShowStatus)");
        query.setParameter("patientId", patient.getPatientId());
        query.setParameter("completedStatus", AppointmentStatusEnum.COMPLETED);
        query.setParameter("noShowStatus", AppointmentStatusEnum.NOSHOW);
        
        return query.getResultList();
    } // end getPastAppointmentsForPatient

    @Override
    public void updateAppointment(Appointment appointment) throws NoResultException {
        Appointment oldA = getAppointment(appointment.getAppointmentId());

        oldA.setDescription(appointment.getDescription());
     
        em.merge(oldA);
    } // end updateAppointment

    @Override
    public void updateStatusCheckIn(Appointment a) throws NoResultException {
        Appointment oldA = getAppointment(a.getAppointmentId());
        
        oldA.setStatus(AppointmentStatusEnum.COMPLETED);
        
        em.merge(oldA);
    } // end updateStatusCheckIn

    @Override
    public void updateStatusNoShow(Appointment a) throws NoResultException {
        Appointment oldA = getAppointment(a.getAppointmentId());
        
        oldA.setStatus(AppointmentStatusEnum.NOSHOW);
        
        em.merge(oldA);
    } // end updateStatusNoShow

    @Override
    public void updateMessage(Appointment a) throws NoResultException {
        Appointment oldA = getAppointment(a.getAppointmentId());
        
        oldA.setMessage(a.getMessage());
        
        em.merge(oldA);
    } // end updateMessage

    @Override
    public void deleteAppointment(Long aId) throws NoResultException {
        Appointment appointment = em.find(Appointment.class, aId);

        if (appointment == null) {
            throw new NoResultException("[AppointmentSession] Appointment not found for ID: " + aId);
        }

        Staff staff = appointment.getStaff();
        if (staff != null) {
            staff.getAppointments().remove(appointment);
            em.merge(staff);
        }
        em.remove(appointment);
    } // end deleteAppointment

}
