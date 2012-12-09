Scalah
======

The Substitution of javah for scala singleton object.Since the singleton object name ends with '$' which is not a valid class name according to javah of JDK7.
So I ported it to fit scala singleton object. It use a different way to register the native methods