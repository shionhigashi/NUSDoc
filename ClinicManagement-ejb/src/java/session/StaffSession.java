
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/J2EE/EJB30/StatelessEjbClass.java to edit this template
 */
package session;

import entity.Staff;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author foozh
 */
@Stateless
public class StaffSession implements StaffSessionLocal {

    @PersistenceContext
    private EntityManager em;

    @Override
    public void createStaff(Staff staff) {
        em.persist(staff);
    } //end createStaff
    
    @Override
    public void updateStaff(Staff staff) {
        em.merge(staff);
    } //end updateStaff
    
}