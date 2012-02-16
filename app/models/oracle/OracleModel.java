/**
 * Author: OMAROMAN
 * Date: 2/15/12
 * Time: 12:06 PM
 */

package models.oracle;

import play.db.jpa.GenericModel;

import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class OracleModel extends GenericModel {

    @Id
    public Long id;
}
