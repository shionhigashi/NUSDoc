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
import enumeration.MedicalSpecialityEnum;
import error.NoResultException;
import javax.inject.Named;
import javax.faces.view.ViewScoped;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import org.primefaces.PrimeFaces;
import session.AppointmentSessionLocal;
import session.GeneralPracSessionLocal;
import session.PatientSessionLocal;
import session.StaffSessionLocal;

/**
 *
 * @author nicholas
 */
@Named(value = "appointmentManagedBean")
@ViewScoped
public class AppointmentManagedBean implements Serializable {
    @EJB
    private AppointmentSessionLocal appointmentSessionLocal;
    
    @EJB
    private GeneralPracSessionLocal generalPracSession;
    
    @EJB
    private PatientSessionLocal patientSession;
    
    @EJB
    private StaffSessionLocal staffSession;

    @Inject
    private AuthenticationManagedBean authenticationManagedBean;
    
    @Inject
    private PatientManagedBean patientManagedBean;
    
    private Date createdDateTime;
    private Date appointDateTime;
    private String appointmentType;
    private AppointmentStatusEnum status;
    private String description;
    private Patient patient;
    private Staff staff;

    private Long selectedAppointmentId;
    private Appointment selectedAppointment;
   
    private Long selectedDoctorId;
    private Long selectedSlotId;
    private String SelectedDocId;
    private MedicalSpecialityEnum selectedSpeciality;
    private int selectedSpecialityInt;
    private MedicalSpecialityEnum[] specialityValues = MedicalSpecialityEnum.values();

    private static final Logger LOGGER = Logger.getLogger(AppointmentManagedBean.class.getName());

    public AppointmentManagedBean() {
    }

    @PostConstruct
    public void init() {
        appointmentType = "1";
        //selectedSpeciality = MedicalSpecialityEnum.ORTHOPAEDICS;
        //selectedSpecialityInt = 1;

        String email = authenticationManagedBean.getEmail();
        try {
            patient = patientSession.getPatientByEmail(email);
        } catch (NoResultException ex) {
            Logger.getLogger(AppointmentManagedBean.class.getName()).log(Level.SEVERE, null, ex);
        }
    } // end init

    public void loadSelectedAppointment() {
        try {
            setSelectedAppointment(appointmentSessionLocal.getAppointment(getSelectedAppointmentId()));
            setCreatedDateTime(getSelectedAppointment().getCreatedDateTime());
            setAppointDateTime(getSelectedAppointment().getAppointDateTime());
            setAppointmentType(getSelectedAppointment().getAppointmentType());
            setStatus(getSelectedAppointment().getStatus());
            setDescription(getSelectedAppointment().getDescription());
            //should we call session beans in order to retrieve latest state of patient and staff?
            setPatient(getSelectedAppointment().getPatient());
            setStaff(getSelectedAppointment().getStaff());
        } catch (NoResultException e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Unable to load appointment"));
        }
    } // end loadSelectedAppointment
    
    public void addAppointment(ActionEvent evt) throws NoResultException {
        FacesContext context = FacesContext.getCurrentInstance();

        Appointment a = new Appointment();
        a.setCreatedDateTime(new Date());
        a.setAppointDateTime(getAppointDateTime());
        a.setAppointmentType(getAppointmentType());
        // default appointment status in enumeration should be...?
        a.setStatus(getStatus());
        a.setDescription(getDescription());
        
        a.setPatient(patient);
        patient.getAppointments().add(a);
        patientSession.updatePatient(patient);
        a.setStaff(staff);
        staff.getAppointments().add(a);
        staffSession.updateStaff(staff);
        
        appointmentSessionLocal.createAppointment(a);
        context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Appointment successfully created"));
    } // end addAppointment

    public void bookAppointment() { // for normal general prac booking only
        FacesContext context = FacesContext.getCurrentInstance();
        LOGGER.info("[AppointmentManaged] Starting to book appointment");
        try {
            Date today = new Date();
            Appointment newAppointment = new Appointment();
            newAppointment.setCreatedDateTime(today);
            newAppointment.setAppointDateTime(today);
            newAppointment.setAppointmentType(appointmentType);
            newAppointment.setDescription(description);
            newAppointment.setStatus(AppointmentStatusEnum.NOTCHECKEDIN);
            
            // Assigning GeneralPrac with shortest queue for the day
            GeneralPrac assignedGeneralPrac = generalPracSession.findDoctorWithLeastAppointments(today);
            LOGGER.info("[AppointmentManaged] GeneralPrac with shortest queue fetched with ID: " + assignedGeneralPrac.getStaffId());
            
            appointmentSessionLocal.createGeneralPracAppointment(newAppointment, assignedGeneralPrac.getStaffId(), patient.getPatientId());
            
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Appointment created successfully!"));
            resetForm();
            PrimeFaces.current().ajax().update("appointmentForm");
            context.getApplication().getNavigationHandler().handleNavigation(FacesContext.getCurrentInstance(), null, "/patient/patientAppointments.xhtml?faces-redirect=true");
        } catch (Exception e) {
            LOGGER.severe("Failed to book appointment: " + e.getMessage());
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to book appointment."));
        }
    } // end bookAppointment

