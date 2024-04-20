
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/J2EE/EJB30/SessionLocal.java to edit this template
 */
package session;

import entity.Staff;
import javax.ejb.Local;

/**
 *
 * @author foozh
 */
@Local
public interface StaffSessionLocal {

    public void createStaff(Staff staff);

    public void updateStaff(Staff staff);

}