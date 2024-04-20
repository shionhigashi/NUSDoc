
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/J2EE/EJB30/StatelessEjbClass.java to edit this template
 */
package session;

import entity.Patient;
import error.NoResultException;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 *
 * @author foozh
 */
@Stateless
public class PatientSession implements PatientSessionLocal {

    @PersistenceContext
    private EntityManager em;

    @Override
    public void createPatient(Patient patient) {
        em.persist(patient);
    } // end createPatient
    
    @Override
    public List<Patient> searchPatients(String email) {
        Query q;
        if (email != null) {
            q = em.createQuery("SELECT p FROM Patient p WHERE "
                    + "LOWER(p.email) LIKE :email");
            q.setParameter("email", "%" + email.toLowerCase() + "%");
        } else {
            q = em.createQuery("SELECT p FROM Patient p");
        }

        return q.getResultList();
    } // end searchPatients

    @Override
    public Patient getPatient(Long pId) throws NoResultException {

        Patient patient = em.find(Patient.class, pId);

        if (patient != null) {
            return patient;
        } else {
            throw new NoResultException("[PatientSession] Patient not found");
        }
    } // end getPatient

    @Override
    public Patient getPatientByEmail(String email) throws NoResultException {
        Query q = em.createQuery("SELECT p FROM Patient p WHERE p.email = :email")
                .setParameter("email", email);

        List<Patient> patients = q.getResultList();

        if (!patients.isEmpty()) {
            return patients.get(0);
        } else {
            throw new NoResultException("[PatientSession] Patient with email " + email + " not found");
        }
    } // end getPatientByEmail

    @Override
    public Boolean isValidPatient(String email, String password) {
        Query q = em.createQuery("SELECT p FROM Patient p WHERE p.email = :email AND p.password = :password")
                .setParameter("email", email)
                .setParameter("password", password);

        return !q.getResultList().isEmpty();
    } // end isValidPatient

    @Override
    public void updatePatient(Patient patient) throws NoResultException {

        Patient patienttoUpdate = getPatient(patient.getPatientId());
        
        patienttoUpdate.setContact(patient.getContact());
        patienttoUpdate.setEmail(patient.getEmail());
        patienttoUpdate.setFirstName(patient.getFirstName());
        patienttoUpdate.setLastName(patient.getLastName());
        patienttoUpdate.setGender(patient.getGender());
        patienttoUpdate.setPassword(patient.getPassword());
        patienttoUpdate.setPatientType(patient.getPatientType());
        patienttoUpdate.setProfilePhoto(patient.getProfilePhoto());
        patienttoUpdate.setAppointments(patient.getAppointments());

        em.merge(patienttoUpdate);
    } // end updatePatient

    @Override
    public void deletePatient(Long pId) throws NoResultException {
        Patient patient = em.find(Patient.class, pId);

        if (patient == null) {
            throw new NoResultException("[PatientSession] Patient not found for ID: " + pId);
        }

        if (patient.getAppointments() != null) {
            new ArrayList<>(patient.getAppointments()).stream().map(appointment -> {
                appointment.setStaff(null);
                return appointment;
            }).forEachOrdered(appointment -> {
                em.merge(appointment);
            });
        }

        em.remove(patient);
    } // end deletePatient

}