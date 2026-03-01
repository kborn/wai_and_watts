package nz.waiwatts.architecture;

import jakarta.persistence.Entity;
import nz.waiwatts.explanations.builder.FactPackBuilder;
import nz.waiwatts.explanations.dto.Explanation;
import nz.waiwatts.explanations.dto.FactPack;
import nz.waiwatts.explanations.generator.ExplanationGenerator;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("contract")
class ArchitectureFitnessContractTest {

    private static final String ROOT_PACKAGE = "nz.waiwatts";
    private static final String API_PACKAGE_PREFIX = "nz.waiwatts.api";
    private static final String EXPLANATION_API_PACKAGE_PREFIX = "nz.waiwatts.explanations.api";
    private static final String REPOSITORY_PACKAGE_PREFIX = "nz.waiwatts.persistence.repositories";
    private static final String DOMAIN_PACKAGE_PREFIX = "nz.waiwatts.domain";
    private static final String API_DTO_SEGMENT = ".dto";
    private static final String EXPLANATION_DTO_PACKAGE_PREFIX = "nz.waiwatts.explanations.dto";
    private static final String SRC_MAIN_JAVA_ROOT = "src/main/java/nz/waiwatts";

    @Test
    void restControllersMustLiveInApiPackages() {
        List<String> violations = new ArrayList<>();
        for (Class<?> controllerClass : findRestControllers()) {
            Package pkg = controllerClass.getPackage();
            String packageName = pkg != null ? pkg.getName() : "";
            if (!packageName.startsWith(API_PACKAGE_PREFIX)
                && !packageName.startsWith(EXPLANATION_API_PACKAGE_PREFIX)) {
                violations.add(controllerClass.getName()
                    + " must live under nz.waiwatts.api.. or nz.waiwatts.explanations.api..");
            }
        }
        assertNoViolations(violations);
    }

    @Test
    void restControllersMustBeNamedController() {
        List<String> violations = new ArrayList<>();
        for (Class<?> controllerClass : findRestControllers()) {
            if (!controllerClass.getSimpleName().endsWith("Controller")) {
                violations.add(controllerClass.getName() + " must end with Controller");
            }
        }
        assertNoViolations(violations);
    }

    @Test
    void controllersMustNotDependOnRepositories() {
        List<String> violations = new ArrayList<>();
        for (Class<?> controllerClass : findRestControllers()) {
            collectFieldAndConstructorViolations(controllerClass, violations, type ->
                isFromRepositoryPackage(type)
                    ? controllerClass.getName() + " depends on repository type " + type.getName()
                    : null
            );
        }
        assertNoViolations(violations);
    }

    @Test
    void controllersMustNotDependOnDomainTypesDirectly() {
        List<String> violations = new ArrayList<>();
        for (Class<?> controllerClass : findRestControllers()) {
            collectFieldAndConstructorViolations(controllerClass, violations, type ->
                isFromDomainPackage(type)
                    ? controllerClass.getName() + " depends on domain type " + type.getName()
                    : null
            );
        }
        assertNoViolations(violations);
    }

    @Test
    void controllerApiBoundaryMustNotExposeDomainEntities() {
        List<String> violations = new ArrayList<>();
        for (Class<?> controllerClass : findRestControllers()) {
            for (Method method : controllerClass.getDeclaredMethods()) {
                if (!isApiHandlerMethod(method)) {
                    continue;
                }
                Set<Class<?>> referencedTypes = new LinkedHashSet<>();
                collectReferencedClasses(method.getGenericReturnType(), referencedTypes);
                for (Type parameterType : method.getGenericParameterTypes()) {
                    collectReferencedClasses(parameterType, referencedTypes);
                }

                referencedTypes.stream()
                    .filter(this::isFromDomainPackage)
                    .forEach(type -> violations.add(
                        controllerClass.getName() + "#" + method.getName()
                            + " exposes domain type " + type.getName()
                    ));
            }
        }
        assertNoViolations(violations);
    }

    @Test
    void controllerApiBoundaryMustUseDtoOrSimpleTypes() {
        List<String> violations = new ArrayList<>();
        for (Class<?> controllerClass : findRestControllers()) {
            for (Method method : controllerClass.getDeclaredMethods()) {
                if (!isApiHandlerMethod(method)) {
                    continue;
                }

                Set<Class<?>> referencedTypes = new LinkedHashSet<>();
                collectReferencedClasses(method.getGenericReturnType(), referencedTypes);
                for (Type parameterType : method.getGenericParameterTypes()) {
                    collectReferencedClasses(parameterType, referencedTypes);
                }

                referencedTypes.stream()
                    .filter(this::isProjectClass)
                    .filter(type -> !isAllowedApiBoundaryType(type))
                    .forEach(type -> violations.add(
                        controllerClass.getName() + "#" + method.getName()
                            + " uses non-DTO API boundary type " + type.getName()
                    ));
            }
        }
        assertNoViolations(violations);
    }

