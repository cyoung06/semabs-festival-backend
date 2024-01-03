import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "3.2.0-SNAPSHOT"
	id("io.spring.dependency-management") version "1.1.3"
	kotlin("jvm") version "1.9.20"
	kotlin("plugin.spring") version "1.9.20"
}

group = "kr.syeyoung"
version = "0.0.1-SNAPSHOT"

java {
	sourceCompatibility = JavaVersion.VERSION_17
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
	all {
		exclude(group="org.springframework.boot", module= "spring-boot-starter-logging")
		exclude(group="ch.qos.logback",module="logback-classic")
		exclude(group="org.apache.logging.log4j",module= "log4j-to-slf4j")
	}
}

repositories {
	mavenCentral()
	maven { url = uri("https://repo.spring.io/milestone") }
	maven { url = uri("https://repo.spring.io/snapshot") }
}
configurations.implementation {
	exclude(module="spring-boot-starter-tomcat")
	exclude(group="org.apache.tomcat")
}


dependencies {
	implementation("org.eclipse.jetty.websocket:websocket-client:9.4.49.v20220914")
	implementation("org.eclipse.jetty:jetty-util:9.4.49.v20220914")
	implementation("org.eclipse.jetty:jetty-io:9.4.49.v20220914")
	implementation("org.eclipse.jetty:jetty-http:9.4.49.v20220914")
	implementation("org.eclipse.jetty:jetty-client:9.4.49.v20220914")
	implementation("org.eclipse.jetty:jetty-alpn-client:9.4.49.v20220914")

	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("org.springframework.boot:spring-boot-starter-websocket")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
	compileOnly("org.projectlombok:lombok")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.projectreactor:reactor-test")
	implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.projectreactor:reactor-test")
	implementation(files("mpv-0.2.0.jar"))
	implementation("com.alibaba:fastjson:2.0.28")
	implementation("io.obs-websocket.community:client:2.0.0")
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs += "-Xjsr305=strict"
		jvmTarget = "17"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
