package dev.gokhun.guavaspringconfigprops;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

final class ApplicationTests {

    @Nested
    @SpringBootTest(classes = ImmutableListTest.Config.class)
    @SuppressWarnings("ClassCanBeStatic")
    final class ImmutableListTest {
        @Autowired Demo demo;

        @ConfigurationProperties("demo")
        @ConstructorBinding
        record Demo(
                @DefaultValue List<String> myList,
                @DefaultValue ImmutableList<String> myImmutableList,
                ImmutableSet<Integer> myImmutableSet,
                ImmutableMap<String, Boolean> myImmutableMap) {}

        @Test
        void contextLoads() {
            assertThat(demo).isNotNull();
            assertThat(demo.myList()).contains("hehe", "haha");
            assertThat(demo.myImmutableList()).contains("hihi", "huhu");
            assertThat(demo.myImmutableSet()).contains(1, 2, 3);
            assertThat(demo.myImmutableMap())
                    .containsExactlyEntriesOf(ImmutableMap.of("demo", true, "omed", false));
        }

        @EnableConfigurationProperties(Demo.class)
        @Import(ImmutableCollectionAdvisor.class)
        static final class Config {}
    }
}
