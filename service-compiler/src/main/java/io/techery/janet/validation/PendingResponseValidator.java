package io.techery.janet.validation;

import java.util.HashSet;
import java.util.Set;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.ElementFilter;

import io.techery.janet.AsyncActionClass;
import io.techery.janet.async.annotations.AsyncAction;
import io.techery.janet.compiler.utils.validation.ValidationError;
import io.techery.janet.compiler.utils.validation.Validator;

public class PendingResponseValidator implements Validator<AsyncActionClass> {

    @Override public Set<ValidationError> validate(AsyncActionClass value) {
        Set<ValidationError> errors = new HashSet<ValidationError>();
        if (value.getResponseInfo() != null) {
            AsyncAction asyncActionAnnotation = value.getResponseInfo().responseFieldType.getAnnotation(AsyncAction.class);
            if (asyncActionAnnotation == null || !asyncActionAnnotation.incoming()) {
                errors.add(new ValidationError("Pending response must be incoming async action", value.getResponseInfo().responseField));
            }
            if (value.getResponseInfo().responseMatcherElement == null) {
                errors.add(new ValidationError("No request-response matcher", value.getResponseInfo().responseField));
            }
            for (ExecutableElement cons :
                    ElementFilter.constructorsIn(value.getResponseInfo().responseMatcherElement.getEnclosedElements())) {
                if (!cons.getParameters().isEmpty()) {
                    errors.add(new ValidationError("The class is missing a default constructor", value.getResponseInfo().responseMatcherElement));
                }
            }
        }
        return errors;
    }
}
