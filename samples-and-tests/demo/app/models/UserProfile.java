/**
 * Author: omar2
 * Date: 16/01/12
 * Time: 03:06 PM
 */
package models;

import net.sf.oval.constraint.MaxLength;
import play.data.validation.Max;
import play.data.validation.Min;
import play.data.validation.Required;
import play.db.jpa.GenericModel;
import play.db.jpa.Model;
import play.modules.oracle.annotations.Sequence;

import javax.persistence.*;
import javax.persistence.GeneratedValue;
import javax.persistence.SequenceGenerator;
import java.lang.String;

@Entity
@Table(name = "user_profiles_xtreme_long_name")
@Sequence(name = "my_own_seq", stepValue = 2)
public class UserProfile extends GenericModel {

//    @Id
//    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator = "user_profiles_two_id_seq_gen")
//    @SequenceGenerator(name="user_profiles_two_id_seq_gen", sequenceName = "user_profiles_two_id_seq", initialValue = 1, allocationSize = 1)
//    @Id @GeneratedValue public Long id;
//    @Id @SequenceGenerator(name = "aName", initialValue = 1, allocationSize = 1) public Long id
    //@Id private String id;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id")
    public User user; // = new User();      // belongs_to_one :user

    @Required
    @MaxLength(50)
    public String name;

    public static UserProfile findByUserId(Long user_id) {
        return UserProfile.find("byUser_id", user_id).first();
    }

}
