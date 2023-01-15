package dev.gokhun.guavaspringconfigprops;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

final class ApplicationTests {

    @Nested
    @SpringBootTest(classes = HappyCollectionsTest.Config.class)
    @ActiveProfiles("happy")
    @SuppressWarnings("ClassCanBeStatic")
    final class HappyCollectionsTest {
        @Autowired Happy happy;

        @DisplayName("Should bind java and guava collections")
        @Test
        void bind() {
            assertThat(happy).isNotNull();

            // Mutable collections are still working
            assertThat(happy.mutableList()).isInstanceOf(ArrayList.class);
            assertThat(happy.mutableList()).contains("item 1", "item 2");
            assertThat(happy.mutableSet()).isInstanceOf(HashSet.class);
            assertThat(happy.mutableSet()).contains("item 1", "item 2");
            assertThat(happy.mutableMap()).isInstanceOf(HashMap.class);
            assertThat(happy.mutableMap())
                    .containsExactlyInAnyOrderEntriesOf(
                            Map.of("key1", "value1", "key2", "value2" /* No Refaster */));

            // Immutable collections are bound to variables
            assertThat(happy.immutableList()).contains("item 1", "item 2");
            assertThat(happy.mutableSet()).contains("item 1", "item 2");
            assertThat(happy.immutableMap())
                    .containsExactlyInAnyOrderEntriesOf(
                            ImmutableMap.of("key1", "value1", "key2", "value2"));
        }

        @ConfigurationProperties("happy")
        @ConstructorBinding
        record Happy(
                List<String> mutableList,
                Set<String> mutableSet,
                Map<String, String> mutableMap,
                ImmutableList<String> immutableList,
                ImmutableSet<Integer> immutableSet,
                ImmutableMap<String, String> immutableMap) {}

        @EnableConfigurationProperties(Happy.class)
        @Import(ImmutableCollectionAdvisor.class)
        static final class Config {}
    }

    @Nested
    @SpringBootTest(classes = DefaultValuesTest.Config.class)
    @ActiveProfiles("default-values")
    @SuppressWarnings("ClassCanBeStatic")
    final class DefaultValuesTest {
        @Autowired DefaultValues defaultValues;

        @DisplayName("Should bind java and guava collections with default values")
        @Test
        void bind() {
            assertThat(defaultValues).isNotNull();
            assertThat(defaultValues.immutableList()).isEmpty();
            assertThat(defaultValues.immutableSet()).isEmpty();
            assertThat(defaultValues.immutableMap()).isEmpty();
        }

        @ConfigurationProperties("default-values")
        @ConstructorBinding
        record DefaultValues(
                @DefaultValue Optional<ImmutableList<String>> immutableList,
                @DefaultValue Optional<ImmutableSet<String>> immutableSet,
                @DefaultValue Optional<ImmutableMap<String, String>> immutableMap) {}

        @EnableConfigurationProperties(DefaultValues.class)
        @Import(ImmutableCollectionAdvisor.class)
        static final class Config {}
    }
}
