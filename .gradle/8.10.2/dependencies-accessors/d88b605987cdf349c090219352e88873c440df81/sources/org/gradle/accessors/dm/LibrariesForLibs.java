package org.gradle.accessors.dm;

import org.gradle.api.NonNullApi;
import org.gradle.api.artifacts.MinimalExternalModuleDependency;
import org.gradle.plugin.use.PluginDependency;
import org.gradle.api.artifacts.ExternalModuleDependencyBundle;
import org.gradle.api.artifacts.MutableVersionConstraint;
import org.gradle.api.provider.Provider;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.internal.catalog.AbstractExternalDependencyFactory;
import org.gradle.api.internal.catalog.DefaultVersionCatalog;
import java.util.Map;
import org.gradle.api.internal.attributes.ImmutableAttributesFactory;
import org.gradle.api.internal.artifacts.dsl.CapabilityNotationParser;
import javax.inject.Inject;

/**
 * A catalog of dependencies accessible via the {@code libs} extension.
 */
@NonNullApi
public class LibrariesForLibs extends AbstractExternalDependencyFactory {

    private final AbstractExternalDependencyFactory owner = this;
    private final AdventureLibraryAccessors laccForAdventureLibraryAccessors = new AdventureLibraryAccessors(owner);
    private final AutoLibraryAccessors laccForAutoLibraryAccessors = new AutoLibraryAccessors(owner);
    private final CheckerLibraryAccessors laccForCheckerLibraryAccessors = new CheckerLibraryAccessors(owner);
    private final Configurate3LibraryAccessors laccForConfigurate3LibraryAccessors = new Configurate3LibraryAccessors(owner);
    private final Configurate4LibraryAccessors laccForConfigurate4LibraryAccessors = new Configurate4LibraryAccessors(owner);
    private final FlareLibraryAccessors laccForFlareLibraryAccessors = new FlareLibraryAccessors(owner);
    private final KyoriLibraryAccessors laccForKyoriLibraryAccessors = new KyoriLibraryAccessors(owner);
    private final Log4jLibraryAccessors laccForLog4jLibraryAccessors = new Log4jLibraryAccessors(owner);
    private final NettyLibraryAccessors laccForNettyLibraryAccessors = new NettyLibraryAccessors(owner);
    private final SpotbugsLibraryAccessors laccForSpotbugsLibraryAccessors = new SpotbugsLibraryAccessors(owner);
    private final VersionAccessors vaccForVersionAccessors = new VersionAccessors(providers, config);
    private final BundleAccessors baccForBundleAccessors = new BundleAccessors(objects, providers, config, attributesFactory, capabilityNotationParser);
    private final PluginAccessors paccForPluginAccessors = new PluginAccessors(providers, config);

    @Inject
    public LibrariesForLibs(DefaultVersionCatalog config, ProviderFactory providers, ObjectFactory objects, ImmutableAttributesFactory attributesFactory, CapabilityNotationParser capabilityNotationParser) {
        super(config, providers, objects, attributesFactory, capabilityNotationParser);
    }

