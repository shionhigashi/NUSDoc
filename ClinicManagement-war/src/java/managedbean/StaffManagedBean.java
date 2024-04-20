/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSF/JSFManagedBean.java to edit this template
 */
package managedbean;

import entity.Appointment;
import entity.GeneralPrac;
import entity.Patient;
import entity.Staff;
import enumeration.AppointmentStatusEnum;
import error.NoResultException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import session.AppointmentSessionLocal;
import session.GeneralPracSessionLocal;

/**
 *
 * @author shionhigashi
 */
@Named(value = "staffManagedBean")
@SessionScoped
public class StaffManagedBean implements Serializable {
    
    @EJB
    private AppointmentSessionLocal appointmentSession;

    @EJB
    private GeneralPracSessionLocal generalPracSession;

    @Inject
    private AuthenticationManagedBean authenticationManagedBean;

    private Long staffId;
    private Staff loggedInStaff;
    private List<Appointment> appointments;
    private List<String> queue;
    
    private Long viewAppointmentId;
    private Appointment selectedAppointmentForViewing;

    public StaffManagedBean() {
    }

    public void loadStaffData() {
        if (authenticationManagedBean.isGeneralPrac()) {
            GeneralPrac generalPrac;
            try {
                generalPrac = generalPracSession.getGeneralPrac(authenticationManagedBean.getUserId());
                setLoggedInStaff(generalPrac);
                
                List<Appointment> notArranged = generalPrac.getAppointments();
                Collections.sort(notArranged, Comparator.comparing(Appointment::getAppointDateTime).reversed());
        
                appointments = notArranged;
            } catch (NoResultException ex) {
                Logger.getLogger(StaffManagedBean.class.getName()).log(Level.SEVERE, null, ex);
            }
        } 
    } // end loadStaffData

    public int calculateTotalAppointmentsForTheDay() {
        int totalAppointments = 0;
        Date today = new Date();
        List<Appointment> appointments = appointmentSession.getAppointmentsByDate(today);
        if (!appointments.isEmpty()) {
            totalAppointments = appointments.size();
        }
        return totalAppointments;
    } // end calculateTotalAppointmentsForTheDay

    public void loadAppointmentForViewing() {
        FacesContext context = FacesContext.getCurrentInstance();
        Long viewAppointmentId = this.getViewAppointmentId();

        if (viewAppointmentId != null) {
            try {
                selectedAppointmentForViewing = appointmentSession.getAppointment(viewAppointmentId);
            } catch (NoResultException ex) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Unable to load appointment details"));
                return;
            }
        }
    } // end loadAppointmentForViewing

    public void checkInPatient() throws NoResultException {
        if (selectedAppointmentForViewing != null) {
            selectedAppointmentForViewing.setStatus(AppointmentStatusEnum.COMPLETED);
            appointmentSession.updateStatusCheckIn(selectedAppointmentForViewing);

            Logger.getLogger(AppointmentManagedBean.class.getName()).log(Level.INFO, "[StaffManaged] Appointment check-in done");
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Patient checked in successfully."));
        } else {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "No appointment selected."));
            Logger.getLogger(AppointmentManagedBean.class.getName()).log(Level.WARNING, "Attempted to check in but no appointment was selected.");
        }
    } // end checkInPatient

    public void markNoShow() throws NoResultException {
        if (selectedAppointmentForViewing != null) {
            appointmentSession.updateStatusNoShow(selectedAppointmentForViewing);
            
            Logger.getLogger(AppointmentManagedBean.class.getName()).log(Level.INFO, "[StaffManaged] Appointment marked as no show done");
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Patient marked as no-show successfuly."));
        } else {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "No appointment selected."));
        }
    } // end markNoShow

    public void sendNextPatient() throws NoResultException {
        List<Appointment> appointments = selectedAppointmentForViewing.getStaff().getAppointments();
        int currentIndex = appointments.indexOf(selectedAppointmentForViewing);

        if (currentIndex >= 0 && currentIndex < appointments.size() - 1) {
            for (int i = currentIndex + 1; i < appointments.size(); i++) {
                Appointment next = appointments.get(i);
                if (next.getStatus().equals(AppointmentStatusEnum.NOTCHECKEDIN)) {
                    next.setMessage("Your appointment is ready. Please enter the doctor's office.");
                    appointmentSession.updateMessage(next);
                    //sendNotification(next.getPatient(), "Your appointment is ready. Please enter the doctor's office.");
                    return;
                }
            }
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Info", "No more appointments to process."));
        } else {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Info", "Current appointment is the last one in your queue."));
        }
    } // end sendNextPatient

    private void sendNotification(Patient patient, String message) {
        // could be set message in patient managed bean - then display on the homepage - refreshed with every view
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Notification", "Message to " + patient.getFirstName() + ": " + message));
    } // end sendNotification

    public String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM dd yyyy");
        
        return sdf.format(date);
    } // end formatDate

    public String getCurrentQueueNumber() {
        Long patientId = generalPracSession.getCurrentQueueNumberForDoctor((GeneralPrac) loggedInStaff);
        return String.valueOf(patientId);
    }
    
    public Long getStaffId() {
        return staffId;
    }

    public void setStaffId(Long staffId) {
        this.staffId = staffId;
    }
    
    public Staff getLoggedInStaff() {
        return loggedInStaff;
    }

    public void setLoggedInStaff(Staff loggedInStaff) {
        this.loggedInStaff = loggedInStaff;
    }

    public List<Appointment> getAppointments() {
        return appointments;
    }

    public void setAppointments(List<Appointment> appointments) {
        this.appointments = appointments;
    }
    
    public List<String> getQueue() {
        return queue;
    }

    public void setQueue(List<String> queue) {
        this.queue = queue;
    }

    public Long getViewAppointmentId() {
        return viewAppointmentId;
    }

    public void setViewAppointmentId(Long viewAppointmentId) {
        this.viewAppointmentId = viewAppointmentId;
    }

    public Appointment getSelectedAppointmentForViewing() {
        return selectedAppointmentForViewing;
    }

    public void setSelectedAppointmentForViewing(Appointment selectedAppointmentForViewing) {
        this.selectedAppointmentForViewing = selectedAppointmentForViewing;
    }

}
