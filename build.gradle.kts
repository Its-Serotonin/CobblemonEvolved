
plugins {
	application
	id("fabric-loom") version "1.10-SNAPSHOT"
	id("maven-publish")
	id("org.jetbrains.kotlin.jvm") version "2.2.20"
	id("java")
	kotlin("plugin.serialization") version "2.2.20"

}

val shadeDeps by configurations.creating {
	isCanBeResolved = true
	isCanBeConsumed = false
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

base {
	archivesName.set(project.property("archives_base_name") as String)
}

fabricApi {
	configureDataGeneration() {
		client = true
	}
}


repositories {

	mavenCentral()
	maven("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/")
	maven("https://maven.impactdev.net/repository/development/")
	maven("https://oss.sonatype.org/content/repositories/snapshots")
	maven("https://maven.terraformersmc.com/")
	maven("https://maven.ladysnake.org/releases")
	maven("https://jitpack.io")
	maven("https://maven.architectury.dev/")
	maven("https://maven.wispforest.io/releases")


	maven { url = uri("https://maven.fabricmc.net/") }
	flatDir{
		dirs("libs")
	}

	exclusiveContent {
		forRepository {
			maven {
				name = "Modrinth"
				url = uri("https://api.modrinth.com/maven")
			}
		}
		filter {
			includeGroup("maven.modrinth")
		}
	}
}


dependencies {

	implementation("org.jetbrains.kotlin:kotlin-stdlib")
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")

	minecraft("net.minecraft:minecraft:1.21.1")
	mappings("net.fabricmc:yarn:1.21.1+build.3:v2")
	modImplementation("dev.architectury:architectury-fabric:16.1.4")

	modImplementation("net.fabricmc:fabric-loader:0.18.5")
	modImplementation("net.fabricmc:fabric-language-kotlin:1.13.10+kotlin.2.3.20")
	modImplementation("net.fabricmc.fabric-api:fabric-api:0.116.9+1.21.1")
	modImplementation(fabricApi.module("fabric-command-api-v2", "0.104.0+1.21.1"))

	modImplementation("com.cobblemon:fabric:1.7.3+1.21.1")
	modImplementation("io.wispforest:accessories-fabric:1.1.0-beta.53+1.21.1")


	shadeDeps("com.zaxxer:HikariCP:6.3.0")
	shadeDeps("org.postgresql:postgresql:42.7.3")
	implementation("com.zaxxer:HikariCP:6.3.0")
	implementation("org.postgresql:postgresql:42.7.3")

	compileOnly("net.luckperms:api:5.4")

// === Local Mod Jars ===
	modImplementation(files("libs/Jade-1.21.1-Fabric-15.10.5.jar"))

	modImplementation("dev.emi:trinkets:3.10.0")

	modImplementation(files("libs/CobbleDollars-fabric-2.0.0+Beta-5.1+1.21.1.jar"))
	modImplementation(files("libs/Cobblemon-Utility+-fabric-1.7.3.jar"))
	modImplementation(files("libs/cobgyms-fabric-3.0.1+1.21.1.jar"))
	modImplementation(files("libs/SimpleTMs-fabric-2.3.3.jar"))


	val backpacksJar = files("libs/sophisticatedbackpacks-1.21.1-3.23.4.3.106.jar")
	modImplementation(backpacksJar)
	implementation(backpacksJar)
	annotationProcessor(backpacksJar)

	implementation("com.squareup.okhttp3:okhttp:4.12.0")
	implementation("org.json:json:20240303")

	val devBackpackJar = file("libs/sophisticatedbackpacks-1.21.1-named.jar")
	if (devBackpackJar.exists()) {
		modLocalRuntime(devBackpackJar)
	}
}


tasks.processResources {
	inputs.property ("version", project.version)

	filesMatching("fabric.mod.json") {
		expand ("version" to project.version)

		//duplicatesStrategy = DuplicatesStrategy.EXCLUDE
	}
}



 tasks.withType<JavaCompile>().configureEach {
	options.release.set(21)
}


tasks.register<net.fabricmc.loom.task.RemapJarTask>("remapSophisticatedBackpacks") {
	inputFile.set(file("libs/sophisticatedbackpacks-1.21.1-3.23.4.3.106.jar"))
	archiveClassifier.set("named")
	addNestedDependencies.set(false)
	targetNamespace.set("named")
}

tasks.register<Copy>("copyRemappedBackpackJar") {
	dependsOn("remapSophisticatedBackpacks")
	from(layout.buildDirectory.map {
		it.dir("libs").file("sophisticatedbackpacks-1.21.1-3.23.4.3.106-named.jar").asFile
	})
	into("libs/")
	rename { "sophisticatedbackpacks-1.21.1-named.jar" }
}

tasks.named("runClient") {
	dependsOn("copyRemappedBackpackJar")
}

java {
	// Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
	// if it is present.
	// If you remove this line, sources will not be generated.
	withSourcesJar()

	sourceCompatibility = JavaVersion.VERSION_21
	targetCompatibility = JavaVersion.VERSION_21
}

tasks.jar {
	inputs.property ("archivesName", project.base.archivesName)

	from("LICENSE") {
		rename { "${it}_${base.archivesName.get()}"}
	}
}

// configure the maven publication
publishing {
	publications {
		create <MavenPublication> ("mavenJava") {
			groupId = "com.gradle.serotonin"
			artifactId = project.name
			from (components["java"])
		}
	}

	// See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
	repositories {
		// Add repositories to publish to here.
		// Notice: This block does NOT have the same function as the block in the top level.
		// The repositories here will be used for publishing your artifact, not for
		// retrieving dependencies.
	}
}


kotlin{
	jvmToolchain(21)
}

tasks.withType<Jar>().configureEach {
	doFirst {
		from(
			configurations.getByName("shadeDeps").filter {
				it.name.contains("HikariCP", ignoreCase = true)
						|| it.name.contains("postgresql", ignoreCase = true)


			}.map { zipTree(it) }
		)
	}
}