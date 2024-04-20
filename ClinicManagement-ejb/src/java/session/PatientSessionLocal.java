/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/J2EE/EJB30/SessionLocal.java to edit this template
 */
package session;

import entity.GeneralPrac;
import entity.Patient;
import error.NoResultException;
import java.util.List;
import javax.ejb.Local;

/**
 *
 * @author foozh
 */
@Local
public interface PatientSessionLocal {

    public void createPatient(Patient patient);

    public List<Patient> searchPatients(String email);

    public Patient getPatient(Long pId) throws NoResultException;

    public Patient getPatientByEmail(String email) throws NoResultException;

    public Boolean isValidPatient(String email, String password);

    public void updatePatient(Patient patient) throws NoResultException;

    public void deletePatient(Long pId) throws NoResultException;

}