    /**
     * Dependency provider for <b>asm</b> with <b>org.ow2.asm:asm</b> coordinates and
     * with version <b>9.7.1</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getAsm() {
        return create("asm");
    }

    /**
     * Dependency provider for <b>brigadier</b> with <b>com.velocitypowered:velocity-brigadier</b> coordinates and
     * with version <b>1.0.0-SNAPSHOT</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getBrigadier() {
        return create("brigadier");
    }

    /**
     * Dependency provider for <b>bstats</b> with <b>org.bstats:bstats-base</b> coordinates and
     * with version <b>3.1.0</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getBstats() {
        return create("bstats");
    }

    /**
     * Dependency provider for <b>caffeine</b> with <b>com.github.ben-manes.caffeine:caffeine</b> coordinates and
     * with version <b>3.1.8</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getCaffeine() {
        return create("caffeine");
    }

    /**
     * Dependency provider for <b>checkstyle</b> with <b>com.puppycrawl.tools:checkstyle</b> coordinates and
     * with version <b>10.18.2</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getCheckstyle() {
        return create("checkstyle");
    }

    /**
     * Dependency provider for <b>completablefutures</b> with <b>com.spotify:completable-futures</b> coordinates and
     * with version <b>0.3.6</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getCompletablefutures() {
        return create("completablefutures");
    }

    /**
     * Dependency provider for <b>disruptor</b> with <b>com.lmax:disruptor</b> coordinates and
     * with version <b>4.0.0</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getDisruptor() {
        return create("disruptor");
    }

    /**
     * Dependency provider for <b>fastutil</b> with <b>it.unimi.dsi:fastutil</b> coordinates and
     * with version <b>8.5.15</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getFastutil() {
        return create("fastutil");
    }

    /**
     * Dependency provider for <b>gson</b> with <b>com.google.code.gson:gson</b> coordinates and
     * with version <b>2.11.0</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getGson() {
        return create("gson");
    }

    /**
     * Dependency provider for <b>guava</b> with <b>com.google.guava:guava</b> coordinates and
     * with version <b>33.3.1-jre</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getGuava() {
        return create("guava");
    }

    /**
     * Dependency provider for <b>guice</b> with <b>com.google.inject:guice</b> coordinates and
     * with version <b>7.0.0</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getGuice() {
        return create("guice");
    }

    /**
     * Dependency provider for <b>jline</b> with <b>org.jline:jline-terminal-jansi</b> coordinates and
     * with version <b>3.27.1</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getJline() {
        return create("jline");
    }

    /**
     * Dependency provider for <b>jopt</b> with <b>net.sf.jopt-simple:jopt-simple</b> coordinates and
     * with version <b>5.0.4</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getJopt() {
        return create("jopt");
    }

    /**
     * Dependency provider for <b>jspecify</b> with <b>org.jspecify:jspecify</b> coordinates and
     * with version <b>1.0.0</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getJspecify() {
        return create("jspecify");
    }

    /**
     * Dependency provider for <b>junit</b> with <b>org.junit.jupiter:junit-jupiter</b> coordinates and
     * with version <b>5.11.3</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getJunit() {
        return create("junit");
    }

    /**
     * Dependency provider for <b>lmbda</b> with <b>org.lanternpowered:lmbda</b> coordinates and
     * with version <b>2.0.0</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getLmbda() {
        return create("lmbda");
    }

    /**
     * Dependency provider for <b>mockito</b> with <b>org.mockito:mockito-core</b> coordinates and
     * with version <b>5.14.2</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getMockito() {
        return create("mockito");
    }

    /**
     * Dependency provider for <b>nightconfig</b> with <b>com.electronwill.night-config:toml</b> coordinates and
     * with version <b>3.8.1</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getNightconfig() {
        return create("nightconfig");
    }

    /**
     * Dependency provider for <b>slf4j</b> with <b>org.slf4j:slf4j-api</b> coordinates and
     * with version <b>2.0.16</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getSlf4j() {
        return create("slf4j");
    }

    /**
     * Dependency provider for <b>snakeyaml</b> with <b>org.yaml:snakeyaml</b> coordinates and
     * with version <b>1.33</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getSnakeyaml() {
        return create("snakeyaml");
    }

    /**
     * Dependency provider for <b>terminalconsoleappender</b> with <b>net.minecrell:terminalconsoleappender</b> coordinates and
     * with version <b>1.3.0</b>
     * <p>
     * This dependency was declared in catalog libs.versions.toml
     */
    public Provider<MinimalExternalModuleDependency> getTerminalconsoleappender() {
        return create("terminalconsoleappender");
    }

    /**
     * Group of libraries at <b>adventure</b>
     */
    public AdventureLibraryAccessors getAdventure() {
        return laccForAdventureLibraryAccessors;
    }

