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
import play.db.jpa.Model;
import play.modules.oracle.annotations.NoSequence;
import play.modules.oracle.annotations.Sequence;
import play.modules.oracle.exceptions.OracleException;

import javax.persistence.*;
import java.util.Map;

public class OracleEnhancer extends Enhancer {

    private CtClass ctClass;
    private ConstPool constPool;

    private static final String SUFFIX_SEQ = "seq";
    private static final String SUFFIX_GEN = "gen";


    @Override
    public void enhanceThisClass(ApplicationClasses.ApplicationClass appClass) throws Exception {

        // Initialize member fields
        ctClass = makeClass(appClass);
        constPool = ctClass.getClassFile().getConstPool();

        if (!isEnhanceableThisClass()) {
            return; // Do NOT enhance this class
        }

        // Only enhance model if oracle names (table, sequence, etc.) are acceptable (less or equal than 30 chars)
        checkOracleNames();

//        injectSequenceGeneratorAnnotation();
        createIdField();
        createIdAccessorMethods();
        create_keyMethod();

        play.Logger.debug("ENHANCED: %s", ctClass.getName());

        // Done - Enhance Class.
        appClass.enhancedByteCode = ctClass.toBytecode();
        ctClass.defrost();
    }

    private void checkOracleNames() throws Exception {
//        // Check jpa.ddl= validate | update | create | create-drop | none
//        String ddl = play.Play.configuration.getProperty("jpa.ddl", ""); //maybe email
//        if (!ddl.equals("none")) {
//            return;
//        }

//        // Review if
//        Map.Entry<CtClass, CtField> modelField = EnhancerUtility.modelHavingFieldAnnotatedWithId(ctClass);
//        CtClass model = modelField.getKey();
//        CtField field = modelField.getValue();
//        if (!model.getName().equals(Model.class.getName())) {
//            // TODO: What the hell I was gonna do??? Id name?
//            if (field.getName().length() > 30) {
//                // Throw exception
//                throw new OracleException(String.format("Explicit Id Name [%s] exceeds 30 chars", field));
//            }
//        }

        // Review if table name > 30
        String tableName = getTableName();
        if (tableName != null) {
            if (tableName.length() > 30) {
                // Throw exception
                throw new OracleException(String.format("Explicit Table Name [%s] exceeds 30 chars", tableName));
            }
        } else {
           if (getModelName().length() > 30) {
               throw new OracleException(String.format("Implicit Table Name [%s] exceeds 30 chars", getModelName()));
           }
        }

        // Review if Sequence.name > 30
        String sequenceName = getSequenceName();
        if (sequenceName != null) {
            if (sequenceName.length() > 30) {
                throw new OracleException(String.format("Implicit Sequence Name [%s] exceeds 30 chars", sequenceName));
            }
        }

        // So far, so good
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

        // Skip enhance model classes if inherit from Model
        if (EnhancerUtility.inheritsFromModel(ctClass)) {
            return false;
        }

        // Skip enhance model classes if doesn't have a field annotated with @Id
        if (!EnhancerUtility.hasModelFieldAnnotatedWithIdWithinInheritance(ctClass)) {
            return false;
        }

        // Skip enhance model classes if doesn't have a field named "id"
        if (!EnhancerUtility.hasModelFieldWithinInheritance(ctClass, "id")) {
            return false;
        }

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
        String seqName;

        try {
            // 1. First try to get an explicit sequenceName
            seqName = getSequenceName();
            if (seqName != null && !seqName.isEmpty()) {
                return seqName;
            }

            // 2. Second try to get an explicit tableName
            String tableName = getTableName();
            if (tableName != null && !tableName.isEmpty()) {
                return formatOracleName(tableName, SUFFIX_SEQ);
            }
        } catch (Exception e) {
            // Do nothing...
        }

        // 3. If there's no sequenceName or tableName then create a default name
        return formatOracleName(ctClass.getSimpleName(), SUFFIX_SEQ);
    }

    private String getSequenceName() throws Exception {
        String sequenceName = null;
        if (isClassAnnotatedWithSequence()) {
            Sequence sequence = (Sequence) EnhancerUtility.getAnnotation(ctClass, Sequence.class);
            sequenceName = sequence.name();
        }
        return sequenceName;
    }
    
