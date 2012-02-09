/**
 * Author: OMAROMAN
 * Date: 1/23/12
 * Time: 4:42 PM
 */

package play.modules.oracle;

import play.PlayPlugin;
import play.classloading.ApplicationClasses;

public class OraclePlugin extends PlayPlugin {

    @Override
    public void onApplicationStart() {
        // Check jpa.ddl= validate | update | create | create-drop | none
        String ddl = play.Play.configuration.getProperty("jpa.ddl", ""); //maybe email
        if (!ddl.equals("none")) {
            // TODO: Validate all explicit table names (if not stated, then model names), sequences names, ids if not inherit from Model
        }
    }

    @Override
    public void enhance(ApplicationClasses.ApplicationClass appClass) throws Exception {
        new OracleEnhancer().enhanceThisClass(appClass);
    }


}
