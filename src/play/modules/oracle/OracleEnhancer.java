/**
 * Author: OMAROMAN
 * Date: 1/23/12
 * Time: 4:42 PM
 */

package play.modules.oracle;

import javassist.*;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.EnumMemberValue;
import javassist.bytecode.annotation.IntegerMemberValue;
import javassist.bytecode.annotation.StringMemberValue;
import net.parnassoft.playutilities.EnhancerUtility;
import play.classloading.ApplicationClasses;
import play.classloading.enhancers.Enhancer;
import play.modules.oracle.annotations.NoSequence;
import play.modules.oracle.annotations.Sequence;

import javax.persistence.*;

public class OracleEnhancer extends Enhancer {

    private CtClass ctClass;
    private ConstPool constPool;

    @Override
    public void enhanceThisClass(ApplicationClasses.ApplicationClass appClass) throws Exception {

        // Initialize member fields
        ctClass = makeClass(appClass);
        constPool = ctClass.getClassFile().getConstPool();

        if (!isEnhanceableThisClass()) {
            return; // Do NOT enhance this class
        }

//        injectSequenceGeneratorAnnotation();
        createIdField();
        createIdAccessorMethods();
        create_keyMethod();

        play.Logger.debug("ENHANCED: %s", ctClass.getName());

        // Done - Enhance Class.
        appClass.enhancedByteCode = ctClass.toBytecode();
        ctClass.defrost();
    }

    private boolean isEnhanceableThisClass() throws Exception {
        // Only enhance model classes.
        if (!EnhancerUtility.isAModel(classPool, ctClass)) {
            return false;
        }

        // Only enhance model classes with Entity annotation.
        if (!EnhancerUtility.isAnEntity(ctClass)) {
            return false;
        }

        // Skip enhance model classes if are annotated with @NoTracking
        if (isClassAnnotatedWithNoSequence()) {
            return false;
        }

        // TODO: Skip enhance model classes if inherit from Model

        // TODO: Skip enhance model classes if already has a field annotated with @Id

        // TODO: Skip enhance model classes if already has a field named "id"

        // Do enhance this class
        return true;
    }

    private boolean isClassAnnotatedWithNoSequence() throws Exception {
        return EnhancerUtility.hasAnnotation(ctClass, NoSequence.class.getName());
    }

    private boolean isClassAnnotatedWithSequence() throws Exception {
        return EnhancerUtility.hasAnnotation(ctClass, Sequence.class.getName());
    }
    
    private String obtainSequenceName() {
        try {
            // 1. First try to get an explicit sequenceName
            String seqName = getSequenceName();
            if (seqName != null && !seqName.isEmpty()) {
                return seqName;    
            }

            // 2. Second try to get an explicit tableName
            String tableName = getTableName();
            if (tableName != null && !tableName.isEmpty()) {
                return String.format("%s_seq", tableName); // TODO: What if table_name_seq > 30
            }
        } catch (Exception e) {
            // Do nothing...
        }

        // 3. If there's no sequenceName or tableName then create a default name
        return String.format("%s_seq", ctClass.getSimpleName()); // TODO: What if model_name_seq > 30
    }

    private String getSequenceName() throws Exception {
        String name = null;
        if (isClassAnnotatedWithSequence()) {
            Sequence sequence = (Sequence) EnhancerUtility.getAnnotation(ctClass, Sequence.class);
            name = sequence.name();
        }
        return name;               
    }

    private String getTableName() throws ClassNotFoundException {
        Table table = (Table) EnhancerUtility.getAnnotation(ctClass, Table.class);
        if (table != null) {
            return table.name();
        }
        return null;
    }

    private String obtainGeneratorName() {
        try {
            // 1. First try to get an explicit tableName
            String tableName = getTableName();
            if (tableName != null && !tableName.isEmpty()) {
                return String.format("%s_gen", tableName); // TODO: What if table_name_seq > 30
            }
        } catch (Exception e) {
            // Do nothing...
        }

        // 3. If there's no sequenceName or tableName then create a default name
        return String.format("%s_gen", ctClass.getSimpleName()); // TODO: What if model_name_seq > 30
    }

    private void createIdField() throws CannotCompileException, NotFoundException {
        // Create track_data field
        String code = "private Long id;";
        CtField id = CtField.make(code, ctClass);
        ctClass.addField(id);

        // NOTE: Just create one and only one instance of AnnotationsAttribute, so that multiple annotations
        // for "id" field are injected correctly
        AnnotationsAttribute attr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);

        Annotation annot;

        // Create SequenceGenerator annotation: @SequenceGenerator(name="seq", sequenceName="db_seq_name", initialValue=1, allocationSize=1)
        annot = new Annotation(SequenceGenerator.class.getName(), constPool);
        // -----
        StringMemberValue stringValue = new StringMemberValue(constPool);
        stringValue.setValue(obtainGeneratorName());
        annot.addMemberValue("name", stringValue);
        attr.addAnnotation(annot);
        // -----
        stringValue = new StringMemberValue(constPool);
        stringValue.setValue(obtainSequenceName());
        annot.addMemberValue("sequenceName", stringValue);
        attr.addAnnotation(annot);
        // -----
        IntegerMemberValue intValue = new IntegerMemberValue(constPool);
        intValue.setValue(1);
        annot.addMemberValue("initialValue", intValue);
        attr.addAnnotation(annot);
        // -----
        intValue = new IntegerMemberValue(constPool);
        intValue.setValue(1);
        annot.addMemberValue("allocationSize", intValue);
        attr.addAnnotation(annot);

        // Annotate id with @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="seq")
        annot = new Annotation(GeneratedValue.class.getName(), constPool);
        // -----
        EnumMemberValue enumValue = new EnumMemberValue(constPool);
        enumValue.setType(GenerationType.class.getName());
        enumValue.setValue(GenerationType.SEQUENCE.name());
        annot.addMemberValue("strategy", enumValue);
        attr.addAnnotation(annot);
//        // -----
        stringValue = new StringMemberValue(constPool);
        stringValue.setValue(obtainGeneratorName());
        annot.addMemberValue("generator", stringValue);
        attr.addAnnotation(annot);

        // Annotate id with @Id
        annot = new Annotation(Id.class.getName(), constPool);
        attr.addAnnotation(annot);

        // Add all annotations to id field
        id.getFieldInfo().addAttribute(attr);
    }

    private void createIdAccessorMethods() throws CannotCompileException {
        String code = "public Long getId(){return id;}";
        final CtMethod getId = CtMethod.make(code, ctClass);
        ctClass.addMethod(getId);
        
        code = "public void setId(Long id){this.id = id;}";
        final CtMethod setId = CtMethod.make(code, ctClass);
        ctClass.addMethod(setId);
    }
    
    
    private void create_keyMethod() throws CannotCompileException {
        String code = "public Object _key(){return getId();}";
        final CtMethod _key = CtMethod.make(code, ctClass);
        ctClass.addMethod(_key);

        Annotation annotation = new Annotation(Override.class.getName(), constPool);
        AnnotationsAttribute attr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
        attr.addAnnotation(annotation);

        _key.getMethodInfo().addAttribute(attr);
    }
}
