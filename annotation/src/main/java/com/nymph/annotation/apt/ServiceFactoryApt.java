package com.nymph.annotation.apt;

import java.io.Writer;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * Created by lirui on 2020-03-08.
 */
public class ServiceFactoryApt extends AbstractProcessor {
    private Messager messager;
    private Types typeUtils;
    private Elements elementUtils;
    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        //初始化我们需要的基础工具
        messager = processingEnv.getMessager();
        typeUtils = processingEnv.getTypeUtils();
        elementUtils = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        System.out.println("================ServiceFactoryApt===================");
        String lineSeparator = System.getProperty("line.separator");

        for (TypeElement annotation : annotations) {
            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(annotation);
            for (Element element : elements) {
                TypeMirror  protocolClass = null;
                List<? extends AnnotationMirror> annotationMirrors = element.getAnnotationMirrors();
                for (AnnotationMirror annotationMirror : annotationMirrors) {
                    if (toRegularName(AptConstaants.SERVICE_FACTORY_CLASS_NAME).endsWith(annotationMirror.getAnnotationType().toString())) {
                        Map<? extends ExecutableElement, ? extends AnnotationValue> elementsValues =  annotationMirror.getElementValues();
                        Set<? extends ExecutableElement> keys = elementsValues.keySet();
                        for (ExecutableElement key : keys) {
                            // find protocol key
                            if ("protocol".equals(key.getSimpleName().toString())) {
                                AnnotationValue annotationValue = elementsValues.get(key);
                                if (annotationValue.getValue() instanceof TypeMirror) {
                                    protocolClass = (TypeMirror) annotationValue.getValue();
                                }
                            }
                        }
                    }
                }

                if (protocolClass == null) {
                    messager.printMessage(Diagnostic.Kind.ERROR, "must set protocol class", element);
                    throw new IllegalStateException();
                }


                StringBuilder source = new StringBuilder();
                Element protocolClassElement = typeUtils.asElement(protocolClass);
                PackageElement packageElement = elementUtils.getPackageOf(protocolClassElement);
                if (!packageElement.isUnnamed()) {
                    Name qualifiedName = packageElement.getQualifiedName();
                    //package $s$qualifiedName;$lineSeparator
                    source.append("package " + qualifiedName + ";" + lineSeparator);
                }

                //final class ${protocolClassElement.simpleName}_sf_ {$lineSeparator
                String className = protocolClassElement.getSimpleName() + "_sf_";
                source.append("final class " + className + " {" + lineSeparator);
                //private ${protocolClassElement.simpleName}_sf_() {}$lineSeparator
                source.append("     private " + className + "()" + "{}" + lineSeparator);
                //private static final String SERVICE_FACTORY_CLASS_NAME = "$element";$lineSeparator}
                source.append("     private static final String SERVICE_FACTORY_CLASS_NAME = \"" + element +  "\";" + lineSeparator + "}");

                try {
                    JavaFileObject javaFileObject = filer.createSourceFile(protocolClassElement + "_sf_");
                    Writer writer = javaFileObject.openWriter();
                    writer.write(source.toString());
                    writer.close();
                    System.out.println(source);
                } catch (Exception exception) {
                    //ignore
                    exception.printStackTrace();
                }
            }
        }
        return true;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        //支持的注解
        HashSet<String> annottions = new HashSet<>();
        annottions.add("com.nymph.module.ServiceFactory");
//        annottions.add("java.lang.Override");
        return annottions;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        //支持的java版本
        return SourceVersion.latestSupported();
    }

    private String toRegularName(String className) {
        return className.replace('/', '.');
    }
}
