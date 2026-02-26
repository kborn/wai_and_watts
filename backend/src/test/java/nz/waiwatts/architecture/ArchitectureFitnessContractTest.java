package nz.waiwatts.architecture;

import nz.waiwatts.explanations.dto.Explanation;
import nz.waiwatts.explanations.dto.FactPack;
import nz.waiwatts.explanations.provider.ExplanationProvider;
import nz.waiwatts.explanations.provider.OpenAiExplanationProvider;
import nz.waiwatts.explanations.provider.StubExplanationProvider;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("contract")
class ArchitectureFitnessContractTest {

    private static final String CONTROLLER_BASE_PACKAGE = "nz.waiwatts";
    private static final String REPOSITORY_PACKAGE_PREFIX = "nz.waiwatts.persistence.repositories";
    private static final String DOMAIN_PACKAGE_PREFIX = "nz.waiwatts.domain";

    @Test
    void controllersMustNotDependOnRepositories() {
        List<String> violations = new ArrayList<>();
        for (Class<?> controllerClass : findRestControllers()) {
            for (Field field : controllerClass.getDeclaredFields()) {
                if (isFromRepositoryPackage(field.getType())) {
                    violations.add(controllerClass.getName() + " field " + field.getName() + " depends on repository");
                }
            }
            for (Constructor<?> ctor : controllerClass.getDeclaredConstructors()) {
                Class<?>[] paramTypes = ctor.getParameterTypes();
                for (Class<?> paramType : paramTypes) {
                    if (isFromRepositoryPackage(paramType)) {
                        violations.add(controllerClass.getName() + " constructor depends on repository type " + paramType.getName());
                    }
                }
            }
        }
        assertTrue(violations.isEmpty(), String.join("\n", violations));
    }

    @Test
    void controllerApiBoundaryMustNotExposeDomainEntities() {
        List<String> violations = new ArrayList<>();
        for (Class<?> controllerClass : findRestControllers()) {
            for (Method method : controllerClass.getDeclaredMethods()) {
                if (!isApiHandlerMethod(method)) {
                    continue;
                }
                Set<Class<?>> referencedTypes = new HashSet<>();
                collectReferencedClasses(method.getGenericReturnType(), referencedTypes);
                for (Type parameterType : method.getGenericParameterTypes()) {
                    collectReferencedClasses(parameterType, referencedTypes);
                }

                referencedTypes.stream()
                    .filter(this::isFromDomainPackage)
                    .forEach(type -> violations.add(
                        controllerClass.getName() + "#" + method.getName() + " exposes domain type " + type.getName()
                    ));
            }
        }
        assertTrue(violations.isEmpty(), String.join("\n", violations));
    }

    @Test
    void explanationProvidersMustHonorFactPackBoundary() throws Exception {
        List<Class<? extends ExplanationProvider>> providers = List.of(
            StubExplanationProvider.class,
            OpenAiExplanationProvider.class
        );

        List<String> violations = new ArrayList<>();
        for (Class<? extends ExplanationProvider> providerClass : providers) {
            assertTrue(ExplanationProvider.class.isAssignableFrom(providerClass),
                providerClass.getName() + " must implement ExplanationProvider");

            for (Field field : providerClass.getDeclaredFields()) {
                if (isFromRepositoryPackage(field.getType()) || isFromDomainPackage(field.getType())) {
                    violations.add(providerClass.getName() + " field " + field.getName()
                        + " crosses explanation boundary: " + field.getType().getName());
                }
            }
            for (Constructor<?> ctor : providerClass.getDeclaredConstructors()) {
                for (Class<?> paramType : ctor.getParameterTypes()) {
                    if (isFromRepositoryPackage(paramType) || isFromDomainPackage(paramType)) {
                        violations.add(providerClass.getName() + " constructor depends on forbidden type " + paramType.getName());
                    }
                }
            }

            Method generateExplanation = providerClass.getMethod("generateExplanation", String.class, FactPack.class);
            assertEquals(Explanation.class, generateExplanation.getReturnType(),
                providerClass.getName() + "#generateExplanation must return Explanation");
        }
        assertTrue(violations.isEmpty(), String.join("\n", violations));
    }

    private List<Class<?>> findRestControllers() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        List<Class<?>> classes = new ArrayList<>();
        scanner.findCandidateComponents(CONTROLLER_BASE_PACKAGE).forEach(beanDefinition -> {
            try {
                classes.add(Class.forName(beanDefinition.getBeanClassName()));
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Unable to load controller class " + beanDefinition.getBeanClassName(), e);
            }
        });
        return classes;
    }

    private boolean isApiHandlerMethod(Method method) {
        return method.isAnnotationPresent(RequestMapping.class)
            || method.isAnnotationPresent(GetMapping.class)
            || method.isAnnotationPresent(PostMapping.class)
            || method.isAnnotationPresent(PutMapping.class)
            || method.isAnnotationPresent(DeleteMapping.class)
            || method.isAnnotationPresent(PatchMapping.class);
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

    private boolean isFromRepositoryPackage(Class<?> type) {
        Package pkg = type.getPackage();
        return pkg != null && pkg.getName().startsWith(REPOSITORY_PACKAGE_PREFIX);
    }

    private boolean isFromDomainPackage(Class<?> type) {
        Package pkg = type.getPackage();
        return pkg != null && pkg.getName().startsWith(DOMAIN_PACKAGE_PREFIX);
    }
}
