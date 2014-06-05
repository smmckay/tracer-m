package us.abbies.b.tracerm;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.List;

public class TraceTransformer implements ClassFileTransformer {
    private final String clazz;
    private final String method;
    private final boolean dumpStack;
    private final boolean dumpThis;
    private final List<String> dumpMembers;

    public TraceTransformer(String clazz, String method, boolean dumpStack, boolean dumpThis, List<String> dumpMembers) {
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
        public TraceClassVisitor(ClassWriter classWriter) {
            super(Opcodes.ASM5, classWriter);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            if (method.equals(name + desc)) {
                return new TraceAdviceAdapter(cv, access, name, desc, signature, exceptions);
            }
            return super.visitMethod(access, name, desc, signature, exceptions);
        }
    }

    private class TraceAdviceAdapter extends AdviceAdapter {
        public TraceAdviceAdapter(ClassVisitor cv, int access, String name, String desc, String signature, String[] exceptions) {
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
        }
    }
}
