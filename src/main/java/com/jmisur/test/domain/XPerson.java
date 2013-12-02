package com.jmisur.test.domain;

import com.jmisur.dto.generator.XField;
import javax.annotation.Generated;

/**
 * Generated by JAnnocessor
 */
@Generated("Easily with JAnnocessor :)")
public class XPerson extends XField<Person> {

    public static final XPerson person = new XPerson();

    public final XField<String> firstName = new XField<String>("firstName", String.class, this);

    public final XField<String> lastName = new XField<String>("lastName", String.class, this);

    public final XAddress address = new XAddress("address", this);

    public XPerson() {
        this("XPerson", null);
    }

    public XPerson(String name, XField<?> source) {
        super(name, Person.class, source);
    }

    public XField<?>[] getFields() {
        return new XField<?>[] {firstName, lastName, address};
    }

    public void getAllStuff() {

    }

    public void isAorB() {

    }

}