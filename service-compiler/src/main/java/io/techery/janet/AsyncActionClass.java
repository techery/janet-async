package io.techery.janet;

import com.squareup.javapoet.ClassName;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import io.techery.janet.async.annotations.AsyncAction;
import io.techery.janet.async.annotations.Payload;
import io.techery.janet.body.BytesArrayBody;
import io.techery.janet.compiler.utils.ActionClass;

public class AsyncActionClass extends ActionClass {

    public final static String WRAPPER_SUFFIX = "Wrapper";

    private final Types typesUtils;
    private String event;
    private boolean incoming;
    private AsyncActionClass parent;


    public AsyncActionClass(Elements elementUtils, Types typesUtils, TypeElement typeElement, AsyncActionClass parent) {
        super(AsyncAction.class, elementUtils, typeElement);
        this.typesUtils = typesUtils;
        this.parent = parent;
        AsyncAction annotation = typeElement.getAnnotation(AsyncAction.class);
        if (annotation != null) {
            this.incoming = annotation.incoming();
            this.event = annotation.value();
        }
    }

    public boolean isAnnotatedClass() {
        return event != null;
    }

    public String getEvent() {
        return event;
    }

    public boolean isIncoming() {
        return incoming;
    }

    public AsyncActionClass getParent() {
        return parent;
    }

    public boolean isBytesPayload() {
        List<Element> payloadFields = getAnnotatedElements(Payload.class);
        for (Element element : payloadFields) {
            if (element instanceof TypeElement) {
                TypeElement typeElement = (TypeElement) element;
                if (ClassName.get(typeElement.asType()).toString()
                        .equals(ClassName.get(BytesArrayBody.class).toString())) {
                    return true;
                }
            }
        }
        if (parent != null) {
            return parent.isBytesPayload();
        }
        return false;
    }

    @Override public List<Element> getAnnotatedElements(Class annotationClass) {
        List<Element> elements = super.getAnnotatedElements(annotationClass);
        for (Iterator<Element> iterator = elements.iterator(); iterator.hasNext(); ) {
            Element element = iterator.next();
            if (!element.getEnclosingElement().equals(getTypeElement())) {
                iterator.remove();
            }
        }
        return elements;
    }

    public List<Element> getAllAnnotatedElements(Class annotationClass) {
        List<Element> elements = new ArrayList<Element>(getAnnotatedElements(annotationClass));
        if (parent != null) {
            elements.addAll(parent.getAllAnnotatedElements(annotationClass));
        }
        return elements;
    }

    public String getWrapperName() {
        return getTypeElement().getSimpleName() + WRAPPER_SUFFIX;
    }

    public String getFullWrapperName() {
        return getPackageName() + "." + getTypeElement().getSimpleName() + WRAPPER_SUFFIX;
    }
}
