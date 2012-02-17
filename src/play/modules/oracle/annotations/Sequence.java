/**
 * Author: OMAROMAN
 * Date: 1/23/12
 * Time: 4:55 PM
 */

package play.modules.oracle.annotations;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Sequence {
    public String name() default "";
    public int initValue() default 1;
    public int stepValue() default 1;
}
