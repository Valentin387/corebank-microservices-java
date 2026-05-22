plugins {
    id("org.springframework.boot") version "4.0.6" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

allprojects {
    group = "com.corebank"
    version = "0.0.2-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "jacoco")

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    tasks.withType<JacocoReport> {
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

    tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
        dependsOn("jacocoTestReport")

        violationRules {
            rule {
                limit {
                    minimum = "0.50".toBigDecimal()
                }
            }
        }

        doLast {
            val reportFile = project.layout.buildDirectory.file("reports/jacoco/test/jacocoTestReport.xml").get().asFile
            if (reportFile.exists()) {
                val factory = javax.xml.parsers.DocumentBuilderFactory.newInstance().apply {
                    setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
                    setFeature("http://xml.org/sax/features/external-general-entities", false)
                    setFeature("http://xml.org/sax/features/external-parameter-entities", false)
                }
                val xml = factory.newDocumentBuilder().parse(reportFile)
                val counters = xml.documentElement.childNodes
                println("\n${"=".repeat(60)}")
                println("  JaCoCo Coverage Summary  —  :${project.name}")
                println("${"=".repeat(60)}")
                for (i in 0 until counters.length) {
                    val node = counters.item(i)
                    if (node.nodeName == "counter") {
                        val type = node.attributes.getNamedItem("type").nodeValue
                        val missed = node.attributes.getNamedItem("missed").nodeValue.toDouble()
                        val covered = node.attributes.getNamedItem("covered").nodeValue.toDouble()
                        val total = missed + covered
                        val pct = if (total > 0) (covered / total) * 100 else 0.0
                        println("  %-14s %6.2f%%   (%s/%s)".format(type, pct, covered.toInt(), total.toInt()))
                    }
                }
                println("${"=".repeat(60)}\n")
            } else {
                println("⚠ JaCoCo XML report not found at: ${reportFile.absolutePath}")
            }
        }
    }

    tasks.named("check") {
        dependsOn("jacocoTestCoverageVerification")
    }
}