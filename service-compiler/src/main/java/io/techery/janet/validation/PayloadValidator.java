package io.techery.janet.validation;

import java.util.HashSet;
import java.util.Set;

import io.techery.janet.AsyncActionClass;
import io.techery.janet.async.annotations.Payload;
import io.techery.janet.compiler.utils.validation.ValidationError;
import io.techery.janet.compiler.utils.validation.Validator;

public class PayloadValidator implements Validator<AsyncActionClass> {

    @Override public Set<ValidationError> validate(AsyncActionClass value) {
        Set<ValidationError> errors = new HashSet<ValidationError>();
        if (value.getAllAnnotatedElements(Payload.class).isEmpty()) {
            errors.add(new ValidationError("Action must have a payload field", value.getTypeElement()));
        }
        return errors;
    }
}
