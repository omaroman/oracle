/**
 * Author: OMAROMAN
 * Date: 1/23/12
 * Time: 4:55 PM
 */

package play.modules.oracle.annotations;


public @interface Sequence {
    public String name() default "";
    public int initValue() default 1;
    public int stepValue() default 1;
}