    /**
     * Group of libraries at <b>auto</b>
     */
    public AutoLibraryAccessors getAuto() {
        return laccForAutoLibraryAccessors;
    }

    /**
     * Group of libraries at <b>checker</b>
     */
    public CheckerLibraryAccessors getChecker() {
        return laccForCheckerLibraryAccessors;
    }

    /**
     * Group of libraries at <b>configurate3</b>
     */
    public Configurate3LibraryAccessors getConfigurate3() {
        return laccForConfigurate3LibraryAccessors;
    }

    /**
     * Group of libraries at <b>configurate4</b>
     */
    public Configurate4LibraryAccessors getConfigurate4() {
        return laccForConfigurate4LibraryAccessors;
    }

    /**
     * Group of libraries at <b>flare</b>
     */
    public FlareLibraryAccessors getFlare() {
        return laccForFlareLibraryAccessors;
    }

    /**
     * Group of libraries at <b>kyori</b>
     */
    public KyoriLibraryAccessors getKyori() {
        return laccForKyoriLibraryAccessors;
    }

    /**
     * Group of libraries at <b>log4j</b>
     */
    public Log4jLibraryAccessors getLog4j() {
        return laccForLog4jLibraryAccessors;
    }

    /**
     * Group of libraries at <b>netty</b>
     */
    public NettyLibraryAccessors getNetty() {
        return laccForNettyLibraryAccessors;
    }

    /**
     * Group of libraries at <b>spotbugs</b>
     */
    public SpotbugsLibraryAccessors getSpotbugs() {
        return laccForSpotbugsLibraryAccessors;
    }

    /**
     * Group of versions at <b>versions</b>
     */
    public VersionAccessors getVersions() {
        return vaccForVersionAccessors;
    }

    /**
     * Group of bundles at <b>bundles</b>
     */
    public BundleAccessors getBundles() {
        return baccForBundleAccessors;
    }

    /**
     * Group of plugins at <b>plugins</b>
     */
    public PluginAccessors getPlugins() {
        return paccForPluginAccessors;
    }

    public static class AdventureLibraryAccessors extends SubDependencyFactory {

        public AdventureLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>bom</b> with <b>net.kyori:adventure-bom</b> coordinates and
         * with version <b>4.17.0</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getBom() {
            return create("adventure.bom");
        }

        /**
         * Dependency provider for <b>facet</b> with <b>net.kyori:adventure-platform-facet</b> coordinates and
         * with version <b>4.3.4</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getFacet() {
            return create("adventure.facet");
        }

    }

    public static class AutoLibraryAccessors extends SubDependencyFactory {
        private final AutoServiceLibraryAccessors laccForAutoServiceLibraryAccessors = new AutoServiceLibraryAccessors(owner);

        public AutoLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>auto.service</b>
         */
        public AutoServiceLibraryAccessors getService() {
            return laccForAutoServiceLibraryAccessors;
        }

    }

    public static class AutoServiceLibraryAccessors extends SubDependencyFactory implements DependencyNotationSupplier {

        public AutoServiceLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>service</b> with <b>com.google.auto.service:auto-service</b> coordinates and
         * with version <b>1.1.1</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> asProvider() {
            return create("auto.service");
        }

