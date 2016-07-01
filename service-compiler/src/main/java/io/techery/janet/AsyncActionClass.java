package io.techery.janet;

import com.squareup.javapoet.ClassName;

import java.util.List;
import java.util.Map;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import io.techery.janet.async.annotations.AsyncAction;
import io.techery.janet.async.annotations.Payload;
import io.techery.janet.async.annotations.PendingResponse;
import io.techery.janet.body.BytesArrayBody;
import io.techery.janet.compiler.utils.ActionClass;

public class AsyncActionClass extends ActionClass {

    public final static String WRAPPER_SUFFIX = "Wrapper";

    private String event;
    private final boolean incoming;
    private PendingResponseInfo responseInfo;
    private VariableElement payloadField;
    private boolean isBytesPayload;
    private Types typesUtils;


    public AsyncActionClass(Elements elementUtils, Types typesUtils, TypeElement typeElement) {
        super(AsyncAction.class, elementUtils, typeElement);
        this.typesUtils = typesUtils;
        AsyncAction annotation = typeElement.getAnnotation(AsyncAction.class);
        this.incoming = annotation.incoming();
        this.event = annotation.value();
        List<Element> payloadFields = getAnnotatedElements(Payload.class);
        for (Element field : payloadFields) {
            this.payloadField = (VariableElement) field;
            break;
        }

        if (payloadField == null) { //validator throw a error
            return;
        }

        //defining payload is bytes
        isBytesPayload = isBytesElement(typesUtils, typesUtils.asElement(payloadField.asType()));
        //getting response info
        List<Element> fields = getAnnotatedElements(PendingResponse.class);
        if (!fields.isEmpty()) {
            responseInfo = new PendingResponseInfo(typesUtils, fields.get(0));
        }
    }

    private boolean isBytesElement(Types typeUtils, Element element) {
        if (element instanceof TypeElement) {
            TypeElement typeElement = (TypeElement) element;
            if (ClassName.get(typeElement.asType()).toString()
                    .equals(ClassName.get(BytesArrayBody.class).toString())) {
                return true;
            }
            if (typeElement.getSuperclass() != null) {
                return isBytesElement(typeUtils, typeUtils.asElement(typeElement.getSuperclass()));
            }
        }
        return false;
    }


    public String getEvent() {
        return event;
    }

    public boolean isIncoming() {
        return incoming;
    }

    public String getWrapperName() {
        return getTypeElement().getSimpleName() + WRAPPER_SUFFIX;
    }

    public String getFullWrapperName() {
        return getPackageName() + "." + getTypeElement().getSimpleName() + WRAPPER_SUFFIX;
    }

    public boolean isBytesPayload() {
        return isBytesPayload;
    }

    public PendingResponseInfo getResponseInfo() {
        return responseInfo;
    }

    public Element getPayloadField() {
        return payloadField;
    }

    final public static class PendingResponseInfo {
        public final String responseEvent;
        public final Element responseField;
        public final TypeElement responseFieldType;
        public TypeElement responseMatcherElement;
        public long responseTimeout;

        public PendingResponseInfo(Types typeUtils, Element responseField) {
            this.responseField = responseField;
            for (AnnotationMirror annotation : responseField.getAnnotationMirrors()) {
                if (ClassName.get(PendingResponse.class).equals(ClassName.get(annotation.getAnnotationType()))) {
                    Map<? extends ExecutableElement, ? extends AnnotationValue> valuesMap = annotation.getElementValues();
                    for (ExecutableElement key : valuesMap.keySet()) {
                        if (key.getSimpleName().contentEquals("value")) {
                            TypeMirror valueMirror = (TypeMirror) valuesMap.get(key).getValue();
                            this.responseMatcherElement = (TypeElement) typeUtils.asElement(valueMirror);
                        }
                        if (key.getSimpleName().contentEquals("timeout")) {
                            responseTimeout = Long.valueOf(valuesMap.get(key).getValue().toString());
                        }
                    }
                    break;
                }
            }
            responseFieldType = (TypeElement) typeUtils.asElement(responseField.asType());
            AsyncAction asyncAction = responseFieldType.getAnnotation(AsyncAction.class);
            this.responseEvent = asyncAction.value();
        }
    }
}
