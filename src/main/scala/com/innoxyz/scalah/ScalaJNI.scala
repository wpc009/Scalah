package com.innoxyz.scalah

import java.io.OutputStream
import java.util.ArrayList
import scala.collection.JavaConversions.asScalaBuffer
import com.sun.tools.javah.Gen
import com.sun.tools.javah.Mangle
import com.sun.tools.javah.TypeSignature
import com.sun.tools.javah.Util
import javax.lang.model.`type`.ArrayType
import javax.lang.model.`type`.TypeKind.ARRAY
import javax.lang.model.`type`.TypeKind.BOOLEAN
import javax.lang.model.`type`.TypeKind.BYTE
import javax.lang.model.`type`.TypeKind.CHAR
import javax.lang.model.`type`.TypeKind.DECLARED
import javax.lang.model.`type`.TypeKind.DOUBLE
import javax.lang.model.`type`.TypeKind.FLOAT
import javax.lang.model.`type`.TypeKind.INT
import javax.lang.model.`type`.TypeKind.LONG
import javax.lang.model.`type`.TypeKind.SHORT
import javax.lang.model.`type`.TypeKind.VOID
import javax.lang.model.`type`.TypeMirror
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.util.ElementFilter
import javax.tools.FileObject
import com.sun.tools.javac.code.Symbol.ClassSymbol

class ScalaJNI(u: Util) extends Gen(u) {
    def getIncludes: String = {
        "#include <jni.h>"
    }
    def write(outstream: OutputStream, clazz: TypeElement): Unit = {
        try {
            
            val cname = clazz.getQualifiedName().toString().replace('.', '_') 
            val pw = wrapWriter(outstream)
            pw.println(guardBegin(cname))
            pw.println(cppGuardBegin());
            val classfields = getFields(clazz)
            /*
         * Write statics
         */
            for (v <- classfields; if v.getModifiers().contains(Modifier.STATIC)) {
                val s = defineForStatic(clazz, v);
                if (s != null) {
                    pw.println(s)
                }
            }
            /*
         * Write methods
         */
            val classmethods = ElementFilter.methodsIn(clazz.getEnclosedElements());
            val nativeMethods = new ArrayList[(String, String)]
            for (m <- classmethods; if m.getModifiers().contains(Modifier.NATIVE)) {
                val mtr = types.erasure(m.getReturnType());
                val sig = sign(m)
                val name = m.getSimpleName().toString()
                val newtypesig = new TypeSignature(elems)
                nativeMethods.add((name, newtypesig.getTypeSignature(sig, mtr)))
                pw.println("/*");
                pw.println(" * Class:     " + cname);
                pw.println(" * Method:    " +
                    mangler.mangle(name, Mangle.Type.FIELDSTUB));
                pw.println(" * Signature: " + newtypesig.getTypeSignature(sig, mtr));
                pw.println(" */");
                pw.println("JNIEXPORT " + jniType(mtr) +
                    " JNICALL " + name)
//                    mangler.mangleMethod(m, clazz, Mangle.Type.METHOD_JNI_SHORT));
                pw.print("  (JNIEnv *, ");
                if (m.getModifiers().contains(Modifier.STATIC))
                    pw.print("jclass")
                else
                    pw.print("jobject")

                for (p <- m.getParameters()) {
                    pw.print(", ")
                    pw.print(jniType(types.erasure(p.asType())))
                }
                pw.println(");" + lineSep)
            }
            /*
             * Write static native register method
             */
            pw.println("static JNINativeMethod nativeMethods[]={")
            pw.println("/* name, signature, funcPtr */")
            var sep = ""
            for (m <- nativeMethods) {
                pw.println(sep)
                pw.println("{\"" + m._1 + "\",\"" + m._2 + "\",(void*)" + m._1 + "}");
                sep = ","
            }
            pw.println("};")
            pw.println("""
static int registerNativeMethods(JNIEnv* env,const char* className,JNINativeMethod* gMethods,int numMethods){
  jclass clazz;
  clazz = (*env).FindClass(className);
  if (clazz == NULL)
    return JNI_FALSE;
  if ((*env).RegisterNatives(clazz, gMethods, numMethods) < 0)
    return JNI_FALSE;
  return JNI_TRUE;
}""")
            pw.print("static int registerNatives(JNIEnv* env){\nif(!registerNativeMethods(env,\"");
            pw.print(cname.replace("_", "/"))
            pw.println("\",nativeMethods,sizeof(nativeMethods)/sizeof(JNINativeMethod)))return JNI_FALSE;\nreturn JNI_TRUE;\n}")
            
            pw.println("""
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm,void* reserved){
     JNIEnv* env = NULL;
	jint result = -1;

	if((*vm).GetEnv((void**) &env,JNI_VERSION_1_2) != JNI_OK){
		return JNI_ERR;
	}
	if(!registerNatives(env)){
		return JNI_ERR;
	}

	result = JNI_VERSION_1_4;
	return result;
}
                    """)

            pw.println(cppGuardEnd())
            pw.println(guardEnd(cname))
        } catch {
            case e: Exception => util.error("jni.sigerror", e.getMessage())
        }
    }

    def getFields(subclazz: TypeElement): ArrayList[VariableElement] = {
        var cd = subclazz
        val fields: ArrayList[VariableElement] = new ArrayList[VariableElement]()
        while (cd != null) {
            fields.addAll(ElementFilter.fieldsIn(cd.getEnclosedElements()))
            cd = types.asElement(cd.getSuperclass()).asInstanceOf[TypeElement]
        }
        fields
    }

    def sign(e: ExecutableElement): String = {
        val sb = new StringBuffer("(");
        var sep = "";
        for (p <- e.getParameters()) {
            sb.append(sep)
            sb.append(types.erasure(p.asType()).toString())
            sep = ","
        }
        sb.append(")")
        sb.toString()
    }

    @throws(classOf[Util.Exit])
    def jniType(t: TypeMirror): String = {
        val tclassDoc = types.asElement(t)
        val jString = elems.getTypeElement("java.lang.String")
        val jClass = elems.getTypeElement("java.lang.Class")
        val throwable = elems.getTypeElement("java.lang.Throwable")
        t.getKind() match {
            case ARRAY =>
                val ct = t.asInstanceOf[ArrayType].getComponentType()
                ct.getKind() match {
                    case BOOLEAN => "jbooleanArray"
                    case BYTE => "jbyteArray"
                    case CHAR => "jcharArray";
                    case SHORT => "jshortArray";
                    case INT => "jintArray";
                    case LONG => "jlongArray";
                    case FLOAT => "jfloatArray";
                    case DOUBLE => "jdoubleArray";
                    case ARRAY => "jobjectArray"
                    case DECLARED => "jobjectArray";
                    case _ => throw new Error(ct.toString())
                }
            case VOID => "void";
            case BOOLEAN => "jboolean";
            case BYTE => "jbyte";
            case CHAR => "jchar";
            case SHORT => "jshort";
            case INT => "jint";
            case LONG => "jlong";
            case FLOAT => "jfloat";
            case DOUBLE => "jdouble";
            case DECLARED => if (tclassDoc.equals(jString)) { "jstring" } else if (types.isAssignable(t, throwable.asType())) { "jthrowable" } else if (types.isAssignable(t, jClass.asType())) { "jclass" } else { "jobject" }
            case _ => util.bug("jni.unknown.type"); null
        }
    }
}