        /**
         * Dependency provider for <b>annotations</b> with <b>com.google.auto.service:auto-service-annotations</b> coordinates and
         * with version <b>1.1.1</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getAnnotations() {
            return create("auto.service.annotations");
        }

    }

    public static class CheckerLibraryAccessors extends SubDependencyFactory {

        public CheckerLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>qual</b> with <b>org.checkerframework:checker-qual</b> coordinates and
         * with version <b>3.48.1</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getQual() {
            return create("checker.qual");
        }

    }

    public static class Configurate3LibraryAccessors extends SubDependencyFactory {

        public Configurate3LibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>gson</b> with <b>org.spongepowered:configurate-gson</b> coordinates and
         * with version reference <b>configurate3</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getGson() {
            return create("configurate3.gson");
        }

        /**
         * Dependency provider for <b>hocon</b> with <b>org.spongepowered:configurate-hocon</b> coordinates and
         * with version reference <b>configurate3</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getHocon() {
            return create("configurate3.hocon");
        }

        /**
         * Dependency provider for <b>yaml</b> with <b>org.spongepowered:configurate-yaml</b> coordinates and
         * with version reference <b>configurate3</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getYaml() {
            return create("configurate3.yaml");
        }

    }

    public static class Configurate4LibraryAccessors extends SubDependencyFactory {

        public Configurate4LibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>gson</b> with <b>org.spongepowered:configurate-gson</b> coordinates and
         * with version reference <b>configurate4</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getGson() {
            return create("configurate4.gson");
        }

        /**
         * Dependency provider for <b>hocon</b> with <b>org.spongepowered:configurate-hocon</b> coordinates and
         * with version reference <b>configurate4</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getHocon() {
            return create("configurate4.hocon");
        }

        /**
         * Dependency provider for <b>yaml</b> with <b>org.spongepowered:configurate-yaml</b> coordinates and
         * with version reference <b>configurate4</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getYaml() {
            return create("configurate4.yaml");
        }

    }

    public static class FlareLibraryAccessors extends SubDependencyFactory {

        public FlareLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>core</b> with <b>space.vectrix.flare:flare</b> coordinates and
         * with version reference <b>flare</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getCore() {
            return create("flare.core");
        }

        /**
         * Dependency provider for <b>fastutil</b> with <b>space.vectrix.flare:flare-fastutil</b> coordinates and
         * with version reference <b>flare</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getFastutil() {
            return create("flare.fastutil");
        }

    }

    public static class KyoriLibraryAccessors extends SubDependencyFactory {

        public KyoriLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>ansi</b> with <b>net.kyori:ansi</b> coordinates and
         * with version <b>1.1.0</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getAnsi() {
            return create("kyori.ansi");
        }

    }

    public static class Log4jLibraryAccessors extends SubDependencyFactory {
        private final Log4jSlf4jLibraryAccessors laccForLog4jSlf4jLibraryAccessors = new Log4jSlf4jLibraryAccessors(owner);

        public Log4jLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>api</b> with <b>org.apache.logging.log4j:log4j-api</b> coordinates and
         * with version reference <b>log4j</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getApi() {
            return create("log4j.api");
        }

        /**
         * Dependency provider for <b>core</b> with <b>org.apache.logging.log4j:log4j-core</b> coordinates and
         * with version reference <b>log4j</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getCore() {
            return create("log4j.core");
        }

        /**
         * Dependency provider for <b>iostreams</b> with <b>org.apache.logging.log4j:log4j-iostreams</b> coordinates and
         * with version reference <b>log4j</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getIostreams() {
            return create("log4j.iostreams");
        }

        /**
         * Dependency provider for <b>jul</b> with <b>org.apache.logging.log4j:log4j-jul</b> coordinates and
         * with version reference <b>log4j</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getJul() {
            return create("log4j.jul");
        }

        /**
         * Group of libraries at <b>log4j.slf4j</b>
         */
        public Log4jSlf4jLibraryAccessors getSlf4j() {
            return laccForLog4jSlf4jLibraryAccessors;
        }

    }

    public static class Log4jSlf4jLibraryAccessors extends SubDependencyFactory {

        public Log4jSlf4jLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>impl</b> with <b>org.apache.logging.log4j:log4j-slf4j2-impl</b> coordinates and
         * with version reference <b>log4j</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getImpl() {
            return create("log4j.slf4j.impl");
        }

    }

