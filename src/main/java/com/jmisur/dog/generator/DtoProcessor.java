package com.jmisur.dog.generator;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Maps.newLinkedHashMap;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.body.BodyDeclaration;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.Parameter;
import japa.parser.ast.body.TypeDeclaration;
import japa.parser.ast.stmt.Statement;
import japa.parser.ast.visitor.CloneVisitor;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;

import org.jannocessor.collection.api.PowerList;
import org.jannocessor.extra.processor.AbstractGenerator;
import org.jannocessor.model.bean.NameBean;
import org.jannocessor.model.bean.structure.JavaClassBean;
import org.jannocessor.model.executable.JavaMethod;
import org.jannocessor.model.modifier.FieldModifiers;
import org.jannocessor.model.modifier.value.ClassModifierValue;
import org.jannocessor.model.structure.JavaClass;
import org.jannocessor.model.util.Fields;
import org.jannocessor.model.util.Methods;
import org.jannocessor.model.util.New;
import org.jannocessor.model.variable.JavaField;
import org.jannocessor.model.variable.JavaParameter;
import org.jannocessor.processor.api.ProcessingContext;

public class DtoProcessor extends AbstractGenerator<JavaClass> {

	private final Map<String, List<MethodDeclaration>> methodCache = newHashMap();

	public DtoProcessor(String destPackage, boolean inDebugMode) {
		super(destPackage, inDebugMode);
	}

	@Override
	protected void generateCodeFrom(PowerList<JavaClass> generators, ProcessingContext context) {
		context.getLogger().info("Processing {} classes", generators.size());

		for (JavaClass generator : generators) {
			Class<?> generatorClass = generator.getType().getTypeClass();

			if (!validGenerator(context, generatorClass)) {
				break;
			}

			com.jmisur.dog.generator.AbstractGenerator instance = createGeneratorInstance(context, generatorClass);
			if (instance == null) {
				break;
			}
			instance.generate();

			processClassGenerators(context, generator, instance);
		}
	}

	private void processClassGenerators(ProcessingContext context, JavaClass generator, com.jmisur.dog.generator.AbstractGenerator instance) {
		for (ClassGenerator classGenerator : instance.getGenerators()) {
			JavaClass dto = createClass(generator, classGenerator);
			Collection<XFieldBase> fields = createFields(classGenerator, dto);
			createGettersSetters(dto, fields, context, classGenerator);
			createCopyMethods(context, classGenerator, dto, fields);
			generate(dto);
		}
	}

	private void createCopyMethods(ProcessingContext context, ClassGenerator classGenerator, JavaClass dto, Collection<XFieldBase> fields) {
		List<XMethod> copyMethods = classGenerator.getCopyMethods();

		if (!copyMethods.isEmpty()) {
			Map<XClass, List<MethodDeclaration>> methodMap = createMethodCache(context, classGenerator.getSourceXClasses());
			if (methodMap == null) {
				return;
			}
			for (XMethod method : copyMethods) {
				copyMethod(dto, methodMap.get(method.getSource()), method, context);
			}
		}
	}

	private void copyMethod(JavaClass dto, List<MethodDeclaration> methodList, XMethod method, ProcessingContext context) {
		for (MethodDeclaration methodMember : methodList) {
			if (methodMember.getName().equals(method.getName()) && paramsEquals(methodMember, method)) {
				copyMethod(dto, methodMember, method);
				return;
			}
		}
		context.getLogger().error("Unable to find method {} in source java file", method);
	}

	private Map<XClass, List<MethodDeclaration>> createMethodCache(ProcessingContext context, List<XClass> sources) {
		Map<XClass, List<MethodDeclaration>> map = newHashMap();

		for (XClass xclass : sources) {
			String cacheKey = xclass.getTypeAsClass().getCanonicalName();

			List<MethodDeclaration> cache = methodCache.get(cacheKey);

			if (cache == null) {
				cache = createMethodCache(context, xclass, cacheKey);
			}

			map.put(xclass, cache);
		}

		return map;
	}

