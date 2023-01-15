package dev.gokhun.guavaspringconfigprops;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.Var;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindHandlerAdvisor;
import org.springframework.boot.context.properties.bind.AbstractBindHandler;
import org.springframework.boot.context.properties.bind.BindContext;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;

public final class ImmutableCollectionAdvisor implements ConfigurationPropertiesBindHandlerAdvisor {
    @Override
    public BindHandler apply(BindHandler bindHandler) {
        return new ImmutableCollectionBinderHandler(bindHandler);
    }

    private static final class ImmutableCollectionBinderHandler extends AbstractBindHandler {

        @Retention(RetentionPolicy.RUNTIME)
        private @interface GuavaCollection {
            boolean optional() default false;
        }

        private static final GuavaCollection GUAVA_COLLECTION =
                AnnotationUtils.synthesizeAnnotation(GuavaCollection.class);
        private static final GuavaCollection OPTIONAL_GUAVA_COLLECTION =
                AnnotationUtils.synthesizeAnnotation(
                        ImmutableMap.of("optional", true), GuavaCollection.class, null);

        private ImmutableCollectionBinderHandler(BindHandler bindHandler) {
            super(bindHandler);
        }

        @Override
        public <T> Bindable<T> onStart(
                ConfigurationPropertyName name, Bindable<T> target, BindContext context) {
            Class<?> originalTarget;
            boolean optional;
            if (target.getType().getRawClass() != null
                    && Optional.class.equals(target.getType().getRawClass())) {
                originalTarget = target.getType().getGenerics()[0].resolve(Object.class);
                optional = true;
            } else {
                originalTarget = target.getType().resolve(Object.class);
                optional = false;
            }
            if (ImmutableList.class.isAssignableFrom(originalTarget)) {
                return createIntermediateTarget(
                        target,
                        optional,
                        optional
                                ? new ParameterizedTypeReference<Optional<List<?>>>() {}
                                : new ParameterizedTypeReference<List<?>>() {});
            } else if (ImmutableSet.class.isAssignableFrom(originalTarget)) {
                return createIntermediateTarget(
                        target,
                        optional,
                        optional
                                ? new ParameterizedTypeReference<Optional<Set<?>>>() {}
                                : new ParameterizedTypeReference<Set<?>>() {});
            } else if (ImmutableMap.class.isAssignableFrom(originalTarget)) {
                return createIntermediateTarget(
                        target,
                        optional,
                        optional
                                ? new ParameterizedTypeReference<Optional<Map<?, ?>>>() {}
                                : new ParameterizedTypeReference<Map<?, ?>>() {});
            }
            return super.onStart(name, target, context);
        }

        @Override
        public Object onSuccess(
                ConfigurationPropertyName name,
                Bindable<?> target,
                BindContext context,
                Object result) {
            Optional<GuavaCollection> guavaCollection =
                    Arrays.stream(target.getAnnotations())
                            .filter(GuavaCollection.class::isInstance)
                            .findFirst()
                            .map(GuavaCollection.class::cast);
            if (guavaCollection.isPresent()) {
                var optional = guavaCollection.orElseThrow().optional();
                Class<?> intermediateTarget;
                if (optional
                        && target.getType().getRawClass() != null
                        && Optional.class.equals(target.getType().getRawClass())) {
                    intermediateTarget = target.getType().getGenerics()[0].resolve(Object.class);
                } else {
                    intermediateTarget = target.getType().resolve(Object.class);
                }
                if (List.class.isAssignableFrom(intermediateTarget)) {
                    var immutableResult = ImmutableList.copyOf((List<?>) result);
                    return optional ? Optional.of(immutableResult) : immutableResult;
                }
                if (Set.class.isAssignableFrom(intermediateTarget)) {
                    var immutableResult = ImmutableSet.copyOf((Set<?>) result);
                    return optional ? Optional.of(immutableResult) : immutableResult;
                }
                if (Map.class.isAssignableFrom(intermediateTarget)) {
                    var immutableResult = ImmutableMap.copyOf((Map<?, ?>) result);
                    return optional ? Optional.of(immutableResult) : immutableResult;
                }
            }
            return super.onSuccess(name, target, context, result);
        }

        private static <T> Bindable<T> createIntermediateTarget(
                Bindable<T> target, boolean optional, ParameterizedTypeReference<?> typeReference) {
            return Bindable.<T>of(ResolvableType.forType(typeReference))
                    .withSuppliedValue(target.getValue())
                    .withAnnotations(markAsGuavaCollection(target, optional));
        }

        private static Annotation[] markAsGuavaCollection(Bindable<?> target, boolean optional) {
            @Var
            var annotations =
                    Arrays.copyOf(target.getAnnotations(), target.getAnnotations().length + 1);
            // Inject a custom annotation to bindable target so other targets are not affected.
            annotations[target.getAnnotations().length] =
                    optional ? OPTIONAL_GUAVA_COLLECTION : GUAVA_COLLECTION;
            return annotations;
        }
    }
}
