## Keep Generated HttpActionHelperFactory
-keep class * extends io.techery.janet.AsyncActionWrapper
-keep class * extends io.techery.janet.AsyncActionsRosterBase
-keep class * implements io.techery.janet.AsyncActionService$AsyncActionWrapperFactory
#
## Annotation processor (compiler) classes should be ignored
-dontwarn javax.servlet.**
-dontwarn com.google.auto.common.**
-dontwarn com.google.auto.service.processor.**
-dontwarn com.squareup.javapoet.**
-dontwarn org.apache.commons.collections.BeanMap
-dontwarn org.apache.tools.**
-dontwarn org.apache.velocity.**
-dontwarn io.techery.janet.compiler.**
-dontwarn io.techery.janet.validation.**
-dontwarn io.techery.janet.JanetAsyncProcessor
