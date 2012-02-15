/**
 * Author: omar2
 * Date: 16/01/12
 * Time: 03:06 PM
 */
package models;

import net.sf.oval.constraint.MaxLength;
import play.data.validation.Required;
import play.data.validation.Unique;
import play.db.jpa.GenericModel;

import javax.persistence.*;
import javax.persistence.GeneratedValue;

@Entity
@Table(name = "users")
public class User extends GenericModel {

//    @Id
//    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator = "users_two_id_seq_gen")
//    @SequenceGenerator(name="users_two_id_seq_gen", sequenceName = "users_two_id_seq", initialValue = 1, allocationSize = 1)
//    @Id @GeneratedValue public Long id;
    //@Id public Long id;

    @OneToOne(mappedBy = "user")
    public UserProfile user_profile; // = new UserProfile();     // has_one :user_profile

    @Column(unique = true)
    // --
    @Required
    @Unique
    @MaxLength(20)
    public String username;

//    @Column(unique = true)
//    // --
//    @Required
//    @Unique
//    public String email;

}
