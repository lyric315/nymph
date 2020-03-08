package com.nymph.plugin.apt

import com.nymph.plugin.ext.toRegularName
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.Diagnostic

class ServiceFactoryApt : AbstractProcessor() {
    private lateinit var messager: Messager
    private lateinit var typeUtils: Types
    private lateinit var elementUtils: Elements
    private lateinit var filer: Filer

    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        //初始化我们需要的基础工具
        messager = processingEnv.messager
        typeUtils = processingEnv.typeUtils
        elementUtils = processingEnv.elementUtils
        filer = processingEnv.filer
    }

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        print("================ServiceFactoryApt===================")
        val lineSeparator = System.getProperty("line.separator")
        annotations.forEach { annotation ->
            roundEnv.getElementsAnnotatedWith(annotation).forEach { element ->

                var protocolClass: TypeMirror? = null

                element.annotationMirrors.forEach element@{ am ->
                    if (SERVICE_FACTORY_CLASS_NAME.toRegularName() == am.annotationType.toString()) {
                        am.elementValues.forEach elementValues@{ (executableElement, annotationValue) ->
                            if ("protocol" == executableElement.simpleName.toString()) {
                                protocolClass = annotationValue.value as? TypeMirror
                                return@elementValues
                            }
                        }

                        return@element
                    }
                }

                if (protocolClass == null) {
                    messager.printMessage(Diagnostic.Kind.ERROR, "must set protocol class", element)
                    throw IllegalStateException()
                }

                val source = buildString {
                    val protocolClassElement = typeUtils.asElement(protocolClass)
                    elementUtils.getPackageOf(protocolClassElement).apply {
                        if (!isUnnamed) {
                            append("package $qualifiedName;$lineSeparator")
                        }
                    }

                    append("final class ${protocolClassElement.simpleName}_sf_ {$lineSeparator")
                    append("    private ${protocolClassElement.simpleName}_sf_() {}$lineSeparator")
                    append("    private static final String SERVICE_FACTORY_CLASS_NAME = \"$element\";$lineSeparator}")
                }

                filer.createSourceFile("${protocolClass}_sf_").openOutputStream().use {
                    it.write(source.toByteArray())
                    print(source)
                }

                print("================ServiceFactoryApt  end===================")

            }
        }
        return true
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        //支持的java版本
        return SourceVersion.latestSupported()
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        //支持的注解
        var annottions = hashSetOf<String>()
        annottions.add(SERVICE_FACTORY_CLASS_NAME.toRegularName())
        return annottions
    }
}