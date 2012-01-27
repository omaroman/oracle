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
    public void enhance(ApplicationClasses.ApplicationClass appClass) throws Exception {
        new OracleEnhancer().enhanceThisClass(appClass);
    }
}
