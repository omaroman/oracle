/**
 * Author: OMAROMAN
 * Date: 2/15/12
 * Time: 12:06 PM
 */

package play.modules.oracle;

import play.db.jpa.GenericModel;
import play.modules.oracle.interfaces.IdAccessable;

public class OracleModel extends GenericModel implements IdAccessable {

    // The following methods are intended to ease the develop and compile phases
    // Later, those methods are overridden via bytecode enhancement

    public Long id() {
        return 0L;
    }

    public Long getId() {
        return 0L;
    }

    public void setId(Long id) {
        // Do Nothing
    }
}
