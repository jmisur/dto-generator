import static com.jmisur.dto.Generator.generate;

import java.lang.reflect.Modifier;

import com.jmisur.dto.XField;
import com.jmisur.dto.XFieldBase;
import com.jmisur.test.Address;
import com.jmisur.test.Person;

public class GeneratorTest {

	// annotations @NotPublished
	// default configuration - all fields with getters/setters, type to id
	// exclusion
	// inclusion if fields explicitely
	// custom methods
	// copy methods
	// equalsAndHashCode
	// comparable
	// JSR303
	// field getter/setter options, nogetter
	// copying included type Person-Address
	// type -> id
	// remember source fields for mapping
	// multiple source classes
	// package
	// validate inputs / strings, classnames, method names, package name, field names
	// superclass
	// interface

	public static void main(String[] args) {
		// import XPerson.person
		XPerson person = XPerson.person;
		XAddress address = XAddress.address;

		// default config -- generate all
		generate("PersonData").from(person);
		// pacakge
		generate("PersonData").from(person);

		// custom fields
		generate("PersonData").from(person).field("fullName", String.class);
		generate("PersonData").from(person).field("fullName", String.class, Modifier.PROTECTED);
		generate("PersonData").from(person).stringField("fullName");
		generate("PersonData").from(person).stringField("fullName").intField("age");

		// onle specific inclusion
		generate("PersonData").from(person).excludeAll().field(person.firstName);
		XField<Address> customAddressDto = generate("AddressData").from(address).done();
		// overwrite
		generate("PersonData").from(person).field(person.firstName.as("name")).field(person.address.as("addr", customAddressDto, Modifier.PROTECTED));
		// id
		generate("PersonData").from(person).field(person.address.asId().noSetter());
		// nested field
		generate("PersonData").from(person).field(person.address.name);
		generate("PersonData").from(person).field(person.address.name.as("addressName"));

		// exclusion
		generate("PersonData").from(person).exclude(person.firstName);

		// custom method
		generate("PersonData").from(person).method(PersonUtils.class, "isVisible");
		// copy method
		generate("PersonData").from(person).method("getAllStuff");

		// equals and hashcode
		XFieldBase<String> name = person.firstName.as("name");
		generate("PersonData").from(person).equals(name, person.firstName, person.address);
		generate("PersonData").from(person).hashCode(person.firstName, person.address);
		generate("PersonData").from(person).equalsAndHashCode(person.firstName, person.address);

	}

	public static class XPerson extends XField<Person> {

		public static XPerson person = new XPerson();

		public XPerson(String name, int modifier, XField<?> source) {
			super(name, Person.class, modifier, source);
		}

		public XPerson() {
			this(null, -1, null);
		}

		public XAddress address = new XAddress("address", Modifier.PUBLIC, this);
		public XField<String> firstName = new XField<String>("firstName", String.class, Modifier.PUBLIC, this);
	}

	public static class XAddress extends XField<Address> {
		public static XAddress address;

		public XAddress(String name, int modifier, XField<?> source) {
			super(name, Address.class, modifier, source);
		}

		public XField<String> id = new XField<String>("id", String.class, Modifier.PUBLIC, this);
		public XField<String> street = new XField<String>("street", String.class, Modifier.PUBLIC, this);
		public XField<String> name = new XField<String>("name", String.class, Modifier.PUBLIC, this);

		public XFieldBase<String> asId() {
			return asId(String.class);
		}
	}

	public static class PersonUtils {
		public static boolean isVisible(Person p) {
			return true;
		}
	}

}