    public void editAppointment() {
        FacesContext context = FacesContext.getCurrentInstance();

        selectedAppointment.setDescription(selectedAppointment.getDescription());

        try {
            appointmentSessionLocal.updateAppointment(selectedAppointment);
        } catch (NoResultException e) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Unable to update appointment"));
            return;
        }
        
        context.addMessage(null, new FacesMessage("Success", "Appointment successfully updated"));
    } // end editAppointment

    
    public void loadAppointmentOptions() {
        //System.out.println("Load options for appointment type: " + appointmentType);
        //System.out.println("Selected Speciality: " + selectedSpeciality.toString());
        if (appointmentType.equals("1")) {

            setAppointDateTime(new Date());
            selectedSpeciality = null;
            
            selectedDoctorId = null;
            selectedSlotId = null;
        } else if (appointmentType.equals("2")) {
            //System.out.println("Selected Speciality: " + selectedSpeciality.toString());
            selectedSpeciality = null;
            
            selectedDoctorId = null;
            selectedSlotId = null;
        }
    } // end loadAppointmentOptions
    
    public void deleteAppointment(Appointment a) throws NoResultException {
        FacesContext context = FacesContext.getCurrentInstance();
        a.getStaff().getAppointments().remove(a);
        staffSession.updateStaff(a.getStaff());
        a.getPatient().getAppointments().remove(a);
        patientSession.updatePatient(a.getPatient());
        appointmentSessionLocal.deleteAppointment(a.getAppointmentId());
        patientManagedBean.init();
        context.addMessage(null, new FacesMessage("Success",
                "Successfully delete appointment"));
    } // end deleteAppointment

    public void resetForm() {
        this.appointmentType = "1";
        this.description = "";
    } // end resetForm

    private MedicalSpecialityEnum convertIntToSpecialityEnum(int index) {
        switch (index) {
            case 0:
                return MedicalSpecialityEnum.ORTHOPAEDICS;
            case 1:
                return MedicalSpecialityEnum.PSYCHIATRY;
            case 2:
                return MedicalSpecialityEnum.PHYSIOTHERAPY;
            case 3:
                return MedicalSpecialityEnum.OCCUPATIONAL_HEALTH;
            default:
                throw new IllegalArgumentException("Unknown index for speciality: " + index);
        }
    } // end convertIntToSpecialityEnum
    
    public String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM dd yyyy");
        
        return sdf.format(date);
    } // end formatDate
    
    public Date getCreatedDateTime() {
        return createdDateTime;
    }

    public void setCreatedDateTime(Date createdDateTime) {
        this.createdDateTime = createdDateTime;
    }

    public Date getAppointDateTime() {
        return appointDateTime;
    }

    public void setAppointDateTime(Date appointDateTime) {
        this.appointDateTime = appointDateTime;
    }

    public String getAppointmentType() {
        System.out.println("[AppointmentManaged] Current appointment type: " + appointmentType);
        return appointmentType;
    }

    public void setAppointmentType(String appointmentType) {
        this.appointmentType = appointmentType;
    }

    public AppointmentStatusEnum getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatusEnum status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public Long getSelectedAppointmentId() {
        return selectedAppointmentId;
    }

    public void setSelectedAppointmentId(Long selectedAppointmentId) {
        this.selectedAppointmentId = selectedAppointmentId;
    }

    public Appointment getSelectedAppointment() {
        return selectedAppointment;
    }

    public void setSelectedAppointment(Appointment selectedAppointment) {
        this.selectedAppointment = selectedAppointment;
    }

    
    public Long getSelectedDoctorId() {
        return selectedDoctorId;
    }

    public void setSelectedDoctorId(Long selectedDoctorId) {
        this.selectedDoctorId = selectedDoctorId;
    }

    public Long getSelectedSlotId() {
        return selectedSlotId;
    }

    public void setSelectedSlotId(Long selectedSlotId) {
        this.selectedSlotId = selectedSlotId;
    }

    public MedicalSpecialityEnum[] getSpecialityValues() {
        return specialityValues;
    }

    public void setSpecialityValues(MedicalSpecialityEnum[] specialityValues) {
        this.specialityValues = specialityValues;
        System.out.println("[AppointmentManaged] Selected Speciality here : " + selectedSpeciality);
    }

    public int getSelectedSpecialityInt() {
        return selectedSpecialityInt;
    }

    public void setSelectedSpecialityInt(int selectedSpecialityInt) {
        this.selectedSpecialityInt = selectedSpecialityInt;
    }

    public String getSelectedDocId() {
        return SelectedDocId;
    }

    public void setSelectedDocId(String SelectedDocId) {
        this.SelectedDocId = SelectedDocId;
    }
 
}
