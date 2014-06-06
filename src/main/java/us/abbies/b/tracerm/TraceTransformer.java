package us.abbies.b.tracerm;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class TraceTransformer implements ClassFileTransformer {
    private static final Logger logger = Logger.getLogger(TraceTransformer.class.getName());

    private final String clazz;
    private final String method;
    private final boolean dumpStack;
    private final boolean dumpThis;
    private final Set<String> dumpMembers;

    public TraceTransformer(String clazz, String method, boolean dumpStack, boolean dumpThis, Set<String> dumpMembers) {
        this.clazz = clazz.replace('.', '/');
        this.method = method;
        this.dumpStack = dumpStack;
        this.dumpThis = dumpThis;
        this.dumpMembers = dumpMembers;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (!className.equals(clazz)) {
            //required behavior
            //noinspection ReturnOfNull
            return null;
        }

        ClassReader classReader = new ClassReader(classfileBuffer);
        ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        classReader.accept(new TraceClassVisitor(classWriter), 0);
        return classWriter.toByteArray();
    }

    private class TraceClassVisitor extends ClassVisitor {
        private final Map<String, String> memberDescriptors = new HashMap<>();

        public TraceClassVisitor(ClassWriter classWriter) {
            super(Opcodes.ASM5, classWriter);
        }

        @Override
        public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
            if (dumpMembers.contains(name)) {
                memberDescriptors.put(name, desc);
            }
            return super.visitField(access, name, desc, signature, value);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            if (method.equals(name + desc)) {
                return new TraceAdviceAdapter(access, name, desc, signature, exceptions);
            }
            return super.visitMethod(access, name, desc, signature, exceptions);
        }

        private class TraceAdviceAdapter extends AdviceAdapter {
            public TraceAdviceAdapter(int access, String name, String desc, String signature, String[] exceptions) {
                super(Opcodes.ASM5, cv.visitMethod(access, name, desc, signature, exceptions), access, name, desc);
            }

            @Override
            protected void onMethodEnter() {
                if (dumpStack) {
                    visitFieldInsn(GETSTATIC, "us/abbies/b/tracerm/AgentMain", "out", "Lus/abbies/b/tracerm/DumpWriter;");
                    visitTypeInsn(NEW, "us/abbies/b/tracerm/TraceThrowable");
                    visitInsn(DUP);
                    visitMethodInsn(INVOKESPECIAL, "us/abbies/b/tracerm/TraceThrowable", "<init>", "()V", false);
                    visitMethodInsn(INVOKEVIRTUAL, "us/abbies/b/tracerm/DumpWriter", "dumpObject", "(Ljava/lang/Object;)V", false);
                }
                if (dumpThis) {
                    visitFieldInsn(GETSTATIC, "us/abbies/b/tracerm/AgentMain", "out", "Lus/abbies/b/tracerm/DumpWriter;");
                    visitVarInsn(ALOAD, 0);
                    visitMethodInsn(INVOKEVIRTUAL, "us/abbies/b/tracerm/DumpWriter", "dumpObject", "(Ljava/lang/Object;)V", false);
                }
                for (Map.Entry<String, String> e : memberDescriptors.entrySet()) {
                    String desc = e.getValue();
                    if (desc.length() > 1) { // array or object type
                        visitFieldInsn(GETSTATIC, "us/abbies/b/tracerm/AgentMain", "out", "Lus/abbies/b/tracerm/DumpWriter;");
                        visitVarInsn(ALOAD, 0);
                        visitFieldInsn(GETFIELD, clazz, e.getKey(), desc);
                        visitMethodInsn(INVOKEVIRTUAL, "us/abbies/b/tracerm/DumpWriter", "dumpObject", "(Ljava/lang/Object;)V", false);
                    } else {
                        String boxClass;
                        switch (desc) {
                        case "Z":
                            boxClass = "java/lang/Boolean";
                            break;
                        case "C":
                            boxClass = "java/lang/Char";
                            break;
                        case "B":
                            boxClass = "java/lang/Byte";
                            break;
                        case "S":
                            boxClass = "java/lang/Short";
                            break;
                        case "I":
                            boxClass = "java/lang/Int";
                            break;
                        case "F":
                            boxClass = "java/lang/Float";
                            break;
                        case "J":
                            boxClass = "java/lang/Long";
                            break;
                        case "D":
                            boxClass = "java/lang/Double";
                            break;
                        default:
                            logger.severe("Unrecognized value type " + desc);
                            continue;
                        }
                        visitFieldInsn(GETSTATIC, "us/abbies/b/tracerm/AgentMain", "out", "Lus/abbies/b/tracerm/DumpWriter;");
                        visitTypeInsn(NEW, boxClass);
                        visitInsn(DUP);
                        visitVarInsn(ALOAD, 0);
                        visitFieldInsn(GETFIELD, clazz, e.getKey(), desc);
                        visitMethodInsn(INVOKESPECIAL, boxClass, "<init>", "(" + desc + ")V", false);
                        visitMethodInsn(INVOKEVIRTUAL, "us/abbies/b/tracerm/DumpWriter", "dumpObject", "(Ljava/lang/Object;)V", false);
                    }
                }
            }
        }
    }
}