    public static class NettyLibraryAccessors extends SubDependencyFactory {
        private final NettyCodecLibraryAccessors laccForNettyCodecLibraryAccessors = new NettyCodecLibraryAccessors(owner);
        private final NettyTransportLibraryAccessors laccForNettyTransportLibraryAccessors = new NettyTransportLibraryAccessors(owner);

        public NettyLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>handler</b> with <b>io.netty:netty-handler</b> coordinates and
         * with version reference <b>netty</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getHandler() {
            return create("netty.handler");
        }

        /**
         * Group of libraries at <b>netty.codec</b>
         */
        public NettyCodecLibraryAccessors getCodec() {
            return laccForNettyCodecLibraryAccessors;
        }

        /**
         * Group of libraries at <b>netty.transport</b>
         */
        public NettyTransportLibraryAccessors getTransport() {
            return laccForNettyTransportLibraryAccessors;
        }

    }

    public static class NettyCodecLibraryAccessors extends SubDependencyFactory implements DependencyNotationSupplier {

        public NettyCodecLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>codec</b> with <b>io.netty:netty-codec</b> coordinates and
         * with version reference <b>netty</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> asProvider() {
            return create("netty.codec");
        }

        /**
         * Dependency provider for <b>haproxy</b> with <b>io.netty:netty-codec-haproxy</b> coordinates and
         * with version reference <b>netty</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getHaproxy() {
            return create("netty.codec.haproxy");
        }

        /**
         * Dependency provider for <b>http</b> with <b>io.netty:netty-codec-http</b> coordinates and
         * with version reference <b>netty</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getHttp() {
            return create("netty.codec.http");
        }

    }

    public static class NettyTransportLibraryAccessors extends SubDependencyFactory {
        private final NettyTransportNativeLibraryAccessors laccForNettyTransportNativeLibraryAccessors = new NettyTransportNativeLibraryAccessors(owner);

        public NettyTransportLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Group of libraries at <b>netty.transport.native</b>
         */
        public NettyTransportNativeLibraryAccessors getNative() {
            return laccForNettyTransportNativeLibraryAccessors;
        }

    }

    public static class NettyTransportNativeLibraryAccessors extends SubDependencyFactory {

        public NettyTransportNativeLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>epoll</b> with <b>io.netty:netty-transport-native-epoll</b> coordinates and
         * with version reference <b>netty</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getEpoll() {
            return create("netty.transport.native.epoll");
        }

        /**
         * Dependency provider for <b>kqueue</b> with <b>io.netty:netty-transport-native-kqueue</b> coordinates and
         * with version reference <b>netty</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getKqueue() {
            return create("netty.transport.native.kqueue");
        }

    }

    public static class SpotbugsLibraryAccessors extends SubDependencyFactory {

        public SpotbugsLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

        /**
         * Dependency provider for <b>annotations</b> with <b>com.github.spotbugs:spotbugs-annotations</b> coordinates and
         * with version <b>4.8.6</b>
         * <p>
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getAnnotations() {
            return create("spotbugs.annotations");
        }

    }

    public static class VersionAccessors extends VersionFactory  {

        public VersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Version alias <b>configurate3</b> with value <b>3.7.3</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getConfigurate3() { return getVersion("configurate3"); }

        /**
         * Version alias <b>configurate4</b> with value <b>4.1.2</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getConfigurate4() { return getVersion("configurate4"); }

        /**
         * Version alias <b>flare</b> with value <b>2.0.1</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getFlare() { return getVersion("flare"); }

        /**
         * Version alias <b>log4j</b> with value <b>2.24.1</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getLog4j() { return getVersion("log4j"); }

        /**
         * Version alias <b>netty</b> with value <b>4.1.114.Final</b>
         * <p>
         * If the version is a rich version and cannot be represented as a
         * single version string, an empty string is returned.
         * <p>
         * This version was declared in catalog libs.versions.toml
         */
        public Provider<String> getNetty() { return getVersion("netty"); }

    }

