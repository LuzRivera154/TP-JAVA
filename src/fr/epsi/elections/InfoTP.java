package fr.epsi.elections;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Annotation personnalisée : elle ne fait rien seule, Main la lit avec getAnnotation
@Target(ElementType.TYPE)            // utilisable sur les classes
@Retention(RetentionPolicy.RUNTIME)  // conservée à l'exécution
public @interface InfoTP {
    String auteur();                 // obligatoire

    String objectif() default "";    // optionnel
}