	private List<MethodDeclaration> createMethodCache(ProcessingContext context, XClass xclass, String cacheKey) {
		List<MethodDeclaration> cache = newArrayList();
		CompilationUnit compilationUnit = parseClass(xclass.getTypeAsClass(), context);

		if (compilationUnit != null) {
			for (TypeDeclaration type : compilationUnit.getTypes()) {
				if (type.getName().equals(xclass.getTypeAsClass().getSimpleName())) {
					cache = newArrayList(getMethods(type.getMembers()));
				}
			}
		}

		methodCache.put(cacheKey, cache);

		return cache;
	}

	private List<MethodDeclaration> getMethods(List<BodyDeclaration> members) {
		List<MethodDeclaration> methods = newArrayList();
		for (BodyDeclaration member : members) {
			if (member instanceof MethodDeclaration) {
				methods.add((MethodDeclaration) member);
			}
		}
		return methods;
	}

	private boolean validGenerator(ProcessingContext context, Class<?> generatorClass) {
		if (!com.jmisur.dog.generator.AbstractGenerator.class.isAssignableFrom(generatorClass)) {
			context.getLogger().error("Class {} does not implement {} interface", generatorClass.getCanonicalName(),
					com.jmisur.dog.generator.AbstractGenerator.class.getCanonicalName());
			return false;
		}

		return true;
	}

	private void copyMethod(JavaClass dto, MethodDeclaration methodMember, XMethod method) {
		JavaMethod methodCopy = New.method(Methods.PUBLIC, method.getReturnType(), method.getName(), asJavaParameters(method.getParams()));
		List<Statement> statements = methodMember.getBody().getStmts();
		String sts = Joiner.on("\n").join(statements);
		methodCopy.getBody().setHardcoded(sts);
		dto.getMethods().add(methodCopy);
	}

	private com.jmisur.dog.generator.AbstractGenerator createGeneratorInstance(ProcessingContext context, Class<?> generatorClass) {
		try {
			return (com.jmisur.dog.generator.AbstractGenerator) generatorClass.newInstance();
		} catch (InstantiationException e) {
			context.getLogger().error("Cannot instantiate generator class {}", generatorClass.getCanonicalName(), e);
		} catch (IllegalAccessException e) {
			context.getLogger().error("Cannot instantiate generator class {}", generatorClass.getCanonicalName(), e);
		}
		return null;
	}

	private CompilationUnit parseClass(Class<?> clazz, ProcessingContext context) {
		try {
			CompilationUnit cu = Helper.parserClass(null, clazz);
			return (CompilationUnit) cu.accept(new CloneVisitor(), null);
		} catch (ParseException e) {
			context.getLogger().error("Unable to parse " + clazz);
			return null;
		}
	}

	private boolean paramsEquals(MethodDeclaration methodMember, XMethod method) {
		List<Parameter> params1 = methodMember.getParameters();
		List<XParam> params2 = method.getParams();

		if (params1 == null) {
			return params2.isEmpty() ? true : false;
		} else {
			int paramCount = params1.size();

			if (paramCount != params2.size()) {
				return false;
			}

			for (int i = 0; i < paramCount; i++) {
				Parameter parameter = params1.get(i);
				XParam parameter2 = params2.get(i);

				if (!parameter.getType().toString().equals(parameter2.getClazz().getSimpleName())) {
					return false;
				}
			}
			return true;
		}
	}

	private List<JavaParameter> asJavaParameters(List<XParam> params) {
		List<JavaParameter> result = newArrayList();
		for (XParam param : params) {
			result.add(New.parameter(param.getClazz(), param.getName()));
		}
		return result;
	}

	private void createGettersSetters(JavaClass dto, Collection<XFieldBase> fields, ProcessingContext context, ClassGenerator classGenerator) {
		for (XFieldBase field : fields) {
			if (field.isCopyGetter()) {
				Map<XClass, List<MethodDeclaration>> methodMap = createMethodCache(context, classGenerator.getSourceXClasses());
				copyMethod(dto, methodMap.get(field.getSource()), new XMethod(field.getSourceXClass(), getterMethodName(field), field.getTypeAsClass()),
						context);
			}
			if (field.isGetter()) {
				JavaMethod getter = New.method(Methods.PUBLIC, field.getType(), getterMethodName(field));
				getter.getBody().setHardcoded("return %s;", field.getName());
				dto.getMethods().add(getter);
			}
			if (field.isCopySetter()) {
				Map<XClass, List<MethodDeclaration>> methodMap = createMethodCache(context, classGenerator.getSourceXClasses());
				copyMethod(dto, methodMap.get(field.getSource()),
						new XMethod(field.getSourceXClass(), setterMethodName(field), void.class, new XParam(field.getTypeAsClass(), field.getName())), context);
			}
			if (field.isSetter()) {
				JavaMethod setter = New.method(Methods.PUBLIC, void.class, setterMethodName(field), New.parameter(field.getType(), field.getName()));
				setter.getBody().setHardcoded("this.%s = %s;", field.getName(), field.getName());
				dto.getMethods().add(setter);
			}
		}
	}