    @Test
    void nonApiClassesMustNotDependOnControllerTypes() {
        List<String> violations = new ArrayList<>();
        for (Class<?> projectClass : findProjectClasses()) {
            if (isRestController(projectClass) || isIgnoredType(projectClass)) {
                continue;
            }
            collectFieldAndConstructorViolations(projectClass, violations, type ->
                isRestController(type)
                    ? projectClass.getName() + " depends on controller type " + type.getName()
                    : null
            );
        }
        assertNoViolations(violations);
    }

    @Test
    void explanationGeneratorsMustHonorFactPackBoundary() throws Exception {
        List<String> violations = new ArrayList<>();
        for (Class<?> generatorClass : findProjectClasses()) {
            if (!isConcreteExplanationGenerator(generatorClass)) {
                continue;
            }

            assertTrue(ExplanationGenerator.class.isAssignableFrom(generatorClass),
                generatorClass.getName() + " must implement ExplanationGenerator");

            collectFieldAndConstructorViolations(generatorClass, violations, type -> {
                if (isFromRepositoryPackage(type) || isFromDomainPackage(type)) {
                    return generatorClass.getName() + " crosses explanation boundary with " + type.getName();
                }
                return null;
            });

            Method generateExplanation = generatorClass.getMethod("generateExplanation", String.class, FactPack.class);
            assertEquals(Explanation.class, generateExplanation.getReturnType(),
                generatorClass.getName() + "#generateExplanation must return Explanation");
        }
        assertNoViolations(violations);
    }

    @Test
    void factPackBuildersMustNotDependOnControllersOrGenerators() {
        List<String> violations = new ArrayList<>();
        for (Class<?> builderClass : findProjectClasses()) {
            if (!isConcreteFactPackBuilder(builderClass)) {
                continue;
            }
            collectFieldAndConstructorViolations(builderClass, violations, type -> {
                if (isRestController(type)) {
                    return builderClass.getName() + " must not depend on controller type " + type.getName();
                }
                if (isExplanationGeneratorType(type)) {
                    return builderClass.getName() + " must not depend on explanation generator type " + type.getName();
                }
                return null;
            });
        }
        assertNoViolations(violations);
    }

    @Test
    void repositoriesMustLiveInPersistencePackageAndExtendJpaRepository() {
        List<String> violations = new ArrayList<>();
        for (Class<?> projectClass : findProjectClasses()) {
            boolean isRepository = JpaRepository.class.isAssignableFrom(projectClass);
            boolean inRepositoryPackage = isFromRepositoryPackage(projectClass);

            if (isRepository && !inRepositoryPackage) {
                violations.add(projectClass.getName()
                    + " extends JpaRepository but is not under nz.waiwatts.persistence.repositories");
            }
            if (inRepositoryPackage && !isRepository) {
                violations.add(projectClass.getName()
                    + " is under nz.waiwatts.persistence.repositories but does not extend JpaRepository");
            }
        }
        assertNoViolations(violations);
    }

    @Test
    void entitiesMustLiveInDomainPackage() {
        List<String> violations = new ArrayList<>();
        for (Class<?> projectClass : findProjectClasses()) {
            if (projectClass.isAnnotationPresent(Entity.class) && !isFromDomainPackage(projectClass)) {
                violations.add(projectClass.getName() + " is annotated with @Entity but is not under nz.waiwatts.domain");
            }
        }
        assertNoViolations(violations);
    }

    @Test
    void configurationTypesMustLiveInConfigPackages() {
        List<String> violations = new ArrayList<>();
        for (Class<?> projectClass : findProjectClasses()) {
            if (projectClass.isAnnotationPresent(Configuration.class)
                && !packageName(projectClass).contains(".config")) {
                violations.add(projectClass.getName()
                    + " is annotated with @Configuration but does not live under a .config package");
            }
            if (projectClass.isAnnotationPresent(ConfigurationProperties.class)
                && !packageName(projectClass).contains(".config")) {
                violations.add(projectClass.getName()
                    + " is annotated with @ConfigurationProperties but does not live under a .config package");
            }
        }
        assertNoViolations(violations);
    }

    @Test
    void configurationPropertiesMustNotLeakIntoRuntimeOrApiBoundaries() {
        List<String> violations = new ArrayList<>();
        for (Class<?> projectClass : findProjectClasses()) {
            if (isIgnoredType(projectClass) || projectClass.isAnnotationPresent(Configuration.class)) {
                continue;
            }
            boolean boundaryType = isRestController(projectClass)
                || isConcreteExplanationGenerator(projectClass)
                || isConcreteFactPackBuilder(projectClass);
            if (!boundaryType) {
                continue;
            }

            collectFieldAndConstructorViolations(projectClass, violations, type ->
                type.isAnnotationPresent(ConfigurationProperties.class)
                    ? projectClass.getName() + " depends on @ConfigurationProperties type " + type.getName()
                    : null
            );
        }
        assertNoViolations(violations);
    }

