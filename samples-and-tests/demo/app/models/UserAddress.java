/**
 * Author: omar2
 * Date: 20/01/12
 * Time: 09:47 AM
 */
package models;

import net.sf.oval.constraint.MaxLength;
import play.data.validation.Required;
import play.db.jpa.GenericModel;

import javax.persistence.*;
import javax.persistence.GeneratedValue;

@Entity
@Table(name = "user_addresses")
public class UserAddress extends GenericModel {

//    @Id
//    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator = "user_addresses_two_id_seq_gen")
//    @SequenceGenerator(name="user_addresses_two_id_seq_gen", sequenceName = "user_addresses_two_id_seq", initialValue = 1, allocationSize = 1)
//    @Id @GeneratedValue public Long id;
    //@Id private Long id;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id")
    public User user; // = new User();      // belongs_to_one :user

    @Required
    @MaxLength(50)
    public String street;

    @Required
    @MaxLength(9999)
    public Integer outer_number;

    public static UserAddress findByUserId(Long user_id) {
        return UserAddress.find("byUser_id", user_id).first();
    }

}