    public static class BundleAccessors extends BundleFactory {

        public BundleAccessors(ObjectFactory objects, ProviderFactory providers, DefaultVersionCatalog config, ImmutableAttributesFactory attributesFactory, CapabilityNotationParser capabilityNotationParser) { super(objects, providers, config, attributesFactory, capabilityNotationParser); }

        /**
         * Dependency bundle provider for <b>configurate3</b> which contains the following dependencies:
         * <ul>
         *    <li>org.spongepowered:configurate-hocon</li>
         *    <li>org.spongepowered:configurate-yaml</li>
         *    <li>org.spongepowered:configurate-gson</li>
         * </ul>
         * <p>
         * This bundle was declared in catalog libs.versions.toml
         */
        public Provider<ExternalModuleDependencyBundle> getConfigurate3() {
            return createBundle("configurate3");
        }

        /**
         * Dependency bundle provider for <b>configurate4</b> which contains the following dependencies:
         * <ul>
         *    <li>org.spongepowered:configurate-hocon</li>
         *    <li>org.spongepowered:configurate-yaml</li>
         *    <li>org.spongepowered:configurate-gson</li>
         * </ul>
         * <p>
         * This bundle was declared in catalog libs.versions.toml
         */
        public Provider<ExternalModuleDependencyBundle> getConfigurate4() {
            return createBundle("configurate4");
        }

        /**
         * Dependency bundle provider for <b>flare</b> which contains the following dependencies:
         * <ul>
         *    <li>space.vectrix.flare:flare</li>
         *    <li>space.vectrix.flare:flare-fastutil</li>
         * </ul>
         * <p>
         * This bundle was declared in catalog libs.versions.toml
         */
        public Provider<ExternalModuleDependencyBundle> getFlare() {
            return createBundle("flare");
        }

        /**
         * Dependency bundle provider for <b>log4j</b> which contains the following dependencies:
         * <ul>
         *    <li>org.apache.logging.log4j:log4j-api</li>
         *    <li>org.apache.logging.log4j:log4j-core</li>
         *    <li>org.apache.logging.log4j:log4j-slf4j2-impl</li>
         *    <li>org.apache.logging.log4j:log4j-iostreams</li>
         *    <li>org.apache.logging.log4j:log4j-jul</li>
         * </ul>
         * <p>
         * This bundle was declared in catalog libs.versions.toml
         */
        public Provider<ExternalModuleDependencyBundle> getLog4j() {
            return createBundle("log4j");
        }

    }

    public static class PluginAccessors extends PluginFactory {
        private final IndraPluginAccessors paccForIndraPluginAccessors = new IndraPluginAccessors(providers, config);

        public PluginAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Plugin provider for <b>shadow</b> with plugin id <b>com.gradleup.shadow</b> and
         * with version <b>8.3.3</b>
         * <p>
         * This plugin was declared in catalog libs.versions.toml
         */
        public Provider<PluginDependency> getShadow() { return createPlugin("shadow"); }

        /**
         * Plugin provider for <b>spotless</b> with plugin id <b>com.diffplug.spotless</b> and
         * with version <b>6.25.0</b>
         * <p>
         * This plugin was declared in catalog libs.versions.toml
         */
        public Provider<PluginDependency> getSpotless() { return createPlugin("spotless"); }

        /**
         * Group of plugins at <b>plugins.indra</b>
         */
        public IndraPluginAccessors getIndra() {
            return paccForIndraPluginAccessors;
        }

    }

    public static class IndraPluginAccessors extends PluginFactory {

        public IndraPluginAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Plugin provider for <b>indra.publishing</b> with plugin id <b>net.kyori.indra.publishing</b> and
         * with version <b>3.1.3</b>
         * <p>
         * This plugin was declared in catalog libs.versions.toml
         */
        public Provider<PluginDependency> getPublishing() { return createPlugin("indra.publishing"); }

    }

}
