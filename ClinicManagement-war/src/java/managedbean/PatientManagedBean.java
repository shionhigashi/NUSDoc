
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSF/JSFManagedBean.java to edit this template
 */
package managedbean;

import entity.Appointment;
import entity.Patient;
import enumeration.AppointmentStatusEnum;
import enumeration.PatientTypeEnum;
import error.NoResultException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import javax.inject.Named;
import java.util.ArrayList;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.Flash;
import javax.inject.Inject;
import session.PatientSessionLocal;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.faces.event.ActionEvent;
import javax.faces.view.ViewScoped;
import javax.servlet.http.Part;
import org.primefaces.component.tabview.TabView;
import org.primefaces.event.TabChangeEvent;
import session.AppointmentSessionLocal;

/**
 *
 * @author 65816
 */
@Named(value = "patientManagedBean")
@ViewScoped
public class PatientManagedBean implements Serializable {

    @EJB
    private AppointmentSessionLocal appointmentSessionLocal;

    @EJB
    private PatientSessionLocal patientSessionLocal;

    private String patientId;
    private Patient loggedInPatient;

    private String firstName;
    private String lastName;
    private String fullName;
    private String profilePhoto;
    private byte gender;
    private String sgender; // gender in string representation
    private String email;
    private String password;
    private String confirmPassword; // used in register.xhtml
    private String contact;
    private PatientTypeEnum patientType;
    private List<Appointment> appointments;
    private List<Appointment> upcomingAppointments;
    private List<Appointment> pastAppointments;

    private Date currentDateTime;
    private boolean editMode = false;
    
    // for uploading of files
    private Part uploadedFile;
    private byte[] uploadedFileContent;

    // for viewing different appointment tabs
    private int activeIndex = 0;;

    private static Logger LOGGER = Logger.getLogger(PatientManagedBean.class.getName());

    @Inject
    private AuthenticationManagedBean authenticationManagedBean;

    public PatientManagedBean() {
    }

    @PostConstruct
    public void init() {
        try {
            setLoggedInPatient(patientSessionLocal.getPatient(authenticationManagedBean.getUserId()));
            setCurrentDateTime(new Date());

            setPatientId(String.valueOf(getLoggedInPatient().getPatientId()));
            setFirstName(getLoggedInPatient().getFirstName());
            setLastName(getLoggedInPatient().getLastName());
            setFullName(getFirstName() + " " + getLastName());
            setProfilePhoto(getLoggedInPatient().getProfilePhoto());
            setGender(getLoggedInPatient().getGender());
            setSgender(getSgender());
            setEmail(getLoggedInPatient().getEmail());
            setPassword(getLoggedInPatient().getPassword());
            setContact(getLoggedInPatient().getContact());
            setPatientType(getLoggedInPatient().getPatientType());
            setAppointments(getLoggedInPatient().getAppointments());

            LOGGER.info("[PatientManaged] Logged in Patient : " + getLoggedInPatient().getFirstName() + getLoggedInPatient().getLastName());
            LOGGER.info("[PatientManaged] Logged in Patient Appointments : " + getLoggedInPatient().getAppointments().toString());
        } catch (NoResultException e) {
            // do nothing
        }

        String tabIndexParam = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("tab");
        if (tabIndexParam != null) {
            try {
                setActiveIndex(Integer.parseInt(tabIndexParam));
            } catch (NumberFormatException e) {
                setActiveIndex(0); // default to the first tab in case of format errors
            }
        }
    } // end init

    public Integer getPatientsInFront() {
        Date today = new Date();
        
        return appointmentSessionLocal.getCountOfPatientsBeforeAppointmentWithStaff(today, getLoggedInPatient());
    } // end getPatientsInFront

    public Integer getWaitingTime() {
        return getPatientsInFront() * 20;
    } // end getWaitingTime

