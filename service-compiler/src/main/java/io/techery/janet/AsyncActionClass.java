package io.techery.janet;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

import java.util.List;
import java.util.Map;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;

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
    private Element payloadField;
    private boolean isBytesPayload;


    public AsyncActionClass(Elements elementUtils, TypeElement typeElement) {
        super(AsyncAction.class, elementUtils, typeElement);
        AsyncAction annotation = typeElement.getAnnotation(AsyncAction.class);
        this.incoming = annotation.incoming();
        this.event = annotation.value();
        List<Element> payloadFields = getAnnotatedElements(Payload.class);
        for (Element field : payloadFields) {
            this.payloadField = field;
            break;
        }

        if (payloadField == null) { //validator throw a error
            return;
        }

        //defining payload is bytes
        ClassName bytesArrayBody = ClassName.get(BytesArrayBody.class);
        TypeMirror payloadSuperClass = elementUtils.getTypeElement(payloadField.asType().toString()).getSuperclass();
        while (payloadSuperClass != null && payloadSuperClass.getKind() != TypeKind.NONE) {
            TypeName typeName = ClassName.get(payloadSuperClass);
            if (typeName.toString().equals(bytesArrayBody.toString())) {
                isBytesPayload = true;
                break;
            }
            payloadSuperClass = elementUtils.getTypeElement(payloadSuperClass.toString()).getSuperclass();
        }
        //getting response info
        List<Element> fields = getAnnotatedElements(PendingResponse.class);
        if (!fields.isEmpty()) {
            responseInfo = new PendingResponseInfo(elementUtils, fields.get(0));
        }
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

        public PendingResponseInfo(Elements elementUtils, Element responseField) {
            this.responseField = responseField;
            for (AnnotationMirror annotation : responseField.getAnnotationMirrors()) {
                if (ClassName.get(PendingResponse.class).equals(ClassName.get(annotation.getAnnotationType()))) {
                    Map<? extends ExecutableElement, ? extends AnnotationValue> valuesMap = annotation.getElementValues();
                    for (ExecutableElement key : valuesMap.keySet()) {
                        if (key.getSimpleName().contentEquals("value")) {
                            TypeMirror valueMirror = (TypeMirror) valuesMap.get(key).getValue();
                            this.responseMatcherElement = elementUtils.getTypeElement(ClassName.get(valueMirror) .toString());
                        }
                        if (key.getSimpleName().contentEquals("timeout")) {
                            responseTimeout = Long.valueOf(valuesMap.get(key).getValue().toString());
                        }
                    }
                    break;
                }
            }
            TypeName responseActionName = ClassName.get(responseField.asType());
            responseFieldType = elementUtils.getTypeElement(responseActionName.toString());
            AsyncAction asyncAction = responseFieldType.getAnnotation(AsyncAction.class);
            this.responseEvent = asyncAction.value();
        }
    }
}
