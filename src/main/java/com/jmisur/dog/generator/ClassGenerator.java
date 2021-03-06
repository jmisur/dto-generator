package com.jmisur.dog.generator;

import static com.google.common.collect.Lists.newArrayList;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClassGenerator {

	private final String className;
	private String packageName;
	private final List<XClass> sourceXClasses;
	private final List<XFieldBase> fields = new ArrayList<XFieldBase>();
	private final List<XField> excludedFields = new ArrayList<XField>();
	private final List<MethodReference> methods = new ArrayList<MethodReference>();
	private final List<XMethod> copyMethods = new ArrayList<XMethod>();
	private final List<XFieldBase> equals = new ArrayList<XFieldBase>();
	private final List<XFieldBase> hashCode = new ArrayList<XFieldBase>();
	private boolean excludeAll;
	private String superclass;
	private final List<Class<?>> interfaces = new ArrayList<Class<?>>();
	private boolean abstract_;
	private boolean default_;
	private boolean final_;

	public ClassGenerator(String className, String packageName, List<XClass> sourceXClasses) {
		this.className = className;
		this.packageName = packageName;
		this.sourceXClasses = sourceXClasses;
	}

	public ClassGenerator method(Class<?> clazz, String name) {
		methods.add(new MethodReference(clazz, name));
		return this;
	}

	public ClassGenerator exclude(XField... fields) {
		excludedFields.addAll(newArrayList(fields));
		return this;
	}

	public XClass build() {
		return new XClass(className, XClass.class, null);
	}

	public ClassGenerator field(XFieldBase field) {
		checkSource(field);
		fields.add(field);
		return this;
	}

	public ClassGenerator fields(XFieldBase... fields) {
		for (XFieldBase field : fields) {
			checkSource(field);
			this.fields.add(field);
		}
		return this;
	}

	private void checkSource(XFieldBase field) {
		XFieldBase root = field.getSource();
		while (root != null && root.getSource() != null) {
			root = root.getSource();
		}

		if (root == null || sourceXClasses.contains(root)) {
			return;
		}
		throw new ClassDefinitionException("You specified field which is not a derived field from " + sourceXClasses);
	}

	public ClassGenerator intField(String name) {
		return field(name, int.class);
	}

	public ClassGenerator stringField(String name) {
		return field(name, String.class);
	}

	public ClassGenerator field(String name, Class<?> type) {
		return field(name, type, Modifier.PRIVATE);
	}

	public ClassGenerator field(String name, Class<?> type, int modifier) {
		return field(new XFieldBase(name, type, modifier, null));
	}

	public ClassGenerator equals(XFieldBase... fields) {
		for (XFieldBase field : fields) {
			checkSource(field);
			// verify field exist in fields..?
			equals.add(field);
		}
		return this;
	}

	public static class MethodReference {

		private final Class<?> clazz;
		private final String name;

		public MethodReference(Class<?> clazz, String name) {
			this.clazz = clazz;
			this.name = name;
		}

		public Class<?> getClazz() {
			return clazz;
		}

		public String getName() {
			return name;
		}
	}

	public ClassGenerator equalsAndHashCode(XFieldBase... fields) {
		equals(fields);
		hashCode(fields);
		return this;
	}

	public ClassGenerator hashCode(XFieldBase... fields) {
		for (XFieldBase field : fields) {
			checkSource(field);
			// verify field exist in fields..?
			hashCode.add(field);
		}
		return this;
	}

	public ClassGenerator excludeAll() {
		excludeAll = true;
		return this;
	}

	public ClassGenerator method(XMethod name) {
		copyMethods.add(name);
		return this;
	}

	public String getClassName() {
		return className;
	}

	public String getPackageName() {
		return packageName;
	}

	public List<XClass> getSourceXClasses() {
		return sourceXClasses;
	}

	public List<XFieldBase> getFields() {
		return fields;
	}

	public List<XField> getExcludedFields() {
		return excludedFields;
	}

	public List<MethodReference> getMethods() {
		return methods;
	}

	public List<XMethod> getCopyMethods() {
		return copyMethods;
	}

	public List<XFieldBase> getEquals() {
		return equals;
	}

	public List<XFieldBase> getHashCode() {
		return hashCode;
	}

	public boolean isExcludeAll() {
		return excludeAll;
	}

	public ClassGenerator superclass(Class<?> superclass) {
		this.superclass = superclass.getCanonicalName();
		return this;
	}

	public void superclass(XFieldBase superc) {
		this.superclass = superc.getName();
	}

	public String getSuperclass() {
		return superclass;
	}

	public ClassGenerator interfaces(Class<?>... interfaces) {
		this.interfaces.addAll(Arrays.asList(interfaces));
		return this;
	}

	public List<Class<?>> getInterfaces() {
		return interfaces;
	}

	public void package_(String pkg) {
		this.packageName = pkg;
	}

	public ClassGenerator abstract_() {
		this.abstract_ = true;
		return this;
	}

	public boolean isAbstract() {
		return abstract_;
	}

	public ClassGenerator default_() {
		this.default_ = true;
		return this;
	}

	public boolean isDefault() {
		return default_;
	}

	public ClassGenerator final_() {
		this.final_ = true;
		return this;
	}

	public boolean isFinal() {
		return final_;
	}
}