    public Boolean isAppointmentDateToday(List<Appointment> appointments) {
        Date today = new Date();

        for (int i = 0; i < appointments.size(); i++) {
            Date appointmentDate = appointments.get(i).getAppointDateTime();
            Calendar cal1 = Calendar.getInstance();
            cal1.setTime(today);
            Calendar cal2 = Calendar.getInstance();
            cal2.setTime(appointmentDate);

            if (cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                    && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)) {
                if (appointments.get(i).getStatus() == AppointmentStatusEnum.NOTCHECKEDIN) {
                    return true;
                }
            }
        }
        return false;
    } // end isAppointmentDateToday

    public int appointmentRoomNum() {
        Date today = new Date();

        for (int i = 0; i < appointments.size(); i++) {
            Date appointmentDate = appointments.get(i).getAppointDateTime();
            Calendar cal1 = Calendar.getInstance();
            cal1.setTime(today);
            Calendar cal2 = Calendar.getInstance();
            cal2.setTime(appointmentDate);

            if (cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                    && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)) {
                if(appointments.get(i).getStatus() == AppointmentStatusEnum.NOTCHECKEDIN) {
                    return appointments.get(i).getStaff().getRoomNumber();
                }
            }
        }
        return 0; 
    }
    
    public void changeTab(TabChangeEvent event) {
        TabView tabView = (TabView) event.getComponent();
        this.setActiveIndex(tabView.getActiveIndex());

        LOGGER.info("[PatientManaged] Active Index : " + this.getActiveIndex());
        
        if (getActiveIndex() == 0) { // Upcoming Appointments tab is clicked
            upcomingAppointments = appointmentSessionLocal.getUpcomingAppointmentsForPatient(getLoggedInPatient());
        } else if (getActiveIndex() == 1) { // Past Appointments tab is clicked
            pastAppointments = appointmentSessionLocal.getPastAppointmentsForPatient(getLoggedInPatient());
        }
    } // end changeTab

    public boolean isPasswordStrong(String password) {
        // At least 8 characters, must have both characters and numerics
        String pattern = "^(?=.*[a-zA-Z])(?=.*[0-9]).{8,}$";
        
        return password.matches(pattern);
    } // end isPasswordStrong

    public String formatTodayDate() {
        Date today = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy");
        
        return sdf.format(today);
    } // end formatTodayDate

    public String formatDayOfWeek() {
        Date today = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("EEE");
        
        return sdf.format(today);
    } // end formatDayOfWeek

    public String formatTime() {
        Date today = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a");
        
        return sdf.format(today);
    } // end formatTime

    public void addPatient() throws NoResultException {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();
        Flash flash = externalContext.getFlash();
        flash.setKeepMessages(true);

        if (!isPasswordStrong(password)) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Validation Error", "Password is too weak: Minimum eight characters, inclusive of at least one letter and one number."));
            return;
        }

        List<Patient> patientWithSameName = patientSessionLocal.searchPatients(email);
        if (!patientWithSameName.isEmpty()) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Validation Error", "A user with this email already exists."));
            setFirstName("");
            setLastName("");
            gender = 0;
            setContact("");
            setEmail("");
            setPassword("");
            setPatientType(PatientTypeEnum.GENERAL);
            return;
        }

        Patient p = new Patient();

        p.setFirstName(getFirstName());
        p.setLastName(getLastName());
        // Not setting profilePhoto to any default String to facilitate rendering of emptyProfilePhoto 
        p.setGender(getGender());
        p.setEmail(getEmail());
        p.setPassword(getPassword());
        p.setContact(getContact());
        p.setPatientType(PatientTypeEnum.GENERAL);

        p.setAppointments(new ArrayList<>());
        patientSessionLocal.createPatient(p);
        
        facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "New account created successfully."));
        authenticationManagedBean.setEmail(getEmail());
        authenticationManagedBean.setPassword(getPassword());

        try {
            // Perform the redirect to the same page
            externalContext.redirect(externalContext.getRequestContextPath() + "/register.xhtml");
            setFirstName("");
            setLastName("");
            gender = 0;
            setContact("");
            setEmail("");
            setPassword("");
        } catch (IOException e) {
            // do nothing
        }
    } // end addPatient
   
    public void toggleEditMode() {
        setEditMode(!isEditMode());
    } // end toggleEditMode

    public void saveProfile(ActionEvent evt) throws NoResultException, IOException {
        FacesContext context = FacesContext.getCurrentInstance();
        
        List<Patient> patientsWithSameEmail = patientSessionLocal.searchPatients(getEmail());
        for (Patient existingPatient : patientsWithSameEmail) {
            if (!existingPatient.getPatientId().equals(loggedInPatient.getPatientId())) {
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Validation Error", "A user with this email already exists."));
                setEmail(getLoggedInPatient().getEmail());
            }
        }

        if (getUploadedFile() != null) {
            try (InputStream input = getUploadedFile().getInputStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = input.read(buffer)) != -1) {
                    output.write(buffer, 0, length);
                }
                setUploadedFileContent(output.toByteArray());
            } catch (IOException e) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Validation Error", "No file selected for upload"));
                throw e;
            }
        }
        
        if (getUploadedFileContent() != null) {
            this.setProfilePhoto(Base64.getEncoder().encodeToString(getUploadedFileContent()));
        }

        getLoggedInPatient().setFirstName(getFirstName());
        getLoggedInPatient().setLastName(getLastName());
        getLoggedInPatient().setGender(gender);
        getLoggedInPatient().setProfilePhoto(getProfilePhoto());
        getLoggedInPatient().setEmail(getEmail());
        getLoggedInPatient().setPassword(getPassword());
        getLoggedInPatient().setContact(getContact());
        getLoggedInPatient().setPatientType(getPatientType());
        getLoggedInPatient().setAppointments(getAppointments());
        patientSessionLocal.updatePatient(getLoggedInPatient());
        setEditMode(false);
        context.addMessage(null, new FacesMessage("Success", "Patient account updated successfully"));
        
        init();
    } // end saveProfile

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public Patient getLoggedInPatient() {
        return loggedInPatient;
    }

    public void setLoggedInPatient(Patient loggedInPatient) {
        this.loggedInPatient = loggedInPatient;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getProfilePhoto() {
        return profilePhoto;
    }

    public void setProfilePhoto(String profilePhoto) {
        this.profilePhoto = profilePhoto;
    }
    
    public byte getGender() {
        return gender;
    }

    public void setGender(byte gender) {
        if (gender == 0) {
            setSgender("Male");
        } else if (gender == 1) {
            setSgender("Female");
        } else {
            setSgender("Unknown");
        }
        this.gender = gender;
    }

    public String getSgender() {
        return sgender;
    }

    public void setSgender(String sgender) {
        this.sgender = sgender;
    }
    
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public PatientTypeEnum getPatientType() {
        return patientType;
    }

    public void setPatientType(PatientTypeEnum patientType) {
        this.patientType = patientType;
    }

    public List<Appointment> getAppointments() {
        return appointments;
    }

    public void setAppointments(List<Appointment> appointments) {
        this.appointments = appointments;
    }

    public List<Appointment> getUpcomingAppointments() {
        // will filter for status that is 'NOTCHECKEDIN'
        upcomingAppointments = getAppointments().stream()
                .filter(a -> a.getStatus().equals(AppointmentStatusEnum.NOTCHECKEDIN))
                .collect(Collectors.toList());
        
        return upcomingAppointments;
    }

    public void setUpcomingAppointments(List<Appointment> upcomingAppointments) {
        this.upcomingAppointments = upcomingAppointments;
    }

    public List<Appointment> getPastAppointments() {
        // will filter for status that is NOT 'NOTCHECKEDIN'
        pastAppointments = getAppointments().stream()
                .filter(a -> !a.getStatus().equals(AppointmentStatusEnum.NOTCHECKEDIN))
                .collect(Collectors.toList());
        
        return pastAppointments;
    }

    public void setPastAppointments(List<Appointment> pastAppointments) {
        this.pastAppointments = pastAppointments;
    }

    public Date getCurrentDateTime() {
        return currentDateTime;
    }

    public void setCurrentDateTime(Date currentDateTime) {
        this.currentDateTime = currentDateTime;
    }

    public boolean isEditMode() {
        return editMode;
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }

    public Part getUploadedFile() {
        return uploadedFile;
    }

    public void setUploadedFile(Part uploadedFile) {
        this.uploadedFile = uploadedFile;
    }

    public byte[] getUploadedFileContent() {
        return uploadedFileContent;
    }

    public void setUploadedFileContent(byte[] uploadedFileContent) {
        this.uploadedFileContent = uploadedFileContent;
    }

    public int getActiveIndex() {
        return activeIndex;
    }

    public void setActiveIndex(int activeIndex) {
        this.activeIndex = activeIndex;
    }

}
