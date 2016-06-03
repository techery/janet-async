package io.techery.janet;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.TypeName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import io.techery.janet.async.actions.SystemAction;
import io.techery.janet.async.annotations.AsyncAction;
import io.techery.janet.compiler.utils.validation.ClassValidator;
import io.techery.janet.compiler.utils.validation.ValidationError;
import io.techery.janet.validation.AsyncActionValidators;

@AutoService(Processor.class)
public class JanetAsyncProcessor extends AbstractProcessor {
    private Elements elementUtils;
    private Messager messager;
    private Types typeUtils;
    private ClassValidator classValidator;
    private AsyncActionValidators asyncActionValidators;
    private AsyncWrappersGenerator wrappersGenerator;
    private AsyncFactoryGenerator factoryGenerator;
    private AsyncRosterGenerator rosterGenerator;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        elementUtils = processingEnv.getElementUtils();
        messager = processingEnv.getMessager();
        typeUtils = processingEnv.getTypeUtils();
        classValidator = new ClassValidator(AsyncAction.class);
        asyncActionValidators = new AsyncActionValidators();
        wrappersGenerator = new AsyncWrappersGenerator(processingEnv.getFiler(), typeUtils);
        factoryGenerator = new AsyncFactoryGenerator(processingEnv.getFiler());
        rosterGenerator = new AsyncRosterGenerator(processingEnv.getFiler());
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotataions = new HashSet<String>();
        annotataions.add(AsyncAction.class.getCanonicalName());
        return annotataions;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) return true;
        ArrayList<AsyncActionClass> actionClasses = new ArrayList<AsyncActionClass>();
        for (Element element : roundEnv.getElementsAnnotatedWith(AsyncAction.class)) {
            TypeElement typeElement = (TypeElement) element;
            boolean isSystem = false;
            for (TypeMirror iface : typeElement.getInterfaces()) {
                if (TypeName.get(iface).toString()
                        .equals(TypeName.get(SystemAction.class).toString())) {
                    isSystem = true;
                    break;
                }
            }
            if (isSystem) continue;
            AsyncActionClass actionClass = createActionClass(typeElement);
            Set<ValidationError> errors = asyncActionValidators.validate(actionClass);
            if (!errors.isEmpty()) {
                printErrors(errors);
                continue;
            }
            actionClasses.add(actionClass);
        }
        if (!actionClasses.isEmpty()) {
            wrappersGenerator.generate(actionClasses);
        }
        factoryGenerator.generate(actionClasses);
        rosterGenerator.generate(actionClasses);
        return true;
    }

    private AsyncActionClass createActionClass(TypeElement actionElement) {
        Set<ValidationError> errors = classValidator.validate(actionElement);
        if (!errors.isEmpty()) {
            printErrors(errors);
            return null;
        }
        AsyncActionClass parent = null;
        if (actionElement.getSuperclass() != null) {
            TypeElement parentElement = (TypeElement) typeUtils.asElement(actionElement.getSuperclass());
            if (parentElement != null) {
                AsyncActionClass subClass = createActionClass(parentElement);
                if (subClass != null && !subClass.getAllAnnotatedMembers().isEmpty()) {
                    parent = subClass;
                }
            }
        }
        return new AsyncActionClass(elementUtils, typeUtils, actionElement, parent);
    }


    private void printErrors(Collection<ValidationError> errors) {
        for (ValidationError error : errors) {
            messager.printMessage(Diagnostic.Kind.ERROR, error.getMessage(), error.getElement());
        }
    }

}