    private List<Class<?>> findProjectClasses() {
        Path root = Paths.get(SRC_MAIN_JAVA_ROOT);
        if (!Files.exists(root)) {
            throw new IllegalStateException("Unable to locate project source root: " + root.toAbsolutePath());
        }

        try (Stream<Path> paths = Files.walk(root)) {
            return paths
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.getFileName().toString().equals("package-info.java"))
                .map(root::relativize)
                .map(this::toClassName)
                .map(this::tryLoadClass)
                .flatMap(Optional::stream)
                .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to scan project classes", e);
        }
    }

    private List<Class<?>> findRestControllers() {
        return findProjectClasses().stream()
            .filter(this::isRestController)
            .toList();
    }

    private String toClassName(Path relativePath) {
        String path = relativePath.toString();
        String withoutExtension = path.substring(0, path.length() - ".java".length());
        return ROOT_PACKAGE + "." + withoutExtension.replace('/', '.').replace('\\', '.');
    }

    private Optional<Class<?>> tryLoadClass(String className) {
        try {
            return Optional.of(Class.forName(className));
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
    }

    private boolean isApiHandlerMethod(Method method) {
        return method.isAnnotationPresent(RequestMapping.class)
            || method.isAnnotationPresent(GetMapping.class)
            || method.isAnnotationPresent(PostMapping.class)
            || method.isAnnotationPresent(PutMapping.class)
            || method.isAnnotationPresent(DeleteMapping.class)
            || method.isAnnotationPresent(PatchMapping.class);
    }

    private void collectFieldAndConstructorViolations(
        Class<?> owner,
        List<String> violations,
        DependencyViolationMapper violationMapper
    ) {
        for (Field field : owner.getDeclaredFields()) {
            addViolationIfPresent(violations, violationMapper.describe(field.getType()));
        }
        for (Constructor<?> constructor : owner.getDeclaredConstructors()) {
            for (Class<?> paramType : constructor.getParameterTypes()) {
                addViolationIfPresent(violations, violationMapper.describe(paramType));
            }
        }
    }

    private void addViolationIfPresent(List<String> violations, String violation) {
        if (violation != null && !violation.isBlank()) {
            violations.add(violation);
        }
    }

    private void collectReferencedClasses(Type type, Set<Class<?>> out) {
        if (type instanceof Class<?> c) {
            out.add(c);
            return;
        }
        if (type instanceof ParameterizedType p) {
            collectReferencedClasses(p.getRawType(), out);
            for (Type arg : p.getActualTypeArguments()) {
                collectReferencedClasses(arg, out);
            }
            return;
        }
        if (type instanceof GenericArrayType a) {
            collectReferencedClasses(a.getGenericComponentType(), out);
            return;
        }
        if (type instanceof WildcardType w) {
            for (Type upper : w.getUpperBounds()) {
                collectReferencedClasses(upper, out);
            }
            for (Type lower : w.getLowerBounds()) {
                collectReferencedClasses(lower, out);
            }
            return;
        }
        if (type instanceof TypeVariable<?>) {
            return;
        }
        throw new IllegalArgumentException("Unsupported type encountered in architecture test: " + type);
    }

    private boolean isRestController(Class<?> type) {
        return type.isAnnotationPresent(RestController.class);
    }

    private boolean isConcreteExplanationGenerator(Class<?> type) {
        return isExplanationGeneratorType(type) && !type.isInterface() && !java.lang.reflect.Modifier.isAbstract(type.getModifiers());
    }

    private boolean isExplanationGeneratorType(Class<?> type) {
        return ExplanationGenerator.class.isAssignableFrom(type);
    }

    private boolean isConcreteFactPackBuilder(Class<?> type) {
        return FactPackBuilder.class.isAssignableFrom(type)
            && !type.isInterface()
            && !java.lang.reflect.Modifier.isAbstract(type.getModifiers());
    }

    private boolean isAllowedApiBoundaryType(Class<?> type) {
        if (!isProjectClass(type)) {
            return true;
        }
        if (isRestController(type) || isFromRepositoryPackage(type) || isFromDomainPackage(type)) {
            return false;
        }
        String packageName = packageName(type);
        return packageName.startsWith(EXPLANATION_DTO_PACKAGE_PREFIX)
            || (packageName.startsWith(API_PACKAGE_PREFIX) && packageName.contains(API_DTO_SEGMENT));
    }

    private boolean isProjectClass(Class<?> type) {
        return packageName(type).startsWith(ROOT_PACKAGE);
    }

    private boolean isIgnoredType(Class<?> type) {
        return type.isAnnotationPresent(Entity.class)
            || JpaRepository.class.isAssignableFrom(type);
    }

    private boolean isFromRepositoryPackage(Class<?> type) {
        return packageName(type).startsWith(REPOSITORY_PACKAGE_PREFIX);
    }

    private boolean isFromDomainPackage(Class<?> type) {
        return packageName(type).startsWith(DOMAIN_PACKAGE_PREFIX);
    }

    private String packageName(Class<?> type) {
        Package pkg = type.getPackage();
        return pkg != null ? pkg.getName() : "";
    }

    private void assertNoViolations(List<String> violations) {
        assertTrue(violations.isEmpty(), String.join("\n", new LinkedHashSet<>(violations)));
    }

    @FunctionalInterface
    private interface DependencyViolationMapper {
        String describe(Class<?> dependencyType);
    }
}
