/**
 * Author: OMAROMAN
 * Date: 1/31/12
 * Time: 3:26 PM
 */

package play.modules.oracle.exceptions;

public class OracleException extends Exception {

    private String mistake;

    //----------------------------------------------
    // Default constructor - initializes instance variable to unknown
    public OracleException() {
        super();             // call superclass constructor
        mistake = "unknown";
    }

    //-----------------------------------------------
    // Constructor receives some kind of message that is saved in an instance variable.
    public OracleException(String err) {
        super(err);     // call super class constructor
        mistake = err;  // save message
    }

    //------------------------------------------------
    // public method, callable by exception catcher. It returns the error message.
    public String getError() {
        return mistake;
    }
}
