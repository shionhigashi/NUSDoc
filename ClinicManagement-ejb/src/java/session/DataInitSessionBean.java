
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package session;

import entity.GeneralPrac;
import entity.Staff;
import enumeration.MedicalSpecialityEnum;
import error.NoResultException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;


import javax.ejb.Singleton;
import javax.ejb.Startup;


/**
 *
 * @author foozh
 */
@Singleton
@Startup
public class DataInitSessionBean {
    
    @EJB
    private GeneralPracSessionLocal generalPracSessionBean;
    
    @EJB
    private StaffSessionLocal staffSessionBean;
    
    @PostConstruct
    public void postConstruct() {
        try {
            generalPracSessionBean.getGeneralPrac(1l);
        } catch (NoResultException ex) {
            doDataInitialisation();
        }
    }
    
    private void doDataInitialisation() {
        // GeneralPrac
        GeneralPrac gp1 = new GeneralPrac("Alex", "Adams", "aa@email.com", "password123", 202);
        generalPracSessionBean.createGeneralPrac(gp1);

        GeneralPrac gp2 = new GeneralPrac("Brenda", "Brown", "bb@email.com", "password123", 204);
        generalPracSessionBean.createGeneralPrac(gp2);

        GeneralPrac gp3 = new GeneralPrac("Chris", "Clark", "cc@email.com", "password123", 205);
        generalPracSessionBean.createGeneralPrac(gp3);

        GeneralPrac gp4 = new GeneralPrac("Diana", "Davis", "dd@email.com", "password123", 206);
        generalPracSessionBean.createGeneralPrac(gp4);

        GeneralPrac gp5 = new GeneralPrac("Ethan", "Evans", "ee@email.com", "password123", 207);
        generalPracSessionBean.createGeneralPrac(gp5);
        
    } // end doDataInitialisation
}