	private String setterMethodName(XFieldBase field) {
		return new NameBean(field.getName()).insertPart(0, "set").getText();
	}

	private String getterMethodName(XFieldBase field) {
		return new NameBean(field.getName()).insertPart(0, "get").getText();
	}

	private Collection<XFieldBase> createFields(ClassGenerator classGenerator, JavaClass dto) {
		Collection<XFieldBase> fields = mergeFields(classGenerator);
		for (XFieldBase field : fields) {
			JavaField javaField = New.field(getModifier(field.getModifier()), field.getType(), field.getName());
			dto.getFields().add(javaField);
		}
		return fields;
	}

	private FieldModifiers getModifier(int modifier) {
		switch (modifier) {
		case Modifier.PUBLIC:
			return Fields.PUBLIC;
		case Modifier.PRIVATE:
			return Fields.PRIVATE;
		case Modifier.PROTECTED:
			return Fields.PROTECTED;
		case 0:
			return Fields.DEFAULT_MODIFIER;
		default:
			throw new IllegalArgumentException("Unknown modifier " + modifier);
		}
	}

	private JavaClass createClass(JavaClass generator, ClassGenerator classGenerator) {
		JavaClassBean dto = (JavaClassBean) New.classs(classGenerator.getClassName());

		// package
		if (classGenerator.getPackageName() != null) {
			dto.setParent(New.packagee(classGenerator.getPackageName()));
		} else {
			dto.setParent(generator.getParent());
		}

		// class name
		dto.setType(New.type(classGenerator.getClassName()));

		// superclass
		if (classGenerator.getSuperclass() != null) {
			dto.setSuperclass(New.type(classGenerator.getSuperclass()));
		}

		// interfaces
		List<Class<?>> interfaces = classGenerator.getInterfaces();
		if (interfaces != null) {
			dto.setInterfaces(New.types(interfaces.toArray(new Class<?>[interfaces.size()])));
		}

		// modifiers
		List<ClassModifierValue> modifiers = new ArrayList<ClassModifierValue>();
		if (!classGenerator.isDefault()) {
			modifiers.add(ClassModifierValue.PUBLIC);
		}
		if (classGenerator.isFinal()) {
			modifiers.add(ClassModifierValue.FINAL);
		}
		if (classGenerator.isAbstract()) {
			modifiers.add(ClassModifierValue.ABSTRACT);
		}
		dto.setModifiers(New.classModifiers(modifiers.toArray(new ClassModifierValue[modifiers.size()])));
		return dto;
	}

	private Collection<XFieldBase> mergeFields(ClassGenerator classGenerator) {
		LinkedHashMap<String, XFieldBase> fields = newLinkedHashMap();
		if (!classGenerator.isExcludeAll()) {
			for (XClass xclass : classGenerator.getSourceXClasses()) {
				for (XFieldBase field : xclass.getFields()) {
					System.out.println("adding " + field.getName());
					fields.put(field.getName(), field);
				}
			}
		}

		for (XField field : classGenerator.getExcludedFields()) {
			fields.remove(field.getName());
		}

		for (XFieldBase field : classGenerator.getFields()) {
			if (field.getSource() == null || classGenerator.getSourceXClasses().contains(field.getSource())) {
				// is existing or custom without source
				System.out.println("adding " + field.getName());
				fields.put(field.getName(), field);
			} else {
				System.out.println("adding custom " + field.getName() + " based on " + field.getSource().getName());
				// custom field based on existing, overwrite under original name
				fields.put(field.getSource().getName(), field);
			}
		}
		// System.out.println("FIELDS: " + fields.keySet());
		// check for duplicate entries in multimap
		// throw new IllegalArgumentException("Duplicate field '" + field.getName() + "'. You must include all fields explicitely");
		return fields.values();
	}
}