    private int getSequenceInitValue() throws Exception {
        int initValue = 1;  // default value
        if (isClassAnnotatedWithSequence()) {
            Sequence sequence = (Sequence) EnhancerUtility.getAnnotation(ctClass, Sequence.class);
            initValue = sequence.initValue();
        }
        return initValue;
    }

    private int getSequenceStepValue() throws Exception {
        int stepValue = 1;  // default value
        if (isClassAnnotatedWithSequence()) {
            Sequence sequence = (Sequence) EnhancerUtility.getAnnotation(ctClass, Sequence.class);
            stepValue = sequence.stepValue();
        }
        return stepValue;
    }

    private String getTableName() throws ClassNotFoundException {
        Table tableName = (Table) EnhancerUtility.getAnnotation(ctClass, Table.class);
        if (tableName != null) {
            return tableName.name();
        }
        return null;
    }
    
    private String getModelName() {
        return ctClass.getSimpleName();
    }

    private String obtainGeneratorName() {
        String generatorName;

        try {
            // 1. First try to get an explicit tableName
            generatorName = getTableName();
            if (generatorName != null && !generatorName.isEmpty()) {
                generatorName = String.format("%s_%s", generatorName, SUFFIX_GEN);
                return generatorName;
            }
        } catch (Exception e) {
            // Do nothing...
        }

        // 3. If there's no sequenceName or tableName then create a default name
        generatorName = String.format("%s_%s", getModelName(), SUFFIX_GEN);
        return generatorName;
    }
    
    private String formatOracleName(String name, String suffix) {
        // Oracle names length mustn't exceed 30 chars

        String compoundName = String.format("%s_%s", name, suffix);
        if (compoundName.length() > 30) {
            compoundName = removeSomeChars(compoundName);
        }
        return compoundName;
    }
    
    private String removeSomeChars(String oracleName) {
        play.Logger.warn("WARNING - In order to have an acceptable oracle name, some chars will be removed from: %s", oracleName);

        StringBuilder sb = new StringBuilder(oracleName).reverse();
        int i = 0;
        while (i <= 30) {
            char c = sb.charAt(i);
            switch (c) {
                case 'a':
                case 'e':
                case 'i':
                case 'o':
                case 'u':
                case '_':
                    sb.deleteCharAt(i); // remove it from StringBuilder
                    // keep the last index: i
                    if (sb.length() == 30) {
                        play.Logger.warn("WARNING - New oracle name after removing vowels and underscores: %s", sb.reverse().toString());
                        return sb.reverse().toString();
                    }
                    continue;
                default: // any other char
                    i++;
            }
        }

        play.Logger.warn("WARNING - Even after removing vowels and underscores the length is greater than 30");
        sb.reverse().delete(31, sb.length());
        play.Logger.warn("WARNING - New oracle name after removing consonants: %s", sb.toString());

        return sb.toString();
    }

    private void createIdField() throws Exception {
        
        // 1. Try to get a field named id:Long|long
        Map.Entry<CtClass, CtField> modelField = EnhancerUtility.modelHavingFieldAnnotatedWithId(ctClass);
        //CtClass c = modelField.getKey();
        CtField id = modelField.getValue();

        // Create track_data field
        /*String code = "private Long id;";
        CtField id = CtField.make(code, ctClass);
        ctClass.addField(id);*/

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
        IntegerMemberValue initValue = new IntegerMemberValue(constPool);
        initValue.setValue(getSequenceInitValue());
        annot.addMemberValue("initialValue", initValue);
        attr.addAnnotation(annot);
        // -----
        IntegerMemberValue stepValue = new IntegerMemberValue(constPool);
        stepValue.setValue(getSequenceStepValue());
        annot.addMemberValue("allocationSize", stepValue);
        attr.addAnnotation(annot);

        // Annotate id with @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="generator_name")
        annot = new Annotation(GeneratedValue.class.getName(), constPool);
        // -----
        EnumMemberValue enumValue = new EnumMemberValue(constPool);
        enumValue.setType(GenerationType.class.getName());
        enumValue.setValue(GenerationType.SEQUENCE.name());
        annot.addMemberValue("strategy", enumValue);
        attr.addAnnotation(annot);
        // -----
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
