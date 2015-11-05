package com.cs5248.android.model.cache;

import com.activeandroid.Model;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;

/**
 * This custom introspector is to ignore the properties from ActiveAndroid Model from
 * being serialized into JSON.
 *
 * @author lpthanh
 */
public class IgnoreAAModelIntrospector extends JacksonAnnotationIntrospector {

    @Override
    public boolean hasIgnoreMarker(final AnnotatedMember m) {
        return m.getDeclaringClass() == Model.class || super.hasIgnoreMarker(m);
    }
}
