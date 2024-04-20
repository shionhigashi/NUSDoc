/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package entity;

import java.io.Serializable;
import java.util.ArrayList;
import javax.persistence.Entity;
import java.util.List;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

/**
 *
 * @author foozh
 */
@Entity
@NamedQueries({
    @NamedQuery(name = "GeneralPrac.findWithLeastAppointments",
                query = "SELECT COUNT(a.staff), a.staff " +
                        "FROM Appointment a " +
                        "WHERE a.appointDateTime >= :startDate AND a.appointDateTime <= :endDate " +
                        "GROUP BY a.staff " +
                        "HAVING COUNT(a.staff) < 19")
})
public class GeneralPrac extends Staff implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private List<String> queue;

    public GeneralPrac() {
    }

    public GeneralPrac(String firstName, String lastName, String email, String password, int roomNum) {
        super(firstName, lastName, email, password, roomNum);
        this.queue = new ArrayList<String>(19);
    }

    public List<String> getQueue() {
        return queue;
    }

    public void setQueue(List<String> queue) {
        this.queue = queue;
    }   
    
}
