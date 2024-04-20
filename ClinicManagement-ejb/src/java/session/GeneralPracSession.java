
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/J2EE/EJB30/StatelessEjbClass.java to edit this template
 */
package session;

import entity.Appointment;
import entity.GeneralPrac;
import entity.Staff;
import enumeration.AppointmentStatusEnum;
import error.NoResultException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
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
public class GeneralPracSession implements GeneralPracSessionLocal {

    @PersistenceContext
    private EntityManager em;

    @Override
    public void createGeneralPrac(GeneralPrac generalPrac) {
        em.persist(generalPrac);
    } //end createGeneralPrac 
    
    @Override
    public GeneralPrac getGeneralPrac(Long gId) throws NoResultException {
        GeneralPrac generalPrac = em.find(GeneralPrac.class, gId);

        if (generalPrac != null) {
            return generalPrac;
        } else {
            throw new NoResultException("Not found");
        }
    } //end getGeneralPrac
    
    @Override
    public GeneralPrac getGeneralPracByEmail(String email) throws NoResultException {
        Query q = em.createQuery("SELECT gp FROM GeneralPrac gp WHERE gp.email = :email")
                .setParameter("email", email);
        
        List<GeneralPrac> generalPracs = q.getResultList();
        
        if (generalPracs != null) {
            return generalPracs.get(0);
        } else {
            throw new NoResultException("Error: GeneralPrac with email " + email + " not found!");
        }
    } // end getGeneralPracByEmail

    @Override
    public Boolean isValidGeneralPrac(String email, String password) {
        Query q = em.createQuery("SELECT gp FROM GeneralPrac gp WHERE gp.email = :email AND gp.password = :password")
                .setParameter("email", email)
                .setParameter("password", password);
        
        return !q.getResultList().isEmpty();
    } // end isValidGeneralPrac
    
    @Override
    public GeneralPrac findDoctorWithLeastAppointments(Date targetDate) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(targetDate);
        cal.set(Calendar.HOUR_OF_DAY, 0); // Set to midnight
        Date startDate = cal.getTime();

        cal.set(Calendar.HOUR_OF_DAY, 23); // Set to end of day
        Date endDate = cal.getTime();

        Query countQuery = em.createNamedQuery("GeneralPrac.findWithLeastAppointments");

        countQuery.setParameter("startDate", startDate, TemporalType.TIMESTAMP);
        countQuery.setParameter("endDate", endDate, TemporalType.TIMESTAMP);
                
        List<Object[]> countResults = countQuery.getResultList();

        // If any GeneralPrac has appointments less than 19, return the one with the least appointments
        if (!countResults.isEmpty()) {
            Query query = em.createQuery(
                    "SELECT a.staff, COUNT(a) AS appointmentCount "
                    + "FROM Appointment a "
                    + "WHERE a.staff IN :staffIds "
                    + "GROUP BY a.staff "
                    + "ORDER BY appointmentCount ASC"
            );

            List<Long> staffIds = countResults.stream()
                    .map(result -> ((Staff) result[1]).getStaffId())
                    .collect(Collectors.toList());

            query.setParameter("staffIds", staffIds);

            List<Object[]> results = query.getResultList();
            if (!results.isEmpty()) {
                Staff staff = (Staff) results.get(0)[0]; 
                return em.find(GeneralPrac.class, staff.getStaffId()); 
            }
        }

        // No GeneralPrac has appointments less than 19, return GeneralPrac with smallest ID
        return em.createQuery(
                "SELECT g FROM GeneralPrac g ORDER BY g.staffId ASC", GeneralPrac.class)
                .setMaxResults(1)
                .getSingleResult();
    } // end findDoctorWithLeastAppointments
    
    @Override
    public void updateGeneralPrac(GeneralPrac generalPrac) throws NoResultException {
        GeneralPrac oldGp = getGeneralPrac(generalPrac.getStaffId());

        oldGp.setQueue(generalPrac.getQueue());
        oldGp.setFirstName(generalPrac.getFirstName());
        oldGp.setLastName(generalPrac.getLastName());
        oldGp.setEmail(generalPrac.getEmail());
        oldGp.setPassword(generalPrac.getPassword());
        oldGp.setAppointments(generalPrac.getAppointments());

        em.merge(oldGp);
    } // end updateGeneralPrac
    
    @Override
    public void deleteGeneralPrac(Long sId) throws NoResultException {

        GeneralPrac generalPrac = em.find(GeneralPrac.class, sId);

        if (generalPrac == null) {
            throw new NoResultException("General Prac not found for ID: " + sId);
        }

        if (generalPrac.getAppointments() != null) {
            new ArrayList<>(generalPrac.getAppointments()).stream().map(appointment -> {
                appointment.setStaff(null);
                return appointment;
            }).forEachOrdered(appointment -> {
                em.merge(appointment);
            });
        }

        em.remove(generalPrac);
    } // end deleteGeneralPrac 
    
    @Override
    public Long getCurrentQueueNumberForDoctor(GeneralPrac generalPrac) {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        
        // Get the list of appointments for the doctor
        List<Appointment> appointments = generalPrac.getAppointments();
        
        // Iterate through the appointments to find the first "NOTCHECKEDIN" appointment for today
        for (Appointment appointment : appointments) {
            Calendar appointmentDate = Calendar.getInstance();
            appointmentDate.setTime(appointment.getAppointDateTime());
            
            // Check if the appointment date is the same as today
            if (appointment.getStatus() == AppointmentStatusEnum.NOTCHECKEDIN &&
                appointmentDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                appointmentDate.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                appointmentDate.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)) {
                // Return the patient ID of the first "NOTCHECKEDIN" appointment
                return appointment.getPatient().getPatientId();
            }
        }
        return 0l; 
    }
}
